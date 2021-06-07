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

package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 工具配置参数
 *
 * @version V1.0
 * @date 2019/6/13
 */
@Data
@ApiModel("工具配置参数")
public class ToolConfigParamJsonVO
{
    @ApiModelProperty(value = "配置信息对应的项目ID", required = true)
    private Long taskId;

    @ApiModelProperty(value = "工具的名称", required = true)
    private String toolName;

    @ApiModelProperty(value = "选中的值", required = true)
    private String chooseValue;

    @ApiModelProperty(value = "键")
    private Long key;

    @ApiModelProperty(value = "显示名称")
    private String labelName;

    @ApiModelProperty(value = "参数类型")
    private String varType;

    @ApiModelProperty(value = "参数名称", required = true)
    private String varName;

    @ApiModelProperty(value = "参数默认值")
    private String varDefault;

    @ApiModelProperty(value = "参数操作")
    private String varOptions;

    @ApiModelProperty(value = "参数提示")
    private String varTips;

    @ApiModelProperty(value = "是否必要请求")
    private Boolean varRequired;

    @ApiModelProperty(value = "参数列表")
    private List<VarOptionListEntity> varOptionList;

    @Data
    public static class VarOptionListEntity
    {
        private String name;
        private String id;

    }

}