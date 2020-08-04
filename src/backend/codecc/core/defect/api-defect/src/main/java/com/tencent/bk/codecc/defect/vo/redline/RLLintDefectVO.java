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

package com.tencent.bk.codecc.defect.vo.redline;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

/**
 * LINT类工具红线告警
 *
 * @version V1.0
 * @date 2019/7/4
 */
@Data
@ApiModel("LINT类工具红线告警")
public class RLLintDefectVO
{

    @ApiModelProperty("接入后严重告警数")
    private long newSerious;

    @ApiModelProperty("接入后一般告警数")
    private long newNormal;

    @ApiModelProperty("接入后提示告警数")
    private long newPrompt;

    @ApiModelProperty("接入前严重告警数")
    private long historySerious;

    @ApiModelProperty("接入前一般告警数")
    private long historyNormal;

    @ApiModelProperty("接入前提示告警数")
    private long historyPrompt;

    @ApiModelProperty("接入前文件规则包告警数")
    private Map<String, Long> newCheckerPkgCounts;

    /**
     * 接入后文件规则包告警数 [ key： pkg_id  value: initValue ]
     */
    @ApiModelProperty("接入后文件规则包告警数")
    private Map<String, Long> historyCheckerPkgCounts;

    /**
     * 规则新增告警数
     */
    @ApiModelProperty("规则新增告警数")
    private Map<String, Long> newCheckerCounts;

    /**
     * 规则历史遗留告警数
     */
    @ApiModelProperty("规则历史遗留告警数")
    private Map<String, Long> historyCheckerCounts;
}
