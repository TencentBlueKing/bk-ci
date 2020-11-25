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

package com.tencent.devops.plugin.service

import com.tencent.devops.model.plugin.tables.TPluginWetestEamilGroup
import com.tencent.devops.plugin.client.WeTestClient
import com.tencent.devops.plugin.dao.WetestEmailGroupDao
import com.tencent.devops.plugin.pojo.wetest.WetestEmailGroup
import com.tencent.devops.plugin.pojo.wetest.WetestReportResponse
import com.tencent.devops.plugin.utils.CommonUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WetestEmailGroupService @Autowired constructor(
    private val wetestEmailGroupDao: WetestEmailGroupDao,
    private val dslContext: DSLContext
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WetestEmailGroupService::class.java)
    }

    fun getUserEmailGroup(projectId: String, userId: String): WetestReportResponse {
        logger.info("get UserEmailGroup, projectId:$projectId, userId:$userId")
        val (secretId, secretKey) = CommonUtils.getCredential(userId)
        val weTestClient = WeTestClient(secretId, secretKey)

        return weTestClient.getWetestGroup()
    }

    fun createWetestEmailGroup(projectId: String, name: String, userInternal: String?, qqExternal: String?, description: String?, wetestGroupId: String?, wetestGroupName: String?): Int {
        logger.info("create weTest email group, projectId:$projectId, name:$name, userInternal: $userInternal, qqExternal: $qqExternal, description: $description")
        return wetestEmailGroupDao.insert(dslContext, projectId, name, userInternal, qqExternal, description, wetestGroupId, wetestGroupName)
    }

    fun updateWetestEmailGroup(projectId: String, id: Int, name: String, userInternal: String?, qqExternal: String?, description: String?, wetestGroupId: String?, wetestGroupName: String?) {
        logger.info("update weTest email group: id: $id, name:$name, userInternal: $userInternal, qqExternal: $qqExternal, description: $description")
        return wetestEmailGroupDao.update(dslContext, projectId, id, name, userInternal, qqExternal, description, wetestGroupId, wetestGroupName)
    }

    fun getWetestEmailGroup(projectId: String, id: Int): WetestEmailGroup? {
        val record = wetestEmailGroupDao.getRecord(dslContext, projectId, id)
        if (null != record) {
            with(TPluginWetestEamilGroup.T_PLUGIN_WETEST_EAMIL_GROUP) {
                return WetestEmailGroup(
                        record.id,
                        record.projectId,
                        record.name,
                        record.userInternal,
                        record.qqExternal,
                        record.description,
                        record.createdTime.toString(),
                        record.updatedTime.toString(),
                        record.wetestGroupId,
                        record.wetestGroupName
                )
            }
        }
        return null
    }

    fun deleteWetestEmailGroup(projectId: String, id: Int) {
        logger.info("delete weTest email group: id: $id, projectId: $projectId")
        return wetestEmailGroupDao.delete(dslContext, projectId, id)
    }

    fun getList(projectId: String, page: Int, pageSize: Int): List<WetestEmailGroup> {
        val recordList = wetestEmailGroupDao.getList(dslContext, projectId, page, pageSize)
        val result = mutableListOf<WetestEmailGroup>()
        if (recordList != null) {
            with(TPluginWetestEamilGroup.T_PLUGIN_WETEST_EAMIL_GROUP) {
                for (item in recordList) {
                    result.add(
                            WetestEmailGroup(
                                    id = item.get(ID),
                                    projectId = item.get(PROJECT_ID),
                                    name = item.get(NAME),
                                    userInternal = item.get(USER_INTERNAL),
                                    qqExternal = item.get(QQ_EXTERNAL),
                                    description = item.get(DESCRIPTION),
                                    createTime = item.get(CREATED_TIME).toString(),
                                    updateTime = item.get(UPDATED_TIME).toString(),
                                    wetestGroupId = item.get(WETEST_GROUP_ID),
                                    wetestGroupName = item.get(WETEST_GROUP_NAME)
                            )
                    )
                }
            }
        }
        return result
    }

    fun getCount(projectId: String): Int {
        return wetestEmailGroupDao.getCount(dslContext, projectId)
    }

    fun getByName(projectId: String, name: String): WetestEmailGroup? {
        val record = wetestEmailGroupDao.getByName(dslContext, projectId, name)
        if (null != record) {
            with(TPluginWetestEamilGroup.T_PLUGIN_WETEST_EAMIL_GROUP) {
                return WetestEmailGroup(
                        record.id,
                        record.projectId,
                        record.name,
                        record.userInternal,
                        record.qqExternal,
                        record.description,
                        record.createdTime.toString(),
                        record.updatedTime.toString(),
                        record.wetestGroupId,
                        record.wetestGroupName
                )
            }
        }
        return null
    }
}