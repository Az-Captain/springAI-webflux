package com.az.springaiwebflux.service;

import com.az.springaiwebflux.config.DeepSeekConfig;
import com.az.springaiwebflux.model.ChatRequest;
import com.az.springaiwebflux.model.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.PrematureCloseException;
import reactor.util.retry.Retry;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

/**
 * @author Az
 * @date 2025/4/16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekService {
    private final DeepSeekConfig config;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void startInterActiveChat() {
        try (Scanner scanner = new Scanner(System.in);
             PrintWriter fileWriter = new PrintWriter(new FileWriter("conversation.txt", true))) {
            while (true) {
                System.out.println("请输入您的问题（输入q退出）：");
                String question = scanner.nextLine().trim();
                if ("q".equalsIgnoreCase(question)) {
                    System.out.println("程序退出");
                    break;
                }
                // 保存问题
                saveToFile(fileWriter, question, true);
                // 发起对话请求
                Flux<String> responseFlux = sendChatRequest(question);
                StringBuilder fullResponse = new StringBuilder();
                responseFlux.doOnNext(chunk -> {
                    System.out.print(chunk);
                    fullResponse.append(chunk);
                }).doOnComplete(() -> {
                    // 保存完整回复
                    saveToFile(fileWriter, fullResponse.toString(), false);
                    System.out.println("\n--------------------------------------");
                    fileWriter.println("\n--------------------------------------");
                    fileWriter.flush();
                }).blockLast();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Flux<String> handleChatWebSocket(String question) {
        return sendChatRequest(question)
                .doOnNext(response -> log.info("发送响应: {}", response))
                .onErrorResume(e -> {
                    log.error("WebSocket通信错误: {}", e.getMessage());
                    return Flux.just("抱歉，服务器处理请求时发生错误，请稍后重试。");
                });
    }

    private Flux<String> sendChatRequest(String question) {
        ChatRequest request = new ChatRequest();
        Message userMessage = new Message();
        userMessage.setRole("user");
        userMessage.setContent(question);
        request.setMessages(Collections.singletonList(userMessage));
        log.info("用户发送请求到：{}", config.getApiUrl());
        return webClientBuilder.build()
                .post()
                .uri(config.getApiUrl())
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(60))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof PrematureCloseException ||
                                throwable instanceof TimeoutException ||
                                throwable instanceof RuntimeException)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            log.error("重试次数已用完，最后一次错误：{}", retrySignal.failure().getMessage());
                            return new RuntimeException("服务暂时不可用，请稍后重试");
                        })
                ).onErrorResume(e -> {
                    log.error("请求处理错误:{}", e.getMessage());
                    return Flux.just("抱歉，服务器处理请求时发生错误：" + e.getMessage());
                })
                .map(response -> {
                    try {
                        if ("[DONE]".equals(response)) {
                            return "\n";
                        }

                        JsonNode jsonNode = objectMapper.readTree(response);
                        JsonNode choises = jsonNode.get("choises");
                        if (choises != null && choises.isArray() && choises.size() > 0) {
                            JsonNode choise = choises.get(0);
                            JsonNode delta = choise.get("delta");
                            if (delta != null && delta.has("content")) {
                                String content = delta.get("content").asText();
                                return content == null ? "" : content;
                            }
                        }
                        return "";
                    } catch (Exception e) {
                        log.error("解析响应时出错: {}", e.getMessage());
                        log.error("原始响应: {}", response);
                        return "";
                    }
                }).filter(content -> !content.isEmpty());


    }

    private void saveToFile(PrintWriter fileWriter, String content, boolean isQuestion) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        if (isQuestion) {
            fileWriter.printf("\n[%s] Question: \n%s\n\n[%s] Answer:\n", timestamp, content, timestamp);
        } else {
            fileWriter.print(isQuestion);
        }
        fileWriter.flush();
    }
}
