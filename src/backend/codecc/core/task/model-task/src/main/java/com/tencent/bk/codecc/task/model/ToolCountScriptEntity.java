package com.tencent.bk.codecc.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 工具数量实体类
 *
 * @version V1.0
 * @date 2020/12/07
 */
@Data
public class ToolCountScriptEntity {
    /**
     * 创建时间
     */
    @Field("create_date")
    private String createDate;

    /**
     * 状态
     */
    @Field("follow_status")
    private Integer followStatus;

    /**
     * 数量
     */
    @Field("count")
    private Integer count;

    /**
     * 工具名
     */
    @Field("tool_name")
    private String toolName;

    /**
     * 任务id
     */
    @Field("task_id")
    private Long taskId;
}