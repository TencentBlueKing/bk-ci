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

package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.remotedev.tables.TRemotedevCvm
import com.tencent.devops.model.remotedev.tables.records.TRemotedevCvmRecord
import com.tencent.devops.remotedev.pojo.op.RemotedevCvmData
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class RemoteDevCvmDao {
    fun batchAddCvm(
        dslContext: DSLContext,
        cvmList: List<RemotedevCvmData>
    ) {
        if (cvmList.isEmpty()) {
            return
        }
        dslContext.batch(
            cvmList.map {
            with(TRemotedevCvm.T_REMOTEDEV_CVM) {
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    IP,
                    ZONE,
                    AVAILABLE_REGION,
                    CPU,
                    MEMORY,
                    SUBNET,
                    CREATE_TIME
                ).values(
                    it.projectId,
                    it.ip,
                    it.zone,
                    it.availableRegion,
                    it.cpu,
                    it.memory,
                    it.subnet,
                    LocalDateTime.now()
                ).onDuplicateKeyIgnore()
            }
        }
        ).execute()
    }

    fun countAllCvmList(
        dslContext: DSLContext,
        projectId: String?,
        zone: String?,
        ips: List<String>?
    ): Long {
            val conditions = mutableListOf<Condition>()
            with(TRemotedevCvm.T_REMOTEDEV_CVM) {
                projectId?.let {
                    conditions.add(PROJECT_ID.eq(it))
                }
                zone?.let {
                    conditions.add(ZONE.eq(it))
                }
                ips?.let {
                    conditions.add(IP.`in`(ips))
                }
            }
            return dslContext.selectCount().from(TRemotedevCvm.T_REMOTEDEV_CVM)
                .where(conditions)
                .skipCheck()
                .fetchOne(0, Long::class.java)!!
    }

    fun queryCvmList(
        dslContext: DSLContext,
        projectId: String?,
        zone: String?,
        ips: List<String>?,
        limit: SQLLimit
    ): Result<TRemotedevCvmRecord> {
        val conditions = mutableListOf<Condition>()
        with(TRemotedevCvm.T_REMOTEDEV_CVM) {
            projectId?.let {
                conditions.add(PROJECT_ID.eq(it))
            }
            zone?.let {
                conditions.add(ZONE.eq(it))
            }
            ips?.let {
                conditions.add(IP.`in`(ips))
            }
        }
        return dslContext.selectFrom(TRemotedevCvm.T_REMOTEDEV_CVM)
            .where(conditions)
            .limit(limit.limit).offset(limit.offset)
            .skipCheck()
            .fetch()
    }

    fun updateCvm(
        id: Long,
        data: RemotedevCvmData,
        dslContext: DSLContext
    ) {
        with(TRemotedevCvm.T_REMOTEDEV_CVM) {
            dslContext.update(this)
                .set(PROJECT_ID, data.projectId)
                .set(IP, data.ip)
                .set(ZONE, data.zone)
                .set(AVAILABLE_REGION, data.availableRegion)
                .set(CPU, data.cpu)
                .set(MEMORY, data.memory)
                .set(SUBNET, data.subnet)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(id))
                .execute()
        }
    }

    fun deleteCvm(
        id: Long,
        dslContext: DSLContext
    ) {
        with(TRemotedevCvm.T_REMOTEDEV_CVM) {
            dslContext.delete(this)
                .where(ID.eq(id))
                .limit(1)
                .execute()
        }
    }
}
