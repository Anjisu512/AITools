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
    // 클릭된 버튼(btn)의 부모(.number-control) 안에서 input 요소 조회
    const container = btn.closest('.number-control');
    const input = container.querySelector('input[type="number"]');
    
    let value = parseInt(input.value) || 0;
    const min = parseInt(input.min);
    const max = parseInt(input.max);

    const newValue = value + step;

    // 범위 체크 후 값 적용
    if (newValue >= min && newValue <= max) {
        input.value = newValue;
    }
}