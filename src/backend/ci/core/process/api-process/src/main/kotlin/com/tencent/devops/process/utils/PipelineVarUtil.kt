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

package com.tencent.devops.process.utils

import com.tencent.devops.common.api.constant.coerceAtMaxLength
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_ACTION
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_AUTHORIZER
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_BASE_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_BASE_REPO_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_BEFORE_SHA
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_BEFORE_SHA_SHORT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_AUTHOR
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_MESSAGE
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT_CONTENT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_HEAD_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_HEAD_REPO_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_ACTION
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_DESC
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_ID
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_IID
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_PROPOSER
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_TITLE
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_CREATE_TIME
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_CREATOR
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_GROUP
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_ID
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_NAME
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA_SHORT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_TAG_FROM
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_TAG_MESSAGE
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_UPDATE_USER
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_YAML_PATH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_DESCRIPTION
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_IID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_MILESTONE_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_OWNER
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_STATE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_TITLE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_REVIEWERS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_IID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_OWNER
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_REVIEWERS
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_STATE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_REPO_NAME
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_BLOCK
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_EVENT_TYPE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_MR_COMMITTER
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_MR_ID
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_NOTE_COMMENT
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_NOTE_ID
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_REPO
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_REPO_TYPE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_REVISION
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_SOURCE_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_SOURCE_URL
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TARGET_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TARGET_URL
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TYPE
import java.util.regex.Pattern

@Suppress("TooManyFunctions")
object PipelineVarUtil {

    private val tPattern = Pattern.compile("\\$[{]{2}(?<double>[^$^{}]+)[}]{2}")

    /**
     * 检查[keyword]字符串是不是一个变量语法， ${{ varName }}， 如果不是则返回false
     * 注意：已不再支持 ${ var } 旧的语法定义变量，只支持全新 ${{ var }} 的语法
     */
    fun isVar(keyword: String?): Boolean {
        return !keyword.isNullOrBlank() && tPattern.matcher(keyword).matches()
    }

    /**
     * 检查[keyword]串中有没有变量，比如  "abc_${{ varName }} is true" 将识别出存在 varName变量，会返回true
     */
    fun haveVar(keyword: String): Boolean {
        return tPattern.matcher(keyword).find()
    }

    /**
     * 前置拼接
     */
    private val oldPrefixMappingNew = mapOf(
        "pipeline.material.url" to PIPELINE_MATERIAL_URL,
        "pipeline.material.branchName" to PIPELINE_MATERIAL_BRANCHNAME,
        "pipeline.material.aliasName" to PIPELINE_MATERIAL_ALIASNAME,
        "pipeline.material.new.commit.id" to PIPELINE_MATERIAL_NEW_COMMIT_ID,
        "pipeline.material.new.commit.times" to PIPELINE_MATERIAL_NEW_COMMIT_TIMES,
        "pipeline.material.new.commit.comment" to PIPELINE_MATERIAL_NEW_COMMIT_COMMENT,
        "BK_REPO_GIT_WEBHOOK_REVIEW" to "BK_CI_REPO_GIT_WEBHOOK_REVIEW",
        "BK_REPO_P4" to "BK_CI_REPO_P4"
    )

    private val newPrefixMappingOld = oldPrefixMappingNew.map { kv -> kv.value to kv.key }.toMap()

