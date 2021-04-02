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

package com.tencent.devops.plugin.service.gcloud

import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.plugin.tables.TPluginGcloudConf
import com.tencent.devops.plugin.dao.GcloudConfDao
import com.tencent.devops.plugin.pojo.GcloudConf
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GcloudConfService @Autowired constructor(
    private val gcloudConfDao: GcloudConfDao,
    private val dslContext: DSLContext
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GcloudConfService::class.java)
    }

    fun createGcloudConf(region: String, address: String, fileAddress: String, userId: String, remark: String?): Int {
        logger.info("craete gcloud conf: region:$region, address: $address, userId: $userId, remark: $remark")
        return gcloudConfDao.insert(dslContext, region, address, fileAddress, userId, remark)
    }

    fun updateGcloudConf(id: Int, region: String, address: String, fileAddress: String, userId: String, remark: String?): Int {
        logger.info("update gcloud conf: id: $id, region:$region, address: $address, userId: $userId, remark: $remark")
        return gcloudConfDao.update(dslContext, id, region, address, fileAddress, userId, remark ?: "")
    }

    fun getGcloudConf(id: Int): GcloudConf? {
        val record = gcloudConfDao.getRecord(dslContext, id)
        if (null != record) {
            with(TPluginGcloudConf.T_PLUGIN_GCLOUD_CONF) {
                return GcloudConf(
                        record.id.toString(),
                        record.region,
                        record.address,
                        record.addressFile,
                        record.updateTime.timestampmilli(),
                        record.userId,
                        record.remark
                )
            }
        }
        return null
    }

    fun deleteGcloudConf(id: Int): Int {
        logger.info("delete gcloud conf: id: $id")
        return gcloudConfDao.delete(dslContext, id)
    }

    fun getList(page: Int, pageSize: Int): List<GcloudConf> {
        val recordList = gcloudConfDao.getList(dslContext, page, pageSize)
        val result = mutableListOf<GcloudConf>()
        if (recordList != null) {
            with(TPluginGcloudConf.T_PLUGIN_GCLOUD_CONF) {
                for (item in recordList) {
                    result.add(
                            GcloudConf(
                                    id = item.get(ID).toString(),
                                    region = item.get(REGION),
                                    address = item.get(ADDRESS),
                                    fileAddress = item.get(ADDRESS_FILE),
                                    updateTime = item.get(UPDATE_TIME).timestamp(),
                                    userId = item.get(USER_ID),
                                    remark = item.get(REMARK)
                            )
                    )
                }
            }
        }
        return result
    }

    fun getCount(): Int {
        return gcloudConfDao.getCount(dslContext)
    }
}
