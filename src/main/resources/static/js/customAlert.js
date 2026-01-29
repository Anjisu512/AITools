document.addEventListener('DOMContentLoaded', function() {
    const alertOverlay = document.getElementById('custom-alert-overlay');
    const alertTitle = document.getElementById('custom-alert-title');
    const alertMessage = document.getElementById('custom-alert-message');
    const alertOkButton = document.getElementById('custom-alert-ok-button');

    // 커스텀 알림창을 보여주는 함수
    window.showAlert = function(title, message) {
        alertTitle.textContent = title;
        alertMessage.textContent = message;
        alertOverlay.style.display = 'flex'; // Flexbox를 사용하여 중앙 정렬
    };

    // '확인' 버튼 클릭 시 알림창 닫기
    alertOkButton.addEventListener('click', function() {
        alertOverlay.style.display = 'none';
    });

    // 오버레이 클릭 시 알림창 닫기 (원하는 경우 활성화)
    // alertOverlay.addEventListener('click', function(event) {
    //     if (event.target === alertOverlay) {
    //         alertOverlay.style.display = 'none';
    //     }
    // });
});