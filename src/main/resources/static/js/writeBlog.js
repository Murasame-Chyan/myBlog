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
        // 工具栏图片按钮保持立即上传（Editor.md 1.5.0 不支持 imageUpload 作为 function callback）
        // 粘贴图片通过 base64 延迟到发布时上传
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
        emoji: true,
        tex: true,
        flowChart: true,
        sequenceDiagram: true,
        searchReplace: true,
        codeFold: true,
        onload: function() {
            // 拦截剪贴板二进制图片粘贴（微信截图/Win+Shift+S/QQ/本地文件）
            // 读图片二进制 → base64 → 插入编辑器（延迟到发布时统一上传 COS）
            initClipboardImagePaste();
        }
    });
});

// ---- 剪贴板二进制图片粘贴（微信截图 / Win+Shift+S / QQ / 本地文件） ----
// 这类粘贴的剪贴板里是 image/png Blob，不是文本，CodeMirror 无法处理，需手动拦截
function initClipboardImagePaste() {
    if (!editor || !editor.cm) {
        setTimeout(initClipboardImagePaste, 200);
        return;
    }
    var cm = editor.cm;
    var wrapper = cm.getWrapperElement();
    if (!wrapper) {
        setTimeout(initClipboardImagePaste, 200);
        return;
    }

    wrapper.addEventListener('paste', function(e) {
        var clipboardData = (e.clipboardData || window.clipboardData);
        if (!clipboardData) return;

        // 检查是否有二进制图片数据
        var items = clipboardData.items;
        if (!items) return;

        var imageBlob = null;
        for (var i = 0; i < items.length; i++) {
            if (items[i].type && items[i].type.startsWith('image/')) {
                imageBlob = items[i].getAsFile();
                break;
            }
        }
        if (!imageBlob) return; // 无二进制图片，走正常文本粘贴

        // 拦截：阻止 CodeMirror 收到空文本
        e.preventDefault();
        e.stopPropagation();

        // 限制图片大小（单张 15MB）
        if (imageBlob.size > 15 * 1024 * 1024) {
            showToast('图片大小不能超过 15MB，请压缩后重试', 'error');
            return;
        }

        var reader = new FileReader();
        reader.onload = function(ev) {
            var dataUrl = ev.target.result;
            var imgMd = '![' + (imageBlob.name || 'image') + '](' + dataUrl + ')';
            cm.replaceSelection(imgMd);
        };
        reader.readAsDataURL(imageBlob);
    });
}

// ---- 延迟上传：base64 图片处理（发布时批量上传 COS） ----
// 编辑阶段不上传，仅在发布时批量处理

// 匹配 Markdown 语法的 base64 图片
var BASE64_IMG_RE = /!\[([^\]]*)\]\((data:image\/(?:png|jpe?g|gif|webp);base64,[A-Za-z0-9+/=]+)\)/gi;
var MAX_BASE64_IMAGES = 50; // 单次发布最多上传 50 张

// 扫描全文，返回去重后的 [{fullMatch, altText, dataUrl}]
function collectBase64Images(markdown) {
    var matches = [];
    var match;
    BASE64_IMG_RE.lastIndex = 0;
    while ((match = BASE64_IMG_RE.exec(markdown)) !== null) {
        if (!matches.some(function(m) { return m.dataUrl === match[2]; })) {
            matches.push({ fullMatch: match[0], altText: match[1], dataUrl: match[2] });
        }
        if (matches.length >= MAX_BASE64_IMAGES) break;
    }
    return matches;
}

// 进度弹窗控制
function showPublishOverlay(text) {
    var overlay = document.getElementById('publishOverlay');
    if (!overlay) return;
    var textEl = document.getElementById('publishProgressText');
    if (textEl) textEl.textContent = text;
    overlay.classList.remove('fading');
    overlay.classList.add('active');
}
function updatePublishProgress(current, total) {
    var textEl = document.getElementById('publishProgressText');
    if (textEl) textEl.textContent = '(' + current + '/' + total + ') 正在发布...';
}
function hidePublishOverlay() {
    var overlay = document.getElementById('publishOverlay');
    if (!overlay) return;
    overlay.classList.add('fading');
    setTimeout(function() {
        overlay.classList.remove('active', 'fading');
    }, 200);
}

// 上传单张 base64 到 COS，返回 URL 或 null
async function uploadSingleBase64(dataUrl) {
    try {
        var res = await authFetch('/api/upload/base64', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ dataUrl: dataUrl })
        });
        var json = await res.json();
        if (json.success === 1 && json.url) return json.url;
        return null;
    } catch (err) {
        return null;
    }
}

// 批量上传，串行；每张完成后调用 onProgress(i, total)
async function uploadBase64Batch(matches, onProgress) {
    var urlMap = {};
    var failed = [];
    var total = matches.length;
    for (var i = 0; i < total; i++) {
        if (onProgress) onProgress(i + 1, total);
        var url = await uploadSingleBase64(matches[i].dataUrl);
        if (url) {
            urlMap[matches[i].dataUrl] = url;
        } else {
            failed.push(matches[i].dataUrl);
        }
    }
    return { urlMap: urlMap, failed: failed };
}

