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

package com.tencent.devops.auth.dao

import com.tencent.devops.auth.pojo.enum.SystemType
import com.tencent.devops.auth.pojo.resource.CreateResourceDTO
import com.tencent.devops.auth.pojo.resource.ResourceInfo
import com.tencent.devops.auth.pojo.resource.UpdateResourceDTO
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.model.auth.tables.TAuthResource
import com.tencent.devops.model.auth.tables.records.TAuthResourceRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ResourceDao {

    fun createResource(
        dslContext: DSLContext,
        userId: String,
        resourceInfo: CreateResourceDTO
    ) {
        with(TAuthResource.T_AUTH_RESOURCE) {
            dslContext.insertInto(
                this,
                RESOURCETYPE,
                NAME,
                ENGLISHNAME,
                DESC,
                ENGLISHDESC,
                PARENT,
                CREATOR,
                CREATETIME,
                DELETE,
                SYSTEM
            ).values(
                resourceInfo.resourceId,
                resourceInfo.name,
                resourceInfo.englishName,
                resourceInfo.desc,
                resourceInfo.englishDes,
                resourceInfo.parent,
                userId,
                LocalDateTime.now(),
                false,
                resourceInfo.system.name
            ).execute()
        }
    }

    fun updateResource(
        dslContext: DSLContext,
        userId: String,
        resourceId: String,
        resourceInfo: UpdateResourceDTO
    ) {
        with(TAuthResource.T_AUTH_RESOURCE) {
            dslContext.update(this)
                .set(NAME, resourceInfo.name)
                .set(DESC, resourceInfo.desc)
                .set(ENGLISHDESC, resourceInfo.englishDes)
                .set(ENGLISHNAME, resourceInfo.englishName)
                .set(SYSTEM, resourceInfo.system.name)
                .where(RESOURCETYPE.eq(resourceId)).execute()
        }
    }

    fun getResourceById(
        dslContext: DSLContext,
        resourceId: String
    ): ResourceInfo? {
        with(TAuthResource.T_AUTH_RESOURCE) {
            val record = dslContext.selectFrom(this)
                .where(RESOURCETYPE.eq(resourceId).and(DELETE.eq(false))).fetchAny()
            return convert(record)
        }
    }

    fun getResourceBySystemId(
        dslContext: DSLContext,
        systemId: String
    ): Result<TAuthResourceRecord?> {
        with(TAuthResource.T_AUTH_RESOURCE) {
            return dslContext.selectFrom(this)
                .where(SYSTEM.eq(systemId).and(DELETE.eq(false)))
                .fetch()
        }
    }

    fun getAllResource(
        dslContext: DSLContext
    ): Result<TAuthResourceRecord?> {
        with(TAuthResource.T_AUTH_RESOURCE) {
            return dslContext.selectFrom(this)
                .where((DELETE.eq(false)))
                .fetch()
        }
    }

    fun convert(record: TAuthResourceRecord?): ResourceInfo? {
        if (record == null) {
            return null
        }
        return ResourceInfo(
            resourceId = record.resourcetype,
            name = record.name,
            desc = record.desc,
            englishDes = record.englishdesc,
            parent = record.parent,
            system = SystemType.get(record.system),
            creator = record.creator,
            creatorTime = DateTimeUtil.convertLocalDateTimeToTimestamp(record.createtime),
            updator = record.updater,
            updateTime = DateTimeUtil.convertLocalDateTimeToTimestamp(record.updatetime),
            englishName = record.englishname
        )
    }
}