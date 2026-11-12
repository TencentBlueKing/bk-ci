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
import com.tencent.devops.store.common.utils.StoreComponentContractPendingUtils.Candidate
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractPendingReasonEnum.MISSING
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractPendingReasonEnum.NEW_VERSION
import com.tencent.devops.store.pojo.common.contract.StoreComponentSourceRefTypeEnum
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class StoreComponentContractPendingUtilsTest {

    private fun release(code: String, version: String = "1.0.0") =
        AtomRelease(atomId = "id-$code", atomCode = code, version = version, codeSrc = "git@$code", pubTime = null)

    private fun source(release: AtomRelease) =
        StoreComponentSource(release.codeSrc!!, "c-${release.atomCode}", StoreComponentSourceRefTypeEnum.COMMIT_ID)

    @Test
    fun staleComesBeforeMissing() {
        val result = StoreComponentContractPendingUtils.mergeCandidates(
            stale = listOf(Candidate("a", NEW_VERSION, 0)),
            hotReleases = listOf(release("b"), release("c")),
            existingCodes = setOf("a"),
            maxFailCount = 3,
            scanSize = 5
        ) { 0 }
        Assertions.assertEquals(listOf("a", "b", "c"), result.map { it.storeCode })
        Assertions.assertEquals(listOf(NEW_VERSION, MISSING, MISSING), result.map { it.reason })
    }

    @Test
    fun missingSkipsExistingContractAndFailedTooOften() {
        val result = StoreComponentContractPendingUtils.mergeCandidates(
            stale = emptyList(),
            hotReleases = listOf(release("has"), release("bad"), release("ok")),
            existingCodes = setOf("has"),
            maxFailCount = 3,
            scanSize = 5
        ) { if (it.atomCode == "bad") 3 else 1 }
        Assertions.assertEquals(listOf("ok"), result.map { it.storeCode })
        Assertions.assertEquals(1, result.single().failCount)
    }

    @Test
    fun scanSizeCapsCandidates() {
        val result = StoreComponentContractPendingUtils.mergeCandidates(
            stale = listOf(Candidate("a", NEW_VERSION, 0), Candidate("b", NEW_VERSION, 0)),
            hotReleases = listOf(release("c")),
            existingCodes = setOf("a", "b"),
            maxFailCount = 3,
            scanSize = 2
        ) { 0 }
        Assertions.assertEquals(listOf("a", "b"), result.map { it.storeCode })
    }

    @Test
    fun pickSkipsUnresolvedAndFillsLimit() {
        val candidates = listOf(
            Candidate("noRelease", NEW_VERSION, 0),
            Candidate("noSource", NEW_VERSION, 1),
            Candidate("x", NEW_VERSION, 2),
            Candidate("y", MISSING, 0),
            Candidate("z", MISSING, 0)
        )
        val releases = listOf("noSource", "x", "y", "z").associateWith { release(it, "2.0.1") }
        val resp = StoreComponentContractPendingUtils.pick(candidates, releases, limit = 2) {
            if (it.atomCode == "noSource") null else source(it)
        }
        Assertions.assertEquals(listOf("x", "y"), resp.items.map { it.storeCode })
        Assertions.assertEquals("2.0.1", resp.items.first().version)
        Assertions.assertEquals(2, resp.items.first().failCount)
        Assertions.assertEquals("c-x", resp.items.first().gitRef)
        Assertions.assertEquals(
            listOf(
                "noRelease:${StoreComponentContractPendingUtils.SKIP_NO_LATEST_RELEASE}",
                "noSource:${StoreComponentContractPendingUtils.SKIP_NO_SOURCE}"
            ),
            resp.skipped.map { "${it.storeCode}:${it.reason}" }
        )
    }

    @Test
    fun pickStopsResolvingAfterLimit() {
        val resolved = mutableListOf<String>()
        val candidates = listOf("a", "b", "c").map { Candidate(it, NEW_VERSION, 0) }
        val releases = listOf("a", "b", "c").associateWith { release(it).copy(pubTime = LocalDateTime.now()) }
        StoreComponentContractPendingUtils.pick(candidates, releases, limit = 1) {
            resolved.add(it.atomCode)
            source(it)
        }
        Assertions.assertEquals(listOf("a"), resolved)
    }
}
