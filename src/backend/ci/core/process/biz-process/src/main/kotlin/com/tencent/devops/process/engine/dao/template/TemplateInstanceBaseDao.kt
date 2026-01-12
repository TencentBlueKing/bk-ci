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

package com.tencent.devops.process.engine.dao.template

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.model.process.tables.TTemplateInstanceBase
import com.tencent.devops.model.process.tables.records.TTemplateInstanceBaseRecord
import com.tencent.devops.common.pipeline.enums.TemplateRefType
import com.tencent.devops.process.pojo.template.TemplateInstanceStatus
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstanceBase
import com.tencent.devops.process.pojo.template.v2.TemplateInstanceType
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class TemplateInstanceBaseDao {

    fun createTemplateInstanceBase(
        dslContext: DSLContext,
        baseId: String,
        templateId: String,
        templateVersion: String,
        useTemplateSettingsFlag: Boolean,
        projectId: String,
        totalItemNum: Int,
        status: String,
        userId: String,
        pac: Boolean? = null,
        targetAction: String? = null,
        description: String? = null,
        type: String? = TemplateInstanceType.UPDATE.name,
        repoHashId: String? = null,
        targetBranch: String? = null,
        templateRefType: TemplateRefType? = null,
        templateRef: String? = null
    ) {
        with(TTemplateInstanceBase.T_TEMPLATE_INSTANCE_BASE) {
            setOf(
                dslContext.insertInto(
                    this,
                    ID,
                    TEMPLATE_ID,
                    TEMPLATE_VERSION,
                    USE_TEMPLATE_SETTINGS_FLAG,
                    PROJECT_ID,
                    TOTAL_ITEM_NUM,
                    STATUS,
                    CREATOR,
                    MODIFIER,
                    PAC,
                    TARGET_ACTION,
                    DESCRIPTION,
                    TYPE,
                    REPO_HASH_ID,
                    TARGET_BRANCH,
                    TEMPLATE_REF_TYPE,
                    TEMPLATE_REF,
                ).values(
                    baseId,
                    templateId,
                    templateVersion,
                    useTemplateSettingsFlag,
                    projectId,
                    totalItemNum,
                    status,
                    userId,
                    userId,
                    pac,
                    targetAction,
                    description,
                    type,
                    repoHashId,
                    targetBranch,
                    templateRefType?.name,
                    templateRef
                )
                    .onDuplicateKeyUpdate()
                    .set(TEMPLATE_ID, templateId)
                    .set(TEMPLATE_VERSION, templateVersion)
                    .set(USE_TEMPLATE_SETTINGS_FLAG, useTemplateSettingsFlag)
                    .set(TOTAL_ITEM_NUM, totalItemNum)
                    .set(STATUS, status)
                    .set(CREATOR, userId)
                    .set(MODIFIER, userId)
                    .set(PAC, pac)
                    .set(TARGET_ACTION, targetAction)
                    .set(TYPE, type)
                    .set(DESCRIPTION, description)
                    .set(REPO_HASH_ID, repoHashId)
                    .set(TARGET_BRANCH, targetBranch)
                    .set(TEMPLATE_REF_TYPE, templateRefType?.name)
                    .set(TEMPLATE_REF, templateRef)
                    .execute()
            )
        }
    }

    fun updateTemplateInstanceBase(
        dslContext: DSLContext,
        projectId: String,
        baseId: String,
        successItemNum: Int? = null,
        failItemNum: Int? = null,
        status: String? = null,
        pullRequestId: Long? = null,
        pullRequestUrl: String? = null,
        userId: String
    ) {
        with(TTemplateInstanceBase.T_TEMPLATE_INSTANCE_BASE) {
            val baseStep = dslContext.update(this)
            if (successItemNum != null) {
                baseStep.set(SUCCESS_ITEM_NUM, successItemNum)
            }
            if (failItemNum != null) {
                baseStep.set(FAIL_ITEM_NUM, failItemNum)
            }
            if (status != null) {
                baseStep.set(STATUS, status)
            }
            if (pullRequestUrl != null) {
                baseStep.set(PULL_REQUEST_URL, pullRequestUrl)
            }
            if (pullRequestId != null) {
                baseStep.set(PULL_REQUEST_ID, pullRequestId)
            }
            baseStep.set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, userId)
                .where(ID.eq(baseId).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun getTemplateInstanceBase(
        dslContext: DSLContext,
        projectId: String,
        baseId: String
    ): PipelineTemplateInstanceBase? {
        return with(TTemplateInstanceBase.T_TEMPLATE_INSTANCE_BASE) {
            dslContext.selectFrom(this)
                .where(ID.eq(baseId).and(PROJECT_ID.eq(projectId)))
                .fetchOne()?.convert()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        projectId: String,
        baseId: String,
        status: TemplateInstanceStatus
    ) {
        return with(TTemplateInstanceBase.T_TEMPLATE_INSTANCE_BASE) {
            dslContext.update(this)
                .set(STATUS, status.name)
                .where(ID.eq(baseId).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun getTemplateInstanceBaseList(
        dslContext: DSLContext,
        statusList: List<String>,
        descFlag: Boolean,
        page: Int,
        pageSize: Int
    ): Result<TTemplateInstanceBaseRecord>? {
        with(TTemplateInstanceBase.T_TEMPLATE_INSTANCE_BASE) {
            val baseStep = dslContext.selectFrom(this).where(STATUS.`in`(statusList))
            if (descFlag) {
                baseStep.orderBy(CREATE_TIME.desc())
            } else {
                baseStep.orderBy(CREATE_TIME.asc())
            }
            return baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        }
    }

    fun deleteByBaseId(dslContext: DSLContext, projectId: String, baseId: String) {
        with(TTemplateInstanceBase.T_TEMPLATE_INSTANCE_BASE) {
            dslContext.deleteFrom(this)
                .where(ID.eq(baseId).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        statusList: List<String>? = null,
        excludeStatusList: List<String>? = null,
        type: TemplateInstanceType? = null,
        templateId: String? = null
    ): List<PipelineTemplateInstanceBase> {
        return with(TTemplateInstanceBase.T_TEMPLATE_INSTANCE_BASE) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .let { if (statusList.isNullOrEmpty()) it else it.and(STATUS.`in`(statusList)) }
                .let { if (excludeStatusList.isNullOrEmpty()) it else it.and(STATUS.notIn(excludeStatusList)) }
                .let { if (type != null) it.and(TYPE.eq(type.name)) else it }
                .let { if (templateId != null) it.and(TEMPLATE_ID.eq(templateId)) else it }
                .fetch().map { it.convert() }
        }
    }

    private fun TTemplateInstanceBaseRecord.convert(): PipelineTemplateInstanceBase {
        return PipelineTemplateInstanceBase(
            baseId = id,
            projectId = projectId,
            templateId = templateId,
            templateVersion = templateVersion.toLong(),
            useTemplateSetting = useTemplateSettingsFlag,
            totalItemNum = totalItemNum,
            successItemNum = successItemNum,
            failItemNum = failItemNum,
            description = description,
            status = TemplateInstanceStatus.valueOf(status),
            pac = pac,
            targetAction = targetAction?.let { CodeTargetAction.valueOf(targetAction) },
            type = TemplateInstanceType.valueOf(type),
            repoHashId = repoHashId,
            targetBranch = targetBranch,
            templateRefType = templateRefType?.let { TemplateRefType.valueOf(it) },
            templateRef = templateRef,
            pullRequestUrl = pullRequestUrl,
            creator = creator,
            modifier = modifier,
            createTime = createTime.timestampmilli(),
            updateTime = updateTime.timestampmilli()
        )
    }
}
