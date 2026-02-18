// ===============================
// 명령프롬포트 사용 여부 체크
// ===============================

const isUsedRadio = document.querySelectorAll('input[name="useAiTool"]');
const isUsedEl = document.querySelector('.isUsed');

// 항상 최신값을 담고 있을 변수
let selectedAiTool = null;

// 값 갱신 + 화면 반영 함수
function updateSelectedAiTool() {
    const checked = document.querySelector('input[name="useAiTool"]:checked');
    selectedAiTool = checked ? checked.value : null;
}


// 최초 로드시 한 번
updateSelectedAiTool();

// 변경될 때마다 갱신
isUsedRadio.forEach(radio => {
    radio.addEventListener('change', updateSelectedAiTool);
});

// Input범위는 1~10
function valueHandler(btn, step) {
    const container = btn.closest('.number-control');
    const input = container.querySelector('input[type="number"]');
    
    let value = parseInt(input.value) || 0;
    const min = parseInt(input.min);
    const max = parseInt(input.max);
    const newValue = value + step;

	// [최소값 체크] 1보다 작아지는 것은 알림 없이 차단 
    if (newValue < min) {
		return;
	}

    // 최대값(10) 이상 클릭하는 경우 알림 및 차단
    const messages = {
        'crawlingBlogQty': '블로그 크롤링 수는 10개를 넘길 수 없습니다.',
        'crawlingNewsQty': '뉴스 크롤링 수는 10개를 넘길 수 없습니다.',
        'tempWriteQty': '블로그 임시 작성 수는 10개를 넘길 수 없습니다.',
        'realWriteQty': '블로그 작성 수는 10개를 넘길 수 없습니다.'
    };

    if (newValue > 10 && messages[input.id]) {
        showAlert("개수 초과", messages[input.id]);
        return;
    }

    // 임시 작성(tempWriteQty)과 실제 작성(realWriteQty)의 합계 체크
    if (input.id === 'tempWriteQty' || input.id === 'realWriteQty') {
        const tempVal = parseInt(document.getElementById('tempWriteQty').value) || 0;
        const realVal = parseInt(document.getElementById('realWriteQty').value) || 0;
        
        // 현재 수정하려는 input이 아닌 '상대방'의 값을 가져와 합산
        const otherVal = (input.id === 'tempWriteQty') ? realVal : tempVal;
        
        if (newValue + otherVal > 10) {
			showAlert("개수 초과", "임시작성과 작성수의 합계는 10개를 넘길 수 없습니다.");
            return; // 합이 10을 넘으면 실행 중단
        }
    }
	
	
    // 모든 조건 통과 시 값 반영
    input.value = newValue;
}


async function blogPostingStart(){
	// storage에 담겨있는 정보와 대시보드내에 있는 정보들 get
	const stored = localStorage.getItem('appSettings');
	const settings = JSON.parse(stored);
	const searchCategory = document.querySelector('input[name="searchCategory"].value');
	const crawlingBlogQty = parseInt(document.getElementById('crawlingBlogQty').value) || 0;
	const crawlingNewsQty = parseInt(document.getElementById('crawlingNewsQty').value) || 0;
	const tempWriteQty = parseInt(document.getElementById('tempWriteQty').value) || 0;
  	const realWriteQty = parseInt(document.getElementById('realWriteQty').value) || 0;
	 
	
	// naver PW가 있다면 값을 복원하여 백엔드로 전달
	let naverPw = "";
    if (settings.naverPW) {		
		settings.naverPW = deobfuscate(settings.naverPW); // 복원된 값을 다시 입력하여 settings자체를 parameter로 전달
    };
	
	// api호출시 log화면에 실시간으로 진행률을 보여주기위함
	const logConsole = document.getElementById("logConsole");
	// 새로운 실행이 시작될 때 기존 로그 제거
    logConsole.innerHTML = "";
	let lastServerLog = ""; // 서버에서 보낸 마지막 텍스트를 저장할 변수 에러인 경우는 마지막 텍스트를 보내줘야함
	
	const body = {        
		settings : settings,
        useAiTool: selectedAiTool,
        searchCategory: document.getElementById('searchCategory')?.value,
        crawlingBlogQty: parseInt(crawlingBlogQty),
        crawlingNewsQty: parseInt(crawlingNewsQty),
        tempWriteQty: parseInt(tempWriteQty),
        realWriteQty: parseInt(realWriteQty)
    };

    try {
        const response = await fetch('/api/ai/posting', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });

        const reader = response.body.getReader();
        const decoder = new TextDecoder();

        // 스트림 읽기 시작
		let remainder = ""; // 처리가 덜 된 문자열 조각을 저장
        while (true) {
            const { value, done } = await reader.read();
            if (done) {
                break;
            } 

			// 이전 청크의 남은 조각 + 현재 청크
		    const chunk = remainder + decoder.decode(value, { stream: true });			
		    const lines = chunk.split('\n');
			
            // 마지막 요소는 줄바꿈이 아직 안 왔을 수 있으므로 다음으로 넘김, data: 로 시작하는걸 읽어오는데 쪼개져서 넘어오는 문제가 확인되어 2번확인하게되었음
            remainder = lines.pop();
			
			lines.forEach(line => {
		        const trimmedLine = line.trim();
		        if (trimmedLine.startsWith('data:')) {
		            const message = trimmedLine.replace('data:', '').trim();
					lastServerLog = message; // 마지막 메시지 업데이트

					// [체크] 이번 출력 메시지가 에러 관련이라면
				    if (lastServerLog.includes("[실패]") || lastServerLog.includes("[에러]")) {
						console.error("서버 에러 감지:", message);
						throw new Error(lastServerLog);
				    }
					
					// 로그 박스에 추가
		            const logEntry = document.createElement("div");
		            logEntry.innerHTML = message;  // "> ... " 형태의 메시지
		            logConsole.appendChild(logEntry);
					
					// 최하단으로 자동 스크롤
		            logConsole.scrollTop = logConsole.scrollHeight;
		        }
		    }); 
        }
		
		
    } catch (error) {
        showAlert("에러!", "로그 스트리밍 중 에러 발생, 로그를 확인해주세요");
		// error.message 대신 서버가 마지막으로 보낸 lastServerLog를 출력
		let finalMsg = lastServerLog;
	    if (!finalMsg || finalMsg === "") {
	        finalMsg = "> [시스템 오류] " + error.message; 
	    }

	    const errorMessage = `
	        <div style="color: #ff6b6b; font-weight: bold; margin-bottom: 5px;">
	            ${finalMsg}
	        </div>
	        <div style="color: #ff6b6b; font-size: 0.9em;">
	            > 로그 스트리밍이 중단되었습니다. 잠시 후 초기화됩니다.
	        </div>
	    `;
	    logConsole.innerHTML += errorMessage;
	    logConsole.scrollTop = logConsole.scrollHeight;
    }
}

// Log영역에 log출력
function appendLog(message) {
    const logBox = document.querySelector(".log-content");
    const div = document.createElement("div");
    div.innerText = message;
    logBox.appendChild(div);
    logBox.scrollTop = logBox.scrollHeight; // 스크롤 하단 고정
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
