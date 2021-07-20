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

package com.tencent.devops.common.api.analysisresult;

import com.tencent.devops.common.api.analysisresult.BaseLastAnalysisResultVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务下的所有有效工具的最近分析结果VO
 *
 * @version V1.0
 * @date 2019/6/8
 */
@Data
@NoArgsConstructor
public class ToolLastAnalysisResultVO
{
    @ApiModelProperty("任务主键id")
    private long taskId;

    @ApiModelProperty("工具名")
    private String toolName;

    @ApiModelProperty("分析开始时间")
    private long startTime;

    @ApiModelProperty("分析结束时间")
    private long endTime;

    @ApiModelProperty("分析耗时")
    private long elapseTime;

    @ApiModelProperty("构建ID")
    private String buildId;

    @ApiModelProperty("构建号")
    private String buildNum;

    @ApiModelProperty("最近分析统计结果")
    private BaseLastAnalysisResultVO lastAnalysisResultVO;

    private int currStep;

    /*
    *
    *   SUCC(1),
        FAIL(2),
        PROCESSING(3),
        ABORT(4);
    *
    * */
    private int flag;

    public ToolLastAnalysisResultVO(long taskId, String toolName)
    {
        this.taskId = taskId;
        this.toolName = toolName;
    }
}
