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

package com.tencent.bk.codecc.defect.vo.common;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 公共文件查询请求视图
 *
 * @version V1.0
 * @date 2019/5/27
 */
@Data
@ApiModel("公共文件查询请求视图")
public class CommonFileQueryReqVO
{
    @ApiModelProperty("工具名")
    @NotNull(message = "工具名不能为空")
    protected String toolName;

    @ApiModelProperty("任务名称")
    private String taskName;

    @ApiModelProperty("规则名")
    private String checker;

    @ApiModelProperty("作者")
    private String author;

    @ApiModelProperty(value = "严重程度：严重（1），一般（2），提示（4）", allowableValues = "{1,2,4,8,16}")
    private List<String> severity;

    @ApiModelProperty(value = "文件类型:新增(1),历史(2), 修复(4),忽略(8)", allowableValues = "{1,2,4,8}")
    private List<String> fileType;

    @ApiModelProperty(value = "文件或路径列表")
    private List<String> fileList;

    @ApiModelProperty(value = "规则包名")
    private String pkgId;
}
