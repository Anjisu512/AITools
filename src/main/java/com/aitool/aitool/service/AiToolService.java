package com.aitool.aitool.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aitool.aitool.config.ApiExceptionBuild;
import com.aitool.aitool.dto.AiQuestionDTO;
import com.aitool.aitool.dto.requestPostingDTO;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;


@Service 
@RequiredArgsConstructor
public class AiToolService {
	// 네이버 블로그 작성을 위한 WebDriver
	private WebDriver driver;
	
	// Gemini API의 베이스 URL 설정
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com/v1beta/") 
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    
	@SuppressWarnings({ "unchecked", "rawtypes" }) // gemini 관련 return값에대한 Warning 제거
	public Set<String> crawlingBlog(requestPostingDTO request, SseEmitter emitter, String category) {
		Map<String, Object> response = new HashMap<>();
	    String answer = "";  
		try {
			String apiKey = (String) request.getSettings().get("aiToolKey");
			
			String encodedCategory = URLEncoder.encode(category, "UTF-8");
	        // 네이버 블로그, 뉴스 베이스 URL 분리
	        String blogUrl = "https://m.search.naver.com/search.naver?ssc=tab.m_blog.all&query=" + encodedCategory;
	        String newsUrl = "https://m.search.naver.com/search.naver?ssc=tab.m_news.all&query=" + encodedCategory;

	        Set<String> uniqueBlogLinks = new LinkedHashSet<>();
	        Set<String> uniqueNewsLinks = new LinkedHashSet<>();
			 
	        // 블로그 크롤링
	        if (request.getCrawlingBlogQty() > 0) {
	            Document doc = fetchDocument(blogUrl);
	            if (doc != null) {
	                buildCrawling(doc, uniqueBlogLinks, blogUrl, emitter, request.getCrawlingBlogQty(), "blog");
	            }
	        }

	        // 뉴스 크롤링
	        if (request.getCrawlingNewsQty() > 0) {
	            Document doc = fetchDocument(newsUrl);
	            if (doc != null) {
	                buildCrawling(doc, uniqueNewsLinks, newsUrl, emitter, request.getCrawlingNewsQty(), "news");
	            }
	        }
	        
	        // link주소 병합
	        Set<String> allLinks = new LinkedHashSet<>(); // 뉴스와 블로그 link를 병합
			if(uniqueBlogLinks.size() > 0) {
				allLinks.addAll(uniqueBlogLinks); // 블로그 링크 추가
			}
			if(uniqueNewsLinks.size() > 0) {
				allLinks.addAll(uniqueNewsLinks); // 뉴스 링크 추가
			}			
			return allLinks;
		} catch (Exception e) {
			answer = "크롤링 중 오류 발생: " + e.getMessage();
			int code = Integer.parseInt(e.getMessage().split(" ")[0]);
			switch (code) {
			case 400:
				throw new ApiExceptionBuild(HttpStatus.BAD_REQUEST, answer);
			case 404:
				throw new ApiExceptionBuild(HttpStatus.NOT_FOUND, answer);
			case 429:
				throw new ApiExceptionBuild(HttpStatus.TOO_MANY_REQUESTS, answer);
			default:
				throw new ApiExceptionBuild(HttpStatus.INTERNAL_SERVER_ERROR, answer);
			}
		}
    }
	
