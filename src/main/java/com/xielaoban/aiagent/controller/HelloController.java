package com.xielaoban.aiagent.controller;


import com.xielaoban.aiagent.common.BaseResponse;
import com.xielaoban.aiagent.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HelloController {

    @GetMapping()
    public BaseResponse<String> healthCheck(){
        return ResultUtils.success("你ok吗");
    }
}
