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

package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "windows 工作空间信息-创建")
data class WindowsWorkspaceCreate(
    @get:Schema(title = "云桌面 配置")
    val windowsType: String,
    @get:Schema(title = "云桌面 地域")
    val windowsZone: String,
    @get:Schema(title = "基础镜像Id")
    val baseImageId: Int = 0,
    @get:Schema(title = "创建实例的数量")
    val count: Int = 1,
    @get:Schema(title = "自定义镜像路径")
    val imageCosFile: String = "",
    @get:Schema(title = "指定工作空间，优先级比count高。")
    val assignNames: List<String> = emptyList(),
    @get:Schema(title = "如需指定工作空间owner，需要和assignNames/count对应。且值不为空")
    val assignOwners: List<String> = emptyList(),
    @get:Schema(title = "指定数据盘大小")
    val pvcs: List<Pvc> = emptyList(),
    @get:Schema(title = "创建时指定污点")
    val specifyTaints: String? = null
)

@Schema(title = "自定义数据盘信息")
data class Pvc(
    val pvcClass: String? = null,
    val pvcSize: String? = null
)
