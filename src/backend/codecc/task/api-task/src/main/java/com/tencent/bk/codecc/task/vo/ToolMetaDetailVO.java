/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
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

package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工具完整信息对象
 *
 * @version V1.0
 * @date 2019/4/25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("工具完整信息对象")
public class ToolMetaDetailVO extends ToolMetaBaseVO
{
    /**
     * 工具简介，一句话介绍语
     */
    @ApiModelProperty(value = "工具简介，一句话介绍语")
    private String briefIntroduction;

    /**
     * 工具描述，较详细的描述
     */
    @ApiModelProperty(value = "工具描述，较详细的描述")
    private String description;

    /**
     * 是否公开：true表示私有、false或者空表示公开
     */
    @ApiModelProperty(value = "是否公开：true表示私有、false或者空表示公开")
    private boolean privated;

    /**
     * 是否同步上蓝盾：true表示同步、false或者空表示不同步
     */
    @ApiModelProperty(value = "是否同步上蓝盾：true表示同步、false或者空表示不同步")
    private boolean syncLD;

    /**
     * 工具的图标
     */
    @ApiModelProperty(value = "工具的图标")
    private String logo;

    /**
     * 工具的图文详情
     */
    @ApiModelProperty(value = "工具的图文详情")
    private String graphicDetails;
}
