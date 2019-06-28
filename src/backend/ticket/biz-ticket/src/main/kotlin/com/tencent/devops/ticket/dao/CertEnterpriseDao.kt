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

package com.tencent.devops.ticket.dao

import com.tencent.devops.model.ticket.tables.TCertEnterprise
import com.tencent.devops.model.ticket.tables.records.TCertEnterpriseRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import javax.ws.rs.NotFoundException

@Repository
class CertEnterpriseDao {
    fun get(dslContext: DSLContext, projectId: String, certId: String): TCertEnterpriseRecord {
        with(TCertEnterprise.T_CERT_ENTERPRISE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(CERT_ID.eq(certId))
                .fetchOne() ?: throw NotFoundException("certId: $certId does not exists")
        }
    }

    fun create(
        dslContext: DSLContext,
        projectId: String,
        certId: String,
        certMpFileName: String,
        certMpFileContent: ByteArray,
        certDeveloperName: String,
        certTeamName: String,
        certUUID: String,
        certExpireDate: LocalDateTime

    ) {
        val now = LocalDateTime.now()
        with(TCertEnterprise.T_CERT_ENTERPRISE) {
            dslContext
                .insertInto(
                    this,
                    PROJECT_ID,
                    CERT_ID,
                    CERT_MP_FILE_NAME,
                    CERT_MP_FILE_CONTENT,
                    CERT_DEVELOPER_NAME,
                    CERT_TEAM_NAME,
                    CERT_UUID,
                    CERT_EXPIRE_DATE,
                    CERT_CREATE_TIME,
                    CERT_UPDATE_TIME
                )
                .values(
                    projectId,
                    certId,
                    certMpFileName,
                    certMpFileContent,
                    certDeveloperName,
                    certTeamName,
                    certUUID,
                    now,
                    certExpireDate,
                    now
                )
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        projectId: String,
        certId: String,
        certMpFileName: String,
        certMpFileContent: ByteArray,
        certDeveloperName: String?,
        certTeamName: String,
        certUUID: String,
        certExpireDate: LocalDateTime
    ) {
        with(TCertEnterprise.T_CERT_ENTERPRISE) {
            dslContext.update(this)
                .set(CERT_MP_FILE_NAME, certMpFileName)
                .set(CERT_MP_FILE_CONTENT, certMpFileContent)
                .set(CERT_DEVELOPER_NAME, certDeveloperName)
                .set(CERT_TEAM_NAME, certTeamName)
                .set(CERT_UUID, certUUID)
                .set(CERT_EXPIRE_DATE, certExpireDate)
                .where(PROJECT_ID.eq(projectId))
                .and(CERT_ID.eq(certId))
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, projectId: String, certId: String) {
        with(TCertEnterprise.T_CERT_ENTERPRISE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(CERT_ID.eq(certId))
                .execute()
        }
    }
}