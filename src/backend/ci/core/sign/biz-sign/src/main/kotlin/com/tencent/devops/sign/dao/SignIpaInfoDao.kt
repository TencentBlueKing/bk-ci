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

package com.tencent.devops.sign.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.sign.tables.TSignIpaInfo
import com.tencent.devops.sign.api.pojo.AppexSignInfo
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.model.sign.tables.records.TSignIpaInfoRecord
import java.time.LocalDateTime

@Repository
class SignIpaInfoDao {

    fun saveSignInfo(
        dslContext: DSLContext,
        resignId: String,
        ipaSignInfoHeader: String,
        info: IpaSignInfo?
    ) {
        with(TSignIpaInfo.T_SIGN_IPA_INFO) {
            dslContext.insertInto(this,
                RESIGN_ID,
                CERT_ID,
                ARCHIVE_TYPE,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                TASK_ID,
                ARCHIVE_PATH,
                MOBILE_PROVISION_ID,
                UNIVERSAL_LINKS,
                KEYCHAIN_ACCESS_GROUPS,
                REPLACE_BUNDLE,
                APPEX_SIGN_INFO,
                FILENAME,
                FILE_SIZE,
                FILE_MD5,
                USER_ID,
                WILDCARD,
                REQUEST_CONTENT,
                CREATE_TIME
            ).values(
                resignId,
                info?.certId,
                info?.archiveType,
                info?.projectId,
                info?.pipelineId,
                info?.buildId,
                info?.taskId,
                info?.archivePath,
                info?.mobileProvisionId,
                if (info?.universalLinks != null) JsonUtil.toJson(info.universalLinks!!) else null,
                if (info?.keychainAccessGroups != null) JsonUtil.toJson(info.keychainAccessGroups!!) else null,
                info?.replaceBundleId,
                if (info?.appexSignInfo != null) JsonUtil.toJson(info.appexSignInfo!!) else null,
                info?.fileName,
                info?.fileSize,
                info?.md5,
                info?.userId,
                info?.wildcard,
                ipaSignInfoHeader,
                LocalDateTime.now()
            ).execute()
        }
    }

    fun getSignInfo(
        dslContext: DSLContext,
        resignId: String
    ): IpaSignInfo? {
        with(TSignIpaInfo.T_SIGN_IPA_INFO) {
            val record = dslContext.selectFrom(this)
                .where(RESIGN_ID.eq(resignId))
                .fetchOne() ?: return null

            val universalLinks = if (record.universalLinks != null) {
                JsonUtil.getObjectMapper().readValue<MutableList<String>>(record.universalLinks!!)
            } else null
            val keychainAccessGroups = if (record.keychainAccessGroups != null) {
                JsonUtil.getObjectMapper().readValue<MutableList<String>>(record.keychainAccessGroups!!)
            } else null
            val appexSignInfo = if (record.appexSignInfo != null) {
                JsonUtil.getObjectMapper().readValue<MutableList<AppexSignInfo>>(record.appexSignInfo!!)
            } else null

            return IpaSignInfo(
                certId = record.certId,
                archiveType = record.archiveType,
                projectId = record.projectId,
                pipelineId = record.pipelineId,
                buildId = record.buildId,
                archivePath = record.archivePath,
                mobileProvisionId = record.mobileProvisionId,
                universalLinks = universalLinks,
                keychainAccessGroups = keychainAccessGroups,
                replaceBundleId = record.replaceBundle,
                appexSignInfo = appexSignInfo,
                fileName = record.filename,
                fileSize = record.fileSize,
                md5 = record.fileMd5,
                userId = record.userId,
                wildcard = record.wildcard
            )
        }
    }

    fun getSignInfoRecord(
        dslContext: DSLContext,
        resignId: String
    ): TSignIpaInfoRecord? {
        with(TSignIpaInfo.T_SIGN_IPA_INFO) {
            return dslContext.selectFrom(this)
                .where(RESIGN_ID.eq(resignId))
                .fetchOne()
        }
    }
}
