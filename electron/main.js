const { app, BrowserWindow, session } = require('electron'); 
const { spawn } = require('child_process');
const path = require('path');
const http = require('http');

let springProcess = null;
let mainWindow = null;
let splashWindow = null; // 로딩 창 변수 추가


// dev일때는 __dirname을 사용
const isDev = !app.isPackaged;

// Spring Boot 시작 함수
function startSpringBoot() {
	const basePath = app.isPackaged
	        ? path.join(process.resourcesPath, 'app.asar.unpacked')
	        : __dirname;
			 
    // root\electron\javaBuild\ 내에있는 jar파일
    const jarPath = path.join(basePath, 'javaBuild', 'AITool.jar');

    // jre설치위치
    const javaPath = path.join(basePath, 'jre', 'bin', 'java.exe');

    console.log('Using Java:', javaPath);
    console.log('Starting Spring Boot:', jarPath);

    springProcess = spawn(javaPath, ['-jar', jarPath], {
        stdio: 'inherit',   // build전에는 사용자에게 콘솔 안 보이게 : ignore
        shell: false
    });

    springProcess.on('close', (code) => {
        console.log(`Spring Boot exited with code ${code}`);
    });
}

// icon의 경로
const iconPath = path.join(__dirname, 'img', 'icon.ico');

// 프로그램창 생성 및 제어 로직
function createWindows() {
	// 앱 시작 시 세션(쿠키, 스토리지 등)을 싹 비우기
    session.defaultSession.clearStorageData();
		
    // [로딩 창 생성]
    splashWindow = new BrowserWindow({
        width: 500, height: 350,
        frame: false,       // 상단 바 제거
        alwaysOnTop: true,  // 항상 위에
        transparent: true,  // 배경 투명 가능
		icon: iconPath, // 로딩창 아이콘
        webPreferences: { contextIsolation: true }
    });
    splashWindow.loadFile('loading.html');

    // [메인 창 생성 - 처음에는 숨김]
    mainWindow = new BrowserWindow({
        width: 1200, height: 800,
        show: false, // 서버 완료 전까지 숨김
		icon: iconPath, // 앱 실행 시 아이콘
		autoHideMenuBar: true, // Alt 키를 누를 때만 메뉴가 나오게 함 
        webPreferences: { contextIsolation: true }
    });
	
	// 아예 상단 메뉴 바를 완전히 삭제하고 싶다면 아래 한 줄 추가
	mainWindow.setMenu(null);

    const url = 'http://localhost:8080/loginLicense';

    // 서버 응답 체크 함수
    const checkServer = () => {
        http.get(url, (res) => {
            // 서버 접속 성공 시
            splashWindow.close(); // 로딩창 닫기
            mainWindow.show();    // 메인창 보이기
            mainWindow.loadURL(url);
        }).on('error', () => {
            // 실패 시 1초 후 다시 핑(Ping)
            setTimeout(checkServer, 1000);
        });
    };

    checkServer();
}

app.whenReady().then(() => {
    startSpringBoot(); 
    createWindows();
});

app.on('window-all-closed', () => {
    if (springProcess) {
        springProcess.kill(); // 자바 프로세스 종료
    }
    if (process.platform !== 'darwin') {
        app.quit();
    }
});
