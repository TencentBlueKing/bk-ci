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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.tencent.bk.codecc.defect.vo.CCNDataReportRspVO;
import com.tencent.bk.codecc.defect.vo.CovKlocDataReportRspVO;
import com.tencent.bk.codecc.defect.vo.DupcDataReportRspVO;
import com.tencent.bk.codecc.defect.vo.LintDataReportRspVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 数据报表返回视图
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Data
@ApiModel("数据报表返回视图")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "toolName", visible = true, include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({@JsonSubTypes.Type(value = LintDataReportRspVO.class, name = "ESLINT"),
        @JsonSubTypes.Type(value = LintDataReportRspVO.class, name = "SPOTBUGS"),
        @JsonSubTypes.Type(value = LintDataReportRspVO.class, name = "CHECKSTYLE"),
        @JsonSubTypes.Type(value = LintDataReportRspVO.class, name = "STYLECOP"),
        @JsonSubTypes.Type(value = LintDataReportRspVO.class, name = "GOML"),
        @JsonSubTypes.Type(value = LintDataReportRspVO.class, name = "DETEKT"),
        @JsonSubTypes.Type(value = LintDataReportRspVO.class, name = "PHPCS"),
        @JsonSubTypes.Type(value = LintDataReportRspVO.class, name = "PYLINT"),
        @JsonSubTypes.Type(value = LintDataReportRspVO.class, name = "OCCHECK"),
        @JsonSubTypes.Type(value = LintDataReportRspVO.class, name = "CPPLINT"),
        @JsonSubTypes.Type(value = LintDataReportRspVO.class, name = "SENSITIVE"),
        @JsonSubTypes.Type(value = LintDataReportRspVO.class, name = "HORUSPY"),
        @JsonSubTypes.Type(value = LintDataReportRspVO.class, name = "WOODPECKER_SENSITIVE"),
        @JsonSubTypes.Type(value = CCNDataReportRspVO.class, name = "CCN"),
        @JsonSubTypes.Type(value = DupcDataReportRspVO.class, name = "DUPC"),
        @JsonSubTypes.Type(value = CovKlocDataReportRspVO.class, name = "COVERITY"),
        @JsonSubTypes.Type(value = CovKlocDataReportRspVO.class, name = "KLOCWORK")
})
public class CommonDataReportRspVO
{

    @ApiModelProperty("任务主键")
    private Long taskId;

    @ApiModelProperty("工具名")
    private String toolName;

}
