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

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiToolController {
	
	private final AiToolService aiToolService;
	
	@PostMapping("/posting")
	public SseEmitter postingControl(@RequestBody requestPostingDTO request) {
		// 1. Emitter 생성 (타임아웃 10분 설정)
		SseEmitter emitter = new SseEmitter(600000L);

		// 2. 별도 스레드에서 작업 수행 (비동기)
		CompletableFuture.runAsync(() -> {
			try {
				// [로그] 시작 알림
				sendChatLog(emitter, "> Ai Tool 시스템이 준비되었습니다.");
				
				// 카테고리 리스트 파싱 (쉼표 기준)
				List<String> categories = Arrays.stream(request.getSearchCategory().split(",")).map(String::trim)
						.filter(s -> !s.isEmpty()).collect(Collectors.toList());

				// [로그] 크롤링 시작
				sendChatLog(emitter, "> 블로그 크롤링을 시작합니다...");
				Map<String,Set<String>> crawlingLinkData = new LinkedHashMap<>();
				for (int i = 0; i < categories.size(); i++) {
					String category = categories.get(i);
					// [로그] 카테고리별 진행 상황
					sendChatLog(emitter, String.format("> [%d/%d] '%s' 카테고리 크롤링 중...", (i + 1), categories.size(), category));

					// 실제 크롤링 서비스 로직 수행
					Set<String> categoryLinks = aiToolService.crawlingBlog(request, emitter, category); // TODO:  내부에서 로그를 더 세분화 하기위해 emitter를 서비스 레이어까지 넘겨줄것
					if(categoryLinks.size() > 0) {
						crawlingLinkData.put(category, categoryLinks);
					}
					sendChatLog(emitter, String.format("> [%s]에 대한 크롤링 완료...", category)); 
				}
				sendChatLog(emitter, "> 모든 카테고리에 대한 크롤링 완료!");
				
				// Link에 대한 블로그 작성을 위하여 정리 시작
				sendChatLog(emitter, "> AI 포스팅 작성을 위해 크롤링하여 가져온 데이터를 정리 합니다.");

				// 카테고리별로 가져온 블로그/뉴스 Link를 토대로 AI에게 블로그 포스팅에 사용될 content를 제작하도록 하는 기능
				List<String> generateResult = aiToolService.generateContent(request, emitter, crawlingLinkData);
				
				// AI를 통해 가져온 content목록을토대로 naver블로그 작성/임시작성
				// JSON형태로 저장되어있으므로 각각 필요한 부분을 추출하여 마지막 단계 진행
				ObjectMapper objectMapper = new ObjectMapper();
				
				
				/** 체험판 느낌으로 검색 결과를 바탕화면에 우선 저장하도록 **/
				// C 드라이브에 AITool 폴더 경로 설정
				File storageDir = new File("C:" + File.separator + "AITool");
				// 폴더가 없으면 생성
				if (!storageDir.exists()) {
				    boolean created = storageDir.mkdirs();
				    if (created) {
				        System.out.println("폴더를 생성했습니다: " + storageDir.getAbsolutePath());
				    }
				}
				for(String answer : generateResult) {
				    // 1. JSON String을 List<Map<String, Object>> 형태로 변환
				    List<Map<String, Object>> postList = objectMapper.readValue(answer, new TypeReference<List<Map<String, Object>>>() {});
	
				    if (!postList.isEmpty()) {
				        String title = (String) postList.get(0).get("title");
				        String content = (String) postList.get(0).get("content");
				        List<String> tagKeywords = (List<String>) postList.get(0).get("tagKeywords");
				        
				        // 파일명 안전 처리
			            String safeFileName = title.replaceAll("[\\\\/:*?\"<>|]", "_");
			            File file = new File(storageDir, safeFileName + ".txt"); 

			            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
			                writer.write("제목: " + title + "\n\n");
			                writer.write("본문:\n" + content + "\n\n");
			                writer.write("태그 키워드: " + String.join(", ", tagKeywords));
							sendChatLog(emitter, String.format("> [%s] Text파일을 C:\\AITool 폴더에 저장하였습니다.", title)); 
			            }
				    }
				} 
				
				sendChatLog(emitter, "> 모든 포스팅 작성이 완료되었습니다."); 

				// 작업 종료 알림 (complete를 호출해야 연결 종료)
				emitter.complete();
			} catch (Exception e) { 
				// 1. 에러 문구를 명확하게 전송 (이게 lastServerLog에 담깁니다)
			    sendChatLog(emitter, "> [실패] " + e.getMessage());
			    
			    try { Thread.sleep(500); } catch (Exception ignored) {}
			    
			    // 2. 에러가 났어도 complete()로 닫아야 브라우저가 마지막 메시지를 안 버립니다.
			    emitter.complete();
			    
//				sendChatLog(emitter, "> [에러 발생] " + e.getMessage());
//				emitter.completeWithError(e);
			}
		});

		return emitter;
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
