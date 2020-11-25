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

package com.tencent.devops.support.services

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.support.dao.AppVersionDao
import com.tencent.devops.support.model.app.AppVersionRequest
import com.tencent.devops.support.model.app.pojo.AppVersion
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.tencent.devops.model.support.tables.records.TAppVersionRecord
import org.jooq.DSLContext

@Service
class AppVersionService @Autowired constructor(
    private val dslContext: DSLContext,
    private val appVersionDao: AppVersionDao
) {
    private val logger = LoggerFactory.getLogger(AppVersionService::class.java)

    fun getAllAppVersion(): List<AppVersion> {
        val appVersionResult = mutableListOf<AppVersion>()
        var appVersionRecords = appVersionDao.getAllAppVersion(dslContext)
        appVersionRecords?.forEach {
            appVersionResult.add(
                    AppVersion(
                            it.id,
                            it.versionId,
                            it.releaseDate.timestampmilli(),
                            it.releaseContent,
                            it.channelType
                    )
            )
        }
        return appVersionResult
    }

    fun getAllAppVersionByChannelType(channelType: Byte): List<AppVersion> {
        val appVersionResult = mutableListOf<AppVersion>()
        var appVersionRecords = appVersionDao.getAllAppVersionByChannelType(dslContext, channelType)
        appVersionRecords?.forEach {
            appVersionResult.add(
                    AppVersion(
                            it.id,
                            it.versionId,
                            it.releaseDate.timestampmilli(),
                            it.releaseContent,
                            it.channelType
                    )
            )
        }
        return appVersionResult
    }

    fun getAppVersion(appVersionId: Long): AppVersion? {
        var appVersionRecord = appVersionDao.getAppVersion(dslContext, appVersionId)
        return if (appVersionRecord == null) {
            null
        } else {
            convertAppVersion(appVersionRecord)
        }
    }

    fun setAppVersion(appVersionId: Long? = null, versionRequest: AppVersionRequest): Int {
        return appVersionDao.setAppVersion(dslContext, appVersionId, versionRequest)
    }

    fun deleteAppVersion(appVersionId: Long): Int {
        return appVersionDao.deleteAppVersion(dslContext, appVersionId)
    }

    fun getLastAppVersion(channelType: Byte): AppVersion? {
        val appVersionRecord = appVersionDao.getLastAppVersion(dslContext, channelType)
        return if (appVersionRecord == null) {
            null
        } else {
            convertAppVersion(appVersionRecord)
        }
    }
    fun convertAppVersion(appVersionRecord: TAppVersionRecord): AppVersion {
        return AppVersion(
                appVersionRecord.id,
                appVersionRecord.versionId,
                appVersionRecord.releaseDate.timestampmilli(),
                appVersionRecord.releaseContent,
                appVersionRecord.channelType
        )
    }
}
