package com.aitool.aitool.controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aitool.aitool.dto.requestPostingDTO;
import com.aitool.aitool.service.AiToolService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiToolController {
	
	private final AiToolService aiToolService;
	 
	@PostMapping("/posting")
	public SseEmitter postingControl(@RequestBody requestPostingDTO request) {
	    SseEmitter emitter = new SseEmitter(600000L);

	    CompletableFuture.runAsync(() -> {
	        try {
	            sendChatLog(emitter, "> Ai Tool 시스템이 준비되었습니다.");
	            
	            List<String> categories = Arrays.stream(request.getSearchCategory().split(","))
	                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
	            
	            // 유효성 검증
	            String errorMsg = checkRequiredData(request, categories);
	            if(!errorMsg.equals("")) throw new Exception(errorMsg);

	            // 1. 모든 카테고리 데이터 수집 및 통합
	            sendChatLog(emitter, "> 블로그 크롤링을 시작합니다...");
	            StringBuilder allDataContext = new StringBuilder();
	            
	            for (int i = 0; i < categories.size(); i++) {
	                String category = categories.get(i);
	                sendChatLog(emitter, String.format("> [%d/%d] '%s' 카테고리 크롤링 중...", (i + 1), categories.size(), category));

	                Set<String> categoryLinks = aiToolService.crawlingBlog(request, emitter, category);
	                if(!categoryLinks.isEmpty()) {
	                    // 모든 데이터를 하나의 컨텍스트로 합침
	                    allDataContext.append(String.format("\n[카테고리: %s] 링크: %s\n", category, categoryLinks));
	                }
	            }
	            sendChatLog(emitter, "> Blog / News 크롤링 완료 및 데이터 통합 성공!");

	            // 2. 파일 저장 경로 설정
	            File storageDir = new File("C:" + File.separator + "AITool");
	            if (!storageDir.exists()) storageDir.mkdirs();
	            
	            // 3. 생성할 총 개수 계산 (5개 임시 + 5개 작성 = 10개)
	            int totalPosts = request.getTempWriteQty() + request.getRealWriteQty();
	            sendChatLog(emitter, String.format("> 총 %d개의 서로 다른 포스팅 생성을 시작합니다.", totalPosts));

	            // 4. Flux.range를 사용하여 1부터 totalPosts까지 병렬 실행
	            ObjectMapper objectMapper = new ObjectMapper(); // 재사용을 위해 루프 밖으로 이동

	            Flux.range(1, totalPosts)
	            .flatMap(i -> {
	                // 결과(String)를 순번(i)과 함께 Map.Entry로 묶어서 반환
	                return aiToolService.generateSinglePost(request, allDataContext.toString(), totalPosts, i)
	                        .map(answer -> Map.entry(i, answer)); 
	            }, 3)
	            .doOnNext(entry -> {
	                Integer currentIndex = entry.getKey();   // 여기서 i 값을 꺼냄
	                String answer = entry.getValue();        // 여기서 API 응답을 꺼냄

	                try {
	                	// 응답 문자열이 JSON 객체 형태( {로 시작)인지 배열 형태( [로 시작)인지 먼저 체크하고 배열이 아니고 error라는 단어가 포함되어있다면 error로그 및 스킵
	                    if (answer.contains("\"error\"") || answer.contains("Gemini API 호출 실패")) {
	                        sendChatLog(emitter, String.format("> [%d번째] AI 응답 실패: %s", currentIndex, answer));
	                        return; // 건너뜀
	                    }
	                	
	                    List<Map<String, Object>> postList = objectMapper.readValue(answer, new TypeReference<List<Map<String, Object>>>() {});
	                    for (Map<String, Object> post : postList) {
	                        String title = (String) post.get("title");
	                        String content = (String) post.getOrDefault("content", "본문 내용 없음");
	                        List<String> tagKeywords = (List<String>) post.get("tagKeywords");
	                        
	                        // 파일 저장 로직
	                        String safeFileName = title.replaceAll("[\\\\/:*?\"<>|]", "_");
	                        File file = new File(storageDir, safeFileName + ".txt");
	                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
	                            writer.write("제목: " + title + "\n\n");
	                            writer.write("본문:\n" + content + "\n\n");
	                            if (tagKeywords != null) writer.write("태그: " + String.join(", ", tagKeywords));
	                            
	                            // currentIndex(i)를 로그에 정상적으로 사용 가능
	                            sendChatLog(emitter, String.format("> [%d/%d] [%s] 저장 완료!", currentIndex, totalPosts, title));
	                        }
	                     // 네이버 로그인 호출 (사용자가 브라우저를 볼 수 있게 됨)
//	        				sendChatLog(emitter, "> 네이버 블로그 자동 로그인을 시도합니다...");
//	        				// request 객체에 사용자의 id, pw가 포함되어 있다고 가정합니다.
//	        				String nID = (String) request.getSettings().get("naverID");
//	        				String nPW = (String) request.getSettings().get("naverPW");
//	        				aiToolService.loginAndPrepare(nID, nPW, emitter);
	                    }
	                } catch (Exception e) {
	                    sendChatLog(emitter, "> [" + currentIndex + "번째] 파싱 오류 발생: " + e.getMessage());
	                }
	            })
	            .collectList()
	            .block();
	            sendChatLog(emitter, "> 모든 포스팅 작성이 완료되었습니다."); 
	            emitter.complete();

	        } catch (Exception e) { 
	            sendChatLog(emitter, "> [실패] " + e.getMessage());
	            emitter.complete();
	        }
	    });

	    return emitter;
	} 
	
	// 필수로 작성되어야하는 Setting값 혹은 데이터가 있는지 여부 체크
	private String checkRequiredData(requestPostingDTO request, List<String>categories) {
		Map<String, Object> settings = request.getSettings();
		// settings가 아예 비어있는경우
		if(settings == null) {
			return "AI Tool을 사용하기 위해서는 설정 페이지에서 존재하는 필수 입력값들이 입력되어있어야 합니다.";
		}
		// AI Tool에 사용될 API key 여부
		String apiKey = (String) settings.get("aiToolKey");
		if(apiKey == null || apiKey.isBlank()) {
			return "AI Tool을 사용하기 위해서는 설정 페이지에서 GPT 혹은 Gemini의 API Key를 필수로 입력해야합니다.";
		}
		
		// 카테고리를 입력하지않았거나 5개 이상의 카테고리를 입력한 경우
		if(categories.size() <= 0) {
			return "크롤링 및 포스팅을 위한 카테고리를 입력해주세요. ";
		}
		if(categories.size() > 5) {
			return "크롤링 및 포스팅을 위한 카테고리는 5개 이하로만 입력해주세요. ";
		}
		
		// 블로그 크롤링 수
		int crawBlog = request.getCrawlingBlogQty();
		// 뉴스 크롤링 수 
		int crawNews = request.getCrawlingNewsQty();
		// 실제작성수
		int realWrite = request.getRealWriteQty();
		// 임시저장수 
		int tempWrite = request.getTempWriteQty();
		
		// 크롤링 관련 Valid 체크 
		boolean isCrawlingTargetSelected = (crawBlog + crawNews >= 1);
		
		// 작성 관련 Valid 체크
		boolean isWritingTargetSelected = (realWrite + tempWrite >= 1);
		if (!isCrawlingTargetSelected) {
	        return "AI Tool을 사용하기 위해서는 블로그 혹은 뉴스에 대한 크롤링 수가 1 이상이여야 합니다.";
	    }
		// 블로그/뉴스 크롤링수가 1 이상인지 먼저 확인 후 작성에 대한 개수 체크
	    if (!isWritingTargetSelected) {
	    	return "AI Tool을 사용하기 위해서는 포스팅 혹은 임시저장에 대한 수가 1 이상이여야 합니다.";
	    }
	    
	    // 네이버 아이디 체크 현재 로그인검증은 무조건 pass
//	    String nID = (String) settings.get("naverPW");
//	    if(nID == null || nID.isBlank()) {
//	    	return "AI Tool을 사용하기 위해서는 블로그 임시저장 및 블로그 포스팅을 위한 NaverID가 필요합니다.";
//	    }
//	    // 네이버 패스워드 체크
//		String nPW = (String) settings.get("naverID");
//	    if(nPW == null || nPW.isBlank()) {
//	    	return "AI Tool을 사용하기 위해서는 블로그 임시저장 및 블로그 포스팅을 위한 NaverPW가 필요합니다.";
//	    }		
	    
	    return "";
	}

	// 로그 전송을 위한 헬퍼 메소드
	private void sendChatLog(SseEmitter emitter, String message) {
		try {
			emitter.send(SseEmitter.event().data(message));
		} catch (IOException e) {
			emitter.completeWithError(e);
		}
	}
}
