// 0. 初始化 Editor.md 编辑器
let editor;
$(function() {
    editor = editormd("editormd", {
        width: "100%",
        height: 640,
        path: "https://cdn.jsdelivr.net/npm/editor.md@1.5.0/lib/",
        placeholder: "请输入博客内容，支持 Markdown 语法",
        syncScrolling: "single",
        saveHTMLToTextarea: true,
        imageUpload: true,
        imageFormats: ["jpg", "jpeg", "gif", "png", "webp"],
        imageUploadURL: "/api/upload/image",
        toolbarIcons: function() {
            return ["undo", "redo", "|",
                    "bold", "italic", "strikethrough", "|",
                    "h1", "h2", "h3", "h4", "h5", "h6", "|",
                    "quote", "list-ul", "list-ol", "hr", "table", "checkbox", "|",
                    "link", "image", "code", "code-block", "|",
                    "emoji", "tex", "|",
                    "search-replace", "html-entities", "help", "|",
                    "watch", "preview", "fullscreen"];
        },
        // 启用 Editor.md 扩展插件
        emoji: true,
        tex: true,
        flowChart: true,
        sequenceDiagram: true,
        searchReplace: true,
        codeFold: true,
        onload: function() {
            console.log("Editor.md 加载完成");
        }
    });
});

// ---- 标签选择器 ----
const MAX_TAGS = 10;
let selectedTags = [];       // { id: null|number, name: string }
let tagSearchTimer = null;

// 初始化：渲染标签池 + 回填编辑模式下的已选标签
function initTags() {
    renderTagPool();
    if (preSelectedTagIds && preSelectedTagIds.length > 0) {
        preSelectedTagIds.forEach(function(tagId) {
            const found = allTags.find(t => t.id === tagId);
            if (found) {
                selectTag(found.id, found.tagName);
            }
        });
    }
}

function renderTagPool() {
    const pool = document.getElementById('tagPool');
    const available = allTags.filter(function(t) {
        return !selectedTags.some(s => s.id === t.id);
    });
    const visible = available.slice(0, 10);
    const hidden = available.length - visible.length;
    let html = '';
    visible.forEach(function(t) {
        html += '<span class="tag-pool-chip" onclick="selectTag(' + t.id + ', \'' + escapeJs(t.tagName) + '\')">' +
                escapeHtml(t.tagName) + '</span>';
    });
    if (hidden > 0) {
        html += '<span class="tag-pool-more">+' + hidden + ' 更多</span>';
    }
    if (available.length === 0) {
        html = '<span class="tag-pool-empty">暂无可用标签，请搜索创建</span>';
    }
    pool.innerHTML = html;
}

function selectTag(tagId, tagName) {
    // 已选中 → 取消选择（toggle 行为）
    const idx = selectedTags.findIndex(s => s.id === tagId && s.name === tagName);
    if (idx !== -1) {
        selectedTags.splice(idx, 1);
        renderTagPool();
        renderSelectedChips();
        return;
    }
    if (selectedTags.length >= MAX_TAGS) {
        showToast('标签最多选择' + MAX_TAGS + '个', 'warning');
        return;
    }
    selectedTags.push({ id: tagId, name: tagName });
    renderTagPool();
    renderSelectedChips();
    document.getElementById('tagSearchInput').value = '';
    hideDropdown();
}

function removeTag(idx) {
    selectedTags.splice(idx, 1);
    renderTagPool();
    renderSelectedChips();
}

function renderSelectedChips() {
    const container = document.getElementById('selectedTags');
    let html = '';
    selectedTags.forEach(function(t, i) {
        const label = t.id ? t.name : t.name + ' (新)';
        html += '<span class="tag-chip tag-chip-selected" onclick="removeTag(' + i + ')" title="点击移除">' +
                escapeHtml(label) +
                '<i class="bi bi-x" onclick="event.stopPropagation(); removeTag(' + i + ')"></i>' +
                '</span>';
    });
    container.innerHTML = html;
}

