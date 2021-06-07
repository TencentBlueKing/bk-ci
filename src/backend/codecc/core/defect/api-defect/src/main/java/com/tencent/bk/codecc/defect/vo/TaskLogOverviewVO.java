package com.tencent.bk.codecc.defect.vo;

import com.tencent.devops.common.api.analysisresult.ToolLastAnalysisResultVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Data;
import org.springframework.data.domain.Page;

@Data
@ApiModel("工具执行记录请求对象")
public class TaskLogOverviewVO {
    @ApiModelProperty(value = "任务ID")
    private Long taskId;

    @ApiModelProperty(value = "构建号")
    private String buildId;

    @ApiModelProperty(value = "构建号")
    private String buildNum;

    @ApiModelProperty(value = "任务状态")
    private Integer status;

    @ApiModelProperty("代码库版本号")
    private String version;

    @ApiModelProperty("分析开始时间")
    private Long startTime;

    @ApiModelProperty("分析结束时间")
    private Long endTime;

    @ApiModelProperty("扫描耗时")
    private Long elapseTime;

    @ApiModelProperty("分析触发用户")
    private String buildUser;

    @ApiModelProperty(value = "工具集", required = true)
    private List<String> tools;

    @ApiModelProperty(value = "工具分析信息")
    List<TaskLogVO> taskLogVOList;

    @ApiModelProperty(value = "代码库字符串信息")
    List<String> repoInfoStrList;

    @ApiModelProperty(value = "本次工具分析结果")
    List<ToolLastAnalysisResultVO> lastAnalysisResultVOList;
}
