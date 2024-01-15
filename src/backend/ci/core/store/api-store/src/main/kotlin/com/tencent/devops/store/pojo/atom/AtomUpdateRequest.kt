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

import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.pojo.atom.enums.AtomCategoryEnum
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "流水线-原子信息请求报文体")
data class AtomUpdateRequest(
    @Schema(name = "原子名称", required = true)
    val name: String,
    @Schema(name = "服务范围", required = true)
    val serviceScope: List<String>,
    @Schema(name = "适用Job类型，AGENT： 编译环境，AGENT_LESS：无编译环境", required = true)
    val jobType: JobTypeEnum,
    @Schema(name = "支持的操作系统", required = true)
    val os: MutableList<String>,
    @Schema(name = "所属分类ID", required = true)
    val classifyId: String,
    @Schema(name = "原子说明文档链接", required = false)
    val docsLink: String?,
    @Schema(name = "原子类型，SELF_DEVELOPED：自研 THIRD_PARTY：第三方开发", required = true)
    val atomType: AtomTypeEnum,
    @Schema(name = "原子简介", required = false)
    val summary: String?,
    @Schema(name = "原子描述", required = false)
    val description: String?,
    @Schema(name = "是否为默认原子（默认原子默认所有项目可见）true：默认原子 false：普通原子", required = true)
    val defaultFlag: Boolean,
    @Schema(name = "原子所属范畴，TRIGGER：触发器类原子 TASK：任务类原子", required = true)
    val category: AtomCategoryEnum,
    @Schema(name = "无构建环境原子是否可以在有构建环境运行标识， TRUE：可以 FALSE：不可以", required = false)
    val buildLessRunFlag: Boolean? = null,
    @Schema(name = "权重（数值越大代表权重越高）")
    val weight: Int?,
    @Schema(name = "自定义扩展容器前端表单属性字段的Json串", required = false)
    val props: String?,
    @Schema(name = "预留字段（设置规则等信息的json串）", required = false)
    val data: String?,
    @Schema(name = "插件logo", required = false)
    val logoUrl: String?,
    @Schema(name = "icon图标base64字符串", required = false)
    val iconData: String?,
    @Schema(name = "是否推荐标识 true：推荐，false：不推荐", required = false)
    val recommendFlag: Boolean? = null,
    @Schema(name = "yaml可用标识 true：是，false：否")
    val yamlFlag: Boolean? = null,
    @Schema(name = "红线可用标识 true：是，false：否")
    val qualityFlag: Boolean? = null,
    @Schema(name = "是否认证标识 true：是，false：否")
    val certificationFlag: Boolean? = null,
    @Schema(name = "发布者", required = false)
    val publisher: String? = null,
    @Schema(name = "项目可视范围", required = false)
    val visibilityLevel: VisibilityLevelEnum? = null,
    @Schema(name = "插件代码库不开源原因", required = false)
    val privateReason: String? = null
)
