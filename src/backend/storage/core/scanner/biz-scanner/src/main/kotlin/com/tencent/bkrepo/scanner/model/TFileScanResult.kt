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
 * 文件扫描结果
 */
@Document("file_scan_result")
@CompoundIndexes(
    CompoundIndex(
        name = "credentialsKey_sha256_idx",
        def = "{'credentialsKey': 1, 'sha256': 1}",
        background = true,
        unique = true
    )
)
data class TFileScanResult(
    val id: String? = null,
    val lastModifiedDate: LocalDateTime,
    /**
     * 文件sha256
     */
    val sha256: String,
    /**
     * 文件所在存储
     */
    var credentialsKey: String? = null,
    /**
     * 文件使用不同扫描器的扫描结果列表
     */
    val scanResult: Map<String, TScanResult>
)

/**
 * 扫描结果
 */
data class TScanResult(
    /**
     * 最后一次是在哪个扫描任务中扫描的
     */
    val taskId: String,
    /**
     * 文件开始扫描的时间戳
     */
    val startDateTime: LocalDateTime,
    /**
     * 文件扫描结束的时间戳
     */
    val finishedDateTime: LocalDateTime,
    /**
     * 扫描器
     */
    val scanner: String,
    /**
     * 扫描器类型
     */
    val scannerType: String,
    /**
     * 扫描器版本
     */
    val scannerVersion: String,
    /**
     * 扫描结果统计
     */
    val overview: Map<String, Any?>
)
