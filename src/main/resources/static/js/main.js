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
