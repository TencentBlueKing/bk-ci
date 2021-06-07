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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

/**
 * 告警可跟踪的工具最近一次分析结果
 *
 * @version V1.0
 * @date 2019/10/27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("lint类工具最近一次分析结果")
public class CommonLastAnalysisResultVO extends BaseLastAnalysisResultVO
{
    @ApiModelProperty("新增数")
    private Integer newCount;

    @ApiModelProperty("遗留总数")
    private Integer existCount;

    @ApiModelProperty("关闭数")
    private Integer closeCount;

    @ApiModelProperty("修复数")
    private Integer fixedCount;

    @ApiModelProperty("屏蔽数")
    private Integer excludeCount;

    @ApiModelProperty("遗留提示告警总数")
    private Integer existPromptCount;

    @ApiModelProperty("遗留一般告警总数")
    private Integer existNormalCount;

    @ApiModelProperty("遗留严重告警总数")
    private Integer existSeriousCount;

    @ApiModelProperty("新增提示告警总数")
    private Integer newPromptCount;

    @ApiModelProperty("新增一般告警总数")
    private Integer newNormalCount;

    @ApiModelProperty("新增严重告警总数")
    private Integer newSeriousCount;

    @ApiModelProperty("新增告警处理人")
    private Set<String> newAuthors;

    @ApiModelProperty("遗留告警处理人")
    private Set<String> existAuthors;

    @ApiModelProperty("提示告警处理人")
    private Set<String> promptAuthors;

    @ApiModelProperty("一般告警处理人")
    private Set<String> normalAuthors;

    @ApiModelProperty("严重告警处理人")
    private Set<String> seriousAuthors;

    /**
     * 存量提示级别处理人
     */
    @ApiModelProperty("存量提示级别处理人")
    private Set<String> existPromptAuthors;

    /**
     * 存量一般级别处理人
     */
    @ApiModelProperty("存量一般级别处理人")
    private Set<String> existNormalAuthors;

    /**
     * 存量严重级别处理人
     */
    @ApiModelProperty("存量严重级别处理人")
    private Set<String> existSeriousAuthors;
}
