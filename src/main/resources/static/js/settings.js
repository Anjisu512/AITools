document.addEventListener('DOMContentLoaded', () => {
    // ===============================
    // localStorage 값으로 초기 상태 복원
    // ===============================
    const stored = localStorage.getItem('appSettings');
    if (stored) {
        try {
            const settings = JSON.parse(stored);

            // AI Tool 라디오 값
            if (settings.aiTool) {
                const radio = document.querySelector(
                    `input[name="aiTool"][value="${settings.aiTool}"]`
                );
                if (radio) radio.checked = true;
            }

            // API Key
            if (settings.aiToolKey) {
                const apiKeyInput = document.querySelector('input[name="aiApiKey"]');
                if (apiKeyInput) {
                    apiKeyInput.value = settings.aiToolKey;
                }
            }

            // 추가 프롬프트
            if (settings.extraPrompt) {
                extraPromptContent = settings.extraPrompt.content || null;
                extraPromptFileName = settings.extraPrompt.fileName || null;

                const fileNameText = document.querySelector('.file-name');
                if (fileNameText && extraPromptFileName) {
                    fileNameText.textContent = extraPromptFileName;
                }
            }
			
            // naver ID
            if (settings.naverID) {
                const naverID = document.querySelector('input[name="naverID"]');
                if (naverID) {
                    naverID.value = settings.naverID;
                }
            }
            // naver PW
            if (settings.naverPW) {
				const naverPW = document.querySelector('input[name="naverPW"]');
				naverPW.value = deobfuscate(settings.naverPW);
            }
        } catch (e) {
			showAlert("에러", "기존 Setting값을 가져오지 못했습니다.");
            console.error('appSettings 파싱 실패', e);
        }
    }
}); 


// ===============================
// 도움말 Modal
// ===============================
const helpModal = document.getElementById('apiKeyHelpModal');
const helpButtons = document.querySelectorAll('.help-icon');

if (helpModal && helpButtons.length > 0) {
    helpButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            helpModal.style.display = 'block';
        });
    });

    const closeBtn = helpModal.querySelector('.modal-close-btn');
    if (closeBtn) {
        closeBtn.addEventListener('click', () => {
            helpModal.style.display = 'none';
        });
    }

    helpModal.addEventListener('click', (e) => {
        if (e.target === helpModal) {
            helpModal.style.display = 'none';
        }
    });
}


// ===============================
// 추가 프롬프트 관련 상태값
// ===============================
let extraPromptContent = null;
let extraPromptFileName = null;

// ===============================
// 설정 저장 버튼
// ===============================
const saveBtn = document.getElementById('saveSettingsBtn'); 
if (saveBtn) {
    saveBtn.addEventListener('click', () => {
        const aiTool = document.querySelector('input[name="aiTool"]:checked')?.value || null;
        const aiToolKey = document.querySelector('input[name="aiApiKey"]')?.value || null;
        const naverID = document.querySelector('input[name="naverID"]')?.value || null;
		const naverPW = document.querySelector('input[name="naverPW"]')?.value || null;
		 
        const settings = {
            aiTool,
            aiToolKey,
            naverID,
			naverPW: obfuscate(naverPW), // 비밀번호 난독화 
            extraPrompt: extraPromptContent ? {
                fileName: extraPromptFileName,
                content: extraPromptContent
            } : null
        };


        localStorage.setItem('appSettings', JSON.stringify(settings));

        showAlert('알림', '설정이 저장되었습니다.', () => {
            window.location.href = '/';
        });
    });
}



// Storage내 pw를 난독화 (보안 아님)
function obfuscate(str) {
    if (!str) {
        return null;
    }

    try {
        return btoa(
            new TextEncoder().encode(str).reduce((data, byte) => data + String.fromCharCode(byte), '')
        );
    } catch (e) {
        console.error('obfuscate 실패', e);
        return null;
    }
}

// ===============================
// 추가 프롬프트 업로드 파일 검증
// ===============================
const fileInput = document.getElementById('extraPromptFile');
const selectBtn = document.querySelector('.file-select-btn');
const fileNameText = document.querySelector('.file-name');

if (fileInput && selectBtn && fileNameText) {

    // 버튼 클릭 → 파일 선택창 열기
    selectBtn.addEventListener('click', () => {
        fileInput.click();
    });

    // 파일 선택 시
    fileInput.addEventListener('change', () => {
        const file = fileInput.files[0];

        if (!file) {
            fileNameText.textContent = '선택된 파일 없음';
            extraPromptContent = null;
            extraPromptFileName = null;
            return;
        }

        const ext = file.name.split('.').pop().toLowerCase();
        if (!['txt', 'xml'].includes(ext)) {
            showAlert('오류', 'txt 또는 xml 파일만 업로드할 수 있습니다.');
            fileInput.value = '';
            fileNameText.textContent = '선택된 파일 없음';
            extraPromptContent = null;
            extraPromptFileName = null;
            return;
        }

        const reader = new FileReader();
        reader.onload = () => {
            extraPromptContent = reader.result;
            extraPromptFileName = file.name;
            fileNameText.textContent = file.name;
        };

        reader.readAsText(file);
    });
} 

// 복원
function deobfuscate(str) {
    if (!str) {
        return null;
    }

    try {
        const binary = atob(str);
        const bytes = Uint8Array.from(binary, c => c.charCodeAt(0));
        return new TextDecoder().decode(bytes);
    } catch (e) {
        console.error('deobfuscate 실패', e);
        return null;
    }
}

