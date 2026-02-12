package com.aitool.aitool.dto;

import java.util.List;

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
}