/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.repository.dao

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.repository.tables.TRepositoryScmConfig
import com.tencent.devops.model.repository.tables.records.TRepositoryScmConfigRecord
import com.tencent.devops.repository.pojo.RepositoryScmConfig
import com.tencent.devops.repository.pojo.enums.RepoCredentialType
import com.tencent.devops.repository.pojo.enums.ScmConfigOauthType
import com.tencent.devops.repository.pojo.enums.ScmConfigStatus
import com.tencent.devops.scm.spring.properties.ScmProviderProperties
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class RepositoryScmConfigDao {

    fun create(
        dslContext: DSLContext,
        scmConfig: RepositoryScmConfig
    ) {
        val now = LocalDateTime.now()
        with(TRepositoryScmConfig.T_REPOSITORY_SCM_CONFIG) {
            dslContext.insertInto(
                this,
                SCM_CODE,
                NAME,
                PROVIDER_CODE,
                SCM_TYPE,
                HOSTS,
                LOGO_URL,
                CREDENTIAL_TYPE_LIST,
                OAUTH_TYPE,
                OAUTH_SCM_CODE,
                STATUS,
                OAUTH2_ENABLED,
                MERGE_ENABLED,
                PAC_ENABLED,
                WEBHOOK_ENABLED,
                PROVIDER_PROPS,
                CREATOR,
                UPDATER,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                scmConfig.scmCode,
                scmConfig.name,
                scmConfig.providerCode,
                scmConfig.scmType.name,
                scmConfig.hosts,
                scmConfig.logoUrl,
                JsonUtil.toJson(scmConfig.credentialTypeList, false),
                scmConfig.oauthType.name,
                scmConfig.oauthScmCode,
                scmConfig.status.name,
                scmConfig.oauth2Enabled,
                scmConfig.mergeEnabled,
                scmConfig.pacEnabled,
                scmConfig.webhookEnabled,
                JsonUtil.toJson(scmConfig.providerProps, false),
                scmConfig.creator,
                scmConfig.updater,
                now,
                now
            ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        scmCode: String,
        scmConfig: RepositoryScmConfig
    ) {
        val now = LocalDateTime.now()
        with(TRepositoryScmConfig.T_REPOSITORY_SCM_CONFIG) {
            dslContext.update(this)
                .set(NAME, scmConfig.name)
                .set(HOSTS, scmConfig.hosts)
                .set(LOGO_URL, scmConfig.logoUrl)
                .set(
                    CREDENTIAL_TYPE_LIST,
                    JsonUtil.toJson(scmConfig.credentialTypeList, false)
                )
                .set(OAUTH_TYPE, scmConfig.oauthType.name)
                .set(OAUTH_SCM_CODE, scmConfig.oauthScmCode)
                .set(STATUS, scmConfig.status.name)
                .set(OAUTH2_ENABLED, scmConfig.oauth2Enabled)
                .set(MERGE_ENABLED, scmConfig.mergeEnabled)
                .set(PAC_ENABLED, scmConfig.pacEnabled)
                .set(WEBHOOK_ENABLED, scmConfig.webhookEnabled)
                .set(PROVIDER_PROPS, JsonUtil.toJson(scmConfig.providerProps, false))
                .set(UPDATER, scmConfig.updater)
                .set(UPDATE_TIME, now)
                .where(SCM_CODE.eq(scmCode))
                .execute()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        scmCode: String,
        status: ScmConfigStatus
    ) {
        with(TRepositoryScmConfig.T_REPOSITORY_SCM_CONFIG) {
            dslContext.update(this)
                .set(STATUS, status.name)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(SCM_CODE.eq(scmCode))
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        scmCode: String
    ): RepositoryScmConfig? {
        with(TRepositoryScmConfig.T_REPOSITORY_SCM_CONFIG) {
            val record = dslContext.selectFrom(this)
                .where(SCM_CODE.eq(scmCode))
                .fetchOne()
            return record?.let { convert(it) }
        }
    }

    fun count(
        dslContext: DSLContext,
        status: ScmConfigStatus? = null,
        excludeStatus: ScmConfigStatus? = null,
        oauth2Enabled: Boolean? = null,
        mergeEnabled: Boolean? = null,
        pacEnabled: Boolean? = null
    ): Long {
        return with(TRepositoryScmConfig.T_REPOSITORY_SCM_CONFIG) {
            val conditions = buildConditions(
                status = status,
                excludeStatus = excludeStatus,
                oauth2Enabled = oauth2Enabled,
                mergeEnabled = mergeEnabled,
                pacEnabled = pacEnabled
            )
            dslContext.selectCount().from(this)
                .where(conditions)
                .fetchOne(0, Long::class.java) ?: 0L
        }
    }

    fun list(
        dslContext: DSLContext,
        status: ScmConfigStatus? = null,
        excludeStatus: ScmConfigStatus? = null,
        oauth2Enabled: Boolean? = null,
        mergeEnabled: Boolean? = null,
        pacEnabled: Boolean? = null,
        scmType: ScmType? = null,
        limit: Int,
        offset: Int
    ): List<RepositoryScmConfig> {
        return with(TRepositoryScmConfig.T_REPOSITORY_SCM_CONFIG) {
            val conditions = buildConditions(
                status = status,
                excludeStatus = excludeStatus,
                oauth2Enabled = oauth2Enabled,
                mergeEnabled = mergeEnabled,
                pacEnabled = pacEnabled,
                scmType = scmType
            )
            dslContext.selectFrom(this)
                .where(conditions)
                .limit(limit)
                .offset(offset)
                .fetch()
                .map { convert(it) }
        }
    }

    private fun TRepositoryScmConfig.buildConditions(
        status: ScmConfigStatus?,
        excludeStatus: ScmConfigStatus?,
        oauth2Enabled: Boolean?,
        mergeEnabled: Boolean?,
        pacEnabled: Boolean?,
        scmType: ScmType? = null
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        if (status != null) {
            conditions.add(STATUS.eq(status.name))
        }
        if (excludeStatus != null) {
            conditions.add(STATUS.notEqual(excludeStatus.name))
        }
        oauth2Enabled?.let { conditions.add(OAUTH2_ENABLED.eq(oauth2Enabled)) }
        mergeEnabled?.let { conditions.add(MERGE_ENABLED.eq(mergeEnabled)) }
        pacEnabled?.let { conditions.add(PAC_ENABLED.eq(pacEnabled)) }
        scmType?.let { conditions.add(SCM_TYPE.eq(it.name)) }
        return conditions
    }

    fun delete(
        dslContext: DSLContext,
        scmCode: String
    ): Int {
        with(TRepositoryScmConfig.T_REPOSITORY_SCM_CONFIG) {
            return dslContext.deleteFrom(this)
                .where(SCM_CODE.eq(scmCode))
                .execute()
        }
    }

    fun convert(record: TRepositoryScmConfigRecord): RepositoryScmConfig {
        return with(record) {
            RepositoryScmConfig(
                scmCode = scmCode,
                name = name,
                providerCode = providerCode,
                scmType = ScmType.valueOf(scmType),
                hosts = hosts,
                logoUrl = logoUrl,
                credentialTypeList = JsonUtil.to(
                    credentialTypeList,
                    object : TypeReference<List<RepoCredentialType>>() {}),
                oauthType = ScmConfigOauthType.valueOf(oauthType),
                oauthScmCode = oauthScmCode,
                status = ScmConfigStatus.valueOf(status),
                oauth2Enabled = oauth2Enabled,
                mergeEnabled = mergeEnabled,
                pacEnabled = pacEnabled,
                webhookEnabled = webhookEnabled,
                providerProps = JsonUtil.to(providerProps, ScmProviderProperties::class.java),
                creator = creator,
                updater = updater,
                createTime = createTime.timestampmilli(),
                updateTime = updateTime.timestampmilli()
            )
        }
    }
}
