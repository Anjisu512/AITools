package com.aitool.aitool.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aitool.aitool.dto.requestPostingDTO;

@RestController
@RequestMapping("/api/ai")
public class AiToolController {

	@PostMapping("/posting") 
	public ResponseEntity<Map<String,Object>> postingControl(@RequestBody requestPostingDTO request) {
        // 여기서 로직 처리
        System.out.println("카테고리: " + request.getSearchCategory());
        System.out.println("총 작성 수: " + (request.getTempWriteQty() + request.getRealWriteQty()));
        
        return ResponseEntity.ok().body(Map.of("200", "데이터 수신 완료"));
    }
}
