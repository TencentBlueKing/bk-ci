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

/**
 * 工具侧上报分析的结果统计数据的请求对象
 *
 * @version V2.6
 * @date 2018/1/9
 */
@Data
@ApiModel("工具侧上报分析的结果统计数据的请求对象")
public class UploadDUPCStatisticVO
{
    /**
     * 流名称,必填
     */
    @ApiModelProperty(value = "流名称", required = true)
    @JsonProperty("stream_name")
    private String streamName;

    @ApiModelProperty(value = "任务id", required = true)
    @JsonProperty("task_id")
    private Long taskId;

    /**
     * 重复率上报的分析结果统计
     */
    @ApiModelProperty(value = "分析结果统计数据", required = true)
    @JsonProperty("scan_summary")
    private DUPCScanSummaryVO scanSummary;

    /**
     * 构建ID
     */
    @ApiModelProperty(value = "构建ID", required = true)
    @JsonProperty("build_id")
    private String buildId;
}
