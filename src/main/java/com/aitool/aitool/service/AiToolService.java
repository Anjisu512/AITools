package com.aitool.aitool.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.aitool.aitool.config.ApiExceptionBuild;
import com.aitool.aitool.dto.AiQuestionDTO;
import com.aitool.aitool.dto.RequestLicenseDTO;
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
	public Map<String, Object> crollingAi(requestPostingDTO request) {
		Map<String, Object> response = new HashMap<>();
		try {
			StringBuilder crawledData = new StringBuilder();
			String apiKey = (String) request.getSettings().get("aiToolKey");
			List<String> categories = Arrays.stream(request.getSearchCategory().split(","))
										.map(String::trim) // 앞뒤 공백 제거
										.filter(s -> !s.isEmpty()) // 빈 문자열 제거 (,, 처럼 쉼표만 있는 경우 대비)
										.collect(Collectors.toList());

			for (String category : categories) {
				// 구글에서 카테고리별 키워드로 검색 (실제 URL은 조정 필요)
				String searchUrl = "https://search.naver.com/search.naver?where=blog&query=" + category;
				Document doc = Jsoup.connect(searchUrl)
				    .userAgent("Mozilla/5.0")
				    .get();

				// 네이버 블로그 검색 결과의 제목과 요약 텍스트만 추출
				String snippets = doc.select(".api_txt_lines.dsc_txt").text(); 
				String snippet = snippets.substring(0, Math.min(snippets.length(), 1000));
				crawledData.append(category).append(" 관련 정보: ").append(snippet).append("\n"); 
			}
	            
			// 크롤링한 데이터를 프롬프트에 결합
			String prompt = String.format(
					"내가 수집한 다음 정보들을 참고해서 카테고리 [%s]에 대한 블로그 포스팅을 작성해줘.\n\n" + "### 수집 데이터 ###\n%s\n\n"
							+ "추가 프롬포트 : %s",
					String.join(", ", categories), crawledData.toString(), "");

			// 이제 이 prompt를 WebClient를 통해 Gemini API로 전송하면 됩니다.
			
			return response;
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
    
    
    String answer = ""; 
    //
//        	try {
//            	// Ai API 키와 모델명 추출
//        		String apiKey = (String) request.getSettings().get("aiToolKey");
//        		// 만약 DTO에 모델명이 없다면 기본값 "gemini-1.5-flash" 사용
//        		String modelName = (String) request.getSettings().get("aiToolModel");
//        		if(modelName.isBlank()) {
//        			modelName = "gemini-3-flash-preview"; // 검색에 사용할 Model 분당 5번이하, 최대입력토큰수 250k, 일일 최대 요청 20회이내로 사용 가능
//        		}
//        		
//        		List<String> categories = Arrays.stream(request.getSearchCategory().split(","))
//        	    .map(String::trim)        // 앞뒤 공백 제거
//        	    .filter(s -> !s.isEmpty()) // 빈 문자열 제거 (,, 처럼 쉼표만 있는 경우 대비)
//        	    .collect(Collectors.toList());
//        		
//        		// 질문 생성 (예시: 카테고리에 맞는 블로그 글 작성 요청)
//                String crollingRequestBody = categories.toString() + "에 대한 블로그 크롤링을 시작해줘.";
//                
//                // Gemini API 요청 바디 구성 (Gemini 전용 구조)
//                AiQuestionDTO body = new AiQuestionDTO(
//                    List.of(new AiQuestionDTO.Content(
//                        List.of(new AiQuestionDTO.Part(crollingRequestBody))
//                    ))
//                );
//                
//                // 질문과 함께 gemini 호출
//                String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent";
    //
//                Map<String, Object> rawResponse = webClient.post()
//                        .uri(url)
//                        .header("x-goog-api-key", apiKey) // curl의 -H 옵션과 동일하게 설정
//                        .bodyValue(body)
//                        .retrieve()
//                        .bodyToMono(Map.class)
//                        .block();
//                 
//                // 질문 답변을 추출
//                if (rawResponse != null && rawResponse.containsKey("candidates")) {
//                    List candidates = (List) rawResponse.get("candidates");
//                    Map firstCandidate = (Map) candidates.get(0);
//                    Map content = (Map) firstCandidate.get("content");
//                    List parts = (List) content.get("parts");
//                    Map firstPart = (Map) parts.get(0);
//                    answer = (String) firstPart.get("text");
//                }
//                
//                // 최종 결과 리턴
//                response.put("status", "200");  
//                response.put("answer", answer);
//                return response;
//            } catch (Exception e) {
//                answer = "데이터 파싱 중 오류 발생: " + e.getMessage();
//                int code = Integer.parseInt(e.getMessage().split(" ")[0]);
//                switch (code) {
//                case 400:
//                    throw new ApiExceptionBuild(HttpStatus.BAD_REQUEST, answer);
//                case 404:
//                    throw new ApiExceptionBuild(HttpStatus.NOT_FOUND, answer);
//                case 429:
//                    throw new ApiExceptionBuild(HttpStatus.TOO_MANY_REQUESTS, answer);
//                default:
//                    throw new ApiExceptionBuild(HttpStatus.INTERNAL_SERVER_ERROR, answer);
//                }
//            }
    
}
