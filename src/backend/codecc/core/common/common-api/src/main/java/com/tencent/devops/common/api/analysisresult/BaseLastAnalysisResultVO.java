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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 最近一次分析结果接口类
 *
 * @version V1.0
 * @date 2019/5/2
 */
@Data
@ApiModel("公共告警详情查询请求视图")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "pattern", visible = true)
@JsonSubTypes({@JsonSubTypes.Type(value = LintLastAnalysisResultVO.class, name = "LINT"),
        @JsonSubTypes.Type(value = CCNLastAnalysisResultVO.class, name = "CCN"),
        @JsonSubTypes.Type(value = DUPCLastAnalysisResultVO.class, name = "DUPC"),
        @JsonSubTypes.Type(value = CommonLastAnalysisResultVO.class, name = "COVERITY"),
        @JsonSubTypes.Type(value = CLOCLastAnalysisResultVO.class, name = "CLOC")
})
public class BaseLastAnalysisResultVO
{
    private String pattern;

    @ApiModelProperty("分析版本号")
    private String analysisVersion;

    @ApiModelProperty("统计的时间")
    private long time;

    @ApiModelProperty("告警总数")
    private Integer defectCount;

    @ApiModelProperty("告警变化数")
    private Integer defectChange;
}
