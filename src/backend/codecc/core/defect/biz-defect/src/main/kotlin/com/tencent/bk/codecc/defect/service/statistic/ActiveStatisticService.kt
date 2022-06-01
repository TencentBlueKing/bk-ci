/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.service.statistic

import com.tencent.bk.codecc.defect.dao.mongorepository.TaskLogRepository
import com.tencent.bk.codecc.defect.model.CLOCStatisticEntity
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO
import com.tencent.devops.common.auth.api.pojo.external.KEY_CREATE_FROM
import com.tencent.devops.common.auth.api.pojo.external.PREFIX_TASK_INFO
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.ComConstants.TOTAL_BLANK
import com.tencent.devops.common.constant.ComConstants.TOTAL_CODE
import com.tencent.devops.common.constant.ComConstants.TOTAL_COMMENT
import com.tencent.devops.common.constant.RedisKeyConstants
import com.tencent.devops.common.util.DateTimeUtils
import com.tencent.devops.common.web.mq.EXCHANGE_CODE_REPO_STAT
import com.tencent.devops.common.web.mq.ROUTE_CODE_REPO_STAT
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class ActiveStatisticService @Autowired constructor(
        private val rabbitTemplate: RabbitTemplate,
        private val taskLogRepository: TaskLogRepository,
        private val thirdPartySystemCaller: ThirdPartySystemCaller,
        private val redisTemplate: RedisTemplate<String, String>
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ActiveStatisticService::class.java)
        private val COMMON_TOOLS = mutableListOf(ComConstants.Tool.COVERITY.name, ComConstants.Tool.KLOCWORK.name,
                ComConstants.Tool.PINPOINT.name)
        // 各工具分析阶段定义值
        private const val BEFORE_ANALYZE_STEP = 2
    }

    /**
     * 统计任务工具分析记录
     */
    fun statTaskAndTool(uploadTaskLogStepVO: UploadTaskLogStepVO) {
        logger.info("active statistic before analyze -> taskLogStepVO: {}", uploadTaskLogStepVO)

        val voTaskId = uploadTaskLogStepVO.taskId
        val voToolName = uploadTaskLogStepVO.toolName
        val voStepNum = uploadTaskLogStepVO.stepNum
        val voFlag = uploadTaskLogStepVO.flag

        val isLastStepNum = if (COMMON_TOOLS.contains(voToolName)) {
            ComConstants.Step4Cov.DEFECT_SYNS.value()
        } else {
            ComConstants.Step4MutliTool.COMMIT.value()
        }

        val dateStr = DateTimeUtils.getDateByDiff(0)

        // 修复任务ID可能为空
        val taskId = if (voTaskId == 0L) {
            try {
                thirdPartySystemCaller.getTaskInfoWithoutToolsByStreamName(uploadTaskLogStepVO.streamName)
                        .taskId.toString()
            } catch (e: Exception) {
                logger.error("getTaskInfo fail: ${e.message}")
                return
            }

        } else {
            voTaskId.toString()
        }

        var createFrom = redisTemplate.opsForHash<String, String>().get(PREFIX_TASK_INFO + taskId, KEY_CREATE_FROM)
        if (ComConstants.DefectStatType.GONGFENG_SCAN.value() != createFrom) {
            createFrom = ComConstants.DefectStatType.USER.value()
        }

        // 统计活跃
        if (voStepNum == 1) {
            // 活跃任务
            val taskKey = "${RedisKeyConstants.PREFIX_ACTIVE_TASK}$dateStr:$createFrom"
            redisTemplate.opsForSet().add(taskKey, taskId)

            // 活跃工具
            val toolKey = "${RedisKeyConstants.PREFIX_ACTIVE_TOOL}$dateStr:$createFrom"
            redisTemplate.opsForHash<String, String>().increment(toolKey, voToolName, 1)
            logger.info("active statistic save.")
        }

        // 区分超快和非超快
        val scanStatTypeStr = if (uploadTaskLogStepVO.isFastIncrement) {
            ComConstants.ScanStatType.IS_FAST_INCREMENT.value
        } else {
            ComConstants.ScanStatType.NOT_FAST_INCREMENT.value
        }

        // 统计分析失败次数 2、4
        if (voFlag == ComConstants.StepFlag.FAIL.value() || voFlag == ComConstants.StepFlag.ABORT.value()) {
            val analyzeFailKey = "${RedisKeyConstants.PREFIX_ANALYZE_FAIL_COUNT}$dateStr:$createFrom:$voToolName"
            redisTemplate.opsForList().rightPush(analyzeFailKey, taskId)

            // 记录每天各工具分析失败次数(按是否超快)
            val analyzeFailToolKey =
                    "${RedisKeyConstants.PREFIX_ANALYZE_FAIL_TOOL}$dateStr:$createFrom:$scanStatTypeStr"
            redisTemplate.opsForHash<String, String>().increment(analyzeFailToolKey, voToolName, 1)
            logger.info("analyze fail statistic save.")
        }

        // 统计分析成功次数 1
        if (voStepNum == isLastStepNum && voFlag == ComConstants.StepFlag.SUCC.value()) {
            val analyzeSuccessKey = "${RedisKeyConstants.PREFIX_ANALYZE_SUCC_COUNT}$dateStr:$createFrom:$voToolName"
            redisTemplate.opsForList().rightPush(analyzeSuccessKey, taskId)
            logger.info("analyze success statistic save.")

            // 记录工具分析耗时
            val voBuildId = uploadTaskLogStepVO.pipelineBuildId
            val lastTaskLogEntity =
                taskLogRepository.findFirstByTaskIdAndToolNameAndBuildId(voTaskId, voToolName, voBuildId)
            val elapseTime = uploadTaskLogStepVO.endTime - lastTaskLogEntity.startTime

            // 记录每天累计耗时
            val elapseTimeKey =
                    "${RedisKeyConstants.PREFIX_ANALYZE_SUCC_ELAPSE_TIME}$dateStr:$createFrom:$scanStatTypeStr"
            val elapseTimeStr = redisTemplate.opsForHash<String, String>().get(elapseTimeKey, voToolName)
            val elapseTimeLong = if (elapseTimeStr.isNullOrBlank()) {
                elapseTime
            } else {
                elapseTime + elapseTimeStr.toLong()
            }
            redisTemplate.opsForHash<String, String>().put(elapseTimeKey, voToolName, elapseTimeLong.toString())

            // 记录每天各工具分析成功次数(按是否超快)
            val analyzeSuccToolKey =
                    "${RedisKeyConstants.PREFIX_ANALYZE_SUCC_TOOL}$dateStr:$createFrom:$scanStatTypeStr"
            redisTemplate.opsForHash<String, String>().increment(analyzeSuccToolKey, voToolName, 1)
            logger.info("tool analyze elapse time statistic save.")
        }

        // 在保存代码库信息完成后，分析阶段之前记录代码库信息
        if (ComConstants.Tool.CLOC.name == voToolName && voStepNum == BEFORE_ANALYZE_STEP
                && voFlag == ComConstants.StepFlag.SUCC.value()) {
            rabbitTemplate.convertAndSend(EXCHANGE_CODE_REPO_STAT, ROUTE_CODE_REPO_STAT, uploadTaskLogStepVO)
            logger.info("convertAndSend success.")
        }

        logger.info("statistic finish.")
    }

    /**
     * 统计代码行
     */
    fun statCodeLineByCloc(clocStatisticEntity: Collection<CLOCStatisticEntity>) {
        logger.info("active statistic after upload -> size: {}", clocStatisticEntity.size)
        val taskId = clocStatisticEntity.iterator().next().taskId
        var createFrom = redisTemplate.opsForHash<String, String>().get(PREFIX_TASK_INFO + taskId, KEY_CREATE_FROM)
        if (ComConstants.DefectStatType.GONGFENG_SCAN.value() != createFrom) {
            createFrom = ComConstants.DefectStatType.USER.value()
        }

        val currentDate = DateTimeUtils.getDateByDiff(0)
        val key = "${RedisKeyConstants.CODE_LINE_STAT}$currentDate:$createFrom"
        var currentMap = redisTemplate.opsForHash<String, String>().entries(key)
        if (currentMap.isNullOrEmpty()) {
            currentMap = mutableMapOf()
        }

        var totalBlank: Long = currentMap.getOrDefault(TOTAL_BLANK, "0").toLong()
        var totalComment: Long = currentMap.getOrDefault(TOTAL_COMMENT, "0").toLong()
        var totalCode: Long = currentMap.getOrDefault(TOTAL_CODE, "0").toLong()

        clocStatisticEntity.forEach {
            if (it.sumBlank > 0) {
                totalBlank += it.sumBlank
            }
            if (it.sumComment > 0) {
                totalComment += it.sumComment
            }
            if (it.sumCode > 0) {
                totalCode += it.sumCode
            }
        }

        currentMap[TOTAL_BLANK] = totalBlank.toString()
        currentMap[TOTAL_COMMENT] = totalComment.toString()
        currentMap[TOTAL_CODE] = totalCode.toString()
        redisTemplate.opsForHash<String, String>().putAll(key, currentMap)

        logger.info("statistic code line finish.")
    }

}