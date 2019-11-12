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

package com.tencent.bk.codecc.task.vo;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Pattern;
import java.util.List;

/**
 * 任务的基本信息
 *
 * @version V1.0
 * @date 2019/4/30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("任务的基本信息")
public class TaskBaseVO extends CommonVO
{
    @ApiModelProperty(value = "任务主键id", required = true)
    private long taskId;

    @ApiModelProperty(value = "任务英文名", required = true)
    @Pattern(regexp = "^[0-9a-zA-Z_]{1,50}$", message = "输入的英文名称不符合命名规则")
    private String nameEn;

    @ApiModelProperty(value = "任务中文名", required = true)
    @Pattern(regexp = "^[a-zA-Z0-9_\\u4e00-\\u9fa5]{1,50}", message = "输入的中文名称不符合命名规则")
    private String nameCn;

    @ApiModelProperty(value = "项目ID", required = true)
    private String projectId;

    @ApiModelProperty(value = "项目名称", required = true)
    private String projectName;

    @ApiModelProperty(value = "流水线ID", required = true)
    private String pipelineId;

    @ApiModelProperty(value = "流水线名称", required = true)
    private String pipelineName;

    @ApiModelProperty(value = "代码语言", required = true)
    private Long codeLang;

    @ApiModelProperty(value = "任务负责人", required = true)
    private List<String> taskOwner;

    @ApiModelProperty(value = "任务状态", required = true)
    private Integer status;

    @ApiModelProperty(value = "创建来源", required = true)
    private String createFrom;

    @ApiModelProperty(value = "定时任务执行时间")
    private String executeTime;

    @ApiModelProperty(value = "定时任务执行时间")
    private List<String> executeDate;

    @ApiModelProperty(value = "启用的工具列表", required = true)
    private List<ToolConfigBaseVO> enableToolList;

    @ApiModelProperty(value = "停用的工具列表", required = true)
    private List<ToolConfigBaseVO> disableToolList;
}
