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

package com.tencent.devops.process.utils

import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType

const val PIPELINE_VERSION = "BK_CI_PIPELINE_VERSION" // "pipeline.version"
const val PIPELINE_START_PARENT_PIPELINE_ID = "BK_CI_PARENT_PIPELINE_ID" // "pipeline.start.parent.pipeline.id"
const val PIPELINE_START_PARENT_BUILD_ID = "BK_CI_PARENT_BUILD_ID" // "pipeline.start.parent.build.id"
const val PIPELINE_START_PARENT_BUILD_TASK_ID = "BK_CI_PARENT_BUILD_TASK_ID" // "pipeline.start.parent.build.task.id"
const val PIPELINE_START_USER_ID = "BK_CI_START_USER_ID" // "pipeline.start.user.id"
const val PIPELINE_START_USER_NAME = "BK_CI_START_USER_NAME" // "pipeline.start.user.name"
const val PIPELINE_START_WEBHOOK_USER_ID = "BK_CI_START_WEBHOOK_USER_ID" // "pipeline.start.webhook.user.id"
const val PIPELINE_START_PIPELINE_USER_ID = "BK_CI_START_PIPELINE_USER_ID" // "pipeline.start.pipeline.user.id"
const val PIPELINE_START_TYPE = "BK_CI_START_TYPE" // "pipeline.start.type"
const val PIPELINE_START_CHANNEL = "BK_CI_START_CHANNEL" // "pipeline.start.channel"
const val PIPELINE_BUILD_NUM = "BK_CI_BUILD_NUM" // "pipeline.build.num"
const val PIPELINE_BUILD_LAST_UPDATE = "BK_CI_BUILD_LAST_UPDATE" // "pipeline.build.last.update"
const val PIPELINE_BUILD_SVN_REVISION = "BK_CI_BUILD_SVN_REVISION" // "pipeline.build.svn.revision"

const val PIPELINE_WEBHOOK_REVISION = "BK_CI_HOOK_REVISION" // hookRevision
const val PIPELINE_WEBHOOK_BRANCH = "BK_CI_HOOK_BRANCH" // hookBranch
const val PIPELINE_WEBHOOK_SOURCE_BRANCH = "BK_CI_HOOK_SOURCE_BRANCH" // hookSourceBranch
const val PIPELINE_WEBHOOK_TARGET_BRANCH = "BK_CI_HOOK_TARGET_BRANCH" // hookTargetBranch
const val PIPELINE_WEBHOOK_SOURCE_URL = "BK_CI_HOOK_SOURCE_URL" // hookSourceUrl
const val PIPELINE_WEBHOOK_TARGET_URL = "BK_CI_HOOK_TARGET_URL" // hookTargetUrl
const val PIPELINE_WEBHOOK_REPO = "BK_CI_HOOK_REPO" // hookRepo
const val PIPELINE_WEBHOOK_REPO_TYPE = "BK_CI_HOOK_REPO_TYPE" // hookRepoType
const val PIPELINE_WEBHOOK_BLOCK = "BK_CI_HOOK_BLOCK" // hookBlock
const val PIPELINE_WEBHOOK_TYPE = "BK_CI_HOOK_TYPE" // hookType
const val PIPELINE_WEBHOOK_EVENT_TYPE = "BK_CI_HOOK_EVENT_TYPE" // hookEventType
const val PIPELINE_WEBHOOK_MR_ID = "BK_CI_HOOK_MR_ID" // bk_hookMergeRequestId
const val PIPELINE_REPO_NAME = "BK_CI_REPO_NAME" // "repoName"
const val PIPELINE_WEBHOOK_MR_COMMITTER = "BK_CI_HOOK_MR_COMMITTER" // "bk_hookMergeRequest_committer"
const val PIPELINE_WEBHOOK_COMMIT_MESSAGE = "BK_CI_HOOK_MESSAGE" // hook message

