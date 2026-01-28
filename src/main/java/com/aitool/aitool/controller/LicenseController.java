package com.aitool.aitool.controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aitool.aitool.dto.RequestLicenseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/login")
public class LicenseController {
	
	// Gist URL
	private final String GIST_RAW_URL = "https://gist.githubusercontent.com/Anjisu512/358ba5c8e4c88c7e45ee8f192062370b/raw/5b9a3627990324d78ecf9fb80726a8b67ec7360f/licenses.json";
	
	
	@PostMapping("/verify")
	public ResponseEntity<RequestLicenseDTO> verify(@RequestBody Map<String, String> body, HttpSession session) {
        String inputKey = body.get("licenseKey");
        
        // 1. Gist에서 정보 조회 (기존에 만든 Map 반환 함수 활용)
        Map<String, String> userInfo = checkGistForLicense(GIST_RAW_URL, inputKey);
        
        RequestLicenseDTO dto = new RequestLicenseDTO();

        if (userInfo != null) {
            // 인증 성공
            session.setAttribute("LICENSE_AUTH", true);
            session.setAttribute("USER_ID", userInfo.get("id"));

            dto.setValid(true);
            dto.setId(userInfo.get("id"));
            dto.setExpireDate(userInfo.get("expire_date"));
            dto.setLicenseKey(userInfo.get("licenseKey"));
            dto.setRedirect("/");
            dto.setMessage("인증에 성공했습니다.");

            return ResponseEntity.ok(dto);
        } else {
            // 인증 실패
            dto.setValid(false);
            dto.setMessage("유효하지 않거나 만료된 라이선스 키입니다.");
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(dto);
        }
    }
	
	// gist 조회
	private Map<String, String> checkGistForLicense(String urlString, String inputKey) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            URL url = new URL(urlString);
            
            // 1. Gist에서 JSON 읽기
            JsonNode root = mapper.readTree(url);
            JsonNode userList = root.get("user_list");

            if (userList != null && userList.isArray()) {
                for (JsonNode user : userList) {
                    // Gist의 "key" 항목과 입력값이 일치하는지 확인
                    if (user.has("licenseKey") && user.get("licenseKey").asText().equals(inputKey)) {
                        
                        // 2. 만료일 검증 (온라인 시간 기준)
                        if (isExpired(user.get("expire_date").asText())) {
                            return null; // 만료됨
                        }

                        // 3. 모든 정보를 Map으로 반환
                        Map<String, String> userData = new HashMap<>();
                        userData.put("id", user.get("id").asText());
                        userData.put("expire_date", user.get("expire_date").asText());
                        userData.put("licenseKey", user.get("licenseKey").asText());
                        return userData;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

	// key 유효기간 확인
    private boolean isExpired(String expireDateStr) {
        if ("9999-12-31".equals(expireDateStr)) return false; // 무제한 패스
        
        try {
            // 온라인 시간 API 호출하여 편법 금지
            URL timeUrl = new URL("http://worldtimeapi.org/api/timezone/Asia/Seoul");
            JsonNode timeNode = new ObjectMapper().readTree(timeUrl);
            LocalDate today = LocalDate.parse(timeNode.get("datetime").asText().substring(0, 10));
            LocalDate expireDate = LocalDate.parse(expireDateStr);
            
            return today.isAfter(expireDate); // 오늘이 만료일보다 뒤면 true(만료)
        } catch (Exception e) {
            return true; // 시간 확인 불가 시 안전하게 만료 처리
        }
    }
}
