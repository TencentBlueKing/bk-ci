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

package com.tencent.bkrepo.scanner.executor.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.util.unit.DataSize
import java.time.Duration

@ConfigurationProperties("scanner.executor")
data class ScannerExecutorProperties(
    /**
     * 扫描执行器工作目录
     */
    var workDir: String = System.getProperty("java.io.tmpdir"),
    /**
     * [workDir]目录下文件过期时间，文件最后修改时间离当前时间超过[fileExpiredMinutes]时将被删除
     */
    var fileExpiredMinutes: Duration = Duration.ofMinutes(60),
    /**
     * 单机最大允许执行的任务数量
     */
    var maxTaskCount: Int = 20,
    /**
     * 最大支持扫描的文件大小
     */
    var fileSizeLimit: DataSize = DataSize.ofGigabytes(10),
    /**
     * 机器当前空闲内存占比，小于这个值后不再认领任务
     */
    var atLeastFreeMemPercent: Double = 0.2,
    /**
     * [workDir]所在硬盘当前可用空间百分比，小于这个值后不再认领任务
     */
    var atLeastUsableDiskSpacePercent: Double = 0.3,
    /**
     * 扫描器日志最大行数
     */
    var maxScannerLogLines: Long = 200L
)
