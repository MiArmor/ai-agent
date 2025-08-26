package com.xielaoban.aiagent.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import com.xielaoban.aiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class FileOperationTool {
    private final String FILE_DIR = FileConstant.FILE_SAVE_DIR + "\\files";

    @Tool(description = "读取文件内容")
    public String readFile(@ToolParam(description = "读取的文件名") String fileName) {
        String path = FILE_DIR + "\\" + fileName;
        try {
            return FileUtil.readUtf8String(path);
        } catch (Exception e) {
            return "读取文件失败"+e.getMessage();
        }
    }

    @Tool(description = "写入文件内容")
    public String  writeFile(@ToolParam(description = "写入的文件名") String fileName,
                             @ToolParam(description = "写入的内容") String content) {
        String path = FILE_DIR + "/" + fileName;

        try {
            FileUtil.mkdir(FILE_DIR);
            FileUtil.writeUtf8String(content, path);
            return "写入文件成功"+path;
        } catch (Exception e) {
            return "写入文件失败"+e.getMessage();
        }
    }

}
