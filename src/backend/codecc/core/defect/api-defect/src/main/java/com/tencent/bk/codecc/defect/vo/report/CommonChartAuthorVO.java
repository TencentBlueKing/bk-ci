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

package com.tencent.bk.codecc.defect.vo.report;

import com.tencent.devops.common.constant.ComConstants;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 数据报表作者列表
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Data
@ApiModel("数据报表作者列表")
public class CommonChartAuthorVO extends ChartAuthorBaseVO
{

    @ApiModelProperty("严重数量")
    private Integer serious;

    @ApiModelProperty("一般数量")
    private Integer normal;

    @ApiModelProperty("提示数量")
    private Integer prompt;

    //@ApiModelProperty("提示语")
    //private String tips;

    public CommonChartAuthorVO()
    {
        total = 0;
        serious = 0;
        normal = 0;
        prompt = 0;
        //tips = "no data!";
    }

    @Override
    public Integer getTotal()
    {
        return serious + normal + prompt;
    }

    public void count(int severity)
    {
        if (severity == ComConstants.SERIOUS)
        {
            serious++;
        }
        else if (severity == ComConstants.NORMAL)
        {
            normal++;
        }
        else if (severity == ComConstants.PROMPT || severity == ComConstants.PROMPT_IN_DB)
        {
            prompt++;
        }
    }
}
