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

package com.tencent.devops.store.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TExtensionServiceEnvInfo
import com.tencent.devops.model.store.tables.records.TExtensionServiceEnvInfoRecord
import com.tencent.devops.store.pojo.ExtServiceEnvCreateInfo
import com.tencent.devops.store.pojo.dto.UpdateExtServiceEnvInfoDTO
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExtServiceEnvDao {
    fun create(
        dslContext: DSLContext,
        extServiceEnvCreateInfo: ExtServiceEnvCreateInfo
    ) {
        with(TExtensionServiceEnvInfo.T_EXTENSION_SERVICE_ENV_INFO) {
            dslContext.insertInto(
                this,
                ID,
                SERVICE_ID,
                LANGUAGE,
                PKG_PATH,
                PKG_SHA_CONTENT,
                DOCKER_FILE_CONTENT,
                IMAGE_PATH,
                CREATOR,
                MODIFIER,
                CREATE_TIME,
                UPDATE_TIME
            )
                .values(
                    UUIDUtil.generate(),
                    extServiceEnvCreateInfo.serviceId,
                    extServiceEnvCreateInfo.language,
                    extServiceEnvCreateInfo.pkgPath,
                    extServiceEnvCreateInfo.pkgShaContent,
                    extServiceEnvCreateInfo.dockerFileContent,
                    extServiceEnvCreateInfo.imagePath,
                    extServiceEnvCreateInfo.creatorUser,
                    extServiceEnvCreateInfo.modifierUser,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                )
                .execute()
        }
    }

    fun updateExtServiceEnvInfo(
        dslContext: DSLContext,
        serviceId: String,
        updateExtServiceEnvInfo: UpdateExtServiceEnvInfoDTO
    ) {
        with(TExtensionServiceEnvInfo.T_EXTENSION_SERVICE_ENV_INFO) {
            val baseStep = dslContext.update(this)
            val pkgPath = updateExtServiceEnvInfo.pkgPath
            if (null != pkgPath) {
                baseStep.set(PKG_PATH, pkgPath)
            }
            val pkgShaContent = updateExtServiceEnvInfo.pkgShaContent
            if (null != pkgShaContent) {
                baseStep.set(PKG_SHA_CONTENT, pkgShaContent)
            }
            val dockerFileContent = updateExtServiceEnvInfo.dockerFileContent
            if (null != dockerFileContent) {
                baseStep.set(DOCKER_FILE_CONTENT, dockerFileContent)
            }
            val imagePath = updateExtServiceEnvInfo.imagePath
            if (null != imagePath) {
                baseStep.set(IMAGE_PATH, imagePath)
            }
            baseStep.set(MODIFIER, updateExtServiceEnvInfo.userId).set(UPDATE_TIME, LocalDateTime.now())
                .where(SERVICE_ID.eq(serviceId))
                .execute()
        }
    }

    fun getMarketServiceEnvInfoByServiceId(dslContext: DSLContext, serviceId: String): TExtensionServiceEnvInfoRecord? {
        return with(TExtensionServiceEnvInfo.T_EXTENSION_SERVICE_ENV_INFO) {
            dslContext.selectFrom(this)
                .where(SERVICE_ID.eq(serviceId))
                .fetchOne()
        }
    }

    fun deleteEnvInfo(dslContext: DSLContext, extServiceIds: List<String>) {
        with(TExtensionServiceEnvInfo.T_EXTENSION_SERVICE_ENV_INFO) {
            dslContext.deleteFrom(this)
                .where(SERVICE_ID.`in`(extServiceIds))
                .execute()
        }
    }
}
