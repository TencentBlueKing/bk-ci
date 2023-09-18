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
import com.tencent.devops.model.remotedev.tables.TWindowsResourceZone
import com.tencent.devops.model.remotedev.tables.records.TWindowsResourceZoneRecord
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfig
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.springframework.stereotype.Repository

@Repository
class WindowsResourceZoneDao {

    fun save(
        dslContext: DSLContext,
        config: WindowsResourceZoneConfig
    ): Long {
        return with(TWindowsResourceZone.T_WINDOWS_RESOURCE_ZONE) {
            dslContext.insertInto(
                this,
                ZONE,
                SHORT_NAME,
                DESCRIPTION
            ).values(
                config.zone,
                config.zoneShortName,
                config.description
            ).returning(ID).fetchOne()!!.id
        }
    }

    fun fetchAll(
        dslContext: DSLContext,
        withUnavailable: Boolean = false
    ): List<WindowsResourceZoneConfig> {
        return with(TWindowsResourceZone.T_WINDOWS_RESOURCE_ZONE) {
            dslContext.selectFrom(this).let {
                if (!withUnavailable) it.where(AVAILABLED.eq(1)) else it
            }
                .skipCheck()
                .fetch(mapper)
        }
    }

    fun fetchAny(
        dslContext: DSLContext,
        zoneId: String
    ): WindowsResourceZoneConfig? {
        return with(TWindowsResourceZone.T_WINDOWS_RESOURCE_ZONE) {
            dslContext.selectFrom(this)
                .where(SHORT_NAME.eq(zoneId))
                .fetchAny(mapper)
        }
    }

    fun updateWindowsResourceZoneConfig(
        id: Long,
        config: WindowsResourceZoneConfig,
        dslContext: DSLContext
    ) {
        with(TWindowsResourceZone.T_WINDOWS_RESOURCE_ZONE) {
            dslContext.update(this)
                .set(ZONE, config.zone)
                .set(SHORT_NAME, config.zoneShortName)
                .set(AVAILABLED, if (config.available == true) 1 else 0)
                .set(DESCRIPTION, config.description)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(id))
                .execute()
        }
    }

    fun deleteWindowsResource(
        id: Long,
        dslContext: DSLContext
    ) {
        with(TWindowsResourceZone.T_WINDOWS_RESOURCE_ZONE) {
            dslContext.delete(this)
                .where(ID.eq(id))
                .limit(1)
                .execute()
        }
    }

    class TWindowsResourceZoneJooqMapper : RecordMapper<TWindowsResourceZoneRecord, WindowsResourceZoneConfig> {
        override fun map(record: TWindowsResourceZoneRecord?): WindowsResourceZoneConfig? {
            return record?.run {
                WindowsResourceZoneConfig(
                    id = id,
                    available = ByteUtils.byte2Bool(availabled),
                    zone = zone,
                    zoneShortName = shortName,
                    description = description
                )
            }
        }
    }

    companion object {
        val mapper = TWindowsResourceZoneJooqMapper()
    }
}
