package com.aitool.aitool.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aitool.aitool.config.ApiExceptionBuild;
import com.aitool.aitool.dto.requestPostingDTO;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor
public class AiToolService {

	  // Gemini API의 베이스 URL 설정
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com/v1beta/") 
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    
	@SuppressWarnings({ "unchecked", "rawtypes" }) // gemini 관련 return값에대한 Warning 제거
	public String crawlingBlog(requestPostingDTO request, SseEmitter emitter, String category) {
		Map<String, Object> response = new HashMap<>();
	    String answer = "";  
		try {
			StringBuilder crawlingData = new StringBuilder();			
			String apiKey = (String) request.getSettings().get("aiToolKey");
			
			String encodedCategory = URLEncoder.encode(category, "UTF-8");
	        // 네이버 블로그, 뉴스 베이스 URL 분리
	        String blogUrl = "https://m.search.naver.com/search.naver?ssc=tab.m_blog.all&query=" + encodedCategory;
	        String newsUrl = "https://m.search.naver.com/search.naver?ssc=tab.m_news.all&query=" + encodedCategory;

	        Set<String> uniqueBlogLinks = new LinkedHashSet<>();
	        Set<String> uniqueNewsLinks = new LinkedHashSet<>();
			 
	     // 1. 블로그 크롤링
	        if (request.getCrawlingBlogQty() > 0) {
	            Document doc = fetchDocument(blogUrl);
	            if (doc != null) {
	                buildCrawling(doc, uniqueBlogLinks, blogUrl, emitter, request.getCrawlingBlogQty(), "blog");
	            }
	        }

	        // 2. 뉴스 크롤링
	        if (request.getCrawlingNewsQty() > 0) {
	            Document doc = fetchDocument(newsUrl);
	            if (doc != null) {
	                buildCrawling(doc, uniqueNewsLinks, newsUrl, emitter, request.getCrawlingNewsQty(), "news");
	            }
	        }
			
			for(String s : uniqueBlogLinks) {
				sendChatLog(emitter, String.format("> 블로그 크롤링 대기 주소 : [%s] ", s));
			}			
			for(String s : uniqueNewsLinks) {
				sendChatLog(emitter, String.format("> 뉴스 크롤링 대기 주소 : [%s] ", s));
			}			
			
			return crawlingData.toString();
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
	
//	try {
//		String apiKey = (String) request.getSettings().get("aiToolKey");
//		 
//		// 구글에서 카테고리별 키워드로 검색 (실제 URL은 조정 필요)
//		String searchUrl = "https://namu.wiki/w/" + URLEncoder.encode(category, "UTF-8");
//
//		// 질문 생성 (예시: 카테고리에 맞는 블로그 글 작성 요청)
//		String crawlingRequestBody = category.toString() + "에 대한 블로그 크롤링을 시작해줘 참고할만한 자료는 해당 URL에 있어 : "+ searchUrl;
//
//		// Gemini API 요청 바디 구성 (Gemini 전용 구조)
//		AiQuestionDTO body = new AiQuestionDTO(List.of(new AiQuestionDTO.Content(List.of(new AiQuestionDTO.Part(crawlingRequestBody)))));
//
//		// 질문과 함께 gemini 호출
//		String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent";
//
//		Map<String, Object> rawResponse = webClient.post().uri(url).header("x-goog-api-key", apiKey).bodyValue(body).retrieve().bodyToMono(Map.class).block();
//
//		// 질문 답변을 추출
//		if (rawResponse != null && rawResponse.containsKey("candidates")) {
//			List candidates = (List) rawResponse.get("candidates");
//			Map firstCandidate = (Map) candidates.get(0);
//			Map content = (Map) firstCandidate.get("content");
//			List parts = (List) content.get("parts");
//			Map firstPart = (Map) parts.get(0);
//			answer = (String) firstPart.get("text");
//		} 
//		
//		return answer;
//	} 
    
	// 로그 전송을 위한 헬퍼 메소드
	private void sendChatLog(SseEmitter emitter, String message) {
		try {
			emitter.send(SseEmitter.event().data(message));
		} catch (IOException e) {
			emitter.completeWithError(e);
		}
	}
    
}
