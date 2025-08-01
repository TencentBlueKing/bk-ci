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
 *
 */

package com.tencent.devops.process.webhook

import com.tencent.devops.common.pipeline.pojo.BuildEnvParameters
import com.tencent.devops.common.pipeline.pojo.BuildParameterGroup
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeP4WebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ACTION
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ACTOR
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_BASE_REF
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_BASE_REPO_URL
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_BEFORE_SHA
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_BEFORE_SHA_SHORT
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_BRANCH
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_BUILD_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_BUILD_MSG
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_BUILD_NO
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_BUILD_NUM
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_BUILD_START_TYPE
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_COMMIT_AUTHOR
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_COMMIT_MESSAGE
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_CREATE_REF
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_CREATE_REF_TYPE
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_CREATE_TIME
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_EVENT
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_EVENT_URL
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_FAILED_TASKNAMES
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_FAILED_TASKS
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_HEAD_REF
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_HEAD_REPO_URL
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ISSUE_DESCRIPTION
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ISSUE_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ISSUE_IID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ISSUE_MILESTONE_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ISSUE_OWNER
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ISSUE_STATE
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_ISSUE_TITLE
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_MILESTONE_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_MILESTONE_NAME
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_MODIFY_TIME
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_MR_DESC
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_MR_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_MR_IID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_MR_PROPOSER
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_MR_REVIEWERS
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_MR_TITLE
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_MR_URL
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_NOTE_AUTHOR
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_NOTE_COMMENT
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_NOTE_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_NOTE_TYPE
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_PIPELINE_CREATOR
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_PIPELINE_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_PIPELINE_MODIFIER
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_PIPELINE_NAME
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_PIPELINE_VERSION
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_PROJECT_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_PROJECT_NAME
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_REMARK
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_REPO
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_REPO_ALIAS_NAME
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_REPO_GROUP
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_REPO_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_REPO_NAME
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_REPO_TYPE
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_REPO_URL
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_REVIEW_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_REVIEW_IID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_REVIEW_OWNER
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_REVIEW_REVIEWERS
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_REVIEW_STATE
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_REVIEW_TYPE
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_SHA
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_SHA_SHORT
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_TAG_DESC
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_TAG_FROM
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_TAPD_ISSUES
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_WORKSPACE
import com.tencent.devops.process.constant.PipelineBuildParamKey.JOB_CONTAINER_NETWORK
import com.tencent.devops.process.constant.PipelineBuildParamKey.JOB_CONTAINER_NODE_ALIAS
import com.tencent.devops.process.constant.PipelineBuildParamKey.JOB_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.JOB_INDEX
import com.tencent.devops.process.constant.PipelineBuildParamKey.JOB_NAME
import com.tencent.devops.process.constant.PipelineBuildParamKey.JOB_OS
import com.tencent.devops.process.constant.PipelineBuildParamKey.JOB_OUTCOME_TEMPLATE
import com.tencent.devops.process.constant.PipelineBuildParamKey.JOB_STAGE_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.JOB_STAGE_NAME
import com.tencent.devops.process.constant.PipelineBuildParamKey.JOB_STATUS_TEMPLATE
import com.tencent.devops.process.constant.PipelineBuildParamKey.STEP_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.STEP_NAME
import com.tencent.devops.process.constant.PipelineBuildParamKey.STEP_OUTCOME_TEMPLATE
import com.tencent.devops.process.constant.PipelineBuildParamKey.STEP_RETRY_COUNT_AUTO
import com.tencent.devops.process.constant.PipelineBuildParamKey.STEP_RETRY_COUNT_MANUAL
import com.tencent.devops.process.constant.PipelineBuildParamKey.STEP_STATUS_TEMPLATE

@SuppressWarnings("TooManyFunctions")
object TriggerBuildParamUtils {
    // map<atomCode, map<event_type, params>>
    private val TRIGGER_BUILD_PARAM_NAME_MAP = mutableMapOf<String, MutableMap<String, List<String>>>()
    private const val TRIGGER_BUILD_PARAM_PREFIX = "trigger.build.param"
    private const val TRIGGER_BUILD_PARAM_DESC = "desc"

