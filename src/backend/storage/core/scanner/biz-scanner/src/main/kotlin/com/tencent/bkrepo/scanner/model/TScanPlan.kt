/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.scanner.model

import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * 扫描方案
 */
@Document("scan_plan")
@CompoundIndexes(
    CompoundIndex(
        name = "unique_index",
        def = "{'projectId': 1, 'name': 1, 'type': 1, 'deleted': 1}",
        unique = true, background = true
    ),
    CompoundIndex(
        name = "latestScanTaskId_index",
        def = "{'latestScanTaskId': 1}",
        background = true
    )
)
data class TScanPlan(
    var id: String? = null,
    var createdBy: String,
    var createdDate: LocalDateTime,
    var lastModifiedBy: String,
    var lastModifiedDate: LocalDateTime,
    val deleted: LocalDateTime? = null,

    /**
     * 扫描方案名
     */
    val name: String,

    /**
     * 扫描方案所属项目
     */
    val projectId: String,

    /**
     * 自动扫描仓库
     */
    val repoNames: List<String> = emptyList(),

    /**
     * 使用的扫描器名
     */
    val scanner: String,

    /**
     * 扫描方案类型
     */
    val type: String,

    /**
     * 扫描方案描述
     */
    val description: String,

    /**
     * 有满足规则的制品上传时触发扫描
     */
    val scanOnNewArtifact: Boolean = false,

    /**
     * 自动扫描规则
     */
    val rule: String,

    /**
     * 最新一次扫描任务id
     */
    val latestScanTaskId: String? = null,

    /**
     * 扫描结果统计信息
     */
    val scanResultOverview: Map<String, Long> = emptyMap()
)