const val GIT_MR_NUMBER = "BK_CI_GIT_MR_NUMBER" // git_mr_number
const val GITHUB_PR_NUMBER = "BK_CI_GITHUB_PR_NUMBER" // github_pr_number

const val PIPELINE_NAME = "BK_CI_PIPELINE_NAME" // "pipeline.name"
const val PIPELINE_ID = "BK_CI_PIPELINE_ID" // "pipeline.id"

const val PIPELINE_TIME_DURATION = "BK_CI_BUILD_TOTAL_TIME" // "pipeline.time.duration"

const val PIPELINE_BUILD_ID = "BK_CI_BUILD_ID" // "pipeline.build.id"
const val PIPELINE_VMSEQ_ID = "BK_CI_BUILD_JOB_ID" // "pipeline.job.id"
const val PIPELINE_ELEMENT_ID = "BK_CI_BUILD_TASK_ID" // "pipeline.task.id"
const val PIPELINE_TURBO_TASK_ID = "BK_CI_TURBO_ID" // "turbo.task.id"
const val PROJECT_NAME = "BK_CI_PROJECT_NAME" // "project.name"
const val REPORT_DYNAMIC_ROOT_URL = "BK_CI_REPORT_DYNAMIC_ROOT_URL" // "report.dynamic.root.url"

const val PROJECT_NAME_CHINESE = "BK_CI_PROJECT_NAME_CN" // "project.name.chinese"

const val PIPELINE_START_MOBILE = "BK_CI_IS_MOBILE" // "pipeline.start.isMobile"

const val PIPELINE_START_TASK_ID = "BK_CI_START_TASK_ID" // "pipeline.start.task.id"
const val PIPELINE_RETRY_COUNT = "BK_CI_RETRY_COUNT" // "pipeline.retry.count"
const val PIPELINE_RETRY_BUILD_ID = "BK_CI_RETRY_BUILD_ID" // "pipeline.retry.build.id"
const val PIPELINE_RETRY_START_TASK_ID = "BK_CI_RETRY_TASK_ID" // "pipeline.retry.start.task.id"

const val BK_CI_BUILD_FAIL_TASKS = "BK_CI_BUILD_FAIL_TASKS"
const val BK_CI_BUILD_FAIL_TASKNAMES = "BK_CI_BUILD_FAIL_TASKNAMES"

const val PIPELINE_VIEW_MY_PIPELINES = "myPipeline"
const val PIPELINE_VIEW_FAVORITE_PIPELINES = "collect"
const val PIPELINE_VIEW_ALL_PIPELINES = "allPipeline"

const val PIPELINE_MATERIAL_URL = "BK_CI_PIEPLEINE_MATERIAL_URL" // pipeline.material.url
const val PIPELINE_MATERIAL_BRANCHNAME = "BK_CI_PIPELINE_MATERIAL_BRANCHNAME" // pipeline.material.branchName
const val PIPELINE_MATERIAL_ALIASNAME = "BK_CI_PIPELINE_MATERIAL_ALIASNAME" // pipeline.material.aliasName
const val PIPELINE_MATERIAL_NEW_COMMIT_ID = "BK_CI_PIPELINE_MATERIAL_NEW_COMMIT_ID" // pipeline.material.new.commit.id
const val PIPELINE_MATERIAL_NEW_COMMIT_COMMENT =
    "BK_CI_PIPELINE_MATERIAL_NEW_COMMIT_COMMENT" // pipeline.material.new.commit.comment
const val PIPELINE_MATERIAL_NEW_COMMIT_TIMES =
    "BK_CI_PIPELINE_MATERIAL_NEW_COMMIT_TIMES" // pipeline.material.new.commit.times

const val MAJORVERSION = "BK_CI_MAJOR_VERSION" // MajorVersion
const val MINORVERSION = "BK_CI_MINOR_VERSION" // MinorVersion
const val FIXVERSION = "BK_CI_FIX_VERSION" // FixVersion
const val BUILD_NO = "BK_CI_BUILD_NO" // "BuildNo"

