/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.dispatch.base.dao

import com.tencent.devops.model.dispatch_bcs.tables.TBuildBuilderPoolNo
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class BuildBuilderPoolNoDao {

    fun setBcsBuildLastBuilder(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        builderName: String,
        poolNo: String
    ) {
        with(TBuildBuilderPoolNo.T_BUILD_BUILDER_POOL_NO) {
            dslContext.insertInto(
                this,
                BUILD_ID,
                VM_SEQ_ID,
                EXECUTE_COUNT,
                BUILDER_NAME,
                POOL_NO,
                CREATE_TIME
            ).values(
                buildId,
                vmSeqId,
                executeCount,
                builderName,
                poolNo,
                LocalDateTime.now()
            ).execute()
        }
    }

    fun getBcsBuildLastBuilder(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String?,
        executeCount: Int
    ): List<Pair<String, String?>> {
        val result = mutableListOf<Pair<String, String?>>()
        with(TBuildBuilderPoolNo.T_BUILD_BUILDER_POOL_NO) {
            if (null == vmSeqId) {
                val records = dslContext.selectFrom(this)
                    .where(BUILD_ID.eq(buildId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .fetch()
                records?.forEach {
                    result.add(Pair(it.vmSeqId, it.builderName))
                }
            } else {
                val record = dslContext.selectFrom(this)
                    .where(BUILD_ID.eq(buildId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .and(VM_SEQ_ID.eq(vmSeqId))
                    .fetchOne()
                result.add(Pair(vmSeqId, record?.builderName))
            }
        }
        return result
    }

    fun getBcsBuildLastPoolNo(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String?,
        executeCount: Int
    ): List<Pair<String, String?>> {
        val result = mutableListOf<Pair<String, String?>>()
        with(TBuildBuilderPoolNo.T_BUILD_BUILDER_POOL_NO) {
            if (null == vmSeqId) {
                val records = dslContext.selectFrom(this)
                    .where(BUILD_ID.eq(buildId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .fetch()
                records?.forEach {
                    result.add(Pair(it.vmSeqId, it.poolNo))
                }
            } else {
                val record = dslContext.selectFrom(this)
                    .where(BUILD_ID.eq(buildId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .and(VM_SEQ_ID.eq(vmSeqId))
                    .fetchOne()

                result.add(Pair(vmSeqId, record?.poolNo))
            }
        }
        return result
    }

    fun deleteBcsBuildLastBuilderPoolNo(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String?,
        executeCount: Int
    ) {
        with(TBuildBuilderPoolNo.T_BUILD_BUILDER_POOL_NO) {
            if (vmSeqId == null) {
                dslContext.delete(this)
                    .where(BUILD_ID.eq(buildId))
                    .execute()
            } else {
                dslContext.delete(this)
                    .where(BUILD_ID.eq(buildId))
                    .and(VM_SEQ_ID.eq(vmSeqId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .execute()
            }
        }
    }
}
