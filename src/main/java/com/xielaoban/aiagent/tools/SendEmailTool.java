package com.xielaoban.aiagent.tools;


import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

@Component
public class SendEmailTool {

    private final JavaMailSender javaMailSender;
    public SendEmailTool(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }


    @Tool(description = "根据发送方、接收方、标题和正文发送邮件",returnDirect = true)
    public String sendMail(
            @ToolParam(description = "发送方") String from,
            @ToolParam(description = "接收方") String to,
            @ToolParam(description = "标题") String title,
            @ToolParam(description = "正文") String content){
        try {
            SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
            simpleMailMessage.setFrom(from);
            simpleMailMessage.setTo(to);
            simpleMailMessage.setSubject(title);
            simpleMailMessage.setText(content);
            javaMailSender.send(simpleMailMessage);
            return "发送成功";
        } catch (MailException e) {
            return "发送失败"+e.getMessage();
        }
    }

    public static void main(String[] args) {
        SendEmailTool sendEmailTool = new SendEmailTool(new JavaMailSenderImpl());
        String result = sendEmailTool.sendMail("3012685723@qq.com", "1259878466@qq.com", "测试邮件", "这是一封测试邮件");
        System.out.println(result);
    }
}
