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

package com.tencent.bkrepo.auth.pojo.user

import com.tencent.bkrepo.auth.pojo.token.Token
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("用户信息")
data class User(
    @ApiModelProperty("用户ID")
    val userId: String,
    @ApiModelProperty("用户名")
    val name: String,
    @ApiModelProperty("用户密码")
    var pwd: String? = null,
    @ApiModelProperty("是否是管理员")
    val admin: Boolean = false,
    @ApiModelProperty("是否锁定")
    val locked: Boolean = false,
    @ApiModelProperty("用户token")
    val tokens: List<Token> = emptyList(),
    @ApiModelProperty("所属角色")
    val roles: List<String> = emptyList(),
    @ApiModelProperty("关联用户")
    val asstUsers: List<String> = emptyList(),
    @ApiModelProperty("群组账号")
    val group: Boolean = false,
    @ApiModelProperty("邮箱")
    val email: String? = null,
    @ApiModelProperty("创建时间")
    val createdDate: LocalDateTime? = null,
    @ApiModelProperty("最近跟新时间")
    val lastModifiedDate: LocalDateTime? = null
)
