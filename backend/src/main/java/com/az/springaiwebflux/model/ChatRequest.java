package com.az.springaiwebflux.model;

import lombok.Data;

import java.util.List;

/**
 * @author Az
 * @date 2025/4/24
 */
@Data
public class ChatRequest {
//    private String model = "deepseek-ai/DeepSeek-V3";
    private String model = "Qwen/QwQ-32B";
    private List<Message> messages;
    private boolean stream = true;
    private int max_tokens = 512;
    private double temperature = 0.7;
    private double top_p = 0.7;
    private int top_k = 50;
    private double frequency_penalty = 0.5;
    private int n = 1;
    private ResponseFormat responseFormat = new ResponseFormat("text");

    @Data
    public static class ResponseFormat {
        private String type;
        public ResponseFormat(String type) {
            this.type = type;
        }
    }
}
