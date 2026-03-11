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

package com.tencent.devops.project.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 文档地址：
 * https://github.com/Tencent/bk-bcs/blob/master/bcs-services/bcs-project/proto/bcsproject/bcsproject.swagger.json#L171
 */
@Schema(title = "创建BCS项目实体类")
data class BcsProjectForCreate(
    @get:Schema(title = "创建时间")
    val createTime: String? = null,
    @get:Schema(title = "项目创建者")
    val creator: String,
    @get:Schema(title = "项目ID, 全局唯一, 长度为32位字符串, 自动生成")
    val projectID: String? = null,
    @get:Schema(title = "项目中文名称, 长度不能超过64字符")
    val name: String,
    @get:Schema(title = "项目编码(英文缩写), 全局唯一, 长度不能超过64字符")
    val projectCode: String,
    @get:Schema(title = "是否使用蓝鲸提供的资源池, 主要用于资源计费, 默认false")
    val useBKRes: Boolean? = false,
    @get:Schema(title = "项目描述, 尽量限制在100字符")
    val description: String,
    @get:Schema(title = "项目是否已经下线, 默认false")
    val isOffline: Boolean? = false,
    @get:Schema(title = "项目中集群类型, 可选k8s/mesos")
    val kind: String? = null,
    @get:Schema(title = "项目绑定的蓝鲸CMDB中业务ID信息")
    val businessID: String? = null,
    @get:Schema(title = "是否为保密项目, 默认为false")
    val isSecret: Boolean? = false,
    @get:Schema(title = "项目类型, 保留字段, 默认为0, 可选 1:手游, 2:端游, 3:页游, 4:平台产品, 5:支撑产品")
    val projectType: Int,
    @get:Schema(title = "业务部署类型, 保留字段, 1:物理机部署, 2:容器部署")
    val deployType: Int? = null,
    @get:Schema(title = "事业群ID, 保留字段, 默认为0")
    @JsonProperty("BGID")
    val bgId: String? = "",
    @get:Schema(title = "事业群名称, 保留字段, 默认为空")
    @JsonProperty("BGName")
    val bgName: String? = "",
    @get:Schema(title = "部门ID, 保留字段, 默认为0")
    val deptID: String? = "",
    @get:Schema(title = "部门名称, 保留字段, 默认为空")
    val deptName: String? = "",
    @get:Schema(title = "中心ID, 保留字段, 默认为0")
    val centerID: String? = "",
    @get:Schema(title = "中心名称, 保留字段, 默认为空")
    val centerName: String? = ""
)
