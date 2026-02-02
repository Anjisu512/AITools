package com.aitool.aitool.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	@GetMapping("/")
    public String home(Model model) {
		
        model.addAttribute("message", "안녕하세요! AI Tool의 메인 페이지입니다.");
        model.addAttribute("initLicense", "TestLicense");
        
        return "home"; 
    }
	
	@GetMapping("/settings")
	public String setting(Model model) {
		
		return "settings";
	}
	
	@GetMapping("/posting")
	public String posting(Model model) {
		
		return "posting";
	}
}
