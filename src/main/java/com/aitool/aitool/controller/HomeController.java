package com.aitool.aitool.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

	@GetMapping("/")
    public String home(Model model) {
		
        model.addAttribute("message", "안녕하세요! AI Tool의 메인 페이지입니다.");
        model.addAttribute("initLicense", "TestLicense");
        
        return "index"; 
    }
}