    init {
        // GIT事件触发参数
        gitWebhookTriggerCommon()
        gitWebhookTriggerPush()
        gitWebhookTriggerMr()
        gitWebhookTriggerTag()
        gitWebhookTriggerIssue()
        gitWebhookTriggerReview()
        gitWebhookTriggerNote()
        // Github事件触发参数
        githubWebhookTrigger()
        // svn事件触发参数
        svnWebhookTrigger()
        // p4事件触发参数
        p4WebhookTrigger()
    }

    fun getBasicParamName() = I18nUtil.getCodeLanMessage("$TRIGGER_BUILD_PARAM_PREFIX.basic")

    fun getBasicBuildParams(): List<BuildEnvParameters> {
        return listOf(
            CI_ACTOR,
            CI_BUILD_MSG,
            CI_BUILD_NO,
            CI_BUILD_NUM,
            CI_BUILD_ID,
            CI_PIPELINE_ID,
            CI_PROJECT_ID,
            CI_PROJECT_NAME,
            CI_PIPELINE_NAME,
            CI_PIPELINE_CREATOR,
            CI_PIPELINE_MODIFIER,
            CI_PIPELINE_VERSION,
            CI_BUILD_START_TYPE,
            CI_WORKSPACE,
            CI_FAILED_TASKNAMES,
            CI_FAILED_TASKS,
            CI_REMARK
        ).sortedBy {
            it
        }.map {
            BuildEnvParameters(
                name = it,
                desc = I18nUtil.getCodeLanMessage(it)
            )
        }
    }

    fun getStepParamName() = I18nUtil.getCodeLanMessage("$TRIGGER_BUILD_PARAM_PREFIX.step")

    fun getStepBuildParams(): List<BuildEnvParameters> {
        return listOf(
            STEP_NAME,
            STEP_ID,
            STEP_RETRY_COUNT_MANUAL,
            STEP_RETRY_COUNT_AUTO,
            STEP_STATUS_TEMPLATE,
            STEP_OUTCOME_TEMPLATE
        ).sortedBy {
            it
        }.map {
            BuildEnvParameters(
                name = it,
                desc = I18nUtil.getCodeLanMessage(it),
                remark = if (fillRemark(it)) {
                    I18nUtil.getCodeLanMessage("$it.remark")
                } else {
                    null
                }
            )
        }
    }

    fun getJobParamName() = I18nUtil.getCodeLanMessage("$TRIGGER_BUILD_PARAM_PREFIX.job")

    fun getJobBuildParams(): List<BuildEnvParameters> {
        return listOf(
            JOB_NAME,
            JOB_ID,
            JOB_OS,
            JOB_CONTAINER_NETWORK,
            JOB_CONTAINER_NODE_ALIAS,
            JOB_STAGE_ID,
            JOB_STAGE_NAME,
            JOB_INDEX,
            JOB_STATUS_TEMPLATE,
            JOB_OUTCOME_TEMPLATE
        ).sortedBy {
            it
        }.map {
            BuildEnvParameters(
                name = it,
                desc = I18nUtil.getCodeLanMessage(it),
                remark = if (fillRemark(it)) {
                    I18nUtil.getCodeLanMessage("$it.remark")
                } else {
                    null
                }
            )
        }
    }

    fun getTriggerParamNameMap(atomCode: String): List<BuildParameterGroup> {
        val paramNameMap = TRIGGER_BUILD_PARAM_NAME_MAP[atomCode] ?: emptyMap()
        return paramNameMap.map { (eventType, paramNames) ->
            val params = paramNames.sortedBy { it }.map { paramName ->
                BuildEnvParameters(
                    name = paramName,
                    desc = I18nUtil.getCodeLanMessage(
                        messageCode = "$TRIGGER_BUILD_PARAM_PREFIX.$atomCode.$paramName.$TRIGGER_BUILD_PARAM_DESC"
                    )
                )
            }
            BuildParameterGroup(
                name = I18nUtil.getCodeLanMessage(
                    messageCode = "$TRIGGER_BUILD_PARAM_PREFIX.$atomCode.$eventType"
                ),
                params = params
            )
        }
    }

