package com.aitool.aitool.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestLicenseDTO {
    private boolean valid;      // 인증 성공 여부
    private int code;			// 상태code
    private String id;          // 사용자 ID
    private String expireDate;  // 만료일
    private String licenseKey;  // 입력한 키
    private String redirect;    // 이동할 페이지 경로
    private String message;     // 에러 메시지 등
}