package com.tencent.bk.codecc.defect.vo.openapi;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Cov告警列表扩展视图(添翼)
 *
 * @version V1.0
 * @date 2019/12/6
 */

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("Cov告警列表扩展视图")
public class DefectDetailExtVO extends DefectDetailVO
{
    @ApiModelProperty("告警唯一标识")
    private String id;

    @ApiModelProperty("任务ID")
    private Long taskId;

    @ApiModelProperty("规则类型")
    private String displayCategory;

    @ApiModelProperty("规则子类")
    private String displayType;

    @ApiModelProperty("告警创建时间")
    private long createTime;

    @ApiModelProperty("告警修复时间")
    private long fixedTime;

    @ApiModelProperty("告警忽略时间")
    private long ignoreTime;

    @ApiModelProperty("告警屏蔽时间")
    private long excludeTime;


}
