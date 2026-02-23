// preload.js
const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
    // 프런트엔드에서 호출할 함수명 : () => 메인으로 신호 보내기	
    quitApp: () => ipcRenderer.send('request-quit') // 프로그램 종료 명령
});