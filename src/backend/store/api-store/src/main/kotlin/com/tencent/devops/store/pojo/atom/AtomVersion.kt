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

package com.tencent.devops.store.pojo.atom

import com.tencent.devops.store.pojo.common.Label
import com.tencent.devops.store.pojo.common.StoreUserCommentInfo
import io.swagger.annotations.ApiModelProperty

data class AtomVersion(
    @ApiModelProperty("插件ID")
    val atomId: String,
    @ApiModelProperty("插件标识")
    val atomCode: String,
    @ApiModelProperty("插件名称")
    val name: String,
    @ApiModelProperty("logo地址")
    val logoUrl: String?,
    @ApiModelProperty("插件分类code")
    val classifyCode: String?,
    @ApiModelProperty("插件分类名称")
    val classifyName: String?,
    @ApiModelProperty("插件范畴")
    val category: String?,
    @ApiModelProperty("插件说明文档链接")
    val docsLink: String?,
    @ApiModelProperty("插件类型")
    val atomType: String?,
    @ApiModelProperty("适用Job类型")
    val jobType: String?,
    @ApiModelProperty("操作系统")
    val os: List<String>?,
    @ApiModelProperty("插件简介")
    val summary: String?,
    @ApiModelProperty("插件描述")
    val description: String?,
    @ApiModelProperty("版本号")
    val version: String?,
    @ApiModelProperty(
        "插件状态，INIT：初始化|COMMITTING：提交中|BUILDING：构建中|BUILD_FAIL：构建失败|TESTING：测试中|AUDITING：审核中|AUDIT_REJECT：审核驳回|RELEASED：已发布|GROUNDING_SUSPENSION：上架中止|UNDERCARRIAGING：下架中|UNDERCARRIAGED：已下架",
        required = true
    )
    val atomStatus: String,
    @ApiModelProperty("发布类型")
    val releaseType: String?,
    @ApiModelProperty("版本日志")
    val versionContent: String?,
    @ApiModelProperty("开发语言")
    val language: String?,
    @ApiModelProperty("代码库链接")
    val codeSrc: String?,
    @ApiModelProperty("发布者")
    val publisher: String?,
    @ApiModelProperty("创建人")
    val creator: String,
    @ApiModelProperty("修改人")
    val modifier: String,
    @ApiModelProperty("创建时间")
    val createTime: String,
    @ApiModelProperty("修改时间")
    val updateTime: String,
    @ApiModelProperty("是否为默认插件（默认插件默认所有项目可见）true：默认插件 false：普通插件")
    val defaultFlag: Boolean?,
    @ApiModelProperty("是否可安装标识")
    val flag: Boolean?,
    @ApiModelProperty("插件的原生项目")
    val projectCode: String?,
    @ApiModelProperty("标签列表")
    val labelList: List<Label>?,
    @ApiModelProperty("插件包名")
    val pkgName: String?,
    @ApiModelProperty("用户评论信息")
    val userCommentInfo: StoreUserCommentInfo
)