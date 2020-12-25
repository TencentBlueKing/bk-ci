package com.tencent.bk.codecc.apiquery.defect.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Set;

/**
 * 描述
 *
 * @version V1.0
 * @date 2019/12/16
 */
@Data
public class BuildDefectModel
{
    /**
     * 工具名称
     */
    @JsonProperty("task_id")
    private Long taskId;

    /**
     * 工具名称
     */
    @JsonProperty("tool_name")
    private String toolName;

    /**
     * 构建ID
     */
    @JsonProperty("build_id")
    private String buildId;

    /**
     * 构建号
     */
    @JsonProperty("build_num")
    private String buildNum;

    /**
     * 文件相对路径
     */
    @JsonProperty("file_rel_path")
    private String fileRelPath;

    /**
     * 文件路径
     */
    @JsonProperty("file_path")
    private String filePath;

    /**
     * Coverity、Klocwork、Pinpoint、CCN工具快照字段，告警唯一ID
     */
    @JsonProperty("defect_id")
    private String defectId;

    /**
     * Lint类工具快照字段----文件包含的告警唯一ID列表
     */
    @JsonProperty("file_defect_ids")
    private Set<String> fileDefectIds;
}
