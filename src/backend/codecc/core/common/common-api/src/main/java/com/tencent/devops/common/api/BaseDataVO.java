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

package com.tencent.devops.common.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 基础数据视图类
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("基础数据视图类")
public class BaseDataVO extends CommonVO
{
    @ApiModelProperty("参数代码")
    private String paramCode;

    @ApiModelProperty("参数名称")
    private String paramName;

    @ApiModelProperty("参数值")
    private String paramValue;

    @ApiModelProperty("参数类型")
    private String paramType;

    @ApiModelProperty("参数状态")
    private String paramStatus;

    @ApiModelProperty("参数扩展字段1")
    private String paramExtend1;

    @ApiModelProperty("参数扩展字段2")
    private String paramExtend2;

    @ApiModelProperty("参数扩展字段3")
    private String paramExtend3;

    @ApiModelProperty("参数扩展字段4")
    private String paramExtend4;

    @ApiModelProperty("参数扩展字段5")
    private String paramExtend5;

    @ApiModelProperty("LANG类型专用")
    private String langFullKey;

    @ApiModelProperty("LANG类型专用")
    private String langType;

    @ApiModelProperty("LANG类型专用")
    private List<OpenSourceCheckerSetVO> openSourceCheckerListVO;
}
