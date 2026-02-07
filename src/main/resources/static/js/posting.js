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
