// 1. 发布博客
document.getElementById('publishForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const form = e.target;

    // 0.此处相对路径拼接有bug，前端硬编码路由绕开ThymeLeaf解析，对应writeBlog.html的表单th:action项
    const isEditMode = !!form.querySelector('input[name="id"]');
    const formAction = isEditMode ? '/blogs/update' : '/blogs/publish';

    // 1.1 将文本保存为安全的html超文本格式（将来还可以添加自定义格式化内容来replace部分，其余markdown语法之类的使用插件替换即可）
    const blogId = document.getElementById('getBlogId').value || '0';
    const rawTitle = document.getElementById('title').value;        // 备份
    const rawContent = document.getElementById('content').value;
    // const htmlContent = escapeHtml(rawContent)
    //         .replace(/\n/g, '<br>');

    // 1.2 以新字段数据发送后端
    const body = new URLSearchParams({
        title:   form.title.value,
        content: rawContent,
        authorId:form.authorId.value,
        id:      form.id?.value || ''   // 编辑模式必须带 id
    });

    // 1.3.1 收集表单数据
    try {
        const res = await fetch(formAction, {
            method:'POST',
            headers:{'Content-Type':'application/x-www-form-urlencoded'},
            body:body
        });
        if (!res.ok) throw await res.text();
        const json = await res.json();
        const resultDiv  = document.getElementById('result');
        const resultMsg  = document.getElementById('resultMsg');

        // 1.3.2 返回状态
        if (json.code === 200) {
            resultDiv.className = 'alert alert-success mt-3';
            resultMsg.textContent = json.msg + ' 即将跳转…';
            setTimeout(() => location.href = isEditMode ? '/blogs/read/' + blogId : '/', 1500);
        } else {
            resultDiv.className = 'alert alert-danger mt-3';
            resultMsg.textContent = json.msg;
        }
        resultDiv.classList.remove('d-none');
    } catch (err) {
        console.error(err);
        alert('发布失败：'+err);
        // 还原用户原文，方便二次编辑
        document.getElementById('title').value = rawTitle || '';
        document.getElementById('content').value = rawContent || '';
    }
});

// // 2. 前端防止书写博客注入符 + 后端MyBatis#{}占位符防止mySQL数据库注入双重防护
// function escapeHtml(str){
//     return str.replace(/[&<>"' \t\n\r\f]/g, c => ({
//         '&': '&amp;',
//         '<': '&lt;',
//         '>': '&gt;',
//         '"': '&quot;',
//         "'": '&#39;',
//         ' ': '&nbsp;',
//         '\t': '&nbsp;&nbsp;&nbsp;&nbsp;', // 制表符用4个nbsp
//         '\n': '<br>',  // 换行符用br标签
//         '\r': '',      // 回车符移除
//         '\f': ' '      // 换页符替换为普通空格
//     })[c] || c); // 如果映射表中没有，返回原字符
// }