	// 단일 포스팅 생성을 위한 메서드로 분리
	public Mono<String> generateSinglePost(requestPostingDTO request, String allDataContext, int totalCount, int currentIndex) {
	    String apiKey = (String) request.getSettings().get("aiToolKey");
	    String extraPrompt = "";
	    Map<String, Object> settings = request.getSettings();
	    
	    // 안전한 extraPrompt 추출 로직
	    if (request.isUseAiTool() && settings != null && settings.get("extraPrompt") != null) {
	        Object rawPrompt = settings.get("extraPrompt");
	        if (rawPrompt instanceof Map) {
	            Map<String, Object> prompt = (Map<String, Object>) rawPrompt;
	            extraPrompt = String.valueOf(prompt.getOrDefault("content", ""));
	        }
	    }

	    // [중요] 1개 분량의 통합 기반 프롬프트 구성
	    String postPrompt = String.format(
	        "너는 수집된 다각도의 정보를 분석하여 독창적인 블로그 포스팅을 작성하는 '콘텐츠 아키텍트'야.\n\n"
	        + "[1. 전체 분석 데이터 소스]:\n%s\n\n"
	        + "[2. 현재 작업 지시]:\n"
	        + "- 너는 위 데이터를 활용해 총 %d개의 서로 다른 포스팅을 만들어야 해.\n"
	        + "- 지금 네가 작성할 포스팅은 그중 [%d번째] 글이야.\n"
	        + "- [필수] 앞선 글들과 내용이나 관점이 중복되지 않도록, 이번 글은 특정 카테고리에 집중하거나 새로운 시각에서 작성해줘.\n\n"
	        + "[3. 출력 형식 및 제약]:\n"
	        + "- 반드시 JSON 배열 [ { 'title': '...', 'content': '...', 'tagKeywords': [...] } ] 형식을 엄수할 것.\n"
	        + "- '서론/본론/결론', '###', '번호 매기기' 절대 금지. 사람이 쓴 것 같은 자연스러운 서사 구조로 작성할 것.\n\n"
	        + "[4. 사용자 추가 요청]: %s", 
	        allDataContext, totalCount, currentIndex, extraPrompt);

	    // 응답 길이를 고려하여 maxOutputTokens 설정 (1개 포스팅이므로 2048~4096이면 충분함)
	    AiQuestionDTO.GenerationConfig config = new AiQuestionDTO.GenerationConfig(2048, 0.8, "application/json");
	    AiQuestionDTO body = new AiQuestionDTO(List.of(new AiQuestionDTO.Content(List.of(new AiQuestionDTO.Part(postPrompt)))), config);

	    String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent";

	    return webClient.post()
	            .uri(url)
	            .header("x-goog-api-key", apiKey)
	            .bodyValue(body)
	            .retrieve()
	            .bodyToMono(Map.class)
	            .map(rawResponse -> {
	                // 응답 추출 및 Null 방어 로직
	                try {
	                    List candidates = (List) rawResponse.get("candidates");
	                    if (candidates != null && !candidates.isEmpty()) {
	                        Map firstCandidate = (Map) candidates.get(0);
	                        Map content = (Map) firstCandidate.get("content");
	                        List parts = (List) content.get("parts");
	                        Map firstPart = (Map) parts.get(0);
	                        return (String) firstPart.get("text");
	                    }
	                } catch (Exception e) {
	                    return "{\"error\": \"응답 파싱 실패\"}";
	                }
	                return "{\"error\": \"결과 없음\"}";
	            })
	            .onErrorResume(e -> Mono.just("{\"error\": \"" + e.getMessage() + "\"}"));
	}
	
	
	public List<String> generateContent(requestPostingDTO request, SseEmitter emitter, Map<String, Set<String>> crawlingLinkData){
		String answer = "";
		try {
			String apiKey = (String) request.getSettings().get("aiToolKey");

			int tempWrite = request.getTempWriteQty(); // 포스팅을 임시 저장 할 갯수 
			int writeQty = request.getRealWriteQty();  // 바로 포스팅할 갯수
			
			// 질문 생성 (예시: 카테고리에 맞는 블로그 글 작성 요청)
			List<String> resultList = new ArrayList<>();
			Set<String> categorys = crawlingLinkData.keySet();
			
			String extraPrompt = "";
			Map<String,Object> settings = request.getSettings();
			// 만약 설정에 사용자가 추가한 추가명령프롬포트가 존재하는경우 추가
			if(request.isUseAiTool()) {
				if (settings.containsKey("extraPrompt") && settings.get("extraPrompt") != null) {
				    Map<String, Object> prompt = (Map<String, Object>) settings.get("extraPrompt");
				    extraPrompt = (String) prompt.getOrDefault("content", "");
				}
			}
			// 모든 카테고리와 링크를 하나로 취합
			StringBuilder allDataContext = new StringBuilder();
			List<String> allCategoryNames = new ArrayList<>();

			for (String category : categorys) {
			    allCategoryNames.add(category);
			    Set<String> links = crawlingLinkData.get(category);
			    allDataContext.append(String.format("\n[카테고리: %s] \n링크 리스트: %s\n", category, links));
			}
			// 2. 통합 프롬프트 구성
			String totalCategories = String.join(", ", allCategoryNames);
			String postPrompt = String.format(
			    "제공된 모든 정보를 기반으로 전문적인 블로그 콘텐츠를 제작하는 '콘텐츠 아키텍트' 역할을 수행해줘.\n"
			    + "\n1. 전체 데이터 소스 분석: "
			    + "\n - 통합 카테고리 범위: [%s] "
			    + "\n - 수집된 전체 정보 내역: \n [%s] \n이 모든 링크의 내용을 유기적으로 분석하여 서로 보완적인 포스팅을 만들어야 해."
			    
			    + "\n2. 생성 규칙: "
			    + "\n - 요청한 총 포스팅 개수: %d개 "
			    + "\n - 각 포스팅은 위 카테고리들의 정보를 골고루 섞거나, 특정 주제를 심화하여 중복되지 않게 구성할 것. "
			    + "\n - 단순 나열이 아닌, 사람이 쓴 것 같은 기승전결이 뚜렷한 서사 구조로 작성할 것."
			    
				+ "\n3. 출력 형식 및 스타일 제약: "
				+ "\n - 반드시 아래 JSON 배열 형식을 지킬 것: [ { 'title': '...', 'content': '...', 'tagKeywords': [...] } ]"
				+ "\n - [문단 구성]: 한 문단은 2~3개 문장을 묶어서 구성하고, 문단 사이에는 단일 줄바꿈만 사용하여 자연스럽게 연결할 것."
				+ "\n - [여백 활용]: 큰 주제가 바뀌어 환기가 필요한 시점에만 두 번의 줄바꿈(\\n\\n)을 사용하여 가독성을 높일 것."
				+ "\n - [절대 금지] '서론/본론/결론', '###', '1.', '가.' 등 번호를 매기거나 목차용 단어를 절대 쓰지 말 것."
				+ "\n - [권장] 소제목은 대괄호나 기호 없이, 본문보다 조금 더 힘이 실린 문장 형태로 작성하고 바로 다음 줄부터 본문을 이어갈 것."
			    
			    + "\n4. 사용자 추가 지시사항: "
			    + "\n - %s", totalCategories, allDataContext.toString(), (tempWrite + writeQty), extraPrompt);
			
				
			// Gemini API 요청 바디 구성 (Gemini 전용 구조)
			// 1. 설정값 생성 (2000자 이상을 위해 maxOutputTokens를 크게 설정, 창의성은 0~2사이 적절하게 0.8)
			AiQuestionDTO.GenerationConfig config = new AiQuestionDTO.GenerationConfig(
					8192, // 충분한 토큰 확보 (한글 기준 약 2,000~4,000자 가능)
					0.8, // 창의적으로 풍부하게 쓰도록 설정
					"application/json" // 기본 텍스트 응답 (JSON이 필요하면 "application/json")
			);

			// 2. 바디 구성 (생성자에 config 추가)
			AiQuestionDTO body = new AiQuestionDTO(List.of(new AiQuestionDTO.Content(List.of(new AiQuestionDTO.Part(postPrompt)))), config);

			// 질문과 함께 gemini 호출
			String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent";

			Map<String, Object> rawResponse = webClient.post().uri(url).header("x-goog-api-key", apiKey).bodyValue(body)
					.retrieve().bodyToMono(Map.class).block();

			// 질문 답변을 추출
			if (rawResponse != null && rawResponse.containsKey("candidates")) {
				List candidates = (List) rawResponse.get("candidates");
				Map firstCandidate = (Map) candidates.get(0);
				Map content = (Map) firstCandidate.get("content");
				List parts = (List) content.get("parts");
				Map firstPart = (Map) parts.get(0);
				answer = (String) firstPart.get("text");
			}

			resultList.add(answer);
			return resultList;
		} catch (Exception e) {
			answer = "크롤링 중 오류 발생: " + e.getMessage();
			int code = Integer.parseInt(e.getMessage().split(" ")[0]);
			switch (code) {
			case 400:
				throw new ApiExceptionBuild(HttpStatus.BAD_REQUEST, answer);
			case 404:
				throw new ApiExceptionBuild(HttpStatus.NOT_FOUND, answer);
			case 429:
				throw new ApiExceptionBuild(HttpStatus.TOO_MANY_REQUESTS, answer);
			default:
				throw new ApiExceptionBuild(HttpStatus.INTERNAL_SERVER_ERROR, answer);
			}
		}
	}
	
	
	public void loginAndPrepare(String id, String pw, SseEmitter emitter) throws Exception {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        
        // 사용자가 작성 과정(브라우저)을 보려면 headless 옵션을 제거해야함
//        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();

        // 로그인 페이지 이동
        driver.get("https://nid.naver.com/nidlogin.login");
        sendChatLog(emitter, "> 네이버 로그인 창을 활성화합니다. 과정을 지켜봐주세요.");

        // 사람이 입력하는 것 처럼 JS로 우회 입력
        sendChatLog(emitter, "> 아이디를 입력 중입니다...");
        typeLikeHuman("id", id);
        Thread.sleep((int)(Math.random() * 500) + 500); // 아이디 입력 후 잠시 멈춤

        sendChatLog(emitter, "> 비밀번호를 입력 중입니다...");
        typeLikeHuman("pw", pw);
        Thread.sleep((int)(Math.random() * 500) + 500); // 비번 입력 후 잠시 멈춤

        // 3. 로그인 버튼 클릭
        driver.findElement(By.id("log.login")).click();
        
        // 4. 로그인 완료 대기 (메인 화면이나 알림창 등이 뜰 때까지 대기)
        Thread.sleep(3000); 
        
        if(driver.getCurrentUrl().contains("login.login")) {
            sendChatLog(emitter, "> [알림] 캡차(보안문자)가 떴을 수 있습니다. 브라우저에서 직접 해결해주세요.");
            // 사용자가 수동으로 풀 시간을 주기 위해 반복문으로 URL 변화를 체크하거나 길게 대기
            Thread.sleep(10000); 
        }
        
        driver.get("https://blog.naver.com/GoBlogWrite.naver");
        
    }
	
