package com.aitool.aitool.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aitool.aitool.config.ApiExceptionBuild;
import com.aitool.aitool.dto.requestPostingDTO;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.input.PromptTemplate;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;


@Service 
@RequiredArgsConstructor
public class AiToolService {
	// 네이버 블로그 작성을 위한 WebDriver
	private WebDriver driver;
	
	// gemini chatModel 생성
	private final Map<String, GoogleAiGeminiChatModel> modelCache = new ConcurrentHashMap<>();

	private GoogleAiGeminiChatModel getGeminiModel(String apiKey) {

	    return modelCache.computeIfAbsent(apiKey, key ->
	        GoogleAiGeminiChatModel.builder()
	                .apiKey(key)
	                .modelName("gemini-2.5-flash")
	                .temperature(0.7)
	                .timeout(Duration.ofSeconds(60))
	                .maxRetries(2)
	                .responseFormat(ResponseFormat.JSON)
	                .build()
	    );
	}
    
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
	/**
     * 단일 포스팅 생성을 위한 메서드 (LangChain4j SDK 적용)
     */
	public Mono<String> generateSinglePost(requestPostingDTO request, String allDataContext, int totalCount, int currentIndex) {

		return Mono.fromCallable(() -> {

			String apiKey = (String) request.getSettings().get("aiToolKey");
			GoogleAiGeminiChatModel model = getGeminiModel(apiKey);
			String extraPrompt = "";
			Map<String, Object> settings = request.getSettings();
			if (request.isUseAiTool() && settings != null && settings.get("extraPrompt") != null) {
				Object rawPrompt = settings.get("extraPrompt");
				if (rawPrompt instanceof Map) {
					Map<String, Object> prompt = (Map<String, Object>) rawPrompt;
					extraPrompt = String.valueOf(prompt.getOrDefault("content", ""));
				}
			}

			PromptTemplate template = PromptTemplate.from("""
					    너는 수집된 정보를 분석하여 독창적인 블로그 포스팅을 작성하는 콘텐츠 아키텍트다.

					    [전체 데이터]
					    {{context}}

					    [작업]
					    총 {{total}}개의 글 중 {{index}}번째 글 작성

						[요청사항]
					    1. 반드시 JSON 배열 형식으로 출력:
					    [
					      {
					        "title": "...",
					        "content": "...",
					        "tagKeywords": ["...", "..."]
					      }
					    ]
						2. 사람처럼 자연스럽게 작성하고, 번호 매기기나 목차용 단어는 쓰지 마
						3. [필수] 앞선 글들과 관점이 중복되지 않게 새로운 시각에서 작성할 것
					    사용자 추가 요청:
					    {{extra}}
					""");

			String prompt = template.apply(Map.of("context", allDataContext, "total", totalCount, "index", currentIndex, "extra", extraPrompt)).text();
            String response = model.chat(UserMessage.from(prompt)).aiMessage().text();
            // JSON 코드블럭 제거
            response = response
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            return response; 

		}).onErrorResume(e -> Mono.just("{\"error\":\"" + e.getMessage() + "\"}"));
	}

//        return Mono.fromCallable(() -> {
//            String apiKey = (String) request.getSettings().get("aiToolKey");
//            String extraPrompt = "";
//            Map<String, Object> settings = request.getSettings();
//
//            if (request.isUseAiTool() && settings != null && settings.get("extraPrompt") != null) {
//                Object rawPrompt = settings.get("extraPrompt");
//                if (rawPrompt instanceof Map) {
//                    Map<String, Object> prompt = (Map<String, Object>) rawPrompt;
//                    extraPrompt = String.valueOf(prompt.getOrDefault("content", ""));
//                }
//            }
//
//            // 1. 프롬프트 구성
//            String postPrompt = String.format(
//                "너는 수집된 정보를 분석하여 독창적인 블로그 포스팅을 작성하는 '콘텐츠 아키텍트'야.\n\n"
//                + "[1. 전체 분석 데이터 소스]:\n%s\n\n"
//                + "[2. 현재 작업 지시]:\n"
//                + "- 총 %d개의 포스팅 중 [%d번째] 글을 작성해.\n"
//                + "- [필수] 앞선 글들과 관점이 중복되지 않게 새로운 시각에서 작성할 것.\n\n"
//                + "[3. 출력 형식]:\n"
//                + "- 반드시 JSON 배열 [ { \"title\": \"...\", \"content\": \"...\", \"tagKeywords\": [...] } ] 형식을 엄수해.\n"
//                + "- 사람처럼 자연스럽게 작성하고, 번호 매기기나 목차용 단어는 쓰지 마.\n"
//                + "[4. 사용자 추가 요청]: %s", 
//                allDataContext, totalCount, currentIndex, extraPrompt);
//
//
//			try {
//				GoogleAiGeminiChatModel model = GoogleAiGeminiChatModel.builder()
//						.apiKey(apiKey)
//						.modelName("gemini-2.5-flash")
//						.responseFormat(ResponseFormat.JSON)
//						.temperature(0.7)
//						.timeout(Duration.ofSeconds(60)).build();
//
//				return model.chat(UserMessage.from(postPrompt)).aiMessage().text();
//			} catch (Exception e) {
//				e.printStackTrace();
//				return "{\"error\": \"Gemini API 호출 실패: " + e.getMessage() + "\"}";
//			} 
//        }); 
	 
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