    /**
     * 以下用于兼容旧参数
     */
    private val oldVarMappingNewVar = mapOf(
        "pipeline.start.isMobile" to PIPELINE_START_MOBILE,
        "repoName" to PIPELINE_REPO_NAME,
        "pipeline.version" to PIPELINE_VERSION,
        "pipeline.start.parent.build.task.id" to PIPELINE_START_PARENT_BUILD_TASK_ID,
        "pipeline.start.parent.build.id" to PIPELINE_START_PARENT_BUILD_ID,
        "MajorVersion" to MAJORVERSION,
        "MinorVersion" to MINORVERSION,
        "FixVersion" to FIXVERSION,
        "BuildNo" to BUILD_NO,
        "pipeline.start.channel" to PIPELINE_START_CHANNEL,
        "pipeline.build.last.update" to PIPELINE_BUILD_LAST_UPDATE,
        "pipeline.build.svn.revision" to PIPELINE_BUILD_SVN_REVISION,
        "pipeline.start.parent.pipeline.id" to PIPELINE_START_PARENT_PIPELINE_ID,
        "pipeline.start.user.id" to PIPELINE_START_USER_ID,
        "pipeline.start.task.id" to PIPELINE_START_TASK_ID,
        "pipeline.retry.start.task.id" to PIPELINE_RETRY_START_TASK_ID,
        "pipeline.retry.build.id" to PIPELINE_RETRY_BUILD_ID,
        "pipeline.id" to PIPELINE_ID,
        "pipeline.retry.count" to PIPELINE_RETRY_COUNT,
        "pipeline.time.start" to PIPELINE_TIME_START,
        "pipeline.time.end" to PIPELINE_TIME_END,
        "pipeline.time.duration" to PIPELINE_TIME_DURATION,
        "project.name.chinese" to PROJECT_NAME_CHINESE,
        "pipeline.name" to PIPELINE_NAME,
        "pipeline.build.num" to PIPELINE_BUILD_NUM,
        "pipeline.start.user.name" to PIPELINE_START_USER_NAME,
        "pipeline.start.type" to PIPELINE_START_TYPE,
        "hookRevision" to PIPELINE_WEBHOOK_REVISION,
        "hookBranch" to PIPELINE_WEBHOOK_BRANCH,
        "hookSourceBranch" to PIPELINE_WEBHOOK_SOURCE_BRANCH,
        "hookTargetBranch" to PIPELINE_WEBHOOK_TARGET_BRANCH,
        "hookSourceUrl" to PIPELINE_WEBHOOK_SOURCE_URL,
        "hookTargetUrl" to PIPELINE_WEBHOOK_TARGET_URL,
        "hookBlock" to PIPELINE_WEBHOOK_BLOCK,
        "hookType" to PIPELINE_WEBHOOK_TYPE,
        "hookRepo" to PIPELINE_WEBHOOK_REPO,
        "hookRepoType" to PIPELINE_WEBHOOK_REPO_TYPE,
        "hookEventType" to PIPELINE_WEBHOOK_EVENT_TYPE,
        "bk_hookMergeRequestId" to PIPELINE_WEBHOOK_MR_ID,
        "bk_hookMergeRequest_committer" to PIPELINE_WEBHOOK_MR_COMMITTER,
        "pipeline.start.webhook.user.id" to PIPELINE_START_WEBHOOK_USER_ID,
        "pipeline.start.pipeline.user.id" to PIPELINE_START_PIPELINE_USER_ID,
        "git_mr_number" to GIT_MR_NUMBER,
        "github_pr_number" to GITHUB_PR_NUMBER,
        "project.name" to PROJECT_NAME,
        "pipeline.build.id" to PIPELINE_BUILD_ID,
        "pipeline.job.id" to PIPELINE_VMSEQ_ID,
        "pipeline.task.id" to PIPELINE_ELEMENT_ID,
        "turbo.task.id" to PIPELINE_TURBO_TASK_ID,
        "report.dynamic.root.url" to REPORT_DYNAMIC_ROOT_URL,
        "BK_REPO_GIT_WEBHOOK_TAG_CREATE_FROM" to "BK_CI_REPO_GIT_WEBHOOK_TAG_CREATE_FROM",
        "BK_REPO_SVN_WEBHOOK_FINAL_INCLUDE_PATH" to "BK_CI_REPO_SVN_WEBHOOK_FINAL_INCLUDE_PATH"
    )

