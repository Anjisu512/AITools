const { app, BrowserWindow, session, ipcMain } = require('electron');
const { spawn } = require('child_process');
const path = require('path');
const http = require('http');

let springProcess = null;
let mainWindow = null;
let splashWindow = null; // 로딩 창 변수 추가
 
// Spring Boot 시작 함수
function startSpringBoot() {
	const basePath = app.isPackaged
	        ? path.join(process.resourcesPath, 'app.asar.unpacked')
	        : __dirname;
			 
    // root\electron\javaBuild\ 내에있는 jar파일
    const jarPath = path.join(basePath, 'javaBuild', 'AITool.jar');

    // jre설치위치
    const javaPath = path.join(basePath, 'jre', 'bin', 'java.exe');

	
 	console.log('Starting Spring Boot in new window...');
	
	// 콘솔창 숨기는 버전(유저배포용)
	springProcess = spawn(javaPath, [
	        '-jar',
	        jarPath
	    ], {
	        cwd: basePath,
	        detached: true,
	        windowsHide: true,
	        stdio: 'ignore'
	    });

	    springProcess.unref();

	    springProcess.on('error', (err) => {
	        console.error('Failed to start Spring Boot:', err);
	    });
		
	/* Windows 환경에서 새로운 CMD 창을 띄워 Java 실행 - 관리자용 배포
    springProcess = spawn('cmd.exe', [
        '/c', 
        'start', 
        '"Spring Boot Server"', // CMD 창의 타이틀
        javaPath, 
        '-jar', 
        jarPath
    ], {
        cwd: basePath,      // 작업 디렉토리 설정
        shell: true,        // 쉘 명령 사용
        detached: true,     // 부모 프로세스 독립
        stdio: 'ignore'     // 새 창에서 출력되므로 현재 프로세스에선 무시
    });
	springProcess.unref();

    springProcess.on('error', (err) => {
        console.error('Failed to start Spring Boot:', err);
    });
	*/
}

// icon의 경로
const iconPath = path.join(__dirname, 'img', 'AiTool.ico');

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
		webPreferences: { 
            contextIsolation: true,
            // preload 파일을 연결해야 Java쪽에서 window.electronAPI가 작동함
            preload: path.join(__dirname, 'preload.js') 
        } 
    });
	
	// 아예 상단 메뉴 바를 완전히 삭제하고 싶다면 아래 한 줄 추가
	mainWindow.setMenu(null);

	// 체험판이므로 license는 접근 금지
    //const url = 'http://localhost:8080/loginLicense';
	const url = 'http://localhost:8080/';

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

// --- 추가된 종료 로직 (IPC 통신) ---

ipcMain.on('request-quit', () => {
    console.log('Safe shutdown requested...');

    // 1. Java 서버에 종료 요청 (Graceful Shutdown)
    const options = {
        hostname: 'localhost',
        port: 8080,
        path: '/api/system/shutdown',
        method: 'POST'
    };

    const req = http.request(options, (res) => {
        console.log('Server is shutting down...');
        app.quit(); // 서버 응답 받으면 Electron 종료
    });

    req.on('error', (e) => {
        console.error('Server shutdown request failed, force quitting Electron...');
        app.quit(); // 에러 시에도 앱은 종료
    });

    req.end();
});

// --------------------------------

app.whenReady().then(() => {
    startSpringBoot(); 
    createWindows();
});

app.on('window-all-closed', () => { 
	// 사용자가 창을 그냥 닫았을 때도 Java를 죽이고 싶다면 아래 실행
    if (process.platform === 'win32') {
        const { exec } = require('child_process');
        // Java 로그창 타이틀이 "Spring Boot Server"이므로 해당 창을 닫음
        exec('taskkill /f /fi "windowtitle eq Spring Boot Server*" /t');
    }

    if (process.platform !== 'darwin') {
        app.quit();
    }
	
});