document.addEventListener('DOMContentLoaded', function() {
    initTags();

    const input = document.getElementById('tagSearchInput');
    // 键入时搜索
    input.addEventListener('input', function() {
        clearTimeout(tagSearchTimer);
        const q = this.value.trim();
        if (!q) { showAllTagsDropdown(); return; }
        tagSearchTimer = setTimeout(function() { searchTags(q); }, 150);
    });
    // 回车添加
    input.addEventListener('keydown', function(e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            handleTagInput();
        }
    });
    // 聚焦时展示所有可选标签
    input.addEventListener('focus', function() {
        const q = this.value.trim();
        if (q) { searchTags(q); }
        else { showAllTagsDropdown(); }
    });

    // + 按钮：点击展开所有可选标签
    document.getElementById('tagAddBtn').addEventListener('click', function(e) {
        e.stopPropagation();
        const dropdown = document.getElementById('tagSearchDropdown');
        if (dropdown.style.display === 'block') {
            hideDropdown();
        } else {
            showAllTagsDropdown();
            document.getElementById('tagSearchInput').focus();
        }
    });

    // 点击外部关闭下拉
    document.addEventListener('click', function(e) {
        const block = document.querySelector('.tag-block');
        if (block && !block.contains(e.target)) {
            hideDropdown();
        }
    });
});

// 展示所有尚未选中的标签（点 + 或聚焦搜索框时触发）
function showAllTagsDropdown() {
    const dropdown = document.getElementById('tagSearchDropdown');
    const results = document.getElementById('tagSearchResults');
    const createHint = document.getElementById('tagCreateHint');
    createHint.style.display = 'none';

    const available = allTags.filter(function(t) {
        return !selectedTags.some(s => s.id === t.id);
    });
    if (available.length === 0) {
        results.innerHTML = '<div class="tag-dropdown-empty">所有标签已选完</div>';
        dropdown.style.display = 'block';
        return;
    }
    let html = '';
    available.slice(0, 8).forEach(function(t) {
        html += '<div class="tag-dropdown-item" onclick="selectTag(' + t.id + ', \'' + escapeJs(t.tagName) + '\')">' +
                escapeHtml(t.tagName) + '</div>';
    });
    if (available.length > 8) {
        html += '<div class="tag-dropdown-empty">还有 ' + (available.length - 8) + ' 个标签，请搜索筛选</div>';
    }
    results.innerHTML = html;
    dropdown.style.display = 'block';
}

function searchTags(q) {
    const dropdown = document.getElementById('tagSearchDropdown');
    const results = document.getElementById('tagSearchResults');
    const createHint = document.getElementById('tagCreateHint');
    const newNameSpan = document.getElementById('tagNewName');

    const keyword = q.toLowerCase();
    // 从 allTags 中搜索（不在 selected 中的）
    const matched = allTags.filter(function(t) {
        return t.tagName.toLowerCase().includes(keyword) &&
               !selectedTags.some(s => s.id === t.id);
    });

    let html = '';
    matched.slice(0, 6).forEach(function(t) {
        html += '<div class="tag-dropdown-item" onclick="selectTag(' + t.id + ', \'' + escapeJs(t.tagName) + '\')">' +
                escapeHtml(t.tagName) + '</div>';
    });
    results.innerHTML = html;

    const exactMatch = allTags.some(t => t.tagName.toLowerCase() === keyword);
    const alreadySelected = selectedTags.some(s => s.name.toLowerCase() === keyword);
    if (!exactMatch && !alreadySelected && q.length > 0) {
        newNameSpan.textContent = q;
        createHint.style.display = 'block';
        createHint.onclick = function() { selectTag(null, q); };
    } else {
        createHint.style.display = 'none';
    }

    dropdown.style.display = (html || createHint.style.display !== 'none') ? 'block' : 'none';
}

function handleTagInput() {
    const input = document.getElementById('tagSearchInput');
    const q = input.value.trim();
    if (!q) return;
    const exact = allTags.find(function(t) {
        return t.tagName.toLowerCase() === q.toLowerCase() &&
               !selectedTags.some(s => s.id === t.id);
    });
    if (exact) {
        selectTag(exact.id, exact.tagName);
    } else if (!selectedTags.some(s => s.name.toLowerCase() === q.toLowerCase())) {
        selectTag(null, q);
    }
    input.value = '';
}

function hideDropdown() {
    document.getElementById('tagSearchDropdown').style.display = 'none';
}

function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

function escapeJs(str) {
    return str.replace(/\\/g, '\\\\').replace(/'/g, "\\'").replace(/"/g, '\\"');
}

