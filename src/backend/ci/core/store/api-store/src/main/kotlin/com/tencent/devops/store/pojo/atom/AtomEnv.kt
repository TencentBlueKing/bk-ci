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

package com.tencent.devops.store.pojo.atom

import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("插件执行环境信息")
data class AtomEnv(
    @ApiModelProperty("插件Id", required = true)
    val atomId: String,
    @ApiModelProperty("插件代码", required = true)
    val atomCode: String,
    @ApiModelProperty("插件名称", required = true)
    val atomName: String,
    @ApiModelProperty("插件状态", required = true)
    val atomStatus: String,
    @ApiModelProperty("插件创建人", required = true)
    val creator: String,
    @ApiModelProperty("插件版本号", required = true)
    val version: String,
    @ApiModelProperty("是否是公共插件", required = true)
    val publicFlag: Boolean,
    @ApiModelProperty("插件简介", required = false)
    val summary: String?,
    @ApiModelProperty("插件说明文档链接", required = false)
    val docsLink: String?,
    @ApiModelProperty("插件自定义json串", required = false)
    val props: String?,
    @ApiModelProperty("无构建环境插件是否可以在有构建环境运行标识", required = false)
    val buildLessRunFlag: Boolean?,
    @ApiModelProperty("插件创建时间", required = true)
    val createTime: Long,
    @ApiModelProperty("插件最后修改时间", required = true)
    val updateTime: Long,
    @ApiModelProperty("插件初始化项目代码", required = false)
    val projectCode: String? = null,
    @ApiModelProperty("安装包路径", required = false)
    val pkgPath: String? = null,
    @ApiModelProperty("插件开发语言", required = false)
    val language: String? = null,
    @ApiModelProperty("支持插件开发语言的最低版本", required = false)
    val minVersion: String? = null,
    @ApiModelProperty("插件执行入口", required = false)
    val target: String? = null,
    @ApiModelProperty("插件SHA签名串", required = false)
    val shaContent: String? = null,
    @ApiModelProperty("插件执行前置命令", required = false)
    val preCmd: String? = null,
    @ApiModelProperty("Job类型", required = false)
    val jobType: JobTypeEnum? = null,
    @ApiModelProperty("插件post信息", required = false)
    val atomPostInfo: AtomPostInfo? = null,
    @ApiModelProperty("所属插件分类代码", required = false)
    val classifyCode: String? = null,
    @ApiModelProperty("所属插件分类名称", required = false)
    val classifyName: String? = null
)
