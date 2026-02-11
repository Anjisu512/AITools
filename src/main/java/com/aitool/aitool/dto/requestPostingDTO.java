package com.aitool.aitool.dto;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class requestPostingDTO {
	private Map<String,Object> settings; // setting 페이지에서 정의된 값
	private boolean useAiTool;			 // 명령프롬포트 사용  여부
    private String searchCategory;		 // , 기준 최대 5개의 카테고리 크롤링 
    private int crollingQty;			 // 크롤링 수
    private int tempWriteQty;			 // 임시저장 수
    private int realWriteQty;			 // 실제 작성 수
}

