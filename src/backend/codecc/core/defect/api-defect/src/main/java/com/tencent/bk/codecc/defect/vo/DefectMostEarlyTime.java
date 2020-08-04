package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("最早修复时间视图")
public class DefectMostEarlyTime{

    @ApiModelProperty("修复动作[修复、忽略、屏蔽]")
    private Integer action;

    @ApiModelProperty("最早修复时间")
    private Long time;

}