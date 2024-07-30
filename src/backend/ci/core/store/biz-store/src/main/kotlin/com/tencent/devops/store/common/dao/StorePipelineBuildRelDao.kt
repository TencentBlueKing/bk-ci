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

package com.tencent.devops.store.common.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TStorePipelineBuildRel
import com.tencent.devops.model.store.tables.TStorePipelineRel
import com.tencent.devops.model.store.tables.records.TStorePipelineBuildRelRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class StorePipelineBuildRelDao {

    fun add(dslContext: DSLContext, storeId: String, pipelineId: String, buildId: String) {
        with(TStorePipelineBuildRel.T_STORE_PIPELINE_BUILD_REL) {
            dslContext.insertInto(this,
                ID,
                STORE_ID,
                PIPELINE_ID,
                BUILD_ID
            )
                .values(
                    UUIDUtil.generate(),
                    storeId,
                    pipelineId,
                    buildId
                ).execute()
        }
    }

    fun getStorePipelineBuildRel(dslContext: DSLContext, storeId: String): TStorePipelineBuildRelRecord? {
        with(TStorePipelineBuildRel.T_STORE_PIPELINE_BUILD_REL) {
            return dslContext.selectFrom(this)
                .where(STORE_ID.eq(storeId))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun getStorePipelineBuildRelByBuildId(dslContext: DSLContext, buildId: String): TStorePipelineBuildRelRecord? {
        with(TStorePipelineBuildRel.T_STORE_PIPELINE_BUILD_REL) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .fetchOne()
        }
    }

    fun deleteStorePipelineBuildRel(dslContext: DSLContext, storeCode: String, storeType: Byte) {
        val tspr = TStorePipelineRel.T_STORE_PIPELINE_REL
        val pipelineIds =
            dslContext.select(tspr.PIPELINE_ID).from(tspr)
                .where(tspr.STORE_CODE.eq(storeCode).and(tspr.STORE_TYPE.eq(storeType)))
                .fetch()
        with(TStorePipelineBuildRel.T_STORE_PIPELINE_BUILD_REL) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.`in`(pipelineIds))
                .execute()
        }
    }

    fun deleteStorePipelineBuildRelByPipelineId(dslContext: DSLContext, pipelineId: String) {
        with(TStorePipelineBuildRel.T_STORE_PIPELINE_BUILD_REL) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }
}