/**
 * 后续新增的变量统一用“BK_CI_大写的变量名称”命名，历史的变量名称统一整改
 */
const val PIPELINE_CREATE_USER = "BK_CI_PIPELINE_CREATE_USER" // "流水线创建用户"
const val PIPELINE_UPDATE_USER = "BK_CI_PIPELINE_UPDATE_USER" // "流水线最后更新用户"
const val PIPELINE_BUILD_REMARK = "BK_CI_BUILD_REMARK" // "流水线构建备注"
const val PIPELINE_ATOM_FRONTEND_DIST_PATH = "BK_CI_CUSTOM_FRONTEND_DIST_PATH" // "流水线插件定制UI文件编译后的路径"

/**
 * 流水线设置-最大排队数量-默认值
 */
const val PIPELINE_SETTING_MAX_QUEUE_SIZE_DEFAULT = 10
/**
 * 流水线插件设置-失败重试最大值
 */
const val TASK_FAIL_RETRY_MAX_COUNT = 5
/**
 * 流水线插件设置-失败重试最小值
 */
const val TASK_FAIL_RETRY_MIN_COUNT = 1

/**
 * 流水线设置-最大排队数量-最小值
 */
const val PIPELINE_SETTING_MAX_QUEUE_SIZE_MIN = 0
/**
 * 流水线设置-最大排队数量-最大值
 */
const val PIPELINE_SETTING_MAX_QUEUE_SIZE_MAX = 20
/**
 * 流水线设置-最大并发数量-默认值
 */
const val PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_DEFAULT = 50
/**
 * 流水线设置-最大并发数量-最大值
 */
const val PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX = 200

/**
 * 流水线设置-最大排队时间-默认值 单位:分钟
 */
const val PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_DEFAULT = 1

/**
 * 流水线设置-最大排队时间-最小值 单位:分钟
 */
const val PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MIN = 1

/**
 * 流水线设置-最大排队时间-默认值 单位:分钟
 */
const val PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MAX = 1440

/**
 * 流水线设置-插件错误信息入库长度最大值 单位:分钟
 */
const val PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX = 4000

/**
 * 流水线设置-流水线错误信息入库长度最大值 单位:分钟
 */
const val PIPELINE_MESSAGE_STRING_LENGTH_MAX = 30000

/**
 * 流水线设置-启动的通知模板代码
 */
const val PIPELINE_STARTUP_NOTIFY_TEMPLATE = "PIPELINE_STARTUP_NOTIFY_TEMPLATE"

/**
 * 流水线设置-启动的详情通知模板代码
 */
const val PIPELINE_STARTUP_NOTIFY_TEMPLATE_DETAIL = "PIPELINE_STARTUP_NOTIFY_TEMPLATE_DETAIL"

/**
 * 流水线设置-执行成功的通知模板代码
 */
const val PIPELINE_SHUTDOWN_SUCCESS_NOTIFY_TEMPLATE = "PIPELINE_SHUTDOWN_SUCCESS_NOTIFY_TEMPLATE"

/**
 * 流水线设置-执行成功的详情通知模板代码
 */
const val PIPELINE_SHUTDOWN_SUCCESS_NOTIFY_TEMPLATE_DETAIL = "PIPELINE_SHUTDOWN_SUCCESS_NOTIFY_TEMPLATE_DETAIL"

/**
 * 流水线设置-执行失败的通知模板代码
 */
const val PIPELINE_SHUTDOWN_FAILURE_NOTIFY_TEMPLATE = "PIPELINE_SHUTDOWN_FAILURE_NOTIFY_TEMPLATE"

/**
 * 流水线设置-执行失败的详情通知模板代码
 */
const val PIPELINE_SHUTDOWN_FAILURE_NOTIFY_TEMPLATE_DETAIL = "PIPELINE_SHUTDOWN_FAILURE_NOTIFY_TEMPLATE_DETAIL"

