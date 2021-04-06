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

import com.tencent.bkrepo.common.api.util.HumanReadable
import com.tencent.bkrepo.common.artifact.path.PathUtils.UNIX_SEPARATOR
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 用于浏览器列表查看节点信息
 */
data class NodeListViewItem(
    val name: String,
    val lastModified: String,
    val createdBy: String,
    val size: String,
    val folder: Boolean,
    val sha256: String
) : Comparable<NodeListViewItem> {

    override fun compareTo(other: NodeListViewItem): Int {
        return if (this.folder && !other.folder) -1
        else if (!this.folder && other.folder) 1
        else this.name.compareTo(other.name)
    }

    companion object {
        private val formatters = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        fun from(nodeInfo: NodeInfo): NodeListViewItem {
            val normalizedName = if (nodeInfo.folder) nodeInfo.name + UNIX_SEPARATOR else nodeInfo.name
            val normalizedSize = if (nodeInfo.folder) "-" else HumanReadable.size(nodeInfo.size)
            val localDateTime = LocalDateTime.parse(nodeInfo.lastModifiedDate, DateTimeFormatter.ISO_DATE_TIME)
            val lastModified = formatters.format(localDateTime)
            val sha256 = if (nodeInfo.folder) "-" else nodeInfo.sha256.orEmpty()
            return NodeListViewItem(
                name = normalizedName,
                lastModified = lastModified,
                createdBy = nodeInfo.createdBy,
                size = normalizedSize,
                folder = nodeInfo.folder,
                sha256 = sha256
            )
        }
    }
}
