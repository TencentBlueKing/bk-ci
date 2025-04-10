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

package com.tencent.devops.repository.dao

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.repository.tables.TRepositoryScmProvider
import com.tencent.devops.model.repository.tables.records.TRepositoryScmProviderRecord
import com.tencent.devops.repository.pojo.RepositoryScmProvider
import com.tencent.devops.repository.pojo.ScmProviderWebhookProps
import com.tencent.devops.repository.pojo.enums.RepoCredentialType
import com.tencent.devops.scm.api.enums.ScmProviderType
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class RepositoryScmProviderDao {

    fun list(dslContext: DSLContext): List<RepositoryScmProvider> {
        with(TRepositoryScmProvider.T_REPOSITORY_SCM_PROVIDER) {
            return dslContext.selectFrom(this)
                .skipCheck()
                .fetch()
                .map { convert(it) }
        }
    }

    fun get(dslContext: DSLContext, providerCode: String): RepositoryScmProvider? {
        with(TRepositoryScmProvider.T_REPOSITORY_SCM_PROVIDER) {
            val record = dslContext.selectFrom(this)
                .where(PROVIDER_CODE.eq(providerCode))
                .fetchOne()
            return record?.let { convert(it) }
        }
    }

    fun convert(record: TRepositoryScmProviderRecord): RepositoryScmProvider {
        return with(record) {
            RepositoryScmProvider(
                providerCode = providerCode,
                providerType = ScmProviderType.valueOf(providerType),
                name = name,
                desc = desc,
                scmType = ScmType.valueOf(scmType),
                logoUrl = logoUrl,
                docUrl = docUrl,
                credentialTypeList = JsonUtil.to(
                    credentialTypeList,
                    object : TypeReference<List<RepoCredentialType>>() {}),
                api = api,
                merge = merge,
                webhook = webhook,
                webhookSecretType = webhookSecretType,
                webhookProps = JsonUtil.to(webhookProps, ScmProviderWebhookProps::class.java),
                pac = pac,
                createTime = createTime.timestampmilli(),
                updateTime = updateTime.timestampmilli()
            )
        }
    }
}
