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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

/**
 * 数据报表的作者列表分布视图
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Data
@ApiModel("数据报表的作者列表分布视图")
public class ChartAuthorListVO
{

    @ApiModelProperty("告警处理人最大值")
    private Integer maxHeight;

    @ApiModelProperty("告警处理人最小值")
    private Integer minHeight;

    @ApiModelProperty("告警作者列表")
    private List<ChartAuthorBaseVO> authorList;

    @ApiModelProperty("告警总计")
    private ChartAuthorBaseVO totalAuthor;


    public void setMaxMinHeight()
    {
        int max = 0;
        int min = 10000;

        if (CollectionUtils.isNotEmpty(authorList))
        {
            for (ChartAuthorBaseVO newAuthor : authorList)
            {
                if (newAuthor.getTotal() > max)
                {
                    max = newAuthor.getTotal();
                }
                if (newAuthor.getTotal() < min)
                {
                    min = newAuthor.getTotal();
                }
            }
        }

        this.maxHeight = max;
        this.minHeight = min;
    }

}
