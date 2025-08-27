package com.xielaoban.aiagent.agent;


import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.xielaoban.aiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReactAgent{
    //可以使用的工具
    private final ToolCallback[] tools;
    //保存了工具调用信息的响应
    private ChatResponse tooCallChatResponse;

    //工具调用管理者
    private final ToolCallingManager toolCallingManager;
    //禁用内置的工具调用机制，自己控制工具调用
    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] tools) {
        this.tools = tools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false)
                .build();
    }

    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动
     */
    @Override
    public boolean think() {
        //校验
        if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList,chatOptions);

        try {
            //获取带工具选项的响应
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(tools)
                    .call()
                    .chatResponse();
            //保存工具调用信息,用于Act
            this.tooCallChatResponse = chatResponse;
            AssistantMessage assistantMessage = tooCallChatResponse.getResult().getOutput();
            //输出提示信息
            String result = assistantMessage.getText();
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            log.info(getName() + "的思考: " + result);
            log.info(getName() + "选择了 " + toolCallList.size() + " 个工具来使用");
            String toolCallInfo = toolCallList.stream().map(toolCall -> String.format("工具名称：%s，工具参数：%s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            if (toolCallList.isEmpty()) {
                //只有不调用工具时，才记录助手消息
                getMessageList().add(assistantMessage);
                return false;
            }else {
                //需要调用工具时，无需记录助手消息，因为调用工具时会自动记录
                return true;
            }
        } catch (Exception e) {
            log.error(getName() + "的思考过程遇到了问题: " + e.getMessage());
            getMessageList().add(
                    new AssistantMessage("处理时遇到错误: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 执行工具调用并处理结果
     *
     * @return 执行结果
     */
    @Override
    public String act() {
        if (!tooCallChatResponse.hasToolCalls()){
            return "没有需要调用的工具";
        }
        //获取工具调用信息
        Prompt prompt = new Prompt(getMessageList(),chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt,tooCallChatResponse);
        //记录消息上下文，conversationHistory 已经包含了助手消息和工具调用返回的结果
        setMessageList(toolExecutionResult.conversationHistory());
        //获取当前工具调用结果
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        String results = toolResponseMessage.getResponses().stream()
                .map(toolResponse -> "工具名称：" + toolResponse.name() + "，工具返回结果：" + toolResponse.responseData())
                .collect(Collectors.joining("\n"));

        //判断是否调用了终止工具
        boolean isTerminateTool = toolResponseMessage.getResponses().stream()
                        .anyMatch(toolResponse -> "doTerminate".equals(toolResponse.name()));
        if (isTerminateTool) {
            //终止工具调用
            setState(AgentState.FINISHED);
        }
        log.info(getName() + "的执行结果: " + results);
        return results;
    }
}
