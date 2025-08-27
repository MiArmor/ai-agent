package com.xielaoban.aiagent.agent;

import com.xielaoban.aiagent.agent.model.AgentState;
import com.xielaoban.aiagent.execption.AgentException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.internal.StringUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 抽象基础代理类，用于管理代理状态和执行流程。
 *
 * 提供状态转换、基于步骤的执行循环的基础功能。
 * 子类必须实现step方法。
 */

@Data
@Slf4j
public abstract class BaseAgent {

    //智能体名称
    private String name;

    //提示词 prompt(系统提示词以及下一步提示词，用于衔接)
    private String systemPrompt;
    private String nextStepPrompt;

    //状态
    private AgentState state = AgentState.IDLE;

    //执行控制
    private int maxSteps = 10;
    private int currentStep = 0;

    //LLM
    private ChatClient chatClient;

    //Memory 需要自主维护会话上下文
    private List<Message> messageList = new ArrayList<>();


    /**
     * 执行代理任务
     *
     * @param userPrompt 用户提示词
     */
    public String run(String userPrompt){
        // 校验
        if (this.state != AgentState.IDLE){
            throw new AgentException("Cannot run agent in state: " + this.state);
        }
        if (StringUtil.isBlank(userPrompt)){
            throw new AgentException("Cannot run agent with empty userPrompt");
        }
        // 更改状态
        state = AgentState.RUNNING;
        // 记录上下文消息
        messageList.add(new UserMessage(userPrompt));
        // 保存结果列表
        List<String> resultList = new ArrayList<>();

        try {
            while(currentStep < maxSteps && state != AgentState.FINISHED){
                currentStep++;
                log.info("当前正在执行第: " + currentStep + "步，/" + maxSteps);
                //单步执行
                String stepResult = step();
                String result = "执行的第" + currentStep + "步，结果: " + stepResult;
                resultList.add(result);
            }
            //检查是否超出步骤限制
            if (currentStep >= maxSteps){
                state = AgentState.FINISHED;
                resultList.add("执行超出了最大步骤("+maxSteps+")限制");
            }
            //返回结果
            return String.join("\n", resultList);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("Agent encountered an error: ", e);
            return "Agent执行错误！！！" + e.getMessage();
        } finally {
            this.clean();
        }
    }

    /**
     * 运行代理（流式输出）
     *
     * @param userPrompt 用户提示词
     * @return SseEmitter实例
     */
    public SseEmitter runStream(String userPrompt) {
        // 创建SseEmitter，设置较长的超时时间
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        // 使用线程异步处理，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                if (this.state != AgentState.IDLE) {
                    emitter.send("错误：无法从状态运行代理: " + this.state);
                    emitter.complete();
                    return;
                }
                if (StringUtil.isBlank(userPrompt)) {
                    emitter.send("错误：不能使用空提示词运行代理");
                    emitter.complete();
                    return;
                }

                // 更改状态
                state = AgentState.RUNNING;
                // 记录消息上下文
                messageList.add(new UserMessage(userPrompt));

                try {
                    for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                        int stepNumber = i + 1;
                        currentStep = stepNumber;
                        log.info("Executing step " + stepNumber + "/" + maxSteps);

                        // 单步执行
                        String stepResult = step();
                        String result = "Step " + stepNumber + ": " + stepResult;

                        // 发送每一步的结果
                        emitter.send(result);
                    }
                    // 检查是否超出步骤限制
                    if (currentStep >= maxSteps) {
                        state = AgentState.FINISHED;
                        emitter.send("执行结束: 达到最大步骤 (" + maxSteps + ")");
                    }
                    // 正常完成
                    emitter.complete();
                } catch (Exception e) {
                    state = AgentState.ERROR;
                    log.error("执行智能体失败", e);
                    try {
                        emitter.send("执行错误: " + e.getMessage());
                        emitter.complete();
                    } catch (Exception ex) {
                        emitter.completeWithError(ex);
                    }
                } finally {
                    // 清理资源
                    this.clean();
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.clean();
            log.warn("SSE connection timed out");
        });

        emitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.clean();
            log.info("SSE connection completed");
        });

        return emitter;
    }



    /**
     * 清理资源
     */
    protected void clean() {
        //TODO 子类重写

    }

    /**
     * 执行单步任务
     *
     * @return 步骤执行结果
     */
    public abstract String step();

}
