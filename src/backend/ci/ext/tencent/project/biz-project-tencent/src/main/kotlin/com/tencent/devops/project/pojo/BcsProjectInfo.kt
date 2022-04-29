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
import io.swagger.annotations.ApiModelProperty

data class BcsProjectInfo(
    @ApiModelProperty("创建时间")
    val createTime: String? = null,
    @ApiModelProperty("更新时间")
    val updateTime: String? = null,
    @ApiModelProperty("项目创建者")
    val creator: String,
    @ApiModelProperty("项目更新者")
    val updater: String,
    @ApiModelProperty("项目管理人员，默认为创建者+更新者")
    val managers: String,
    @ApiModelProperty("项目ID, 全局唯一, 长度为32位字符串, 自动生成")
    val projectID: String? = null,
    @ApiModelProperty("项目中文名称, 长度不能超过64字符")
    val name: String,
    @ApiModelProperty("项目编码(英文缩写), 全局唯一, 长度不能超过64字符")
    val projectCode: String,
    @ApiModelProperty("是否使用蓝鲸提供的资源池, 主要用于资源计费, 默认false")
    val useBKRes: Boolean? = false,
    @ApiModelProperty("项目描述, 尽量限制在100字符")
    val description: String,
    @ApiModelProperty("项目是否已经下线, 默认false")
    val isOffline: Boolean? = false,
    @ApiModelProperty("项目中集群类型, 可选k8s/mesos")
    val kind: String? = null,
    @ApiModelProperty("项目绑定的蓝鲸CMDB中业务ID信息")
    val businessID: String? = null,
    @ApiModelProperty("是否为保密项目, 默认为false")
    val isSecret: Boolean? = false,
    @ApiModelProperty("项目类型, 保留字段, 默认为0, 可选 1:手游, 2:端游, 3:页游, 4:平台产品, 5:支撑产品")
    val projectType: Int,
    @ApiModelProperty("业务部署类型, 保留字段, 1:物理机部署, 2:容器部署")
    val deployType: Int? = null,
    @ApiModelProperty("事业群ID, 保留字段, 默认为0")
    @JsonProperty("BGID")
    val bgId: String? = "",
    @ApiModelProperty("事业群名称, 保留字段, 默认为空")
    @JsonProperty("BGName")
    val bgName: String? = "",
    @ApiModelProperty("部门ID, 保留字段, 默认为0")
    val deptID: String? = "",
    @ApiModelProperty("部门名称, 保留字段, 默认为空")
    val deptName: String? = "",
    @ApiModelProperty("中心ID, 保留字段, 默认为0")
    val centerID: String? = "",
    @ApiModelProperty("中心名称, 保留字段, 默认为空")
    val centerName: String? = ""
)
