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

package com.tencent.devops.prebuild.dao

import com.tencent.devops.model.prebuild.tables.TWebideIdeinfo
import com.tencent.devops.model.prebuild.tables.records.TWebideIdeinfoRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class WebIDEStatusDao {
    fun create(
        dslContext: DSLContext,
        owner: String,
        ip: String,
        machineType: String,
        agentStatus: Int,
        ideStatus: Int,
        ideVersion: String,
        serverCreateTime: Long,
        pipelineId: String
    ) {
        with(TWebideIdeinfo.T_WEBIDE_IDEINFO) {
            dslContext.insertInto(
            this,
            OWNER,
            IP,
            SERVER_TYPE,
            AGENT_STATUS,
            IDE_STATUS,
            IDE_VERSION,
            SERVER_CREATE_TIME,
            PIPELINE_ID
            ).values(
                    owner,
                    ip,
                    machineType,
                    agentStatus,
                    ideStatus,
                    ideVersion,
                    serverCreateTime,
                    pipelineId
            ).execute()
        }
    }

    fun get(dslContext: DSLContext, owner: String): List<TWebideIdeinfoRecord> {
        with(TWebideIdeinfo.T_WEBIDE_IDEINFO) {
            return dslContext.selectFrom(this)
                    .where(OWNER.eq(owner))
                    .fetch()
        }
    }

    fun getByIp(dslContext: DSLContext, owner: String, ip: String): TWebideIdeinfoRecord {
        with(TWebideIdeinfo.T_WEBIDE_IDEINFO) {
            return dslContext.selectFrom(this)
                    .where(OWNER.eq(owner))
                    .and(IP.eq(ip))
                    .fetchOne()!!
        }
    }

    fun del(dslContext: DSLContext, owner: String, ip: String) {
        with(TWebideIdeinfo.T_WEBIDE_IDEINFO) {
            dslContext.deleteFrom(this)
                    .where(OWNER.eq(owner))
                    .and(IP.eq(ip))
                    .execute()
        }
    }

    fun update(dslContext: DSLContext, owner: String, ip: String, agentStatus: Int, ideStatus: Int, ideVersion: String) {
        with(TWebideIdeinfo.T_WEBIDE_IDEINFO) {
            dslContext.update(this)
                    .set(AGENT_STATUS, agentStatus)
                    .set(IDE_STATUS, ideStatus)
                    .set(IDE_VERSION, ideVersion)
                    .where(OWNER.eq(owner))
                    .and(IP.eq(ip))
                    .execute()
        }
    }

    fun updatePipelineId(dslContext: DSLContext, owner: String, ip: String, pipelineId: String) {
        with(TWebideIdeinfo.T_WEBIDE_IDEINFO) {
            dslContext.update(this)
                    .set(PIPELINE_ID, pipelineId)
                    .where(OWNER.eq(owner))
                    .and(IP.eq(ip))
                    .execute()
        }
    }

    fun updateIDEHeartBeat(dslContext: DSLContext, owner: String, ip: String) {
        with(TWebideIdeinfo.T_WEBIDE_IDEINFO) {
            dslContext.update(this)
                    .set(IDE_LAST_UPDATE, System.currentTimeMillis())
                    .where(OWNER.eq(owner))
                    .and(IP.eq(ip))
                    .execute()
        }
    }
}
