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

package com.tencent.devops.store.service.template.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.process.api.template.ServiceTemplateResource
import com.tencent.devops.project.api.service.ServiceProjectOrganizationResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.common.StoreDeptRelDao
import com.tencent.devops.store.dao.template.MarketTemplateDao
import com.tencent.devops.store.pojo.common.DeptInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreVisibleDeptService
import com.tencent.devops.store.service.template.TemplateVisibleDeptService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 模板可见范围逻辑类
 * since: 2019-01-08
 */
@Service
class TemplateVisibleDeptServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val storeDeptRelDao: StoreDeptRelDao,
    private val marketTemplateDao: MarketTemplateDao,
    private val marketAtomDao: MarketAtomDao,
    private val atomDao: AtomDao,
    private val storeVisibleDeptService: StoreVisibleDeptService
) : TemplateVisibleDeptService {

    private val logger = LoggerFactory.getLogger(TemplateVisibleDeptServiceImpl::class.java)

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
        stageList.forEach { stage ->
            val containerList = stage.containers
            containerList.forEach { container ->
                val elementList = container.elements
                elementList.forEach { element ->
                    // 判断插件的可见范围是否在模板的可见范围之内
                    val atomCode = element.getAtomCode()
                    val atomVersion = element.version
                    logger.info("the atomCode is:$atomCode，atomVersion is:$atomVersion")
                    val atomRecord = if (atomVersion.isNotEmpty()) {
                        atomDao.getPipelineAtom(dslContext, atomCode, atomVersion.replace("*", ""))
                    } else {
                        marketAtomDao.getLatestAtomByCode(dslContext, atomCode) // 兼容历史存量原子插件的情况
                    }
                    logger.info("the atomRecord is:$atomRecord")
                    if (null == atomRecord) {
                        return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(atomCode))
                    }
                    val atomDeptRelRecords = storeDeptRelDao.batchList(dslContext, listOf(atomCode), StoreTypeEnum.ATOM.type.toByte())
                    logger.info("the atomCode is :$atomCode,atomDeptRelRecords is :$atomDeptRelRecords")
                    // 如果插件是默认插件，则无需校验与模板的可见范围
                    if (!atomRecord.defaultFlag) {
                        var flag = false
                        atomDeptRelRecords?.forEach deptEach@{ deptRel ->
                            logger.info("the begin atomDeptId is:${deptRel.deptId}")
                            val atomDeptId = deptRel.deptId
                            logger.info("atomDeptId is:$atomDeptId")
                            val atomDeptName = deptRel.deptName
                            val atomDepts = atomDeptName.split("/")
                            val atomDeptSize = atomDepts.size
                            logger.info("atomDeptSize is:$atomDeptSize")
                            deptInfos?.forEach { dept ->
                                val templateDeptId = dept.deptId
                                logger.info("templateDeptId is:$templateDeptId")
                                val templateDeptName = dept.deptName
                                val templateDepts = templateDeptName.split("/")
                                val templateDeptSize = templateDepts.size
                                logger.info("templateDeptSize is:$templateDeptSize")
                                if (templateDeptSize < atomDeptSize) {
                                    // 原子可见范围比模板可见范围小，不符合要求
                                } else {
                                    if (templateDeptId == atomDeptId) {
                                        flag = true // 原子插件在模板的可见范围内
                                        return@deptEach
                                    }
                                    val gap = templateDeptSize - atomDeptSize
                                    // 判断模板的上级机构是否属于原子插件的可见范围
                                    val parentDeptInfoList = client.get(ServiceProjectOrganizationResource::class).getParentDeptInfos(templateDeptId.toString(), gap + 1).data
                                    logger.info("the parentDeptInfoList is:$parentDeptInfoList")
                                    parentDeptInfoList?.forEach {
                                        logger.info("the validate atomDeptId is:$atomDeptId,parentTemplateDeptId is:${it.id}")
                                        if (it.id.toInt() == atomDeptId) {
                                            flag = true // 原子插件在模板的可见范围内
                                            return@deptEach
                                        }
                                    }
                                }
                            }
                        }
                        // 判断每个插件下的可见范围是否都在模板的可见范围之内
                        logger.info("the flag is:$flag")
                        if (!flag) {
                            logger.info("template dept visible is large than atom")
                            invalidAtomList.add(element.name)
                        }
                    }
                }
            }
        }
        if (invalidAtomList.isNotEmpty()) {
            // 存在不在原子插件的可见范围内的模板，给出错误提示
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_TEMPLATE_ATOM_VISIBLE_DEPT_IS_INVALID, arrayOf(JsonUtil.toJson(invalidAtomList)), false)
        }
        return Result(true)
    }
}
