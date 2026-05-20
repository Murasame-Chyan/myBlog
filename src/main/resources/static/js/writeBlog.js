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
            return ["bold", "italic", "heading", "|", "list-ul", "list-ol", "|",
                    "link", "image", "code", "code-block", "|", "watch", "preview", "fullscreen"];
        },
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
        const body = new URLSearchParams({
            title:   form.title.value,
            content: rawContent,
            id:      form.id?.value || '',
            tagIds:  tagIds.join(',')
        });
        // 不再需要 newTagNames，标签已在前端创建完毕
        body.append('newTagNames', '');

        const res = await fetch(formAction, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: body
        });
        if (res.status === 401) {
            openAuthModal('login');
            showToast('请先登录后再发布', 'warning');
            return;
        }
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

// ---- Emoji 表情选择器 ----
(function() {
    var EMOJIS = [
        '😀','😃','😄','😁','😅','😂','🤣','😊','😇','🙂','😉','😍','😘','🤗','🤩','😎','😏','😒','😞','😔','😣','😖','😫','😩','😢','😭','😤','😡','🤬','😱','😨','😰','😳','🤔','🤨','😴','🤤','😋','😛','😝','🤪','😷','🤒','🤕','🥴','🥺','🧐',
        '👍','👎','👌','✌','🤞','🤘','👋','✋','👏','🙌','🤝','💪','🙏','✍','👀','🧠','👑','💍',
        '❤','🧡','💛','💚','💙','💜','🖤','🤍','💔','❣','💕','💞','💓','💗','💖','💘','💝',
        '💯','🔥','⭐','🌟','✨','💫','🎉','🎊','🎈','🎂','🎁','🏆','🥇','✅','❌','💡','💻','📱','⌨','🎵','🎶','📚','📝','✏','✂','🔒','🔑','🔨','🔧','💊','🛁',
        '☀','☁','🌧','⛈','❄','🌈','🌙','⚡','💧','🌊','🌍','🌸','🌹','🌻','🍀','🎄','⭐',
        '🍎','🍕','🍔','🍟','🍩','🍰','🍺','☕','🍿','🍦','🎂','🍬','🍭','🍪','🍷','🍹'
    ];
    var targetId = null;
    var popup = null;

    function build() {
        if (popup) return;
        popup = document.createElement('div');
        popup.className = 'emoji-popup';
        popup.style.display = 'none';
        var html = '<div class="emoji-grid">';
        EMOJIS.forEach(function(e) {
            html += '<span class="emoji-item" data-emoji="' + e + '">' + e + '</span>';
        });
        html += '</div>';
        popup.innerHTML = html;
        document.body.appendChild(popup);
        popup.querySelectorAll('.emoji-item').forEach(function(item) {
            item.addEventListener('click', function() {
                insertEmoji(this.getAttribute('data-emoji'));
            });
        });
        document.addEventListener('click', function(e) {
            if (!popup || popup.style.display !== 'block') return;
            var btn = document.querySelector('.btn-emoji-inline');
            if (!popup.contains(e.target) && (!btn || !btn.contains(e.target))) {
                popup.style.display = 'none';
            }
        });
    }

    function insertEmoji(emoji) {
        if (!targetId) return;
        if (targetId === 'content' && typeof editor !== 'undefined' && editor && editor.cm) {
            editor.cm.replaceSelection(emoji);
            editor.cm.focus();
            if (popup) popup.style.display = 'none';
            return;
        }
        var el = document.getElementById(targetId);
        if (!el) return;
        var s = el.selectionStart, end = el.selectionEnd;
        el.value = el.value.substring(0, s) + emoji + el.value.substring(end);
        el.selectionStart = el.selectionEnd = s + emoji.length;
        el.focus();
        if (popup) popup.style.display = 'none';
    }

    document.addEventListener('DOMContentLoaded', function() {
        build();
        document.querySelectorAll('.btn-emoji-inline').forEach(function(btn) {
            btn.addEventListener('click', function(e) {
                e.stopPropagation();
                targetId = this.getAttribute('data-target');
                if (popup.style.display === 'block') {
                    popup.style.display = 'none';
                    return;
                }
                var rect = this.getBoundingClientRect();
                popup.style.top = (rect.bottom + window.scrollY + 4) + 'px';
                popup.style.left = Math.max(0, rect.left + window.scrollX - 100) + 'px';
                popup.style.display = 'block';
                popup.style.zIndex = 9999;
            });
        });
    });
})();

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
