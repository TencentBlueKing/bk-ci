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

package com.tencent.devops.environment.dao.slave

import com.tencent.devops.environment.pojo.slave.SlaveGateway
import com.tencent.devops.model.environment.tables.TEnvironmentSlaveGateway
import com.tencent.devops.model.environment.tables.records.TEnvironmentSlaveGatewayRecord
import org.jooq.DSLContext
import org.jooq.UpdateSetMoreStep
import org.springframework.stereotype.Repository

@Repository
class SlaveGatewayDao {

    fun list(dslContext: DSLContext): List<TEnvironmentSlaveGatewayRecord> {
        with(TEnvironmentSlaveGateway.T_ENVIRONMENT_SLAVE_GATEWAY) {
            return dslContext.selectFrom(this).fetch()
        }
    }

    fun refreshGateway(dslContext: DSLContext, oldToNewMap: Map<String, String>) {
        with(TEnvironmentSlaveGateway.T_ENVIRONMENT_SLAVE_GATEWAY) {
            dslContext.transaction { configuration ->
                val updates = mutableListOf<UpdateSetMoreStep<TEnvironmentSlaveGatewayRecord>>()
                val transactionContext = org.jooq.impl.DSL.using(configuration)
                transactionContext.selectFrom(this).fetch().forEach { record ->
                    oldToNewMap.forEach nextOne@{ (old, new) ->
                        if (!record.gateway.contains(old)) return@nextOne
                        val update = transactionContext.update(this)
                            .set(GATEWAY, record.gateway.replace(old, new))
                        update.where(ID.eq(record.id))
                        updates.add(update)
                    }
                }
                transactionContext.batch(updates).execute()
            }
        }
    }

    fun add(dslContext: DSLContext, gateWay: SlaveGateway): Boolean {
        with(TEnvironmentSlaveGateway.T_ENVIRONMENT_SLAVE_GATEWAY) {
            return dslContext.insertInto(this, NAME, SHOW_NAME, GATEWAY, FILE_GATEWAY, VISIBILITY)
                .values(gateWay.zoneName, gateWay.showName, gateWay.gateway, gateWay.fileGateway, gateWay.visibility)
                .execute() > 0
        }
    }

    fun update(dslContext: DSLContext, gateWay: SlaveGateway): Boolean {
        with(TEnvironmentSlaveGateway.T_ENVIRONMENT_SLAVE_GATEWAY) {
            return dslContext.update(this)
                .set(SHOW_NAME, gateWay.showName)
                .set(GATEWAY, gateWay.gateway)
                .set(FILE_GATEWAY, gateWay.fileGateway)
                .set(VISIBILITY, gateWay.visibility)
                .where(NAME.eq(gateWay.zoneName))
                .execute() > 0
        }
    }

    fun delete(dslContext: DSLContext, name: String): Boolean {
        with(TEnvironmentSlaveGateway.T_ENVIRONMENT_SLAVE_GATEWAY) {
            return dslContext.delete(this).where(NAME.eq(name)).execute() > 0
        }
    }
}