    /**
     * CI预置上下文转换映射关系
     */
    private val contextVarMappingBuildVar = mapOf(
        "ci.workspace" to WORKSPACE,
        "ci.pipeline_id" to PIPELINE_ID,
        "ci.pipeline_name" to PIPELINE_NAME,
        "ci.actor" to PIPELINE_START_USER_NAME,
        "ci.build_id" to PIPELINE_BUILD_ID,
        "ci.build_num" to PIPELINE_BUILD_NUM,
        "ci.pipeline_start_time" to PIPELINE_TIME_START,
        "ci.pipeline_execute_time" to PIPELINE_TIME_DURATION,
        "ci.ref" to PIPELINE_GIT_REF,
        "ci.head_ref" to PIPELINE_GIT_HEAD_REF,
        "ci.base_ref" to PIPELINE_GIT_BASE_REF,
        "ci.repo" to PIPELINE_GIT_REPO,
        "ci.repo_id" to PIPELINE_GIT_REPO_ID,
        "ci.repo_name" to PIPELINE_GIT_REPO_NAME,
        "ci.repo_group" to PIPELINE_GIT_REPO_GROUP,
        "ci.event_content" to PIPELINE_GIT_EVENT_CONTENT,
        "ci.event_url" to PIPELINE_GIT_EVENT_URL,
        "ci.sha" to PIPELINE_GIT_SHA,
        "ci.sha_short" to PIPELINE_GIT_SHA_SHORT,
        "ci.before_sha" to PIPELINE_GIT_BEFORE_SHA,
        "ci.before_sha_short" to PIPELINE_GIT_BEFORE_SHA_SHORT,
        "ci.commit_message" to PIPELINE_GIT_COMMIT_MESSAGE,
        "ci.repo_url" to PIPELINE_GIT_REPO_URL,
        "ci.base_repo_url" to PIPELINE_GIT_BASE_REPO_URL,
        "ci.head_repo_url" to PIPELINE_GIT_HEAD_REPO_URL,
        "ci.mr_url" to PIPELINE_GIT_MR_URL,
        "ci.commit_author" to PIPELINE_GIT_COMMIT_AUTHOR,
        "ci.pipeline_update_user" to PIPELINE_GIT_UPDATE_USER,
        "ci.authorizer" to PIPELINE_GIT_AUTHORIZER,
        "ci.mr_id" to PIPELINE_GIT_MR_ID,
        "ci.mr_iid" to PIPELINE_GIT_MR_IID,
        "ci.tag_message" to PIPELINE_GIT_TAG_MESSAGE,
        "ci.tag_from" to PIPELINE_GIT_TAG_FROM,
        "ci.mr_title" to PIPELINE_GIT_MR_TITLE,
        "ci.mr_desc" to PIPELINE_GIT_MR_DESC,
        "ci.mr_proposer" to PIPELINE_GIT_MR_PROPOSER,
        "ci.mr_action" to PIPELINE_GIT_MR_ACTION,
        "ci.issue_title" to BK_REPO_GIT_WEBHOOK_ISSUE_TITLE,
        "ci.issue_id" to BK_REPO_GIT_WEBHOOK_ISSUE_ID,
        "ci.issue_iid" to BK_REPO_GIT_WEBHOOK_ISSUE_IID,
        "ci.issue_description" to BK_REPO_GIT_WEBHOOK_ISSUE_DESCRIPTION,
        "ci.issue_state" to BK_REPO_GIT_WEBHOOK_ISSUE_STATE,
        "ci.issue_owner" to BK_REPO_GIT_WEBHOOK_ISSUE_OWNER,
        "ci.issue_milestone_id" to BK_REPO_GIT_WEBHOOK_ISSUE_MILESTONE_ID,
        "ci.review_id" to BK_REPO_GIT_WEBHOOK_REVIEW_ID,
        "ci.review_iid" to BK_REPO_GIT_WEBHOOK_REVIEW_IID,
        "ci.review_owner" to BK_REPO_GIT_WEBHOOK_REVIEW_OWNER,
        "ci.review_state" to BK_REPO_GIT_WEBHOOK_REVIEW_STATE,
        "ci.review_reviewers" to BK_REPO_GIT_WEBHOOK_REVIEW_REVIEWERS,
        "ci.note_comment" to PIPELINE_WEBHOOK_NOTE_COMMENT,
        "ci.note_id" to PIPELINE_WEBHOOK_NOTE_ID,
        "ci.action" to PIPELINE_GIT_ACTION,
        "ci.build_url" to PIPELINE_BUILD_URL,
        "ci.mr_reviewers" to BK_REPO_GIT_WEBHOOK_MR_REVIEWERS,
        "ci.pipeline_path" to PIPELINE_GIT_YAML_PATH,
        "ci.repo_create_time" to PIPELINE_GIT_REPO_CREATE_TIME,
        "ci.repo_creator" to PIPELINE_GIT_REPO_CREATOR
    )

    /**
     * CI预置上下文转换映射关系
     */
    private val contextVarMappingBuildVarRevert = mapOf(
        PIPELINE_START_USER_NAME to "ci.actor"
    )

    private val newVarMappingOldVar = oldVarMappingNewVar.map { kv -> kv.value to kv.key }.toMap()

    private val reverseContextVarMappingBuildVar =
        contextVarMappingBuildVar.values.zip(contextVarMappingBuildVar.keys).toMap()

    fun fetchReverseVarName(contextKey: String): String? {
        val varMap = reverseContextVarMappingBuildVar.toMutableMap()
        varMap.putAll(contextVarMappingBuildVarRevert)
        return varMap[contextKey]
    }

