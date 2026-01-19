package com.aitool.aitool.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginLicensePageController {

	@GetMapping("/loginLicense")
	public String licensePage() {
		return "loginLicense"; // templates/license.html
	}
}
