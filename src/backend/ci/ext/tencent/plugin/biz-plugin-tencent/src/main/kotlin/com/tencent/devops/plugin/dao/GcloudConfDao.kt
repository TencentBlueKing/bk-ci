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

package com.tencent.devops.plugin.dao

import com.tencent.devops.model.plugin.tables.TPluginGcloudConf
import com.tencent.devops.model.plugin.tables.records.TPluginGcloudConfRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

@Repository
class GcloudConfDao {

    fun insert(
        dslContext: DSLContext,
        region: String,
        address: String,
        fileAddress: String,
        userId: String,
        remark: String?
    ): Int {

        with(TPluginGcloudConf.T_PLUGIN_GCLOUD_CONF) {
            val data = dslContext.insertInto(this,
                    REGION,
                    ADDRESS,
                    ADDRESS_FILE,
                    UPDATE_TIME,
                    USER_ID,
                    REMARK)
                    .values(region,
                            address,
                            fileAddress,
                            LocalDateTime.ofInstant(Date(System.currentTimeMillis()).toInstant(),
                                ZoneId.systemDefault()),
                            userId,
                            remark)
                    .returning(ID)
                    .fetchOne()!!
            return data.id
        }
    }

    fun update(
        dslContext: DSLContext,
        id: Int,
        region: String,
        address: String,
        fileAddress: String,
        userId: String,
        remark: String
    ): Int {
        with(TPluginGcloudConf.T_PLUGIN_GCLOUD_CONF) {
            return dslContext.update(this)
                    .set(REGION, region)
                    .set(ADDRESS, address)
                    .set(ADDRESS_FILE, fileAddress)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .set(USER_ID, userId)
                    .set(REMARK, remark)
                    .where(ID.eq(id))
                    .execute()
        }
    }

    fun getList(
        dslContext: DSLContext,
        page: Int,
        pageSize: Int
    ): Result<TPluginGcloudConfRecord>? {
        with(TPluginGcloudConf.T_PLUGIN_GCLOUD_CONF) {
            return dslContext.selectFrom(this)
                    .orderBy(UPDATE_TIME.desc())
                    .limit(pageSize).offset((page - 1) * pageSize)
                    .fetch()
        }
    }

    fun getCount(dslContext: DSLContext): Int {
        with(TPluginGcloudConf.T_PLUGIN_GCLOUD_CONF) {
            return dslContext.selectCount().from(this)
                    .fetchOne()!!.get(0) as Int
        }
    }

    fun getRecord(
        dslContext: DSLContext,
        id: Int
    ): TPluginGcloudConfRecord? {
        with(TPluginGcloudConf.T_PLUGIN_GCLOUD_CONF) {
            return dslContext.selectFrom(this)
                    .where(ID.eq(id))
                    .fetchOne()
        }
    }

    fun delete(
        dslContext: DSLContext,
        id: Int
    ): Int {
        with(TPluginGcloudConf.T_PLUGIN_GCLOUD_CONF) {
            return dslContext.deleteFrom(this)
                    .where(ID.eq(id))
                    .execute()
        }
    }
}

// CREATE TABLE `devops_plugin`.`T_PLUGIN_GCLOUD_CONF`(
// `ID` int(11) NOT NULL AUTO_INCREMENT,
// `REGION` varchar(1024) DEFAULT NULL,
// `ADDRESS` varchar(1024) DEFAULT NULL,
// `UPDATE_TIME` datetime DEFAULT NULL,
// `USER_ID` varchar(64) DEFAULT NULL,
// `REMARK` varchar(1024) DEFAULT NULL,
// PRIMARY KEY (`ID`)
// ) ENGINE=InnoDB DEFAULT CHARSET='utf8' COLLATE='utf8_general_ci';