	// 브라우저 종료
    public void quit() {
        if (driver != null) {
        	driver.quit();
        }
    }
	
	// Jsoup 접속 공통화
	private Document fetchDocument(String url) {
	    try {
	        return Jsoup.connect(url)
	                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
	                .header("Referer", "https://www.naver.com")
	                .timeout(15000) // 타임아웃 15초
	                .get();
	    } catch (IOException e) {
	        return null;
	    }
	}
	
	// 카테고리에 해당하는 블로그 포스팅 혹은 뉴스 링크를 가져옴
	private void buildCrawling (Document doc, Set<String> uniqueLinks, String searchUrl, SseEmitter emitter, int crawlingSize, String flag) {
		// 블로그 포스팅, 뉴스정보를 추출
		// 변수명을 공통적인 이름으로 변경
		Elements links;
		if (flag.equals("news")) {
			// 뉴스는 naver.com이 들어가며 article이 존재하는 link를 추출
		    links = doc.body().select("a[href*='.naver.com'][href*='/article/']");
		} else {
		    links = doc.body().select("ul.lst_view a[href*='blog.naver.com']");
		}
		
		for (Element link : links) {
			// 블로그 포스팅, 뉴스에 대한 크롤링 수에 도달했다면 break
			if(uniqueLinks.size() >= crawlingSize) {
				break;
			}
		    String href = link.attr("abs:href"); // 전체 URL 추출

		    // 유효한 주소인지 확인 (숫자로 끝나는지 등)
		    if (isValidPostUrl(href, flag)) {
		        // Set에 추가 (중복이면 자동으로 무시됨)
		        uniqueLinks.add(href);
		    }
		}  
	}
	
