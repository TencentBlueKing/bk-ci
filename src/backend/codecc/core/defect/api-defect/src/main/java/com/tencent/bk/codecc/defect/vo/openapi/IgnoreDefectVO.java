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

import com.tencent.devops.common.constant.ComConstants;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 各忽略类型的告警数
 *
 * @version V1.0
 * @date 2019/11/13
 */


@Data
@ApiModel("各忽略类型的告警数")
public class IgnoreDefectVO
{
    @ApiModelProperty("已忽略的误报告警数")
    private Integer error;

    @ApiModelProperty("已忽略的设计如此告警数")
    private Integer special;

    @ApiModelProperty("已忽略的其他原因告警数")
    private Integer other;


    IgnoreDefectVO()
    {
        error = 0;
        special = 0;
        other = 0;
    }

    public int getTotal()
    {
        return error + special + other;
    }

    public void count(int ignoreReasonType)
    {
        if (ComConstants.IgnoreReasonType.ERROR_DETECT.value() == ignoreReasonType)
        {
            error++;
        }
        else if (ComConstants.IgnoreReasonType.SPECIAL_PURPOSE.value() == ignoreReasonType)
        {
            special++;
        }
        else if (ComConstants.IgnoreReasonType.OTHER.value() == ignoreReasonType)
        {
            other++;
        }
    }
}