    /**
     * 填充CI预置变量
     */
    fun fillContextVarMap(varMap: MutableMap<String, String>, buildVar: Map<String, String>) {
        contextVarMappingBuildVar.forEach { (contextKey, varKey) ->
            if (!buildVar[varKey].isNullOrBlank()) varMap[contextKey] = buildVar[varKey]!!
        }
    }

    /**
     * 填充CI预置变量
     */
    fun fillContextVarMap(buildVar: Map<String, String>): Map<String, String> {
        val varMap = mutableMapOf<String, String>()
        contextVarMappingBuildVar.forEach { (contextKey, varKey) ->
            if (!buildVar[varKey].isNullOrBlank()) varMap[contextKey] = buildVar[varKey]!!
        }
        return buildVar.plus(varMap)
    }

    /**
     * 获取CI预置变量
     */
    fun fetchContextInBuildVars(contextKey: String, buildVar: Map<String, String>): String? {
        return buildVar[contextVarMappingBuildVar[contextKey]]
    }

    /**
     * 获取CI预置变量名
     */
    fun fetchVarName(contextKey: String): String? {
        return contextVarMappingBuildVar[contextKey]
    }

    /**
     * 填充旧变量名，兼容用户在流水线中旧的写法
     */
    fun fillOldVarWithType(varMaps: Map<String, Pair<String, BuildFormPropertyType>>) {
        val mutableList = varMaps.toMutableMap()
        turningWithType(newVarMappingOldVar, mutableList)
        prefixTurningWithType(newPrefixMappingOld, mutableList)
    }

    /**
     * 填充旧变量名，兼容用户在流水线中旧的写法
     */
    fun fillOldVar(vars: MutableMap<String, String>) {
        turning(newVarMappingOldVar, vars)
        prefixTurning(newPrefixMappingOld, vars)
    }

    /**
     * 从新变量前缀的变量中查出并增加旧变量，会做去重
     * @param vars 变量Map
     * @return 包含新旧变量的Map
     */
    fun mixOldVarAndNewVar(vars: MutableMap<String, String>): MutableMap<String, String> {
        // 旧流水线的前缀变量追加 一些旧代码类插件生成的
        prefixTurning(newPrefixMappingOld, vars)

        val allVars = mutableMapOf<String, String>()
        vars.forEach {
            // 从新转旧: 新流水线产生的变量 兼容在旧流水线中已经使用到的旧变量
            val oldVarName = newVarToOldVar(it.key)
            if (!oldVarName.isNullOrBlank()) {
                allVars[oldVarName] = it.value // 旧变量写入，如果之前有了，则替换
                allVars[it.key] = it.value // 新变量仍然写入
            } else {
                // 从旧转新: 兼容从旧入口写入的数据转到新的流水线运行
                val newVarName = oldVarToNewVar(it.key)
                // 新变量已经存在，忽略旧变量转换
                if (!newVarName.isNullOrBlank() && !vars.contains(newVarName)) {
                    allVars[newVarName] = it.value
                }
                // 已经存在从新变量转化过来的旧变量，则不覆盖，放弃
                if (!allVars.containsKey(it.key) || it.key == "BuildNo") {
                    allVars[it.key] = it.value
                }
            }
        }
        return allVars
    }

    /**
     * 将[varMaps]找到旧变量并替换成新变量，并删除掉旧变量
     */
    fun replaceOldByNewVar(varMaps: MutableMap<String, Pair<String, BuildFormPropertyType>>) {
        turningWithType(oldVarMappingNewVar, varMaps, true)
        prefixTurningWithType(oldPrefixMappingNew, varMaps, true)
    }

    /**
     *  遍历[varMaps]中的fullKey，与[mapping]中的key做完成匹配，匹配成功后从[mapping]取出value作newKey
     *  并写入到[varMaps],最后根据[replace]值为true决定要删除[varMaps]中的fullKey
     */
    private fun turningWithType(
        mapping: Map<String, String>,
        varMaps: MutableMap<String, Pair<String, BuildFormPropertyType>>,
        replace: Boolean = false
    ) {
        mapping.forEach {
            // 如果新旧key同时存在，则保留原value
            if (varMaps[it.key] != null && varMaps[it.value] == null) {
                varMaps[it.value] = varMaps[it.key]!!
                if (replace && it.key != "BuildNo") {
                    varMaps.remove(it.key)
                }
            }
        }
    }

