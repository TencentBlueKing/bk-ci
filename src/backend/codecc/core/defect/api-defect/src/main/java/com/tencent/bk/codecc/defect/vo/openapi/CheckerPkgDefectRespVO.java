package com.tencent.bk.codecc.defect.vo.openapi;

import com.tencent.devops.common.api.pojo.Page;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

/**
 * 工具规则包告警统计响应视图
 *
 * @date 2019/11/13
 * @version V1.0
 */

@Data
@ApiModel("工具规则包告警统计响应视图")
public class CheckerPkgDefectRespVO
{
    @ApiModelProperty("按规则包维度统计告警数")
    private Map<String, PkgDefectDetailVO> statisticsChecker;

    @ApiModelProperty("按任务维度统计告警数")
    private Page<TaskDefectVO> statisticsTask;

    @ApiModelProperty("规则数")
    private Integer checkerCount;

    @ApiModelProperty("任务数")
    private Integer taskCount;

}
