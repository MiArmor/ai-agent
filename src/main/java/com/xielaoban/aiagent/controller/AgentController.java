package com.xielaoban.aiagent.controller;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.xielaoban.aiagent.agent.Manus;
import com.xielaoban.aiagent.app.TravelApp;
import com.xielaoban.aiagent.common.BaseResponse;
import com.xielaoban.aiagent.common.ResultUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/agent")
public class AgentController {

    @Autowired
    private TravelApp travelApp;
    @Autowired
    private ToolCallback[] tools;
    @Autowired
    private DashScopeChatModel dashscopeChatModel;


    /**
     * 同步调用，待结果生成完毕，一次性返回
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("love_app/chat/sync")
    public BaseResponse<String> doChatWithLoveAppSync(String message,String chatId) {
        return ResultUtils.success(travelApp.doChat(message,chatId));
    }

    /**
     * 流式调用，类似于打字机效果
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "love_app/chat/sse",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(String message,String chatId) {
        return travelApp.doChatWithStream(message,chatId);
    }

    /**
     * 流式调用 Manus超级智能体
     * @param message
     * @return
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        //每次对话都要创建一个新的实例
        Manus manus = new Manus(tools,dashscopeChatModel);
        return manus.runStream(message);
    }


}
