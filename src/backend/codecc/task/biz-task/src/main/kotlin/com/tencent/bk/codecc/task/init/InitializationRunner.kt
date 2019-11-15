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

package com.tencent.bk.codecc.task.init

import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.bk.codecc.task.service.code.InitResponseCode
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.RedisKeyConstants
import com.tencent.devops.common.service.utils.SpringContextUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class InitializationRunner @Autowired constructor(
        private val initResponseCode: InitResponseCode,
        private val taskRepository: TaskRepository
) : CommandLineRunner {

    companion object {
        private val logger = LoggerFactory.getLogger(InitializationRunner::class.java)
    }

    override fun run(vararg arg: String?) {
        val redisTemplate: RedisTemplate<String, String> = SpringContextUtil.getBean(RedisTemplate::class.java, "redisTemplate") as RedisTemplate<String, String>
        val currentVal = redisTemplate.opsForValue().get(RedisKeyConstants.CODECC_TASK_ID)
        if (null == currentVal || currentVal.toLong() < ComConstants.COMMON_NUM_10000L) {
            logger.info("start to initialize redis key!")
            val taskInfoEntity = taskRepository.findFirstByTaskIdExistsOrderByTaskIdDesc(true)
            if(null == taskInfoEntity)
            {
                redisTemplate.opsForValue().set(RedisKeyConstants.CODECC_TASK_ID, ComConstants.COMMON_NUM_10000L.toString())
            }
            else
            {
                redisTemplate.opsForValue().set(RedisKeyConstants.CODECC_TASK_ID, (taskInfoEntity.taskId + 1).toString())
            }
        }

        // 初始化工具元数据到redis缓存
        val stringRedisTemplate: StringRedisTemplate = SpringContextUtil.getBean(StringRedisTemplate::class.java, "stringRedisTemplate")
        val lintTools = arrayOf("CPPLINT", "PYLINT", "ESLINT", "CHECKSTYLE", "STYLECOP", "GOML", "SPOTBUGS", "CPPCHECK", "PHPCS")
        lintTools.forEach {
            val pattern = stringRedisTemplate.opsForHash<Any, Any>().get("${RedisKeyConstants.PREFIX_TOOL}$it", RedisKeyConstants.FILED_PATTERN)
            if(null == pattern)
            {
                stringRedisTemplate.opsForHash<Any, Any>().put("${RedisKeyConstants.PREFIX_TOOL}$it", RedisKeyConstants.FILED_PATTERN, "LINT")
            }
            else
            {
                logger.info("current pattern: $pattern, tool name: $it")
            }
        }
        stringRedisTemplate.opsForHash<Any, Any>().put("${RedisKeyConstants.PREFIX_TOOL}CCN", RedisKeyConstants.FILED_PATTERN, "CCN")
        stringRedisTemplate.opsForHash<Any, Any>().put("${RedisKeyConstants.PREFIX_TOOL}DUPC", RedisKeyConstants.FILED_PATTERN, "DUPC")

        // 国际化操作[ 响应码、操作记录、规则包、规则名称、报表日期、工具参数、工具描述、操作类型 ]
        globalMessage(redisTemplate)

    }


    /**
     * 国际化处理
     */
    fun globalMessage(redisTemplate: RedisTemplate<String, String>) {
        // 响应码、操作记录国际化
        val responseCodeMap = initResponseCode.getGlobalMessageMap()
        for (key in responseCodeMap.keys) {
            redisTemplate.opsForValue().set(key, responseCodeMap[key])
        }

        // 规则包国际化
        val checkerPackageMap = initResponseCode.getCheckerPackage()
        redisTemplate.opsForHash<String, String>().putAll(RedisKeyConstants.GLOBAL_CHECKER_PACKAGE_MSG, checkerPackageMap)

        // 数据报表日期国际化
        val dataReportDate = initResponseCode.getDataReportDate()
        redisTemplate.opsForHash<String, String>().putAll(RedisKeyConstants.GLOBAL_DATA_REPORT_DATE, dataReportDate)

        // 工具描述国际化
        val toolDescription = initResponseCode.getToolDescription()
        redisTemplate.opsForHash<String, String>().putAll(RedisKeyConstants.GLOBAL_TOOL_DESCRIPTION, toolDescription)

        // 工具参数标签[ labelName ]国际化
        val labelName = initResponseCode.getToolParams()
        redisTemplate.opsForHash<String, String>().putAll(RedisKeyConstants.GLOBAL_TOOL_PARAMS_LABEL_NAME, labelName)

        // 工具参数提示[ tips ]国际化
        val tips = initResponseCode.getToolParamsTips()
        redisTemplate.opsForHash<String, String>().putAll(RedisKeyConstants.GLOBAL_TOOL_PARAMS_TIPS, tips)

        // 操作类型国际化
        val operTypeMap = initResponseCode.getOperTypeMap()
        redisTemplate.opsForHash<String, String>().putAll(RedisKeyConstants.GLOBAL_PREFIX_OPERATION_TYPE, operTypeMap)

        val checkDescMap = initResponseCode.getCheckerDescMap()
        redisTemplate.opsForHash<String, String>().putAll(RedisKeyConstants.GLOBAL_CHECKER_DESC, checkDescMap)

    }
}