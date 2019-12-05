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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.dao.container

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.store.tables.TBuildResource
import com.tencent.devops.model.store.tables.TContainerResourceRel
import com.tencent.devops.model.store.tables.records.TBuildResourceRecord
import com.tencent.devops.store.pojo.container.BuildResource
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class BuildResourceDao {

    fun add(
        dslContext: DSLContext,
        id: String,
        defaultFlag: Boolean,
        buildResourceCode: String,
        buildResourceName: String
    ) {
        with(TBuildResource.T_BUILD_RESOURCE) {
            dslContext.insertInto(
                this,
                ID,
                BUILD_RESOURCE_CODE,
                BUILD_RESOURCE_NAME,
                DEFAULT_FLAG
            )
                .values(
                    id,
                    buildResourceCode,
                    buildResourceName,
                    defaultFlag
                ).execute()
        }
    }

    fun countByCode(dslContext: DSLContext, buildResourceCode: String): Record1<Int>? {
        with(TBuildResource.T_BUILD_RESOURCE) {
            return dslContext.selectCount().from(this).where(BUILD_RESOURCE_CODE.eq(buildResourceCode)).fetchOne()
        }
    }

    fun delete(dslContext: DSLContext, id: String) {
        with(TBuildResource.T_BUILD_RESOURCE) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun setAllBuildResourceDefaultFlag(dslContext: DSLContext, defaultFlag: Boolean) {
        with(TBuildResource.T_BUILD_RESOURCE) {
            dslContext.update(this)
                .set(DEFAULT_FLAG, defaultFlag)
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        id: String,
        defaultFlag: Boolean,
        buildResourceCode: String,
        buildResourceName: String
    ) {
        with(TBuildResource.T_BUILD_RESOURCE) {
            dslContext.update(this)
                .set(BUILD_RESOURCE_CODE, buildResourceCode)
                .set(BUILD_RESOURCE_NAME, buildResourceName)
                .set(DEFAULT_FLAG, defaultFlag)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getBuildResource(dslContext: DSLContext, id: String): TBuildResourceRecord? {
        with(TBuildResource.T_BUILD_RESOURCE) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun getAllBuildResource(dslContext: DSLContext): Result<TBuildResourceRecord> {
        with(TBuildResource.T_BUILD_RESOURCE) {
            return dslContext
                .selectFrom(this)
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    fun getBuildResourceByContainerId(
        dslContext: DSLContext,
        containerId: String?,
        defaultFlag: Boolean?
    ): Result<out Record>? {
        val a = TBuildResource.T_BUILD_RESOURCE.`as`("a")
        val b = TContainerResourceRel.T_CONTAINER_RESOURCE_REL.`as`("b")
        val conditions = mutableListOf<Condition>()
        if (!containerId.isNullOrBlank()) {
            conditions.add(b.CONTAINER_ID.eq(containerId))
        }
        if (null != defaultFlag) conditions.add(a.DEFAULT_FLAG.eq(defaultFlag))
        return dslContext.select(
            a.BUILD_RESOURCE_CODE.`as`("buildResourceCode"),
            a.BUILD_RESOURCE_NAME.`as`("buildResourceName"),
            a.DEFAULT_FLAG.`as`("defaultFlag")
        )
            .from(a)
            .join(b)
            .on(a.ID.eq(b.RESOURCE_ID))
            .where(conditions)
            .fetch()
    }

    fun convert(record: TBuildResourceRecord): BuildResource {
        with(record) {
            return BuildResource(
                id = id,
                buildResourceCode = buildResourceCode,
                buildResourceName = buildResourceName,
                defaultFlag = defaultFlag,
                createTime = createTime.timestampmilli(),
                updateTime = updateTime.timestampmilli()
            )
        }
    }
}