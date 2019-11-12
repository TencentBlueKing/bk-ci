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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 数据报表告警遗留列表视图[折线图]
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Data
@ApiModel("数据报表告警遗留列表视图[折线图]")
public class ChartLegacyListVO
{

    @ApiModelProperty("新告警图表展示的最高数量")
    private Integer newMaxHeight;

    @ApiModelProperty("新告警图表展示的最低数量")
    private Integer newMinHeight;

    @ApiModelProperty("历史告警图表展示的最高数量")
    private Integer historyMaxHeight;

    @ApiModelProperty("历史告警图表展示的最低数量")
    private Integer historyMinHeight;

    @ApiModelProperty("告警遗留列表")
    private List<ChartLegacyVO> legacyList;


    public void setMaxMinHeight()
    {
        int newMax = 0;
        int newMin = 10000;
        int oldMax = 0;
        int oldMin = 10000;
        for (ChartLegacyVO author : legacyList)
        {
            if (author.getNewCount() > newMax)
            {
                newMax = author.getNewCount();
            }
            if (author.getNewCount() < newMin)
            {
                newMin = author.getNewCount();
            }

            if (author.getHistoryCount() > oldMax)
            {
                oldMax = author.getHistoryCount();
            }
            if (author.getHistoryCount() < oldMin)
            {
                oldMin = author.getHistoryCount();
            }
        }
        this.newMaxHeight = newMax;
        this.newMinHeight = newMin;
        this.historyMaxHeight = oldMax;
        this.historyMinHeight = oldMin;
    }

}
