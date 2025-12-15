// 1. 发布博客
document.getElementById('publishForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const form = e.target;
    const data = new FormData(form);

    // 1.1 将文本保存为安全的html超文本格式（将来还可以添加自定义格式化内容来replace部分，其余markdown语法之类的使用插件替换即可）
    const htmlContent = escapeHtml(document.getElementById('content').value)
            .replace(/\n/g, '<br>');

    // 1.2 以新字段数据发送后端
    const body = new URLSearchParams({
        title:   form.title.value,
        content: htmlContent,
        authorId:form.authorId.value
    });


    try {
        const res = await fetch('/blogs/publish', {
            method:'POST',
            headers:{'Content-Type':'application/x-www-form-urlencoded'},
            body:body
        });
        if (!res.ok) throw await res.text();
        const json = await res.json();
        const resultDiv  = document.getElementById('result');
        const resultMsg  = document.getElementById('resultMsg');

        if (json.code === 200) {
            resultDiv.className = 'alert alert-success mt-3';
            resultMsg.textContent = json.msg + ' 即将跳转…';
            setTimeout(() => location.href = '/', 1500);
        } else {
            resultDiv.className = 'alert alert-danger mt-3';
            resultMsg.textContent = json.msg;
        }
        resultDiv.classList.remove('d-none');
    } catch (err) {
        console.error(err);
        alert('发布失败：'+err);
    }
});

// 前端防止书写博客注入符 + 后端MyBatis#{}占位符防止mySQL数据库注入双重防护
function escapeHtml(str){
    return str.replace(/[&<>"']/g, c => ({
        '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'
    })[c]);
}