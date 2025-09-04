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

import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.common.service.utils.ByteUtils
import com.tencent.devops.model.remotedev.tables.TWindowsResourceType
import com.tencent.devops.model.remotedev.tables.records.TWindowsResourceTypeRecord
import com.tencent.devops.remotedev.pojo.WindowsResourceTypeConfig
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class WindowsResourceTypeDao {

    fun save(
        dslContext: DSLContext,
        config: WindowsResourceTypeConfig
    ): Long {
        return with(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE) {
            dslContext.insertInto(
                this,
                SIZE,
                TYPE,
                GPU,
                VGPU,
                CPU,
                VCPU,
                MEMORY,
                VMEMORY,
                DISK,
                HDISK,
                SDISK,
                WEIGHT,
                DESCRIPTION,
                SPEC_MODEL
            ).values(
                config.size,
                config.type ?: "",
                config.gpu,
                config.vgpu,
                config.cpu,
                config.vcpu,
                config.memory,
                config.vmemory,
                config.disk,
                config.hdisk,
                config.sdisk,
                config.weight ?: 0,
                config.description,
                config.specModel
            ).returning(ID).fetchOne()!!.id
        }
    }

    fun fetchAll(
        dslContext: DSLContext,
        withUnavailable: Boolean = false,
        specModel: Boolean?
    ): List<WindowsResourceTypeConfig> {
        return with(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE) {
            val sql = dslContext.selectFrom(this)
            if (!withUnavailable && specModel == null) {
                sql.where(AVAILABLED.eq(1))
            } else if (withUnavailable && specModel != null) {
                sql.where(SPEC_MODEL.eq(specModel))
            } else if (!withUnavailable && specModel != null) {
                sql.where(AVAILABLED.eq(1)).and(SPEC_MODEL.eq(specModel))
            }

            sql.orderBy(WEIGHT)
                .skipCheck()
                .fetch(mapper)
        }
    }

    fun fetchAny(
        dslContext: DSLContext,
        id: Int
    ): WindowsResourceTypeConfig? {
        return with(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE) {
            dslContext.selectFrom(this)
                .where(ID.eq(id.toLong()))
                .fetchAny(mapper)
        }
    }

    fun fetchAny(
        dslContext: DSLContext,
        machineType: String
    ): WindowsResourceTypeConfig? {
        with(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE) {
            val conditions = mutableListOf<Condition>()
            machineType.let { conditions.add(SIZE.eq(it)) }
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetchAny(mapper)
        }
    }

    fun updateWindowsResourceConfig(
        id: Long,
        config: WindowsResourceTypeConfig,
        dslContext: DSLContext
    ) {
        with(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE) {
            dslContext.update(this)
                .set(SIZE, config.size)
                .set(TYPE, config.type ?: "")
                .set(VGPU, config.vgpu)
                .set(VCPU, config.vcpu)
                .set(VMEMORY, config.vmemory)
                .set(DISK, config.disk)
                .set(HDISK, config.hdisk)
                .set(SDISK, config.sdisk)
                .set(AVAILABLED, if (config.available == true) 1 else 0)
                .set(DESCRIPTION, config.description)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(WEIGHT, config.weight ?: 0)
                .set(SPEC_MODEL, config.specModel)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun deleteWindowsResource(
        id: Long,
        dslContext: DSLContext
    ) {
        with(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE) {
            dslContext.delete(this)
                .where(ID.eq(id))
                .limit(1)
                .execute()
        }
    }

    class TWindowsResourceTypeJooqMapper : RecordMapper<TWindowsResourceTypeRecord, WindowsResourceTypeConfig> {
        override fun map(record: TWindowsResourceTypeRecord?): WindowsResourceTypeConfig? {
            return record?.run {
                WindowsResourceTypeConfig(
                    id = id,
                    available = ByteUtils.byte2Bool(availabled),
                    size = size,
                    type = type,
                    gpu = gpu,
                    vgpu = vgpu,
                    cpu = cpu,
                    vcpu = vcpu,
                    memory = memory,
                    vmemory = vmemory,
                    disk = disk,
                    hdisk = hdisk,
                    sdisk = sdisk,
                    weight = weight,
                    description = description,
                    specModel = specModel
                )
            }
        }
    }

    companion object {
        val mapper = TWindowsResourceTypeJooqMapper()
    }
}
