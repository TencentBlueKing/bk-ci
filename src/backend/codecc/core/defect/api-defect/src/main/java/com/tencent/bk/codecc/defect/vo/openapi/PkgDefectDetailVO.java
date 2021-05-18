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

package com.tencent.bk.codecc.defect.vo.openapi;

import com.tencent.bk.codecc.defect.vo.report.CommonChartAuthorVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 规则维度的告警统计视图
 *
 * @version V1.0
 * @date 2019/11/14
 */

@Data
@ApiModel("规则维度的告警统计视图")
public class PkgDefectDetailVO
{
    @ApiModelProperty("规则名称")
    private String checkerName;

    @ApiModelProperty("开启该规则的项目数")
    private Integer taskCount;

    @ApiModelProperty("已忽略告警数")
    private IgnoreDefectVO ignoreCount;

    @ApiModelProperty("已修复告警数")
    private CommonChartAuthorVO fixedCount;

    @ApiModelProperty("待修复告警数")
    private CommonChartAuthorVO existCount;

    @ApiModelProperty("已屏蔽告警数")
    private CommonChartAuthorVO excludedCount;

    public PkgDefectDetailVO()
    {
        taskCount = 0;
        ignoreCount = new IgnoreDefectVO();
        fixedCount = new CommonChartAuthorVO();
        existCount = new CommonChartAuthorVO();
        excludedCount = new CommonChartAuthorVO();
    }

    public void addTaskCount()
    {
        taskCount++;
    }
}
