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

package com.tencent.bkrepo.common.artifact.repository.migration

/**
 * 包迁移详情信息
 */
data class PackageMigrateDetail(
    /**
     * 包名
     */
    val packageName: String,

    /**
     * 版本迁移成功集合，记录名称
     */
    val successVersionList: MutableSet<String> = mutableSetOf(),

    /**
     * 版本迁移失败集合，记录名称和错误原因
     */
    val failureVersionDetailList: MutableSet<VersionMigrateErrorDetail> = mutableSetOf()
) {
    /**
     * 获取总的迁移包数量
     */
    fun getVersionCount(): Int = successVersionList.size + failureVersionDetailList.size

    /**
     * 添加版本[version]到成功列表
     */
    fun addSuccessVersion(version: String) {
        this.successVersionList.add(version)
    }

    /**
     * 添加版本[version]到失败列表，[reason]为失败原因
     */
    fun addFailureVersion(version: String, reason: String) {
        this.failureVersionDetailList.add(VersionMigrateErrorDetail(version, reason))
    }
}
