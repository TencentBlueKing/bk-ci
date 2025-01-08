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

package com.tencent.devops.remotedev.pojo.project

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "提供给安全侧的项目下云桌面信息")
data class WeSecProjectWorkspace(
    @get:Schema(title = "工作空间名称", name = "workspace_name")
    @JsonProperty("workspace_name")
    val workspaceName: String,
    @get:Schema(title = "项目ID", name = "project_id")
    @JsonProperty("project_id")
    val projectId: String,
    @get:Schema(title = "工作空间创建人")
    val creator: String,
    @get:Schema(title = "工作空间拥有者")
    val owner: String? = null,
    @get:Schema(title = "工作空间创建时间", name = "create_time")
    @JsonProperty("create_time")
    val createTime: String? = null,
    @get:Schema(title = "region_id", name = "region_id")
    @JsonProperty("region_id")
    val regionId: String,
    @get:Schema(title = "inner_ip", name = "inner_ip")
    @JsonProperty("inner_ip")
    val innerIp: String?,
    @get:Schema(title = "状态")
    val status: String?,
    @get:Schema(title = "工作空间实际拥有者，待分配时为空", name = "real_owner")
    @JsonProperty("real_owner")
    val realOwner: String? = null,
    @get:Schema(title = "云桌面别名", name = "display_name")
    @JsonProperty("display_name")
    val displayName: String? = null,
    @get:Schema(title = "拥有者所属组织信息")
    val ownerDepartments: List<DepartmentsInfo>?,
    @get:Schema(title = "当前登录人")
    val currentLoginUsers: Set<String>?,
    @get:Schema(title = "机型", name = "machine_type")
    @JsonProperty("machine_type")
    val machineType: String? = null,
    @JsonProperty("mac_address")
    @get:Schema(title = "mac地址", name = "mac_address")
    val macAddress: String? = null,
    @JsonProperty("zone_type")
    @get:Schema(title = "专区类型", name = "zone_type")
    val zoneType: String? = null,
    @get:Schema(title = "共享人")
    val viewers: List<String>? = emptyList(),
    @get:Schema(title = "母机IP")
    val nodeIp: String? = null
)

@Schema(title = "组织信息")
data class DepartmentsInfo(
    @get:Schema(title = "组织ID")
    val deptId: String?,
    @get:Schema(title = "组织名称")
    val deptName: String?
)