/**
 * 流水线设置-执行取消的通知模板代码
 */
const val PIPELINE_SHUTDOWN_CANCEL_NOTIFY_TEMPLATE = "PIPELINE_SHUTDOWN_CANCEL_NOTIFY_TEMPLATE"

/**
 * 流水线设置-执行取消的详情通知模板代码
 */
const val PIPELINE_SHUTDOWN_CANCEL_NOTIFY_TEMPLATE_DETAIL = "PIPELINE_SHUTDOWN_CANCEL_NOTIFY_TEMPLATE_DETAIL"

/**
 * 流水线设置-人工审核插件的通知模板代码
 */
const val PIPELINE_MANUAL_REVIEW_ATOM_NOTIFY_TEMPLATE = "MANUAL_REVIEW_ATOM_NOTIFY_TEMPLATE"

/**
 * 流水线设置-stage阶段审核的通知模板代码
 */
const val PIPELINE_MANUAL_REVIEW_STAGE_NOTIFY_TEMPLATE = "MANUAL_REVIEW_STAGE_NOTIFY_TEMPLATE"

const val PIPELINE_TIME_START = "BK_CI_BUILD_START_TIME" // "pipeline.time.start"

const val PIPELINE_TIME_END = "BK_CI_BUILD_END_TIME" // "pipeline.time.end"

const val PIPELINE_BUILD_MSG = "BK_CI_BUILD_MSG"

/**
 * 保存流水线编排的最大个数
 */
const val PIPELINE_RES_NUM_MIN = 50

object PipelineVarUtil {

    /**
     * 前置拼接
     */
    private val oldPrefixMappingNew = mapOf(
        "pipeline.material.url" to PIPELINE_MATERIAL_URL,
        "pipeline.material.branchName" to PIPELINE_MATERIAL_BRANCHNAME,
        "pipeline.material.aliasName" to PIPELINE_MATERIAL_ALIASNAME,
        "pipeline.material.new.commit.id" to PIPELINE_MATERIAL_NEW_COMMIT_ID,
        "pipeline.material.new.commit.times" to PIPELINE_MATERIAL_NEW_COMMIT_TIMES,
        "pipeline.material.new.commit.comment" to PIPELINE_MATERIAL_NEW_COMMIT_COMMENT
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
        "report.dynamic.root.url" to REPORT_DYNAMIC_ROOT_URL
    )

    private val newVarMappingOldVar = oldVarMappingNewVar.map { kv -> kv.value to kv.key }.toMap()

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
                allVars[oldVarName!!] = it.value // 旧变量写入，如果之前有了，则替换
                allVars[it.key] = it.value // 新变量仍然写入
            } else {
                // 从旧转新: 兼容从旧入口写入的数据转到新的流水线运行
                val newVarName = oldVarToNewVar(it.key)
                // 新变量已经存在，忽略旧变量转换
                if (!newVarName.isNullOrBlank() && !vars.contains(newVarName)) {
                    allVars[newVarName!!] = it.value
                }
                // 已经存在从新变量转化过来的旧变量，则不覆盖，放弃
                if (!allVars.containsKey(it.key)) {
                    allVars[it.key] = it.value
                }
            }
        }
        return allVars
    }

    /**
     * 旧变量转新变量
     */
    fun replaceOldByNewVar(varMaps: MutableMap<String, Pair<String, BuildFormPropertyType>>) {
        turningWithType(oldVarMappingNewVar, varMaps, true)
        prefixTurningWithType(oldPrefixMappingNew, varMaps, true)
    }

    private fun turningWithType(
        mapping: Map<String, String>,
        varMaps: MutableMap<String, Pair<String, BuildFormPropertyType>>,
        replace: Boolean = false
    ) {
        mapping.forEach {
            // 如果新旧key同时存在，则保留原value
            if (varMaps[it.key] != null && varMaps[it.value] == null) {
                varMaps[it.value] = varMaps[it.key]!!
                if (replace) {
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
}