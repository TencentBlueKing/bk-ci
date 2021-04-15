/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;


/**
 * 上报任务分析记录步骤的VO
 *
 * @version V1.0
 * @date 2019/4/30
 */
@Data
@ApiModel("上报任务分析记录步骤的VO")
public class UploadTaskLogStepVO
{
    @ApiModelProperty(value = "流名称", required = true)
    @JsonProperty("stream_name")
    private String streamName;

    @ApiModelProperty(value = "任务主键id")
    @JsonProperty("task_id")
    private long taskId;

    @ApiModelProperty(value = "工具名（ID）", required = true)
    private String toolName;

    @NotNull(message = "构建Id不能为空")
    @ApiModelProperty(value = "构建Id", required = true)
    private String pipelineBuildId;

    @ApiModelProperty(value = "触发来源,用于保存任务手动触发的触发人")
    private String triggerFrom;

    @ApiModelProperty(value = "当前步骤", required = true)
    private int stepNum;

    @ApiModelProperty(value = "步骤状态", required = true)
    private int flag;

    @ApiModelProperty(value = "开始时间", required = true)
    private long startTime;

    @ApiModelProperty(value = "结束时间")
    private long endTime;

    @ApiModelProperty(value = "步骤信息")
    private String msg;

    @ApiModelProperty(value = "步骤耗时")
    private long elapseTime;

    @ApiModelProperty(value = "GOML目录结构是否符合规范,true/false")
    private Boolean dirStructSuggestParam;

    @ApiModelProperty(value = "GOML编译是否成功，true（成功）/false（失败）")
    private Boolean compileResult;

    @ApiModelProperty(value = "流水线运行失败")
    private Boolean pipelineFail;

    /**
     * 是否是快速增量, 用于判断是否需要执行提单等操作
     * true: 是快速增量扫描，不需要做提单等操作
     * false:不是快速增量扫描，需要做提单等操作
     */
    private boolean fastIncrement;

    /**
     * 是否是最后一步并且状态成功
     * 在告警上报完成后写入 True，在这之前都是未完成状态
     * 用于切面
     */
    private boolean isFinish = false;
}
