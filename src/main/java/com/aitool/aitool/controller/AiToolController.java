package com.aitool.aitool.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aitool.aitool.dto.requestPostingDTO;
import com.aitool.aitool.service.AiToolService;

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
				throw new Exception("이건에러");
//				// 카테고리 리스트 파싱 (쉼표 기준)
//				List<String> categories = Arrays.stream(request.getSearchCategory().split(",")).map(String::trim)
//						.filter(s -> !s.isEmpty()).collect(Collectors.toList());
//
//				// [로그] 크롤링 시작
//				sendChatLog(emitter, "> 블로그 크롤링을 시작합니다...");
//
//				for (int i = 0; i < categories.size(); i++) {
//					String cat = categories.get(i);
//					// [로그] 카테고리별 진행 상황
//					sendChatLog(emitter, String.format("> [%d/%d] '%s' 카테고리 크롤링 중...", (i + 1), categories.size(), cat));
//
//					// 실제 크롤링 서비스 로직 수행
//					// aiToolService.crollingAi(request) TODO:  내부에서 로그를 더 세분화 하기위해 emitter를 서비스 레이어까지 넘겨줄것
//				}
//
//				// [로그] 글 작성 단계
//				sendChatLog(emitter, "> 크롤링 완료! 이제 AI 포스팅 작성을 시작합니다...");
//
//				// TODO: 실제 AI 포스팅 서비스 호출
//				// Map<String, Object> result = aiToolService.generatePost(request);
//
//				sendChatLog(emitter, "> 모든 포스팅 작성이 완료되었습니다.");
//
//				// 작업 종료 알림 (complete를 호출해야 연결 종료)
//				emitter.complete();
//
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
