package com.xielaoban.aiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.io.exceptions.IOException;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.xielaoban.aiagent.constant.FileConstant;
import com.xielaoban.aiagent.utils.ALiYunOssOperator;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;

@Component
public class PDFGenerationTool {

    private final ALiYunOssOperator aliyunOSSOperator;
    public PDFGenerationTool(ALiYunOssOperator aliyunOSSOperator) {
        this.aliyunOSSOperator = aliyunOSSOperator;
    }

//    @Resource
//    private ALiYunOssOperator aliyunOSSOperator;

    @Tool(description = "Generate a PDF file with given content",returnDirect = true) // returnDirect直接将结果返回给调用者，不再返回模型
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content) {
        String fileDir = FileConstant.FILE_SAVE_DIR + "\\pdf";
        String filePath = fileDir + "\\" + fileName;
        String url;
        try {
            // 创建目录
            FileUtil.mkdir(fileDir);
            // 创建 PdfWriter 和 PdfDocument 对象
            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {
                // 自定义字体（需要人工下载字体文件到特定目录）
                String fontPath = Paths.get("src/main/resources/static/fonts/SimSun.ttf")
                        .toAbsolutePath().toString();
                PdfFont font = PdfFontFactory.createFont(fontPath,
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                // 使用内置中文字体
//                PdfFont font = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
                document.setFont(font);
                // 创建段落
                Paragraph paragraph = new Paragraph(content);
                // 添加段落并关闭文档
                document.add(paragraph);
            }
            url = aliyunOSSOperator.uploadFile(fileName, FileUtil.readBytes(filePath));
            return "PDF generated successfully to: " + filePath + "OSS URL: "+url ;
        } catch (IOException | java.io.IOException e) {
            return "Error generating PDF: " + e.getMessage();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