// ---- 发布博客 ----
document.getElementById('publishForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const form = e.target;

    if (editor && typeof editor.getMarkdown === 'function') {
        document.getElementById('content').value = editor.getMarkdown();
    }

    const isEditMode = !!form.querySelector('input[name="id"]');
    const formAction = isEditMode ? '/blogs/update' : '/blogs/publish';

    const blogId = document.getElementById('getBlogId').value || '0';
    const rawTitle = document.getElementById('title').value;
    const rawContent = document.getElementById('content').value;

    if (rawTitle.length > 255) {
        showToast('标题不能超过255个字符', 'warning');
        return;
    }

    if (selectedTags.length === 0) {
        showToast('请至少选择一个标签！', 'warning');
        return;
    }

    // 先创建所有新标签（id 为 null 的），再收集全部 tagIds
    try {
        const tagIds = await resolveAllTagIds();
        const coverInput = document.getElementById('coverImageUrl');
        const coverValue = coverInput ? (coverInput.value || '') : '';
        const body = new URLSearchParams({
            title:   form.title.value,
            content: rawContent,
            id:      form.id?.value || '',
            tagIds:  tagIds.join(','),
            coverImage: coverValue
        });
        // 不再需要 newTagNames，标签已在前端创建完毕
        body.append('newTagNames', '');

        const res = await authFetch(formAction, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: body
        });
        if (!res.ok) throw await res.text();
        const json = await res.json();
        if (json.code === 200) {
            showToast(json.msg || '发布成功', 'success');
            setTimeout(function() {
                location.href = isEditMode ? '/blogs/read/' + blogId : '/';
            }, 1500);
        } else {
            showToast(json.msg || '发布失败', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('发布失败：' + err, 'error');
        document.getElementById('title').value = rawTitle || '';
        document.getElementById('content').value = rawContent || '';
    }
});

// 对新标签（id==null）逐个调用 POST /tags 创建，返回完整的 ID 列表
async function resolveAllTagIds() {
    const ids = [];
    for (const t of selectedTags) {
        if (t.id != null) {
            ids.push(t.id);
        } else {
            try {
                const body = new URLSearchParams({ name: t.name });
                const res = await fetch('/tags', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: body });
                const json = await res.json();
                if (json.code === 200 && json.data && json.data.id) {
                    t.id = json.data.id;
                    ids.push(t.id);
                } else {
                    throw new Error(json.msg || '创建标签失败');
                }
            } catch (err) {
                showToast('创建标签 "' + t.name + '" 失败: ' + err, 'error');
                throw err;
            }
        }
    }
    return ids;
}

// ---- 封面图片上传与预览 ----
(function() {
    var coverInput   = document.getElementById('coverImageFile');
    var coverPreview = document.getElementById('coverPreview');
    var coverPreviewImg = document.getElementById('coverPreviewImg');
    var coverPlaceholder = document.getElementById('coverPlaceholder');
    var coverUrlHidden = document.getElementById('coverImageUrl');
    var coverRemoveBtn = document.getElementById('coverRemoveBtn');
    var coverArea    = document.getElementById('coverUploadArea');

    if (!coverArea) return;

    // 编辑模式下回显已有封面
    var existingCover = coverUrlHidden.value;
    if (existingCover && existingCover.trim()) {
        showCoverPreview(existingCover.trim());
    }

    // 点击区域触发文件选择
    coverArea.addEventListener('click', function() {
        coverInput.click();
    });

    // 文件选择后立即上传
    coverInput.addEventListener('change', function() {
        var file = coverInput.files[0];
        if (!file) return;
        // 前端尺寸预检
        if (file.size > 15 * 1024 * 1024) {
            showToast('封面图片大小不能超过 15MB', 'error');
            coverInput.value = '';
            return;
        }
        uploadCover(file);
    });

    // 拖拽上传支持
    coverArea.addEventListener('dragover', function(e) { e.preventDefault(); });
    coverArea.addEventListener('drop', function(e) {
        e.preventDefault();
        var file = e.dataTransfer.files[0];
        if (!file || !file.type.startsWith('image/')) return;
        if (file.size > 15 * 1024 * 1024) {
            showToast('封面图片大小不能超过 15MB', 'error');
            return;
        }
        uploadCover(file);
    });

    // 移除封面
    coverRemoveBtn.addEventListener('click', function(e) {
        e.stopPropagation();
        coverUrlHidden.value = '';
        coverPreview.style.display = 'none';
        coverPlaceholder.style.display = 'flex';
    });

    // 上传封面到 /blogs/uploadCover
    async function uploadCover(file) {
        var formData = new FormData();
        formData.append('coverImageFile', file);
        try {
            var res = await authFetch('/blogs/uploadCover', {
                method: 'POST',
                body: formData
            });
            var json = await res.json();
            if (json.code === 200) {
                showCoverPreview(json.data);
                coverUrlHidden.value = json.data;
                showToast('封面上传成功', 'success');
            } else {
                showToast(json.msg || '封面上传失败', 'error');
            }
        } catch (err) {
            showToast('封面上传失败: ' + err, 'error');
        }
    }

    function showCoverPreview(url) {
        coverPreviewImg.src = url;
        coverPreview.style.display = 'block';
        coverPlaceholder.style.display = 'none';
    }
})();
