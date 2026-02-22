package com.aitool.aitool.dto;

import java.util.List;

import com.google.cloud.vertexai.api.GenerationConfig;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AiQuestionDTO {
	
    private List<Content> contents;
    private GenerationConfig generationConfig; // AI의 답변 규칙(길이, 창의성 등)을 설정하는 필드
    
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Content { // static 추가
        private List<Part> parts;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Part { // static 추가
        private String text;
    }
    
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GenerationConfig {
        private Integer maxOutputTokens; // 최대 글자 수
        private Double temperature;      // 창의성 (0.0 ~ 2.0)
        private String responseMimeType; // 응답 형식 (필요시 "application/json")
    }
}
