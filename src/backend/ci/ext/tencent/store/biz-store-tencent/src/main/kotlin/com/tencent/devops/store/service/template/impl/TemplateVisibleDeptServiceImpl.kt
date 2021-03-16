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

package com.tencent.devops.store.service.template.impl

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.store.tables.records.TStoreDeptRelRecord
import com.tencent.devops.process.api.template.ServiceTemplateResource
import com.tencent.devops.project.api.service.ServiceProjectOrganizationResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.template.MarketTemplateDao
import com.tencent.devops.store.pojo.common.DeptInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreVisibleDeptService
import com.tencent.devops.store.service.template.MarketTemplateService
import com.tencent.devops.store.service.template.TemplateVisibleDeptService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * 模板可见范围逻辑类
 * since: 2019-01-08
 */
@Service
class TemplateVisibleDeptServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val marketTemplateDao: MarketTemplateDao,
    private val marketAtomDao: MarketAtomDao,
    private val marketTemplateService: MarketTemplateService,
    private val storeVisibleDeptService: StoreVisibleDeptService
) : TemplateVisibleDeptService {

    private val logger = LoggerFactory.getLogger(TemplateVisibleDeptServiceImpl::class.java)

    private val cache = CacheBuilder.newBuilder().maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(object : CacheLoader<String, Boolean?>() {
            override fun load(atomCode: String): Boolean? {
                val atomRecord = marketAtomDao.getLatestAtomByCode(dslContext, atomCode)
                return atomRecord?.defaultFlag
            }
        })

    /**
     * 设置模板可见范围
     */
    override fun addVisibleDept(userId: String, templateCode: String, deptInfos: List<DeptInfo>): Result<Boolean> {
        logger.info("the userId is :$userId,templateCode is :$templateCode,deptInfos is :$deptInfos")
        val validateResult = validateTemplateVisibleDept(templateCode, deptInfos)
        logger.info("the validateResult is :$validateResult")
        if (validateResult.isNotOk()) {
            return validateResult
        }
        return storeVisibleDeptService.addVisibleDept(userId, templateCode, deptInfos, StoreTypeEnum.TEMPLATE)
    }

    private fun getTemplateModel(templateCode: String): Result<Model?> {
        val templateRecord = marketTemplateDao.getUpToDateTemplateByCode(dslContext, templateCode)
        logger.info("the templateRecord is :$templateRecord")
        if (null == templateRecord) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(templateCode))
        }
        val result = client.get(ServiceTemplateResource::class).getTemplateDetailInfo(templateCode, templateRecord.publicFlag)
        logger.info("the result is :$result")
        if (result.isNotOk()) {
            // 抛出错误提示
            return Result(result.status, result.message ?: "")
        }
        val templateDetailInfo = result.data
        val templateModel = templateDetailInfo?.templateModel
        return Result(templateModel)
    }

    override fun validateTemplateVisibleDept(templateCode: String, deptInfos: List<DeptInfo>?): Result<Boolean> {
        logger.info("validateTemplateVisibleDept templateCode is :$templateCode,deptInfos is :$deptInfos")
        val templateModelResult = getTemplateModel(templateCode)
        logger.info("the templateModelResult is :$templateModelResult")
        if (templateModelResult.isNotOk()) {
            // 抛出错误提示
            return Result(templateModelResult.status, templateModelResult.message ?: "")
        }
        val templateModel = templateModelResult.data
        logger.info("the templateModel is :$templateModel")
        if (null == templateModel) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
        }
        return validateTemplateVisibleDept(templateModel, deptInfos)
    }

    override fun validateTemplateVisibleDept(templateModel: Model, deptInfos: List<DeptInfo>?): Result<Boolean> {
        logger.info("validateTemplateVisibleDept templateModel is :$templateModel,deptInfos is :$deptInfos")
        val invalidAtomList = mutableListOf<String>()
        val stageList = templateModel.stages
        val stageAtomDeptMap = marketTemplateService.getStageAtomDeptMap(stageList)
        stageList.forEach { stage ->
            val stageId = stage.id
            val currentStageAtomDeptMap = stageAtomDeptMap[stageId]
            val containerList = stage.containers
            containerList.forEach { container ->
                val elementList = container.elements
                elementList.forEach { element ->
                    // 判断插件的可见范围是否在模板的可见范围之内
                    val atomCode = element.getAtomCode()
                    val atomName = element.name
                    // 判断插件是否为默认插件
                    val defaultFlag = cache.get(atomCode)
                        ?: throw ErrorCodeException(
                            errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                            params = arrayOf(atomCode)
                        )
                    // 如果插件是默认插件，则无需校验与模板的可见范围
                    if (!defaultFlag) {
                        handleInvalidAtomList(
                            currentStageAtomDeptMap = currentStageAtomDeptMap,
                            atomCode = atomCode,
                            deptInfos = deptInfos,
                            invalidAtomList = invalidAtomList,
                            atomName = atomName
                        )
                    }
                }
            }
        }
        logger.info("validateTemplateVisibleDept invalidAtomList:$invalidAtomList")
        if (invalidAtomList.isNotEmpty()) {
            // 存在不在插件的可见范围内的模板，给出错误提示
            return MessageCodeUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_TEMPLATE_ATOM_VISIBLE_DEPT_IS_INVALID,
                params = arrayOf(JsonUtil.toJson(invalidAtomList)),
                data = false
            )
        }
        return Result(true)
    }

    private fun handleInvalidAtomList(
        currentStageAtomDeptMap: Map<String, List<TStoreDeptRelRecord>>?,
        atomCode: String,
        deptInfos: List<DeptInfo>?,
        invalidAtomList: MutableList<String>,
        atomName: String
    ) {
        var flag = false
        val atomDeptRelRecords = currentStageAtomDeptMap?.get(atomCode)
        atomDeptRelRecords?.forEach deptEach@{ deptRel ->
            val atomDeptId = deptRel.deptId
            val atomDeptName = deptRel.deptName
            val atomDepts = atomDeptName.split("/")
            val atomDeptSize = atomDepts.size
            deptInfos?.forEach { dept ->
                val templateDeptId = dept.deptId
                val templateDeptName = dept.deptName
                val templateDepts = templateDeptName.split("/")
                val templateDeptSize = templateDepts.size
                if (templateDeptSize < atomDeptSize) {
                    // 插件可见范围比模板可见范围小，不符合要求
                } else {
                    if (templateDeptId == atomDeptId) {
                        flag = true // 原子插件在模板的可见范围内
                        return@deptEach
                    }
                    val gap = templateDeptSize - atomDeptSize
                    // 判断模板的上级机构是否属于插件的可见范围
                    val parentDeptInfoList = client.get(ServiceProjectOrganizationResource::class)
                        .getParentDeptInfos(templateDeptId.toString(), gap + 1).data
                    parentDeptInfoList?.forEach {
                        if (it.id.toInt() == atomDeptId) {
                            flag = true // 插件在模板的可见范围内
                            return@deptEach
                        }
                    }
                }
            }
        }
        // 判断每个插件下的可见范围是否都在模板的可见范围之内
        if (!flag) {
            invalidAtomList.add(atomName)
        }
    }
}
