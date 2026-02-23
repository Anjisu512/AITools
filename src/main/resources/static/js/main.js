document.addEventListener('DOMContentLoaded', () => {

    const currentPath = window.location.pathname;
    const menuLinks = document.querySelectorAll('.menu-list a');

    menuLinks.forEach(link => {
        const linkPath = link.getAttribute('href');

        if (!linkPath || linkPath === '#') {
            return
        };

        // "/" 경로는 특별 처리 모든 path는 / 로 시작하기때문
        if (linkPath === '/' && currentPath === '/') {
            link.classList.add('active');
        }
        // 그 외 경로
        else if (linkPath !== '/' && currentPath.startsWith(linkPath)) {
            link.classList.add('active');
        }
        else {
            link.classList.remove('active');
        }
		
    });

});


function systemShutdown() {
    showConfirm(
        "알림","정말 종료하시겠습니까? \n 확인을 누르면 서버가 안전하게 종료됩니다.",
        function() {
            // 확인 클릭 시: Electron 메인 프로세스로 신호 전달
            window.electronAPI.quitApp();
        },
		// 취소 클릭 시
        function() {}
    );
}