	// 포스팅 상세 페이지 검증 메소드 (유저 홈 주소 제외)
	private boolean isValidPostUrl(String url, String flag) { 
	    if (url == null || url.isEmpty()) {
	    	return false;
	    }
	    //쿼리 스트링 제거
	    String cleanUrl = url.split("\\?")[0];

	    if ("news".equals(flag)) {
			// 네이버 도메인인지 확인, 경로에 /article/언론사코드/기사번호 형태가 포함되는지 확인
			return cleanUrl.contains(".naver.com") && cleanUrl.matches(".*\\/article/\\d+/\\d+.*");
	    } else {
	    	// https://m.blog.naver.com/{ID}/{POST_NUMBER} 형태이므로
		    // 슬래시로 나눴을 때 마지막 조각이 숫자인 경우만 포스팅 주소로 간주
	        String[] parts = cleanUrl.split("/");
	        if (parts.length >= 5) {
	        	String lastPart = parts[parts.length - 1];
		        return lastPart.matches("\\d+"); 
	        }
	        return false;
	    }
	}
	

    
	// 로그 전송을 위한 헬퍼 메소드
	private void sendChatLog(SseEmitter emitter, String message) {
		try {
			emitter.send(SseEmitter.event().data(message));
		} catch (IOException e) {
			emitter.completeWithError(e);
		}
	}
    
	// 사용자가 입력하는것 처럼 눈속임
	public void typeLikeHuman(String elementSelector, String text) throws InterruptedException {
	    JavascriptExecutor js = (JavascriptExecutor) driver;
	    String currentText = "";
	    
	    for (char c : text.toCharArray()) {
	        currentText += c;
	        // 한 글자씩 추가하며 입력
	        js.executeScript("document.getElementsByName('" + elementSelector + "')[0].value='" + currentText + "';");
	        
	        // 기본 300ms ~ 800ms 사이 랜덤
	        int delay = (int) (Math.random() * 500) + 300; 
	        
	        // 5글자마다 한 번씩 0.5초~1초 정도 더 길게 쉼 (생각하는 척)
	        if (currentText.length() % 5 == 0) {
	            delay += (int) (Math.random() * 500) + 500;
	        }
	        
	        Thread.sleep(delay);
	    }
	}
	
}
