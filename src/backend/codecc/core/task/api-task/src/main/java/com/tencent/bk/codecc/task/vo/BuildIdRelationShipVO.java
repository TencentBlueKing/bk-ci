package com.tencent.bk.codecc.task.vo;

import lombok.Data;

@Data
public class BuildIdRelationShipVO {
    private Long taskId;

    private String codeccBuildId;

    private String pipelineId;

    private String buildId;

    private String buildNum;

    private Integer status;

    private Long elapseTime;

    private String commitId;

    private Boolean firstTrigger;

    private String errMsg;
}
