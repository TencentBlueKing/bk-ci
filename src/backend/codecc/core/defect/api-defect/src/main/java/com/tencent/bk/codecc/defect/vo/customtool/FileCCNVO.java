package com.tencent.bk.codecc.defect.vo.customtool;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FileCCNVO
{
    /**
     * 文件路径
     */
    @JsonProperty("file_path")
    private String filePath;

    /**
     * 文件相对路径
     */
    @JsonProperty("file_rel_path")
    private String fileRelPath;

    /**
     * 文件所有函数圈复杂度之和
     */
    @JsonProperty("total_ccn_count")
    private String totalCCNCount;
}
