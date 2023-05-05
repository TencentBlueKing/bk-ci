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

package com.tencent.devops.process.engine.service.rule

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.coerceAtMaxLength
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.engine.dao.PipelineRuleDao
import com.tencent.devops.process.engine.service.rule.processor.ProcessorService
import com.tencent.devops.process.pojo.pipeline.PipelineRule
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service
class PipelineRuleService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineRuleDao: PipelineRuleDao,
    private val redisOperation: RedisOperation
) {

    companion object {
        private const val MAX_BUILD_NUM_ALIAS_LENGTH = 256
        private const val PIPELINE_RULE_PROCESSOR_KEY_PREFIX = "PIPELINE_RULE_PROCESSOR_KEY"
        private val logger = LoggerFactory.getLogger(PipelineRuleService::class.java)
    }

    fun savePipelineRule(userId: String, pipelineRule: PipelineRule): Boolean {
        logger.info("savePipelineRule userId:$userId,pipelineRule:$pipelineRule")
        val ruleName = pipelineRule.ruleName
        val busCode = pipelineRule.busCode
        // 判断同业务的规则名称是否存在
        val nameCount = pipelineRuleDao.countByName(dslContext, ruleName, busCode)
        if (nameCount > 0) {
            // 抛出错误提示
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf("$ruleName+$busCode")
            )
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            pipelineRuleDao.add(context, pipelineRule, userId)
            redisOperation.hset(getPipelineRuleProcessorKey(busCode), ruleName, pipelineRule.processor)
        }
        return true
    }

    fun updatePipelineRule(userId: String, ruleId: String, pipelineRule: PipelineRule): Boolean {
        logger.info("savePipelineRule userId:$userId,ruleId:$ruleId,pipelineRule:$pipelineRule")
        val ruleName = pipelineRule.ruleName
        val busCode = pipelineRule.busCode
        // 判断同业务的规则名称是否存在
        val nameCount = pipelineRuleDao.countByName(dslContext, ruleName, busCode)
        if (nameCount > 0) {
            val rule = pipelineRuleDao.getPipelineRuleById(dslContext, ruleId)
            // 判断信息是否被修改
            if (null != rule && !(ruleName == rule.ruleName && busCode == rule.busCode)) {
                // 抛出错误提示
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_EXIST,
                    params = arrayOf("$ruleName+$busCode")
                )
            }
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            pipelineRuleDao.update(
                dslContext = context,
                ruleId = ruleId,
                pipelineRule = pipelineRule,
                userId = userId
            )
            redisOperation.hset(getPipelineRuleProcessorKey(busCode), ruleName, pipelineRule.processor)
        }
        return true
    }

    fun deletePipelineRule(userId: String, ruleId: String): Boolean {
        val pipelineRule = getPipelineRule(userId, ruleId) ?: return true
        val ruleName = pipelineRule.ruleName
        val busCode = pipelineRule.busCode
        dslContext.transaction { t ->
            val context = DSL.using(t)
            redisOperation.hdelete(getPipelineRuleProcessorKey(busCode), ruleName)
            pipelineRuleDao.delete(context, ruleId)
        }
        return true
    }

    fun getPipelineRule(userId: String, ruleId: String): PipelineRule? {
        val pipelineRuleRecord = pipelineRuleDao.getPipelineRuleById(dslContext, ruleId)
        return if (pipelineRuleRecord != null) {
            PipelineRule(
                ruleName = pipelineRuleRecord.ruleName,
                busCode = pipelineRuleRecord.busCode,
                processor = pipelineRuleRecord.processor
            )
        } else {
            null
        }
    }

    fun getPipelineRuleProcessor(busCode: String, ruleName: String): String? {
        var processor = redisOperation.hget(getPipelineRuleProcessorKey(busCode), ruleName)
        if (processor.isNullOrEmpty()) {
            val pipelineRuleRecord = pipelineRuleDao.getPipelineRuleByName(dslContext, ruleName, busCode)
            if (pipelineRuleRecord != null) {
                processor = pipelineRuleRecord.processor
                redisOperation.hset(getPipelineRuleProcessorKey(busCode), ruleName, processor)
            }
        }
        return processor
    }

    private fun getPipelineRuleProcessorKey(busCode: String) = "$PIPELINE_RULE_PROCESSOR_KEY_PREFIX:$busCode"

    fun getPipelineRules(
        userId: String,
        ruleName: String? = null,
        busCode: String? = null,
        page: Int = 1,
        pageSize: Int = 10
    ): Page<PipelineRule>? {
        val pipelineRuleList = pipelineRuleDao.getPipelineRules(
            dslContext = dslContext,
            ruleName = ruleName,
            busCode = busCode,
            page = page,
            pageSize = pageSize
        )?.map { pipelineRuleRecord ->
            PipelineRule(
                ruleName = pipelineRuleRecord.ruleName,
                busCode = pipelineRuleRecord.busCode,
                processor = pipelineRuleRecord.processor
            )
        }
        val pipelineRuleCount = pipelineRuleDao.getPipelineRuleCount(dslContext, ruleName, busCode)
        val totalPages = PageUtil.calTotalPage(pageSize, pipelineRuleCount)
        return Page(
            count = pipelineRuleCount,
            page = page,
            pageSize = pageSize,
            totalPages = totalPages,
            records = pipelineRuleList ?: listOf()
        )
    }

    fun parsePipelineRule(
        projectId: String,
        pipelineId: String,
        buildId: String,
        busCode: String,
        ruleStr: String
    ): String {
        val validRuleProcessorMap = validateRuleStr(ruleStr, busCode)
        val validRuleValueMap = mutableMapOf<String, String>()
        validRuleProcessorMap.map { validRule ->
            // 根据规则名称获取具体的规则值
            val ruleName = validRule.key
            val processorName = validRule.value
            val processor = SpringContextUtil.getBean(ProcessorService::class.java, processorName)
            val ruleValue = processor.getRuleValue(projectId, ruleName, pipelineId, buildId)
            validRuleValueMap[ruleName] = ruleValue ?: ""
        }
        return generateReplaceRuleStr(ruleStr, validRuleValueMap)
    }

    fun validateRuleStr(ruleStr: String, busCode: String): MutableMap<String, String> {
        val ruleNameList = getRuleNameList(ruleStr)
        val validRuleProcessorMap = mutableMapOf<String, String>()
        ruleNameList.forEach { ruleName ->
            val processor = getPipelineRuleProcessor(busCode, ruleName)
            if (!processor.isNullOrEmpty()) {
                validRuleProcessorMap[ruleName] = processor
            }
        }
        // 判断用户填的规则是否合法
        if (ruleNameList.size != validRuleProcessorMap.size) {
            ruleNameList.removeAll(validRuleProcessorMap.keys)
            // 判断规则是否符合通用规则
            validateCommonRuleName(ruleNameList, busCode, validRuleProcessorMap)
            if (ruleNameList.isNotEmpty()) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf("$ruleStr(error rule:${JsonUtil.toJson(ruleNameList, formatted = false)})")
                )
            }
        }
        return validRuleProcessorMap
    }

    private fun validateCommonRuleName(
        ruleNameList: MutableList<String>,
        busCode: String,
        validRuleProcessorMap: MutableMap<String, String>
    ) {
        val commonRuleNameMap = mutableMapOf<String, String>()
        ruleNameList.forEach { ruleName ->
            val pattern = Pattern.compile("(?<=:\")(.+?)(?=\")")
            val matcher = pattern.matcher(ruleName)
            if (matcher.find()) {
                val content = matcher.group()
                val commonRuleName = ruleName.replace(":\"$content\"", ":\"(.+?)\"")
                commonRuleNameMap[ruleName] = commonRuleName
            }
        }
        // 判断通用规则是否存在
        if (commonRuleNameMap.isNotEmpty()) {
            val realCommonRuleNameList = mutableListOf<String>()
            commonRuleNameMap.forEach { (ruleName, commonRuleName) ->
                val processor = getPipelineRuleProcessor(busCode, commonRuleName)
                if (!processor.isNullOrEmpty()) {
                    realCommonRuleNameList.add(ruleName)
                    validRuleProcessorMap[ruleName] = processor
                }
            }
            ruleNameList.removeAll(realCommonRuleNameList)
        }
    }

    fun generateReplaceRuleStr(ruleStr: String, validRuleValueMap: MutableMap<String, String>): String {
        var replaceRuleStr = ruleStr
        validRuleValueMap.forEach { (ruleName, ruleValue) ->
            // 占位符替换
            replaceRuleStr = replaceRuleStr.replace("\${{$ruleName}}", ruleValue)
        }
        return replaceRuleStr.coerceAtMaxLength(MAX_BUILD_NUM_ALIAS_LENGTH)
    }

    fun getRuleNameList(ruleStr: String): MutableList<String> {
        val ruleNameList = mutableListOf<String>()
        val pattern = Pattern.compile("(?<=\\$\\{\\{)(.+?)(?=}})")
        val matcher = pattern.matcher(ruleStr)
        // 根据${{xxx}}提取规则名称xxx
        while (matcher.find()) {
            val ruleName = matcher.group()
            ruleNameList.add(ruleName)
        }
        return ruleNameList
    }
}
