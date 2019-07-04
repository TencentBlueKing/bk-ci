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

import com.tencent.devops.model.ticket.tables.TCert
import com.tencent.devops.model.ticket.tables.records.TCertRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import javax.ws.rs.NotFoundException

@Repository
class CertDao {
    fun has(dslContext: DSLContext, projectId: String, certId: String): Boolean {
        with(TCert.T_CERT) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(CERT_ID.eq(certId))
                .fetchOne() != null
        }
    }

    fun get(dslContext: DSLContext, projectId: String, certId: String): TCertRecord {
        with(TCert.T_CERT) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(CERT_ID.eq(certId))
                .fetchOne() ?: throw NotFoundException("证书($certId)不存在")
        }
    }

    fun getOrNull(dslContext: DSLContext, projectId: String, certId: String): TCertRecord? {
        with(TCert.T_CERT) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(CERT_ID.eq(certId))
                .fetchOne()
        }
    }

    fun create(
        dslContext: DSLContext,
        projectId: String,
        certId: String,
        certUserId: String,
        certType: String,
        certRemark: String,
        certP12FileName: String,
        certP12FileContent: ByteArray,
        certMpFileName: String,
        certMpFileContent: ByteArray,
        certJksFileName: String,
        certJksFileContent: ByteArray,
        certJksAlias: String?,
        certJksAliasCredentialId: String?,
        certDeveloperName: String,
        certTeamName: String,
        certUUID: String,
        certExpireDate: LocalDateTime,
        credentialId: String?
    ) {
        val now = LocalDateTime.now()
        with(TCert.T_CERT) {
            dslContext
                .insertInto(
                    this,
                    PROJECT_ID,
                    CERT_ID,
                    CERT_USER_ID,
                    CERT_TYPE,
                    CERT_REMARK,
                    CERT_P12_FILE_NAME,
                    CERT_P12_FILE_CONTENT,
                    CERT_MP_FILE_NAME,
                    CERT_MP_FILE_CONTENT,
                    CERT_JKS_FILE_NAME,
                    CERT_JKS_FILE_CONTENT,
                    CERT_JKS_ALIAS,
                    CERT_JKS_ALIAS_CREDENTIAL_ID,
                    CERT_DEVELOPER_NAME,
                    CERT_TEAM_NAME,
                    CERT_UUID,
                    CERT_EXPIRE_DATE,
                    CERT_CREATE_TIME,
                    CERT_UPDATE_TIME,
                    CREDENTIAL_ID
                )
                .values(
                    projectId,
                    certId,
                    certUserId,
                    certType,
                    certRemark,
                    certP12FileName,
                    certP12FileContent,
                    certMpFileName,
                    certMpFileContent,
                    certJksFileName,
                    certJksFileContent,
                    certJksAlias,
                    certJksAliasCredentialId,
                    certDeveloperName,
                    certTeamName,
                    certUUID,
                    certExpireDate,
                    now,
                    now,
                    credentialId
                )
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        projectId: String,
        certId: String,
        certUserId: String,
        certRemark: String,
        certP12FileName: String?,
        certP12FileContent: ByteArray?,
        certMpFileName: String?,
        certMpFileContent: ByteArray?,
        certJksFileName: String?,
        certJksFileContent: ByteArray?,
        certJksAlias: String?,
        certJksAliasCredentialId: String?,
        certDeveloperName: String?,
        certTeamName: String?,
        certUUID: String?,
        certExpireDate: LocalDateTime?,
        credentialId: String?
    ) {
        with(TCert.T_CERT) {
            val step = dslContext.update(this)
                .set(CERT_USER_ID, certUserId)
                .set(CERT_REMARK, certRemark)

            val step1 = if (certP12FileName != null) step.set(CERT_P12_FILE_NAME, certP12FileName) else step
            val step2 = if (certP12FileContent != null) step.set(CERT_P12_FILE_CONTENT, certP12FileContent) else step1
            val step3 = if (certMpFileName != null) step.set(CERT_MP_FILE_NAME, certMpFileName) else step2
            val step4 = if (certMpFileContent != null) step.set(CERT_MP_FILE_CONTENT, certMpFileContent) else step3
            val step5 = if (certJksFileName != null) step.set(CERT_JKS_FILE_NAME, certJksFileName) else step4
            val step6 = if (certJksFileContent != null) step.set(CERT_JKS_FILE_CONTENT, certJksFileContent) else step5
            val step7 = if (certJksAlias != null) step.set(CERT_JKS_ALIAS, certJksAlias) else step6
            val step8 = if (certJksAliasCredentialId != null) step.set(
                CERT_JKS_ALIAS_CREDENTIAL_ID,
                certJksAliasCredentialId
            ) else step7
            val step9 = if (certDeveloperName != null) step.set(CERT_DEVELOPER_NAME, certDeveloperName) else step8
            val step10 = if (certTeamName != null) step.set(CERT_TEAM_NAME, certTeamName) else step9
            val step11 = if (certUUID != null) step.set(CERT_UUID, certUUID) else step10
            val step12 = if (certExpireDate != null) step.set(CERT_EXPIRE_DATE, certExpireDate) else step11
            val step13 = if (credentialId != null) step.set(CREDENTIAL_ID, credentialId) else step12

            step13.where(PROJECT_ID.eq(projectId))
                .and(CERT_ID.eq(certId))
                .execute()
        }
    }

    fun countByProject(
        dslContext: DSLContext,
        projectId: String,
        certType: String?,
        certIds: Set<String>
    ): Long {
        with(TCert.T_CERT) {
            return when (certType) {
                null -> {
                    dslContext.selectCount()
                        .from(this)
                        .where(PROJECT_ID.eq(projectId))
                        .and(CERT_ID.`in`(certIds))
                        .fetchOne(0, Long::class.java)
                }
                else -> {
                    dslContext.selectCount()
                        .from(this)
                        .where(PROJECT_ID.eq(projectId))
                        .and(CERT_ID.`in`(certIds))
                        .and(CERT_TYPE.eq(certType))
                        .fetchOne(0, Long::class.java)
                }
            }
        }
    }

    fun listByProject(
        dslContext: DSLContext,
        projectId: String,
        certType: String?,
        certIds: Set<String>,
        offset: Int,
        limit: Int
    ): Result<TCertRecord> {
        return with(TCert.T_CERT) {
            val query = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
            if (certType != null) {
                query.and(CERT_TYPE.eq(certType))
            }
            if (certIds.isNotEmpty()) {
                query.and(CERT_ID.`in`(certIds))
            }
            query.orderBy(CERT_CREATE_TIME.desc())
                .limit(offset, limit)
                .fetch()
        }
    }

    fun delete(dslContext: DSLContext, projectId: String, certId: String) {
        with(TCert.T_CERT) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(CERT_ID.eq(certId))
                .execute()
        }
    }

    fun listIdByProject(dslContext: DSLContext, projectId: String, offset: Int, limit: Int): List<Int> {
        return with(TCert.T_CERT) {
            dslContext.select(CERT_ID)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .orderBy(CERT_CREATE_TIME.desc())
                .limit(offset, limit)
                .fetch(CERT_ID, Int::class.java)
        }
    }
}
