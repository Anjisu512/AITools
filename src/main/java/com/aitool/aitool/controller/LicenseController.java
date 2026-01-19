package com.aitool.aitool.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/login")
public class LicenseController {

	@PostMapping("/verify")
	public Map<String, Object> verify(@RequestBody Map<String, String> body, HttpSession session) {
		String key = body.get("licenseKey");

		boolean valid = "TEST-1234".equals(key);

		if (valid) {
			session.setAttribute("LICENSE_AUTH", true); // 🔑 핵심
		}

		return Map.of("valid", valid);
	}

}