    /**
     * git事件触发公共变量名列表
     */
    private fun gitWebhookTriggerCommon() {
        val commonParams = listOf(
            CI_ACTOR,
            CI_REPO_ID,
            CI_REPO_TYPE,
            CI_REPO_URL,
            CI_REPO,
            CI_REPO_GROUP,
            CI_REPO_NAME,
            CI_REPO_ALIAS_NAME,
            CI_EVENT,
            CI_EVENT_URL,
            CI_BRANCH,
            CI_BUILD_MSG,
            CI_COMMIT_MESSAGE,
            CI_ACTION
        )

        TRIGGER_BUILD_PARAM_NAME_MAP[CodeGitWebHookTriggerElement.classType] =
            mutableMapOf("common" to commonParams)
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeGithubWebHookTriggerElement.classType] =
            mutableMapOf("common" to commonParams)
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeTGitWebHookTriggerElement.classType] =
            mutableMapOf("common" to commonParams)
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeGitlabWebHookTriggerElement.classType] =
            mutableMapOf("common" to commonParams)
    }

    /**
     * git事件触发push变量名列表
     */
    private fun gitWebhookTriggerPush() {
        val params = listOf(
            CI_BEFORE_SHA,
            CI_BEFORE_SHA_SHORT,
            CI_SHA,
            CI_SHA_SHORT
        )
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeGitWebHookTriggerElement.classType]?.putAll(
            mapOf(CodeEventType.PUSH.name to params)
        )
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeGithubWebHookTriggerElement.classType]?.putAll(
            mapOf(CodeEventType.PUSH.name to params)
        )
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeGitlabWebHookTriggerElement.classType]?.putAll(
            mapOf(CodeEventType.PUSH.name to params)
        )
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeTGitWebHookTriggerElement.classType]?.putAll(
            mapOf(CodeEventType.PUSH.name to params)
        )
    }

    /**
     * git事件触发MR变量名列表
     */
    private fun gitWebhookTriggerMr() {
        val params = mutableListOf(
            CI_MR_PROPOSER,
            CI_HEAD_REPO_URL,
            CI_BASE_REPO_URL,
            CI_HEAD_REF,
            CI_BASE_REF,
            CI_MR_ID,
            CI_MR_IID,
            CI_MR_DESC,
            CI_MR_TITLE,
            CI_MR_URL,
            CI_MR_REVIEWERS,
            CI_MILESTONE_NAME,
            CI_MILESTONE_ID
        )
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeGitWebHookTriggerElement.classType]?.putAll(
            mapOf(CodeEventType.MERGE_REQUEST.name to params.plus(CI_TAPD_ISSUES))
        )
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeGithubWebHookTriggerElement.classType]?.putAll(
            mapOf(CodeEventType.PULL_REQUEST.name to params)
        )
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeTGitWebHookTriggerElement.classType]?.putAll(
            mapOf(CodeEventType.MERGE_REQUEST.name to params.plus(CI_TAPD_ISSUES))
        )
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeGitlabWebHookTriggerElement.classType]?.putAll(
            mapOf(CodeEventType.MERGE_REQUEST.name to params)
        )
    }

    /**
     * git事件触发Tag变量名列表
     */
    private fun gitWebhookTriggerTag() {
        val params = listOf(
            CI_COMMIT_AUTHOR,
            CI_TAG_FROM
        )
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeGitWebHookTriggerElement.classType]?.putAll(
            mapOf(CodeEventType.TAG_PUSH.name to params.plus(CI_TAG_DESC))
        )
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeTGitWebHookTriggerElement.classType]?.putAll(
            mapOf(CodeEventType.TAG_PUSH.name to params.plus(CI_TAG_DESC))
        )
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeGitlabWebHookTriggerElement.classType]?.putAll(
            mapOf(CodeEventType.TAG_PUSH.name to params)
        )
    }

    /**
     * git事件触发Note变量名列表
     */
    private fun gitWebhookTriggerNote() {
        val params = listOf(
            CI_NOTE_COMMENT,
            CI_NOTE_ID,
            CI_NOTE_TYPE,
            CI_NOTE_AUTHOR,
            CI_CREATE_TIME,
            CI_MODIFY_TIME
        )
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeGitWebHookTriggerElement.classType]?.putAll(
            mapOf(CodeEventType.NOTE.name to params)
        )

        TRIGGER_BUILD_PARAM_NAME_MAP[CodeGithubWebHookTriggerElement.classType]?.putAll(
            mapOf(CodeEventType.NOTE.name to params)
        )

        TRIGGER_BUILD_PARAM_NAME_MAP[CodeTGitWebHookTriggerElement.classType]?.putAll(
            mapOf(CodeEventType.NOTE.name to params)
        )
    }

    /**
     * git事件触发Issue变量名列表
     */
    private fun gitWebhookTriggerIssue() {
        val params = listOf(
            CI_ISSUE_TITLE,
            CI_ISSUE_ID,
            CI_ISSUE_IID,
            CI_ISSUE_DESCRIPTION,
            CI_ISSUE_STATE,
            CI_ISSUE_OWNER,
            CI_ISSUE_MILESTONE_ID
        )
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeGitWebHookTriggerElement.classType]?.putAll(
            mapOf(CodeEventType.ISSUES.name to params)
        )
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeGithubWebHookTriggerElement.classType]?.putAll(
            mapOf(CodeEventType.ISSUES.name to params)
        )
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeTGitWebHookTriggerElement.classType]?.putAll(
            mapOf(CodeEventType.ISSUES.name to params)
        )
    }
    /**
     * git事件触发Review变量名列表
     */
    private fun gitWebhookTriggerReview() {
        val params = listOf(
            CI_REVIEW_ID,
            CI_REVIEW_IID,
            CI_REVIEW_TYPE,
            CI_REVIEW_REVIEWERS,
            CI_REVIEW_STATE,
            CI_REVIEW_OWNER
        )
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeGitWebHookTriggerElement.classType]?.putAll(
            mapOf(CodeEventType.REVIEW.name to params)
        )
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeGithubWebHookTriggerElement.classType]?.putAll(
            mapOf(CodeEventType.REVIEW.name to params)
        )
    }

    /**
     * github事件触发create事件变量名列表
     */
    private fun githubWebhookTrigger() {
        val params = listOf(
            CI_CREATE_REF,
            CI_CREATE_REF_TYPE
        )
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeGithubWebHookTriggerElement.classType]?.putAll(
            mapOf(CodeEventType.CREATE.name to params)
        )
    }

    /**
     * svn事件触发变量名列表
     */
    private fun svnWebhookTrigger() {
        val params = listOf(
            CI_SHA,
            CI_ACTOR,
            CI_EVENT,
            CI_BUILD_MSG,
            CI_REPO,
            CI_REPO_ALIAS_NAME,
            CI_REPO_URL
        )
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeSVNWebHookTriggerElement.classType] =
            mutableMapOf(CodeEventType.POST_COMMIT.name to params)
    }

    /**
     * p4事件触发变量名列表
     */
    private fun p4WebhookTrigger() {
        val params = listOf(
            CI_SHA,
            CI_ACTOR,
            CI_EVENT,
            CI_BUILD_MSG,
            CI_REPO,
            CI_REPO_ALIAS_NAME,
            CI_REPO_URL
        )
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeP4WebHookTriggerElement.classType] = mutableMapOf("common" to params)
    }

    /**
     * 是否需要填充备注信息
     */
    private fun fillRemark(key: String) = listOf(
        STEP_NAME,
        STEP_ID,
        STEP_STATUS_TEMPLATE,
        STEP_OUTCOME_TEMPLATE,
        JOB_NAME,
        JOB_ID,
        JOB_INDEX,
        JOB_STATUS_TEMPLATE,
        JOB_OUTCOME_TEMPLATE
    ).contains(key)
}
