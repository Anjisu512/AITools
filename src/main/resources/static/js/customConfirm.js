document.addEventListener('DOMContentLoaded', function() {
    const confirmOverlay = document.getElementById('custom-confirm-overlay');
    const confirmTitle = document.getElementById('custom-confirm-title');
    const confirmMessage = document.getElementById('custom-confirm-message');
    const confirmOkButton = document.getElementById('custom-confirm-ok-button');
    const confirmCancelButton = document.getElementById('custom-confirm-cancel-button');
	
    let onConfirmCallback = null;
    let onCancelCallback = null;
	
    // 커스텀 컨펌창을 보여주는 함수
    window.showConfirm = function(title, message, onConfirm, onCancel) {
        confirmTitle.textContent = title;
        confirmMessage.textContent = message;
        confirmOverlay.style.display = 'flex';
		
        // 콜백 함수 저장
        onConfirmCallback = typeof onConfirm === 'function' ? onConfirm : null;
        onCancelCallback = typeof onCancel === 'function' ? onCancel : null;
    };

    // '확인' 버튼 클릭 시
    confirmOkButton.addEventListener('click', function() {
        confirmOverlay.style.display = 'none';
        if (onConfirmCallback) {
            onConfirmCallback();
            resetCallbacks();
        }
    });

    // '취소' 버튼 클릭 시
    confirmCancelButton.addEventListener('click', function() {
        confirmOverlay.style.display = 'none';
        if (onCancelCallback) {
            onCancelCallback();
            resetCallbacks();
        }
    });

    function resetCallbacks() {
        onConfirmCallback = null;
        onCancelCallback = null;
    }
});