// 用 split/join 将全文中的 base64 URL 替换为 COS URL
function replaceBase64InMarkdown(markdown, urlMap) {
    var text = markdown;
    Object.keys(urlMap).forEach(function(b64) {
        text = text.split(b64).join(urlMap[b64]);
    });
    return text;
}

// 封面延迟上传：选文件时 base64 暂存，发布时上传
var _pendingCoverBase64 = null;

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
    var rawContent = document.getElementById('content').value;

    if (rawTitle.length > 255) {
        showToast('标题不能超过255个字符', 'warning');
        return;
    }

    if (selectedTags.length === 0) {
        showToast('请至少选择一个标签！', 'warning');
        return;
    }

    // ---- 延迟上传：扫描正文 + 封面中的 base64 图片，批量上传 COS ----
    var finalContent = rawContent;
    var finalCover = (document.getElementById('coverImageUrl')?.value || '');

    // 收集正文 base64 图片
    var bodyMatches = collectBase64Images(rawContent);

    // 收集待上传封面（新选的，非已有 COS URL）
    var coverNeedUpload = !!_pendingCoverBase64;
    if (coverNeedUpload && !bodyMatches.some(function(m) { return m.dataUrl === _pendingCoverBase64; })) {
        bodyMatches.push({ fullMatch: '', altText: 'cover', dataUrl: _pendingCoverBase64 });
    }

    if (bodyMatches.length > 0) {
        showPublishOverlay('正在准备发布...');

        var batchResult = await uploadBase64Batch(bodyMatches, function(current, total) {
            updatePublishProgress(current, total);
        });

        // 替换正文中的 base64
        if (Object.keys(batchResult.urlMap).length > 0) {
            finalContent = replaceBase64InMarkdown(rawContent, batchResult.urlMap);
            document.getElementById('content').value = finalContent;
        }

        // 封面 base64 上传成功 → 作为最终封面 URL
        if (coverNeedUpload && batchResult.urlMap[_pendingCoverBase64]) {
            finalCover = batchResult.urlMap[_pendingCoverBase64];
            _pendingCoverBase64 = null;
        }

        hidePublishOverlay();

        if (batchResult.failed.length > 0) {
            showToast('发布成功（' + batchResult.failed.length + ' 张图片已保留在正文中）', 'info');
        }
    }

    // ---- 提交发布 ----
    try {
        const tagIds = await resolveAllTagIds();
        const body = new URLSearchParams({
            title:   form.title.value,
            content: finalContent,
            id:      form.id?.value || '',
            tagIds:  tagIds.join(','),
            coverImage: finalCover
        });
        body.append('newTagNames', '');

        const res = await authFetch(formAction, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: body
        });
        if (!res.ok) throw await res.text();
        const json = await res.json();
        if (json.code === 200) {
            clearDraft();
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

// ---- 封面图片上传与预览（延迟上传：选文件时仅预览，发布时批量上传） ----
(function() {
    var coverInput   = document.getElementById('coverImageFile');
    var coverPreview = document.getElementById('coverPreview');
    var coverPreviewImg = document.getElementById('coverPreviewImg');
    var coverPlaceholder = document.getElementById('coverPlaceholder');
    var coverUrlHidden = document.getElementById('coverImageUrl');
    var coverRemoveBtn = document.getElementById('coverRemoveBtn');
    var coverArea    = document.getElementById('coverUploadArea');

    if (!coverArea) return;

    // 编辑模式下回显已有封面（COS URL）
    var existingCover = coverUrlHidden.value;
    if (existingCover && existingCover.trim()) {
        showCoverPreview(existingCover.trim());
    }

    // 点击区域触发文件选择
    coverArea.addEventListener('click', function() {
        coverInput.click();
    });

    // 文件选择后转 base64 暂存（不上传），预览用 blob URL
    coverInput.addEventListener('change', function() {
        var file = coverInput.files[0];
        if (!file) return;
        if (file.size > 15 * 1024 * 1024) {
            showToast('封面图片大小不能超过 15MB，请压缩后重试', 'error');
            coverInput.value = '';
            return;
        }
        handleCoverFile(file);
    });

    // 拖拽上传支持
    coverArea.addEventListener('dragover', function(e) { e.preventDefault(); });
    coverArea.addEventListener('drop', function(e) {
        e.preventDefault();
        var file = e.dataTransfer.files[0];
        if (!file || !file.type.startsWith('image/')) return;
        if (file.size > 15 * 1024 * 1024) {
            showToast('封面图片大小不能超过 15MB，请压缩后重试', 'error');
            return;
        }
        handleCoverFile(file);
    });

    // 移除封面
    coverRemoveBtn.addEventListener('click', function(e) {
        e.stopPropagation();
        _pendingCoverBase64 = null;
        if (coverPreviewImg.src && coverPreviewImg.src.startsWith('blob:')) {
            URL.revokeObjectURL(coverPreviewImg.src);
        }
        coverUrlHidden.value = '';
        coverPreviewImg.src = '';
        coverPreview.style.display = 'none';
        coverPlaceholder.style.display = 'flex';
    });

    // 读取 File 为 blob URL 预览 + base64 暂存
    function handleCoverFile(file) {
        // 先释放旧 blob URL
        if (coverPreviewImg.src && coverPreviewImg.src.startsWith('blob:')) {
            URL.revokeObjectURL(coverPreviewImg.src);
        }
        // blob URL 即时预览（无需网络）
        var blobUrl = URL.createObjectURL(file);
        showCoverPreview(blobUrl);

        // 异步读 base64，发布时上传
        var reader = new FileReader();
        reader.onload = function(e) {
            _pendingCoverBase64 = e.target.result;
            // 清空 hidden input（表示封面尚未上传，发布时需处理）
            coverUrlHidden.value = '';
        };
        reader.readAsDataURL(file);
    }

    function showCoverPreview(url) {
        coverPreviewImg.src = url;
        coverPreview.style.display = 'block';
        coverPlaceholder.style.display = 'none';
    }
})();

// ---- 草稿保存与恢复（localStorage 自动恢复） ----
var DRAFT_KEY = 'blog_draft';

// 保存草稿：标题 + 正文 + 标签 + 封面 base64 → JSON → localStorage
function saveDraft() {
    if (!editor || typeof editor.getMarkdown !== 'function') return;
    var draft = {
        title: document.getElementById('title').value || '',
        content: editor.getMarkdown(),
        tags: selectedTags.map(function(t) { return { id: t.id, name: t.name }; }),
        coverBase64: _pendingCoverBase64 || null,
        coverUrl: (document.getElementById('coverImageUrl')?.value || ''),
        savedAt: Date.now()
    };
    try {
        localStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
        showToast('草稿已保存', 'success');
    } catch (e) {
        showToast('草稿保存失败：存储空间不足', 'error');
    }
}

// 清除草稿（发布成功后调用）
function clearDraft() {
    try {
        localStorage.removeItem(DRAFT_KEY);
    } catch (e) { /* 静默 */ }
}

// 页面加载时自动恢复草稿（仅新建模式）
function checkAndRestoreDraft() {
    // 编辑模式不恢复
    var hiddenId = document.querySelector('input[name="id"]');
    var isEditMode = !!(hiddenId && hiddenId.value && hiddenId.value !== '0');
    if (isEditMode) return;

    var raw;
    try { raw = localStorage.getItem(DRAFT_KEY); } catch (e) { return; }
    if (!raw) return;

    var draft;
    try {
        draft = JSON.parse(raw);
    } catch (e) {
        try { localStorage.removeItem(DRAFT_KEY); } catch (e2) {}
        return;
    }
    if (!draft || !draft.content) return;

    // 恢复标题
    if (draft.title) document.getElementById('title').value = draft.title;

    // 恢复正文（等 Editor.md 初始化完成后设置）
    function restoreContent() {
        if (draft.content && editor && editor.cm) {
            editor.cm.setValue(draft.content);
        } else {
            setTimeout(restoreContent, 100);
        }
    }
    setTimeout(restoreContent, 300);

    // 恢复标签
    if (draft.tags && draft.tags.length > 0) {
        selectedTags = draft.tags;
        // 标签渲染需等 DOM ready + allTags 加载
        var retryTags = 0;
        function restoreTags() {
            if (allTags && allTags.length > 0) {
                renderTagPool();
                renderSelectedChips();
            } else if (retryTags < 20) {
                retryTags++;
                setTimeout(restoreTags, 100);
            }
        }
        setTimeout(restoreTags, 500);
    }

    // 恢复封面
    if (draft.coverBase64) {
        _pendingCoverBase64 = draft.coverBase64;
        var img = document.getElementById('coverPreviewImg');
        var preview = document.getElementById('coverPreview');
        var placeholder = document.getElementById('coverPlaceholder');
        var urlHidden = document.getElementById('coverImageUrl');
        if (img) img.src = draft.coverBase64;
        if (preview) preview.style.display = 'block';
        if (placeholder) placeholder.style.display = 'none';
        if (urlHidden) urlHidden.value = '';
    } else if (draft.coverUrl) {
        var urlHidden2 = document.getElementById('coverImageUrl');
        var img2 = document.getElementById('coverPreviewImg');
        var preview2 = document.getElementById('coverPreview');
        var placeholder2 = document.getElementById('coverPlaceholder');
        if (urlHidden2) urlHidden2.value = draft.coverUrl;
        if (img2) img2.src = draft.coverUrl;
        if (preview2) preview2.style.display = 'block';
        if (placeholder2) placeholder2.style.display = 'none';
    }
}

// 绑定"保存草稿"按钮
function initDraftButton() {
    var btn = document.getElementById('saveDraftBtn');
    if (btn) btn.addEventListener('click', saveDraft);
}

// 页面初始化：草稿支持
document.addEventListener('DOMContentLoaded', function() {
    initDraftButton();
    checkAndRestoreDraft();
});
