/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.store.common.utils

import com.tencent.devops.store.common.service.StoreComponentSourceResolver.AtomRelease
import com.tencent.devops.store.common.service.StoreComponentSourceResolver.StoreComponentSource
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractPendingItem
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractPendingReasonEnum
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractPendingResp
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractPendingResp.Skipped

object StoreComponentContractPendingUtils {

    const val SKIP_NO_LATEST_RELEASE = "NO_LATEST_RELEASE"
    const val SKIP_NO_SOURCE = "NO_SOURCE"
    const val SKIP_NOT_REPOSITORY = "NOT_REPOSITORY"

    // 内置插件的 CODE_SRC 指向仓库里的某个源码文件或目录，不能检出
    private val NOT_REPOSITORY_PATH = Regex("/(blob|tree)/")

    data class Candidate(
        val storeCode: String,
        val reason: StoreComponentContractPendingReasonEnum,
        val failCount: Int
    )

    /**
     * 过期的排在前面，缺失的按执行次数补位，总数不超过 [scanSize]；已租出的不占名额
     */
    fun mergeCandidates(
        stale: List<Candidate>,
        hotReleases: List<AtomRelease>,
        existingCodes: Set<String>,
        maxFailCount: Int,
        scanSize: Int,
        leased: (String) -> Boolean = { false },
        missingFailCount: (AtomRelease) -> Int
    ): List<Candidate> {
        val result = stale.filter { it.failCount < maxFailCount && !leased(it.storeCode) }
            .take(scanSize).toMutableList()
        val seen = stale.map { it.storeCode }.toMutableSet()
        for (release in hotReleases) {
            if (result.size >= scanSize) {
                break
            }
            if (release.atomCode in existingCodes || !seen.add(release.atomCode) || leased(release.atomCode)) {
                continue
            }
            val failCount = missingFailCount(release)
            if (failCount < maxFailCount) {
                result.add(Candidate(release.atomCode, StoreComponentContractPendingReasonEnum.MISSING, failCount))
            }
        }
        return result
    }

    /**
     * 按顺序解析代码来源，解析不出的放进跳过清单，凑满 [limit] 个为止。
     * [acquire] 返回 false 表示被并发调用抢先租走，换下一个，不进跳过清单
     */
    fun pick(
        candidates: List<Candidate>,
        releases: Map<String, AtomRelease>,
        limit: Int,
        acquire: (AtomRelease) -> Boolean = { true },
        resolve: (AtomRelease) -> StoreComponentSource?
    ): StoreComponentContractPendingResp {
        val items = mutableListOf<StoreComponentContractPendingItem>()
        val skipped = mutableListOf<Skipped>()
        for (candidate in candidates) {
            if (items.size >= limit) {
                break
            }
            val release = releases[candidate.storeCode]
            if (release == null) {
                skipped.add(Skipped(candidate.storeCode, null, SKIP_NO_LATEST_RELEASE))
                continue
            }
            val source = resolve(release)
            if (source == null) {
                skipped.add(Skipped(candidate.storeCode, release.version, SKIP_NO_SOURCE))
                continue
            }
            if (NOT_REPOSITORY_PATH.containsMatchIn(source.repoUrl)) {
                skipped.add(Skipped(candidate.storeCode, release.version, SKIP_NOT_REPOSITORY))
                continue
            }
            if (!acquire(release)) {
                continue
            }
            items.add(
                StoreComponentContractPendingItem(
                    storeCode = candidate.storeCode,
                    version = release.version,
                    reason = candidate.reason,
                    failCount = candidate.failCount,
                    repoUrl = source.repoUrl,
                    gitRef = source.gitRef,
                    refType = source.refType
                )
            )
        }
        return StoreComponentContractPendingResp(items = items, skipped = skipped)
    }
}
