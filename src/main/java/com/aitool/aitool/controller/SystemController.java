package com.aitool.aitool.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SystemController {

		// 시스템 종료
		@PostMapping("/system/shutdown")
		public ResponseEntity<?> shutdown() {
		    new Thread(() -> {
		        try {
		            Thread.sleep(1000); // Electron이 응답을 받을 시간을 충분히 줌
		            System.exit(0);
		        } catch (InterruptedException e) {
		            e.printStackTrace();
		        }
		    }).start();
		    return ResponseEntity.ok("Shutting down...");
		}
		
}
