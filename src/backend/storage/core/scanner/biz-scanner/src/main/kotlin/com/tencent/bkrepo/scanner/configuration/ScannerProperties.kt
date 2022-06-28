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

package com.tencent.bkrepo.scanner.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("scanner")
data class ScannerProperties(
    /**
     * 默认项目扫描子任务数量限制
     */
    var defaultProjectSubScanTaskCountLimit: Int = DEFAULT_SUB_SCAN_TASK_COUNT_LIMIT,
    var supportFileNameExt: Set<String> = DEFAULT_SUPPORT_FILE_NAME_EXTENSION
) {
    companion object {
        val DEFAULT_SUPPORT_FILE_NAME_EXTENSION = setOf(
            "apk", "apks", "aab", "exe", "so", "ipa", "dmg", "jar", "gz", "tar", "zip"
        )
        const val DEFAULT_PROJECT_SCAN_PRIORITY = 0
        const val DEFAULT_SCAN_TASK_COUNT_LIMIT = 1
        const val DEFAULT_SUB_SCAN_TASK_COUNT_LIMIT = 20
    }
}
