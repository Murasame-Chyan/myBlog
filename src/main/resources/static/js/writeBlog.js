document.getElementById('publishForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const form = e.target;
    const data = new FormData(form);

    try {
        const res = await fetch('/blogs/publish', { method: 'POST', body: data });
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
        alert('网络错误：' + err);
    }
});