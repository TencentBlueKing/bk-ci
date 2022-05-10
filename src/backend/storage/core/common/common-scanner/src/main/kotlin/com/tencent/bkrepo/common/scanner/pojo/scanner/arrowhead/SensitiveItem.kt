/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.common.scanner.pojo.scanner.arrowhead

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonAlias
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("敏感信息数据")
@JsonIgnoreProperties(ignoreUnknown = true)
data class SensitiveItem(
    @ApiModelProperty("存在敏感信息的文件路径")
    @JsonAlias("Source")
    val path: String,

    /**
     * uri, ipv4, ipv6, email, secret
     */
    @ApiModelProperty("敏感信息类型,")
    @JsonAlias("Class")
    val type: String,

    /**
     * uri, ipv4, ipv6, email, common_key
     */
    @ApiModelProperty("敏感信息子类型")
    @JsonAlias("SubClass")
    val subtype: String,

    @ApiModelProperty("敏感信息内容")
    @JsonAlias("Content")
    val content: String,

    @ApiModelProperty("敏感信息为uri或者email时生效")
    @JsonAlias("Domain")
    val domain: String,

    @ApiModelProperty("敏感信息属性，存放文件类型，uri协议等信息")
    @JsonAlias("Attr")
    val attr: Map<String, String>
) {
    companion object {
        const val TYPE = "SENSITIVE_ITEM"
    }
}
