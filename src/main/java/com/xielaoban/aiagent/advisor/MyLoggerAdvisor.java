package com.xielaoban.aiagent.advisor;


import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;

/*
 * @Author xielaoban
 * 自定义日志 Advisor
 * 打印info级别日志，只输出单词用户提示词和AI回复的文本
 */




@Slf4j
public class MyLoggerAdvisor implements CallAdvisor, StreamAdvisor {



    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        this.logRequest(chatClientRequest);
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        this.logResponse(chatClientResponse);
        return chatClientResponse;
    }

    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        this.logRequest(chatClientRequest);
        Flux<ChatClientResponse> chatClientResponses = streamAdvisorChain.nextStream(chatClientRequest);
        return (new ChatClientMessageAggregator()).aggregateChatClientResponse(chatClientResponses, this::logResponse);
    }


    private void logRequest(ChatClientRequest request) {
        log.info("AI request: {}",request.prompt().getUserMessage().getText());
    }

    private void logResponse(ChatClientResponse chatClientResponse) {
        log.info("AI response: {}", chatClientResponse.chatResponse().getResult().getOutput().getText());
    }

    public String getName() {
//        return this.getClass().getSimpleName();
        return "蟹老板自定义LogAdvisors";
    }

    public int getOrder() {
        return 0;
    }
}


