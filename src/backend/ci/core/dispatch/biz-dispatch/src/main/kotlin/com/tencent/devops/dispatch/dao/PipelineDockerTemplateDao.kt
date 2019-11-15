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

package com.tencent.devops.dispatch.dao

import com.tencent.devops.model.dispatch.tables.TDispatchPipelineDockerTemplate
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerTemplateRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineDockerTemplateDao {

    fun updateTemplate(
        dslContext: DSLContext,
        versionId: Int,
        showVersionId: Int,
        showVersionName: String,
        deploymentId: Int,
        deploymentName: String,
        ccAppId: Long,
        bcsProjectId: String,
        clusterId: String
    ) {
        with(TDispatchPipelineDockerTemplate.T_DISPATCH_PIPELINE_DOCKER_TEMPLATE) {
            dslContext.insertInto(this,
                    VERSION_ID,
                    SHOW_VERSION_ID,
                    SHOW_VERSION_NAME,
                    DEPLOYMENT_ID,
                    DEPLOYMENT_NAME,
                    CC_APP_ID,
                    BCS_PROJECT_ID,
                    CLUSTER_ID,
                    CREATED_TIME)
                    .values(
                            versionId,
                            showVersionId,
                            showVersionName,
                            deploymentId,
                            deploymentName,
                            ccAppId,
                            bcsProjectId,
                            clusterId,
                            LocalDateTime.now()
                    )
                    .execute()
        }
    }

    fun getTemplate(dslContext: DSLContext): TDispatchPipelineDockerTemplateRecord? {
        with(TDispatchPipelineDockerTemplate.T_DISPATCH_PIPELINE_DOCKER_TEMPLATE) {
            return dslContext.selectFrom(this)
                    .orderBy(ID.desc())
                    .limit(1)
                    .fetchOne()
        }
    }

    fun getTemplateById(dslContext: DSLContext, id: Int): TDispatchPipelineDockerTemplateRecord? {
        with(TDispatchPipelineDockerTemplate.T_DISPATCH_PIPELINE_DOCKER_TEMPLATE) {
            return dslContext.selectFrom(this)
                    .where(ID.eq(id))
                    .orderBy(ID.desc())
                    .limit(1)
                    .fetchOne()
        }
    }
}