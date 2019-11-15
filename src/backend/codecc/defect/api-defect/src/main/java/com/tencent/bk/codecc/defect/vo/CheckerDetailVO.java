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

package com.tencent.bk.codecc.defect.vo;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 规则详情视图实体类
 *
 * @version V1.0
 * @date 2019/5/5
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("规则详情视图实体类")
public class CheckerDetailVO extends CommonVO
{

    @ApiModelProperty(value = "工具名")
    private String toolName;

    @ApiModelProperty(value = "告警类型key")
    private String checkerKey;

    @ApiModelProperty(value = "规则名称")
    private String checkerName;

    @ApiModelProperty(value = "规则详细描述")
    private String checkerDesc;

    @ApiModelProperty(value = "规则严重程度，1=>严重，2=>一般，3=>提示", allowableValues = "{1,2,3}")
    private Integer severity;

    @ApiModelProperty(value = "规则所属语言（针对KLOCKWORK）")
    private Integer language;

    @ApiModelProperty(value = "规则状态 2=>打开 1=>关闭;", allowableValues = "{1,2}")
    private Integer status;

    @ApiModelProperty("规则状态是否打开")
    private Boolean checkerStatus;

    @ApiModelProperty(value = "规则类型")
    private String checkerType;

    @ApiModelProperty(value = "规则类型说明")
    private String checkerTypeDesc;

    @ApiModelProperty(value = "规则类型排序序列号")
    private String checkerTypeSort;

    @ApiModelProperty(value = "所属规则包")
    private String pkgKind;

    @ApiModelProperty(value = "项目框架（针对Eslint工具,目前有vue,react,standard）")
    private String frameworkType;

    @ApiModelProperty(value = "规则映射")
    private String checkerMapped;

    @ApiModelProperty(value = "规则配置")
    private String props;

    @ApiModelProperty(value = "规则所属标准")
    private Integer standard;

    @ApiModelProperty(value = "规则是否支持配置true：支持;空或false：不支持")
    private String editable;

    @ApiModelProperty(value = "示例代码")
    private String codeExample;

}
