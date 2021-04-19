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

package com.tencent.bkrepo.repository.pojo.node

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * 节点详细信息
 */
@ApiModel("节点详细信息")
data class NodeDetail(
    @ApiModelProperty("节点基本信息")
    @Deprecated("冗余信息，nodeInfo信息已包含在NodeDetail字段中，nodeInfo将来会删除")
    val nodeInfo: NodeInfo,

    @ApiModelProperty("创建者")
    val createdBy: String = nodeInfo.createdBy,
    @ApiModelProperty("创建时间")
    val createdDate: String = nodeInfo.createdDate,
    @ApiModelProperty("修改者")
    val lastModifiedBy: String = nodeInfo.lastModifiedBy,
    @ApiModelProperty("修改时间")
    val lastModifiedDate: String = nodeInfo.lastModifiedDate,

    @ApiModelProperty("是否为文件夹")
    val folder: Boolean = nodeInfo.folder,
    @ApiModelProperty("路径")
    val path: String = nodeInfo.path,
    @ApiModelProperty("资源名称")
    val name: String = nodeInfo.name,
    @ApiModelProperty("完整路径")
    val fullPath: String = nodeInfo.fullPath,
    @ApiModelProperty("文件大小，单位byte")
    val size: Long = nodeInfo.size,
    @ApiModelProperty("文件sha256")
    val sha256: String? = nodeInfo.sha256,
    @ApiModelProperty("文件md5")
    val md5: String? = nodeInfo.md5,
    @ApiModelProperty("元数据")
    val metadata: Map<String, Any> = nodeInfo.metadata.orEmpty(),
    @ApiModelProperty("所属项目id")
    val projectId: String = nodeInfo.projectId,
    @ApiModelProperty("所属仓库名称")
    val repoName: String = nodeInfo.repoName
)
