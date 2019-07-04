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

import com.tencent.devops.store.pojo.atom.enums.AtomCategoryEnum
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import com.tencent.devops.store.pojo.atom.enums.ReleaseTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("插件市场工作台-升级插件请求报文体")
data class MarketAtomUpdateRequest(
    @ApiModelProperty("插件代码", required = true)
    val atomCode: String,
    @ApiModelProperty("插件名称", required = true)
    val name: String,
    @ApiModelProperty("插件所属范畴，TRIGGER：触发器类插件 TASK：任务类插件", required = true)
    val category: AtomCategoryEnum,
    @ApiModelProperty("所属插件分类代码", required = true)
    val classifyCode: String,
    @ApiModelProperty("适用Job类型，AGENT： 编译环境，AGENT_LESS：无编译环境", required = true)
    val jobType: JobTypeEnum = JobTypeEnum.AGENT,
    @ApiModelProperty("支持的操作系统", required = true)
    val os: ArrayList<String>,
    @ApiModelProperty("插件简介", required = false)
    val summary: String?,
    @ApiModelProperty("插件描述", required = false)
    val description: String?,
    @ApiModelProperty("logo地址", required = false)
    val logoUrl: String?,
    @ApiModelProperty("版本号", required = true)
    val version: String,
    @ApiModelProperty(
        "发布类型，NEW：新上架 INCOMPATIBILITY_UPGRADE：非兼容性升级 COMPATIBILITY_UPGRADE：兼容性功能更新 COMPATIBILITY_FIX：兼容性问题修正",
        required = true
    )
    val releaseType: ReleaseTypeEnum,
    @ApiModelProperty("版本日志内容", required = true)
    val versionContent: String,
    @ApiModelProperty("发布者", required = true)
    val publisher: String,
    @ApiModelProperty("插件标签列表", required = false)
    val labelIdList: ArrayList<String>?,
    @ApiModelProperty("插件包名", required = true)
    val pkgName: String = "",
    @ApiModelProperty("可执行包sha摘要内容", required = true)
    val packageShaContent: String = ""
)