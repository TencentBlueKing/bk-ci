package com.tencent.bk.codecc.apiquery.defect.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 告警分组统计对象
 *
 * @version V4.0
 * @date 2020/6/15
 */
@Data
public class DefectStatModel
{
    @JsonProperty("task_id")
    private Long taskId;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("severity")
    private Integer severity;

    @JsonProperty("count")
    private Integer count;

}
