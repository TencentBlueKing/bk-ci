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

package com.tencent.devops.quality.service.v2

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import com.tencent.devops.quality.api.v2.pojo.RuleIndicatorSet
import com.tencent.devops.quality.api.v2.pojo.RuleTemplate
import com.tencent.devops.quality.api.v2.pojo.op.TemplateData
import com.tencent.devops.quality.api.v2.pojo.op.TemplateIndicatorMap
import com.tencent.devops.quality.api.v2.pojo.op.TemplateUpdateData
import com.tencent.devops.quality.constant.QUALITY_CONTROL_POINT_NAME_KEY
import com.tencent.devops.quality.constant.QUALITY_RULE_TEMPLATE_DESC_KEY
import com.tencent.devops.quality.constant.QUALITY_RULE_TEMPLATE_NAME_KEY
import com.tencent.devops.quality.constant.QUALITY_RULE_TEMPLATE_STAGE_KEY
import com.tencent.devops.quality.dao.v2.QualityControlPointDao
import com.tencent.devops.quality.dao.v2.QualityIndicatorDao
import com.tencent.devops.quality.dao.v2.QualityRuleTemplateDao
import com.tencent.devops.quality.dao.v2.QualityTemplateIndicatorMapDao
import com.tencent.devops.quality.pojo.po.QualityRuleTemplatePO
import java.io.File
import java.util.concurrent.Executors
import javax.annotation.PostConstruct
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

