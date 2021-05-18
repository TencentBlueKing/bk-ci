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

package com.tencent.bk.codecc.defect.vo.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 工具快照视图
 *
 * @version V1.0
 * @date 2019/6/28
 */
@Data
@ApiModel("工具快照视图")
public class ToolSnapShotVO
{
    @ApiModelProperty("工具中文名")
    @JsonProperty("tool_name_cn")
    private String toolNameCn;

    @ApiModelProperty("工具英文名")
    @JsonProperty("tool_name_en")
    private String toolNameEn;

    @ApiModelProperty("工具告警详情页面")
    @JsonProperty("defect_detail_url")
    private String defectDetailUrl;

    @ApiModelProperty("工具报表页面")
    @JsonProperty("defect_report_url")
    private String defectReportUrl;

    @ApiModelProperty("工具分析结果状态")
    @JsonProperty("result_status")
    private String resultStatus;

    @ApiModelProperty("工具分析结果状态描述")
    @JsonProperty("result_message")
    private String resultMessage;

}
