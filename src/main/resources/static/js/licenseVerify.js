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
    }).then(async res => {
		const result = await res.json();
        if (!res.ok) {
			throw new Error( result.message || '인증 실패');
		};
        return result;
    }).then(data => {
        if (data.valid) {
            showAlert('인증 성공!', '인증 성공!');
            window.location.href = data.redirect;
        } else {
            showAlert('인증 실패!', data.message || '유효하지 않은 키입니다.');
        }
    }).catch(err => {
        showAlert('인증 실패!', err);
        console.error('통신 에러:', err);
    });
}