@Service
class QualityTemplateService @Autowired constructor(
    private val dslContext: DSLContext,
    private val ruleTemplateDao: QualityRuleTemplateDao,
    private val indicatorService: QualityIndicatorService,
    private val controlPointService: QualityControlPointService,
    private val qualityControlPointDao: QualityControlPointDao,
    private val ruleTemplateIndicatorDao: QualityTemplateIndicatorMapDao,
    private val indicatorDao: QualityIndicatorDao,
    private val redisOperation: RedisOperation,
    val commonConfig: CommonConfig
) {

    @PostConstruct
    fun init() {
        val redisLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "QUALITY_RULE_TEMPLATE_INIT_LOCK",
            expiredTimeInSeconds = 60

        )
        Executors.newFixedThreadPool(1).submit {
            if (redisLock.tryLock()) {
                try {
                    logger.info("start init quality rule template")
                    val classPathResource = ClassPathResource(
                        "i18n${File.separator}ruleTemplate_${commonConfig.devopsDefaultLocaleLanguage}.json"
                    )
                    val inputStream = classPathResource.inputStream
                    val json = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val qualityRuleTemplatePOs =
                        JsonUtil.to(json, object : TypeReference<List<QualityRuleTemplatePO>>() {})
                    ruleTemplateDao.batchCrateQualityRuleTemplate(dslContext, qualityRuleTemplatePOs)
                    logger.info("init quality rule template end")
                } catch (ignored: Throwable) {
                    logger.warn("init quality rule template fail! error:${ignored.message}")
                } finally {
                    redisLock.unlock()
                }
            }
        }
    }

    fun userListIndicatorSet(): List<RuleIndicatorSet> {
        return ruleTemplateDao.listIndicatorSetEnable(dslContext)?.map { record ->
            val indicatorIds = ruleTemplateIndicatorDao.queryTemplateMap(record.id, dslContext)
                ?.map { item -> item.indicatorId } ?: listOf()
            val indicators = indicatorService.serviceList(indicatorIds)
            RuleIndicatorSet(
                hashId = HashUtil.encodeLongId(record.id),
                name = I18nUtil.getCodeLanMessage(
                    messageCode = QUALITY_RULE_TEMPLATE_NAME_KEY.format("${record.id}"),
                    defaultMessage = record.name
                ),
                desc = I18nUtil.getCodeLanMessage(
                    messageCode = QUALITY_RULE_TEMPLATE_DESC_KEY.format("${record.id}"),
                    defaultMessage = record.desc
                ),
                indicators = indicators
            )
        } ?: listOf()
    }

    fun userList(projectId: String): List<RuleTemplate> {
        val templateList = ruleTemplateDao.listTemplateEnable(dslContext)
        val controlPointRecords = qualityControlPointDao.list(
            dslContext = dslContext,
            elementType = templateList?.map { it.controlPoint } ?: listOf()
        )
        return templateList?.map { record ->
            val indicatorIds = ruleTemplateIndicatorDao.queryTemplateMap(record.id, dslContext)
                ?.map { item -> item.indicatorId } ?: listOf()

            val controlPoint = controlPointService.serviceGet(controlPointRecords, projectId)
            val indicators = indicatorService.serviceList(indicatorIds)
            RuleTemplate(
                hashId = HashUtil.encodeLongId(record.id),
                name = I18nUtil.getCodeLanMessage(
                    messageCode = QUALITY_RULE_TEMPLATE_NAME_KEY.format("${record.id}"),
                    defaultMessage = record.name
                ),
                desc = I18nUtil.getCodeLanMessage(
                    messageCode = QUALITY_RULE_TEMPLATE_DESC_KEY.format("${record.id}"),
                    defaultMessage = record.desc
                ),
                indicators = indicators,
                stage = I18nUtil.getCodeLanMessage(
                    messageCode = QUALITY_RULE_TEMPLATE_STAGE_KEY.format("${record.id}"),
                    defaultMessage = record.stage
                ),
                controlPoint = record.controlPoint,
                controlPointName = controlPoint?.let {
                    I18nUtil.getCodeLanMessage(
                        messageCode = QUALITY_CONTROL_POINT_NAME_KEY.format("${it.elementType}"),
                        defaultMessage = it.name
                    )
                } ?: "",
                controlPointPosition = ControlPointPosition.create(record.controlPointPosition),
                availablePosition = listOf(ControlPointPosition.create("BEFORE"), ControlPointPosition.create("AFTER"))
            )
        } ?: listOf()
    }

    fun opList(userId: String, page: Int?, pageSize: Int?): Page<TemplateData> {
        val controlPointMap = controlPointService.listAllControlPoint().map {
            it.name = it.name
            it.elementType to it
        }.toMap()

        val data = ruleTemplateDao.list(userId, page!!, pageSize!!, dslContext).map { record ->
            val templateIndicatorMap = ruleTemplateIndicatorDao.listByTemplateId(record.id, dslContext)
            val indicatorIds = templateIndicatorMap.map { item -> item.indicatorId }.toHashSet()
            val indicatorList = indicatorDao.listByIds(dslContext, indicatorIds)

            val templateIndicatorMaps = templateIndicatorMap.map { it1 ->
                val indicatorInst = indicatorList?.filter { it2 -> it2.id == it1.indicatorId }?.getOrNull(0)
                val indicatorName = if (indicatorInst != null) {
                    "${indicatorInst.elementName}-${indicatorInst.elementDetail}-${indicatorInst.cnName}"
                } else null
                TemplateIndicatorMap(
                    id = it1.id,
                    templateId = it1.templateId,
                    indicatorId = it1.indicatorId,
                    indicatorName = indicatorName,
                    operation = it1.operation,
                    threshold = it1.threshold
                )
            }
            TemplateData(
                id = record.id,
                name = record.name,
                type = record.type,
                desc = record.desc,
                stage = record.stage,
                elementType = record.controlPoint,
                elementName = controlPointMap[record.controlPoint]?.name,
                controlPointPostion = record.controlPointPosition,
                enable = record.enable,
                indicatorNum = templateIndicatorMap.size,
                indicatorDetail = templateIndicatorMaps
            )
        }
        val count = ruleTemplateDao.count(dslContext)
        return Page(page = page, pageSize = pageSize, count = count, records = data)
    }

    fun opCreate(userId: String, templateUpdateData: TemplateUpdateData): Boolean {
        dslContext.transaction { configuration ->
            val tDSLContext = DSL.using(configuration)
            val templateId = ruleTemplateDao.create(userId, templateUpdateData.templateUpdate, tDSLContext)

            if (templateUpdateData.indicatorDetail != null) {
                templateUpdateData.indicatorDetail!!.forEach { it.templateId = templateId }
            }
            ruleTemplateIndicatorDao.batchCreate(tDSLContext, templateUpdateData.indicatorDetail)
        }
        return true
    }

    fun opDelete(userId: String, id: Long): Boolean {
        logger.info("user($userId) is deleting the rule($id)")
        dslContext.transaction { configuration ->
            val tDSLContext = DSL.using(configuration)
            ruleTemplateDao.delete(userId, id, tDSLContext)
            ruleTemplateIndicatorDao.deleteRealByTemplateId(tDSLContext, id)
        }
        return true
    }

    fun opUpdate(userId: String, id: Long, templateUpdateData: TemplateUpdateData): Boolean {
        logger.info("user($userId) is updating the rule($id)")
        dslContext.transaction { configuration ->
            val tDSLContext = DSL.using(configuration)
            ruleTemplateDao.update(userId, id, templateUpdateData.templateUpdate, tDSLContext)
            ruleTemplateIndicatorDao.deleteRealByTemplateId(tDSLContext, id)
            ruleTemplateIndicatorDao.batchCreate(tDSLContext, templateUpdateData.indicatorDetail)
        }
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(QualityTemplateService::class.java)
    }
}
