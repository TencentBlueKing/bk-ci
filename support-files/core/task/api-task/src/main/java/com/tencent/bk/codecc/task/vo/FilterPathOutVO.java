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
 * 屏蔽路径树输出参数实体
 *
 * @version V1.0
 * @date 2019/5/17
 */
@Data
@ApiModel("屏蔽路径树输出参数视图")
public class FilterPathOutVO
{

    @ApiModelProperty(value = "任务Id")
    private Long taskId;

    @ApiModelProperty(value = "默认屏蔽路径")
    private List<String> defaultFilterPath;

    @ApiModelProperty(value = "默认添加屏蔽路径列表")
    private List<String> defaultAddPaths;

    @ApiModelProperty(value = "自定义屏蔽路径列表")
    private List<String> filterPaths;

    @ApiModelProperty(value = "code.yml屏蔽路径列表")
    private List<String> testSourceFilterPath;

    @ApiModelProperty(value = "code.yml屏蔽路径列表")
    private List<String> autoGenFilterPath;

    @ApiModelProperty(value = "code.yml屏蔽路径列表")
    private List<String> thirdPartyFilterPath;

    @ApiModelProperty(value = "过滤路径类型")
    private String pathType;


}