    private fun turning(mapping: Map<String, String>, vars: MutableMap<String, String>, replace: Boolean = false) {
        mapping.forEach {
            // 如果新旧key同时存在，则保留原value
            if (vars[it.key] != null && vars[it.value] == null) {
                vars[it.value] = vars[it.key]!!
                if (replace) {
                    vars.remove(it.key)
                }
            }
        }
    }

    @Suppress("NestedBlockDepth")
    /**
     *  遍历[vars]中的fullKey，与[mapping]中的key做前缀匹配，匹配成功后从[mapping]取出value,替换掉fullKey的前缀key，组成newKey
     *  并写入到[vars],最后根据[replace]值为true决定要删除[vars]中的fullKey
     */
    private fun prefixTurning(
        mapping: Map<String, String>,
        vars: MutableMap<String, String>,
        replace: Boolean = false
    ) {
        val keys = HashSet(vars.keys)
        keys.forEach v@{ fullKey ->
            mapping.forEach m@{ (key, value) ->
                if (fullKey.startsWith(key)) {
                    vars["$value${fullKey.substring(key.length)}"] = vars[fullKey]!!
                    if (replace) {
                        vars.remove(fullKey)
                    }
                    return@v
                }
            }
        }
    }

    @Suppress("NestedBlockDepth")
    /**
     *  遍历[varMaps]中的fullKey，与[mapping]中的key做前缀匹配，匹配成功后从[mapping]取出value,替换掉fullKey的前缀key，组成newKey
     *  并写入到[varMaps],最后根据[replace]值为true决定要删除[varMaps]中的fullKey
     */
    private fun prefixTurningWithType(
        mapping: Map<String, String>,
        varMaps: MutableMap<String, Pair<String, BuildFormPropertyType>>,
        replace: Boolean = false
    ) {
        val keys = HashSet(varMaps.keys)
        keys.forEach v@{ fullKey ->
            mapping.forEach m@{ (key, value) ->
                if (fullKey.startsWith(key)) {
                    varMaps["$value${fullKey.substring(key.length)}"] = varMaps[fullKey]!!
                    if (replace) {
                        varMaps.remove(fullKey)
                    }
                    return@v
                }
            }
        }
    }

    fun oldVarToNewVar(oldVarName: String): String? = oldVarMappingNewVar[oldVarName]

    fun newVarToOldVar(newVarName: String): String? = newVarMappingOldVar[newVarName]

    const val MAX_VERSION_LEN = 64

    /**
     * 从流水线启动参数[buildParameters]中找出推荐版本号,由[MAJORVERSION].[MINORVERSION].[FIXVERSION].[BUILD_NO]组成
     * 需要注意的是，旧的参数命名继续兼容有效，由{MarjorVersion}.{MinorVersion}.{FixVersion}.{BuildNo} 组成，所以启动参数切记
     * 不要与此命名相同造成了冲突
     */
    fun getRecommendVersion(buildParameters: List<BuildParameters>): String? {
        val recommendVersionPrefix = getRecommendVersionPrefix(buildParameters) ?: return null

        val buildNo = buildParameters.firstOrNull { it.key == BUILD_NO || it.key == "BuildNo" }?.value?.toString()
            ?: return null

        return "$recommendVersionPrefix.$buildNo".coerceAtMaxLength(MAX_VERSION_LEN)
    }

    /**
     * 从流水线启动参数[buildParameters]中找出版本号前缀，由[MAJORVERSION].[MINORVERSION].[FIXVERSION] 组成，
     * 如果[buildParameters]中不存在上述3类参数，则返回空
     * 需要注意的是，旧的参数命名继续兼容有效，由{MarjorVersion}.{MinorVersion}.{FixVersion} 组成，所以启动参数切记
     * 不要与此命名相同造成了冲突
     */
    fun getRecommendVersionPrefix(buildParameters: List<BuildParameters>): String? {
        val majorVersion = buildParameters.firstOrNull { it.key == MAJORVERSION || it.key == "MajorVersion" }
            ?.value?.toString() ?: return null

        val minorVersion = buildParameters.firstOrNull { it.key == MINORVERSION || it.key == "MinorVersion" }
            ?.value?.toString() ?: return null

        val fixVersion = buildParameters.firstOrNull { it.key == FIXVERSION || it.key == "FixVersion" }
            ?.value?.toString() ?: return null

        return "$majorVersion.$minorVersion.$fixVersion"
    }
}
