/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.artifact.pojo.configuration.composite

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * 代理源
 */
@ApiModel("代理源设置")
data class ProxyChannelSetting(
    @ApiModelProperty("是否为公有源", required = true)
    val public: Boolean,
    @ApiModelProperty("公有源id, 公有源必须提供", required = false)
    val channelId: String? = null,
    @ApiModelProperty("名称，私有源必选参数", required = false)
    val name: String? = null,
    @ApiModelProperty("地址，私有源必选参数", required = false)
    val url: String? = null,
    @ApiModelProperty("鉴权凭据key，私有源可选参数", required = false)
    val credentialKey: String? = null,
    @ApiModelProperty("代理源认证用户名，私有源可选参数", required = false)
    val username: String? = null,
    @ApiModelProperty("代理源认证密码，私有源可选参数", required = false)
    val password: String? = null
)
