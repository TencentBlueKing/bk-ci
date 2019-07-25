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

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TContainerResourceRel
import com.tencent.devops.model.store.tables.records.TContainerResourceRelRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class ContainerResourceRelDao {

    fun add(dslContext: DSLContext, id: String, containerId: String, resourceId: String) {
        with(TContainerResourceRel.T_CONTAINER_RESOURCE_REL) {
            dslContext.insertInto(
                this,
                ID,
                CONTAINER_ID,
                RESOURCE_ID
            )
                .values(
                    id,
                    containerId,
                    resourceId
                )
                .execute()
        }
    }

    fun batchAdd(dslContext: DSLContext, containerId: String, resourceIdList: List<String>) {
        with(TContainerResourceRel.T_CONTAINER_RESOURCE_REL) {
            val bachExceute =
                dslContext.batch("INSERT INTO T_CONTAINER_RESOURCE_REL(ID, CONTAINER_ID, RESOURCE_ID) VALUES (?,?,?)")
            for (item in resourceIdList) {
                bachExceute.bind(UUIDUtil.generate(), containerId, item)
            }
            bachExceute.execute()
        }
    }

    fun listByContainerId(dslContext: DSLContext, containerId: String): Result<TContainerResourceRelRecord>? {
        with(TContainerResourceRel.T_CONTAINER_RESOURCE_REL) {
            return dslContext.selectFrom(this)
                .where(CONTAINER_ID.eq(containerId))
                .fetch()
        }
    }

    fun delete(dslContext: DSLContext, id: String) {
        with(TContainerResourceRel.T_CONTAINER_RESOURCE_REL) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun deleteByContainerId(dslContext: DSLContext, containerId: String) {
        with(TContainerResourceRel.T_CONTAINER_RESOURCE_REL) {
            dslContext.deleteFrom(this)
                .where(CONTAINER_ID.eq(containerId))
                .execute()
        }
    }
}