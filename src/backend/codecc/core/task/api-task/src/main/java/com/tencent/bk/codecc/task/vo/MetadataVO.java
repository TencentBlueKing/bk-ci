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

/**
 * 元数据信息
 *
 * @version V4.0
 * @date 2019/2/13
 */
@Data
@ApiModel("元数据信息")
public class MetadataVO
{
    @ApiModelProperty("键值")
    private String key;

    @ApiModelProperty("数据名")
    private String name;

    @ApiModelProperty("数据全名")
    private String fullName;

    /**
     * 状态：测试（T）、发布（P）， 注：测试状态时只有管理员可以在页面上看到
     */
    @ApiModelProperty(value = "状态：测试（T）、发布（P）", allowableValues = "{T,P}")
    private String status;

    @ApiModelProperty(value = "创建人")
    private String creator;

    @ApiModelProperty(value = "创建时间")
    private long createTime;

    /**
     * 别名，用于跟第三方系统对接的时候兼容
     */
    @ApiModelProperty(value = "别名")
    private String aliasNames;

    @ApiModelProperty("LANG类型专用，lang_full_key")
    private String langFullKey;

    @ApiModelProperty("LANG类型专用，lang_type")
    private String langType;
}
