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

import java.time.LocalDateTime

open class SubScanTaskDefinition(
    var id: String? = null,
    val createdBy: String,
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
     * 所属扫描任务
     */
    val parentScanTaskId: String,
    /**
     * 使用的扫描计划
     */
    val planId: String? = null,

    /**
     * 制品所属项目
     */
    val projectId: String,
    /**
     * 制品所属仓库
     */
    val repoName: String,
    /**
     * 制品所属仓库类型
     */
    val repoType: String,
    /**
     * 制品为依赖包时候存在，依赖包key
     */
    val packageKey: String? = null,
    /**
     * 制品为依赖包时候存在，依赖包版本
     */
    val version: String? = null,
    /**
     * 文件路径
     */
    var fullPath: String,
    /**
     * 制品名称
     * repoType为[com.tencent.bkrepo.common.artifact.pojo.RepositoryType.GENERIC]时为nodeName
     * 其他情况为packageName
     */
    val artifactName: String,

    /**
     * 子任务状态
     */
    val status: String,

    /**
     * 使用的扫描器
     */
    val scanner: String,
    /**
     * 扫描器类型
     */
    val scannerType: String,
    /**
     * 文件sha256
     */
    val sha256: String,
    /**
     * 文件大小
     */
    val size: Long,
    /**
     * 文件所在存储使用的凭据
     */
    val credentialsKey: String?,
    /**
     * 扫描结果统计信息
     */
    val scanResultOverview: Map<String, Number>? = null
)
