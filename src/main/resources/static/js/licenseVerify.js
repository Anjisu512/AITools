function verify() {
    const keyInput = document.getElementById('licenseKey');
    if (!keyInput) {
        console.error("HTML에 id='licenseKey'인 엘리먼트가 없습니다.");
        return;
    }

    const key = keyInput.value;
    
    fetch('/api/login/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ licenseKey: key })
    })
    .then(res => {
        if (!res.ok) throw new Error('인증 실패');
        return res.json();
    })
    .then(data => {
        if (data.valid) {
            alert('인증 성공!');
            window.location.href = data.redirect; 
        } else {
            document.getElementById('result').innerText = data.message || '유효하지 않은 키입니다.';
        }
    })
    .catch(err => {
        console.error('통신 에러:', err);
        document.getElementById('result').innerText = '서버와 통신 중 에러가 발생했습니다.';
    });
}