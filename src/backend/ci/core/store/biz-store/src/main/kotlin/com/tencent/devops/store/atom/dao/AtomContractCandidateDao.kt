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

package com.tencent.devops.store.atom.dao

import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.TStoreStatisticsTotal
import com.tencent.devops.store.common.service.StoreComponentSourceResolver.AtomRelease
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Repository

/**
 * 契约自动抽取用的插件最新发布版本查询
 */
@Repository
class AtomContractCandidateDao {

    fun listLatestReleases(dslContext: DSLContext, atomCodes: Collection<String>): Map<String, AtomRelease> {
        if (atomCodes.isEmpty()) {
            return emptyMap()
        }
        val tAtom = TAtom.T_ATOM
        return dslContext.select(tAtom.ID, tAtom.ATOM_CODE, tAtom.VERSION, tAtom.CODE_SRC, tAtom.PUB_TIME)
            .from(tAtom)
            .where(tAtom.ATOM_CODE.`in`(atomCodes))
            .and(tAtom.LATEST_FLAG.eq(true))
            .and(tAtom.DELETE_FLAG.eq(false))
            .fetch()
            .map { toRelease(it, tAtom) }
            .associateBy { it.atomCode }
    }

    /**
     * 按近期执行次数降序列出已发布插件的最新版本
     */
    fun listHotLatestReleases(
        dslContext: DSLContext,
        minRecentExecuteNum: Int,
        limit: Int
    ): List<AtomRelease> {
        val tAtom = TAtom.T_ATOM
        val tStatistics = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL
        return dslContext.select(tAtom.ID, tAtom.ATOM_CODE, tAtom.VERSION, tAtom.CODE_SRC, tAtom.PUB_TIME)
            .from(tAtom)
            .join(tStatistics)
            .on(tStatistics.STORE_CODE.eq(tAtom.ATOM_CODE))
            .and(tStatistics.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
            .where(tAtom.LATEST_FLAG.eq(true))
            .and(tAtom.DELETE_FLAG.eq(false))
            .and(tAtom.ATOM_STATUS.eq(AtomStatusEnum.RELEASED.status.toByte()))
            .and(tStatistics.RECENT_EXECUTE_NUM.ge(minRecentExecuteNum))
            .orderBy(tStatistics.RECENT_EXECUTE_NUM.desc())
            .limit(limit)
            .fetch()
            .map { toRelease(it, tAtom) }
    }

    private fun toRelease(record: Record, tAtom: TAtom) = AtomRelease(
        atomId = record.get(tAtom.ID),
        atomCode = record.get(tAtom.ATOM_CODE),
        version = record.get(tAtom.VERSION),
        codeSrc = record.get(tAtom.CODE_SRC),
        pubTime = record.get(tAtom.PUB_TIME)
    )
}
