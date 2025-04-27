package com.az.springaiwebflux.model;

import lombok.Data;

import java.util.List;

/**
 * @author Az
 * @date 2025/4/24
 */
@Data
public class ChatResponse {

    private List<Choice> choiceList;

    @Data
    public static class Choice {
        private Delta delta;
    }

    @Data
    public static class Delta {
        private String content;
    }
}
