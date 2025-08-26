package com.xielaoban.aiagent.app;

import com.xielaoban.aiagent.advisor.MyLoggerAdvisor;
import com.xielaoban.aiagent.chatMemory.FileBasedChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;


import java.util.List;

@Slf4j
@Component
public class TravelApp {
    private final ChatClient chatClient;

    @Resource
    private ToolCallback[] allTools;


    private static final String SYSTEM_PROMPT = """
            你是一名专业的AI旅行规划师，名为TravelMaster。你的核心职责是为用户提供个性化、安全可靠的旅行方案。
            【核心原则】
            以用户为中心：主动询问偏好、预算、时间等关键信息。
            安全第一：提供准确信息，并建议用户核查官方来源。
            激发灵感：用热情专业的语言描绘目的地，拒绝模板化行程。
            【关键能力】
            能处理完整行程规划、目的地深度游、主题建议、实时问答和预算规划等多种场景。
            【输出要求】
            回复可以包含：
            （1）行程亮点与每日详细安排（含时间、活动、贴士）。
            （2）住宿与美食推荐。
            （3）重要提醒（天气情况等），并附上免责声明。
            语气需友好且专业，适当使用表情符号增强可读性，精简回答内容，避免冗长多余。
            """;

//    private final VectorStore loveAppVectorStore;
    @Resource
    private VectorStore pgVectorVectorStore;

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /*
     *初始化AI客户端
     */
    public TravelApp(ChatModel dashscopeChatModel, VectorStore loveAppVectorStore) {
        String fileDir = System.getProperty("user.dir") + "/chat-memory";
        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        ChatMemory chatMemory1 = MessageWindowChatMemory
                .builder()
                //可注释下面的选项
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 自定义消息文件持久化
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        //自定义日志
                        new MyLoggerAdvisor()
                        //Re2技术，自定义推理增强技术，可按需开启
//                        ,new ReReadingAdvisor()
                ).build();
    }

    /**
     *
     *AI基础对话，支持多轮
     */
    public String doChat(String message,String chatId) {
        ChatResponse chatResponse = chatClient.prompt().user(message)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
//        log.info("content:{}",content);
        log.info("token:{}",chatResponse.getMetadata().toString());
        return content;
    }


    //记录类,用于AI标准化输出
    record TravelReport(String title, List<String> suggestions) {
    }
    /**
     * AI 旅游报告功能，结构化输出
     * @param message
     * @param chatId
     * @return
     */
    public TravelReport doChatWithReport(String message,String chatId) {
        TravelReport travelReport = chatClient.prompt()
                .system(SYSTEM_PROMPT + "每次对话后都需要生成旅游计划，标题为{用户名}的旅游计划，内容为建议列表")
                .user(message)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(TravelReport.class);
        log.info("travelReport:{}", travelReport);
        return travelReport;
    }

    /**
     * 使用简单的QuestionAnswerAdvisor，实现问答功能
     */
    public String doChatWithRag(String message, String chatId) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatId))
                //应用知识库问答
                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content:{}",content);
        return content;
    }

    @Resource
    private Advisor travelAppRagCloudAdvisor;

    public String doChatWithAliRag(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
//                // 开启日志，便于观察效果
//                .advisors(new MyLoggerAdvisor())
                // 应用增强检索服务（云知识库服务）
                .advisors(travelAppRagCloudAdvisor)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    public String doChatWithTools(String message, String chatId) {
        ChatResponse response = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


    public String doChatWithMcp(String message, String chatId) {
        ChatResponse response = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


    /**
     * 流式调用方法
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatWithStream(String message, String chatId) {
        return chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

}
