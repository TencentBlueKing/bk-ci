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

@Document("scan_task")
@CompoundIndexes(
    CompoundIndex(name = "status_idx", def = "{'status': 1}", background = true),
    CompoundIndex(name = "planId_idx", def = "{'planId': 1}", background = true)
)
data class TScanTask(
    val id: String? = null,
    val createdBy: String,
    /**
     * 触发扫描时间
     */
    val createdDate: LocalDateTime,
    val lastModifiedBy: String,
    val lastModifiedDate: LocalDateTime,
    /**
     * 开始扫描时间
     */
    val startDateTime: LocalDateTime? = null,
    /**
     * 结束扫描时间
     */
    val finishedDateTime: LocalDateTime? = null,
    /**
     * 触发类型，手动、新构件上传、定时扫描
     */
    val triggerType: String,
    /**
     * 使用的扫描方案id
     */
    val planId: String? = null,
    /**
     * 任务状态
     */
    val status: String,
    /**
     * 扫描文件匹配规则
     */
    val rule: String?,
    /**
     * 需要扫描的文件数
     */
    val total: Long,
    /**
     * 扫描中的文件数
     */
    val scanning: Long,
    /**
     * 扫描失败的文件数量
     */
    val failed: Long,
    /**
     * 已扫描文件数
     */
    val scanned: Long,
    /**
     * 使用的扫描器
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
     * 扫描结果统计信息
     */
    val scanResultOverview: Map<String, Long> = emptyMap()
)
