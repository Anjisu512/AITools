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

    // 개별 입력창의 최소/최대 범위 체크
    if (newValue < min || newValue > max) {
		return;
	};

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


function blogPostingStart(){
	// storage에 담겨있는 정보 get
	const stored = localStorage.getItem('appSettings');
	const settings = JSON.parse(stored);
	const searchCategory = document.querySelector('input[name="searchCategory"].value');
	const crollingQty = parseInt(document.getElementById('crollingQty').value) || 0;
	const tempWriteQty = parseInt(document.getElementById('tempWriteQty').value) || 0;
  	const realWriteQty = parseInt(document.getElementById('realWriteQty').value) || 0;
	 
	
	// naver PW가 있다면 값을 복원하여 백엔드로 전달
	let naverPw = "";
    if (settings.naverPW) {		
		settings.naverPW = deobfuscate(settings.naverPW); // 복원된 값을 다시 입력하여 settings자체를 parameter로 전달
    };
	 
	const body = {        
		settings : settings,
        useAiTool: selectedAiTool,
        searchCategory: document.getElementById('searchCategory')?.value,
        crollingQty: parseInt(document.getElementById('crollingQty')?.value),
        tempWriteQty: parseInt(document.getElementById('tempWriteQty')?.value),
        realWriteQty: parseInt(document.getElementById('realWriteQty')?.value)
    };
	
	// 2. 백엔드 전송 (fetch 호출)
    fetch('/api/ai/posting', { // 실제 백엔드 URL로 변경하세요
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(body) // 객체를 JSON 문자열로 변환
    })
    .then(response => {
        if (response.ok) return response.json();
        throw new Error('Network response was not ok.');
    })
    .then(result => {
        showAlert("Posting성공!","성공!");
    })
    .catch(error => {
		showAlert("에러!", "Posting 요청 중 에러가 발생했습니다.");
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

