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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.utils.SUB_PIPELINE_ENABLE_RETRY_SYNC_PARAM
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Service

/**
 * #8609 子流水线同步模式重试联动父步骤的一次性绑定凭证与开关解析服务
 *
 * 核心设计思路：
 * 1. 当同步模式子流水线被点击重试并联动触发父流水线单步重试时，在父构建分布式锁 BuildIdLock 内写入一条
 *    绑定到 (parentProjectId, parentBuildId, parentTaskId, expectedParentExecuteCount, subBuildId) 的一次性凭证；
 * 2. 父步骤拉起调用 callOtherProjectPipelineStartup / callPipelineStartup 时，先经四维强校验并原子消费
 *    （Lua Compare-And-Delete）该凭证，跳过新建子构建，直接返回正在重试的原 subBuildId 供插件轮询，
 *    从而确保 100% 复用原插件轮询/变量回写逻辑且绝不会轮询错对象；
 * 3. 父构建手动重试或取消时主动清理残留凭证，彻底杜绝手动重试误复用旧子构建。
 */
@Service
class SubPipelineRetryAttachService @Autowired constructor(
    private val redisOperation: RedisOperation
) {

    data class SubPipelineRetryAttachInfo(
        val parentProjectId: String,
        val parentPipelineId: String,
        val parentBuildId: String,
        val parentTaskId: String,
        val expectedParentExecuteCount: Int,
        val subProjectId: String,
        val subPipelineId: String,
        val subPipelineName: String,
        val subBuildId: String,
        val subBuildNum: Int? = null,
        val subExecuteCount: Int,
        val operator: String? = null,
        val triggerTime: Long
    )

    companion object {
        private val logger = LoggerFactory.getLogger(SubPipelineRetryAttachService::class.java)

        // 凭证保留 7 天，远大于父构建可能经历的排队等待时长，避免排队较久时凭证过早过期
        private const val ATTACH_EXPIRED_SECONDS = 604800L

        private val COMPARE_AND_DELETE_SCRIPT = DefaultRedisScript<Long>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long::class.java
        )

        /**
         * 解析「子流水线调用」插件级开关 enableRetrySync：
         * - 研发商店插件（SubPipelineExec / SubStreamExec）：位于 taskParams["data"]["input"]["enableRetrySync"]
         * - 内置插件（SubPipelineCallElement）：位于 taskParams["enableRetrySync"]
         * 存量未配置该字段或为 false 的插件一律返回 false，确保对存量流水线零影响。
         */
        fun isRetrySyncEnabled(task: PipelineBuildTask): Boolean {
            val params = task.taskParams
            val directVal = params[SUB_PIPELINE_ENABLE_RETRY_SYNC_PARAM]
            if (parseBoolean(directVal)) {
                return true
            }
            val dataMap = params["data"] as? Map<*, *> ?: return false
            val inputMap = dataMap["input"] as? Map<*, *> ?: return false
            return parseBoolean(inputMap[SUB_PIPELINE_ENABLE_RETRY_SYNC_PARAM])
        }

        private fun parseBoolean(value: Any?): Boolean {
            return when (value) {
                is Boolean -> value
                is String -> value.equals("true", ignoreCase = true)
                else -> false
            }
        }
    }

    fun saveAttachInfo(info: SubPipelineRetryAttachInfo) {
        val attachKey = getAttachKey(info.parentProjectId, info.parentBuildId, info.parentTaskId)
        val tasksIndexKey = getBuildTasksIndexKey(info.parentProjectId, info.parentBuildId)
        redisOperation.set(
            key = attachKey,
            value = JsonUtil.toJson(info, formatted = false),
            expiredInSecond = ATTACH_EXPIRED_SECONDS
        )
        redisOperation.sadd(tasksIndexKey, info.parentTaskId)
        redisOperation.expire(tasksIndexKey, ATTACH_EXPIRED_SECONDS)
    }

    /**
     * 四维强校验 + Lua 原子消费一次性凭证，防止轮询错对象或二次复用：
     * 1. actualExecuteCount == expectedParentExecuteCount
     * 2. callProjectId/callPipelineId == attach.subProjectId/subPipelineId
     * 3. parentTask.subBuildId/subProjectId == attach.subBuildId/subProjectId（双保险防中间被改写）
     * 4. Lua Compare-And-Delete 确保并发下仅消费一次
     */
    fun consumeAttachInfo(
        parentProjectId: String,
        parentBuildId: String,
        parentTaskId: String,
        actualExecuteCount: Int,
        callProjectId: String,
        callPipelineId: String,
        taskSubProjectId: String?,
        taskSubBuildId: String?
    ): SubPipelineRetryAttachInfo? {
        val attachKey = getAttachKey(parentProjectId, parentBuildId, parentTaskId)
        val raw = redisOperation.get(attachKey) ?: return null
        val attach = runCatching {
            JsonUtil.to(raw, SubPipelineRetryAttachInfo::class.java)
        }.onFailure {
            logger.warn("SUB_PIPELINE_RETRY_ATTACH|invalid json|$parentBuildId|$parentTaskId", it)
        }.getOrNull() ?: return null

        if (attach.expectedParentExecuteCount != actualExecuteCount) {
            logger.warn(
                "SUB_PIPELINE_RETRY_ATTACH|executeCount mismatch|$parentBuildId|$parentTaskId|" +
                    "expected=${attach.expectedParentExecuteCount},actual=$actualExecuteCount"
            )
            return null
        }
        if (attach.subProjectId != callProjectId || attach.subPipelineId != callPipelineId) {
            logger.warn(
                "SUB_PIPELINE_RETRY_ATTACH|target pipeline mismatch|$parentBuildId|$parentTaskId|" +
                    "expected=${attach.subProjectId}/${attach.subPipelineId},actual=$callProjectId/$callPipelineId"
            )
            return null
        }
        if (taskSubBuildId != attach.subBuildId || taskSubProjectId != attach.subProjectId) {
            logger.warn(
                "SUB_PIPELINE_RETRY_ATTACH|task subBuildId mismatch|$parentBuildId|$parentTaskId|" +
                    "expected=${attach.subProjectId}/${attach.subBuildId},actual=$taskSubProjectId/$taskSubBuildId"
            )
            return null
        }

        val deleted = compareAndDelete(attachKey, raw)
        if (!deleted) {
            logger.info("SUB_PIPELINE_RETRY_ATTACH|already consumed|$parentBuildId|$parentTaskId")
            return null
        }
        redisOperation.sremove(getBuildTasksIndexKey(parentProjectId, parentBuildId), parentTaskId)
        return attach
    }

    /**
     * 清理单个父步骤残留的绑定凭证（用于手动单步重试、联动重试异常回滚等场景）。
     */
    fun clearTaskAttachInfo(parentProjectId: String, parentBuildId: String, parentTaskId: String) {
        redisOperation.delete(getAttachKey(parentProjectId, parentBuildId, parentTaskId))
        redisOperation.sremove(getBuildTasksIndexKey(parentProjectId, parentBuildId), parentTaskId)
    }

    /**
     * 清理整条父构建上残留的全部绑定凭证（用于父构建全量/Stage/Job 手动重试或构建取消）。
     */
    fun clearBuildAttachInfos(parentProjectId: String, parentBuildId: String) {
        val tasksIndexKey = getBuildTasksIndexKey(parentProjectId, parentBuildId)
        val taskIds = redisOperation.getSetMembers(tasksIndexKey)
        if (!taskIds.isNullOrEmpty()) {
            val keys = taskIds.map { getAttachKey(parentProjectId, parentBuildId, it) }
            redisOperation.delete(keys)
        }
        redisOperation.delete(tasksIndexKey)
    }

    private fun compareAndDelete(key: String, expectedRaw: String): Boolean {
        val result = redisOperation.execute(COMPARE_AND_DELETE_SCRIPT, listOf(key), expectedRaw)
        return (result ?: 0L) > 0L
    }

    private fun getAttachKey(parentProjectId: String, parentBuildId: String, parentTaskId: String): String {
        return "subpipeline:retry:attach:$parentProjectId:$parentBuildId:$parentTaskId"
    }

    private fun getBuildTasksIndexKey(parentProjectId: String, parentBuildId: String): String {
        return "subpipeline:retry:attach:tasks:$parentProjectId:$parentBuildId"
    }
}
