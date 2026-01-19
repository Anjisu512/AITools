const { app, BrowserWindow } = require('electron');
const { spawn } = require('child_process');
const path = require('path');

let springProcess = null;

// dev일때는 __dirname을 사용
const isDev = !app.isPackaged;

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
        stdio: 'ignore',   // 사용자에게 콘솔 안 보이게
        shell: false
    });

    springProcess.on('close', (code) => {
        console.log(`Spring Boot exited with code ${code}`);
    });
}



// 프로그램창 띄우기
function createWindow() {
    const win = new BrowserWindow({
        width: 1200,
        height: 800,
        webPreferences: {
            contextIsolation: true
        }
    });

    // 서버가 뜰때 waiting
    setTimeout(() => {
        win.loadURL('http://localhost:8080/loginLicense');
    }, 5000);
}

app.whenReady().then(() => {
    startSpringBoot();
    createWindow();
});

app.on('window-all-closed', () => {
    if (springProcess) {
        springProcess.kill();
    }
    app.quit();
});
