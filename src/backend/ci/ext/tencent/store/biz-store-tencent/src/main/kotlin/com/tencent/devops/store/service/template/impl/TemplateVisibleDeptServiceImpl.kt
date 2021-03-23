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
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.api.template.ServiceTemplateResource
import com.tencent.devops.project.api.service.ServiceProjectOrganizationResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.common.AbstractStoreCommonDao
import com.tencent.devops.store.dao.template.MarketTemplateDao
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.DeptInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreDeptService
import com.tencent.devops.store.service.common.StoreVisibleDeptService
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
    private val atomDao: AtomDao,
    private val storeDeptService: StoreDeptService,
    private val storeVisibleDeptService: StoreVisibleDeptService
) : TemplateVisibleDeptService {

    private val logger = LoggerFactory.getLogger(TemplateVisibleDeptServiceImpl::class.java)

    private val cache = CacheBuilder.newBuilder().maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(object : CacheLoader<String, Boolean?>() {
            override fun load(atomCode: String): Boolean? {
                val atomStatusList = listOf(
                    AtomStatusEnum.RELEASED.status.toByte(),
                    AtomStatusEnum.UNDERCARRIAGING.status.toByte()
                )
                val atomRecord = atomDao.getPipelineAtom(
                    dslContext = dslContext,
                    atomCode = atomCode,
                    atomStatusList = atomStatusList
                )
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
        val invalidImageList = mutableListOf<String>()
        val invalidAtomList = mutableListOf<String>()
        val stageList = templateModel.stages
        // 获取模板下镜像的机构信息
        val templateImageDeptMap = storeDeptService.getTemplateImageDeptMap(stageList)
        // 获取模板下插件的机构信息
        val stageAtomDeptMap = storeDeptService.getStageAtomDeptMap(stageList)
        stageList.forEach { stage ->
            val stageId = stage.id
            val currentStageAtomDeptMap = stageAtomDeptMap[stageId]
            val containerList = stage.containers
            containerList.forEach { container ->
                // 判断镜像的可见范围是否在模板的可见范围之内
                handleImageVisible(container, templateImageDeptMap, deptInfos, invalidImageList)
                val elementList = container.elements
                elementList.forEach { element ->
                    // 判断插件的可见范围是否在模板的可见范围之内
                    handleAtomVisible(element, currentStageAtomDeptMap, deptInfos, invalidAtomList)
                }
            }
        }
        logger.info("validateTemplateVisibleDept invalidImageList:$invalidImageList")
        if (invalidImageList.isNotEmpty()) {
            // 存在不在插件的可见范围内的模板，给出错误提示
            return MessageCodeUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_TEMPLATE_IMAGE_VISIBLE_DEPT_IS_INVALID,
                params = arrayOf(JsonUtil.toJson(invalidImageList)),
                data = false
            )
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

    private fun handleAtomVisible(
        element: Element,
        currentStageAtomDeptMap: Map<String, List<DeptInfo>?>?,
        deptInfos: List<DeptInfo>?,
        invalidAtomList: MutableList<String>
    ) {
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
            handleInvalidStoreList(
                storeDeptMap = currentStageAtomDeptMap,
                storeCode = atomCode,
                templateDeptInfos = deptInfos,
                invalidStoreList = invalidAtomList,
                storeName = atomName
            )
        }
    }

    private fun handleImageVisible(
        container: Container,
        templateImageDeptMap: Map<String, List<DeptInfo>?>,
        deptInfos: List<DeptInfo>?,
        invalidImageList: MutableList<String>
    ) {
        val storeType = StoreTypeEnum.IMAGE.name
        if (container is VMBuildContainer && container.dispatchType is DockerDispatchType) {
            val dispatchType = container.dispatchType as DockerDispatchType
            val imageCode = dispatchType.imageCode
            val imageName = dispatchType.imageName
            if (!imageCode.isNullOrBlank()) {
                handleInvalidImageList(
                    storeType = storeType,
                    imageCode = imageCode,
                    imageName = imageName,
                    templateImageDeptMap = templateImageDeptMap,
                    deptInfos = deptInfos,
                    invalidImageList = invalidImageList
                )
            }
        }
    }

    private fun handleInvalidImageList(
        storeType: String,
        imageCode: String,
        imageName: String?,
        templateImageDeptMap: Map<String, List<DeptInfo>?>,
        deptInfos: List<DeptInfo>?,
        invalidImageList: MutableList<String>
    ) {
        val storeCommonDao = try {
            SpringContextUtil.getBean(AbstractStoreCommonDao::class.java, "${storeType}_COMMON_DAO")
        } catch (e: Exception) {
            logger.warn("StoreCommonDao is not exist")
            null
        }
        if (storeCommonDao != null) {
            val storeBaseInfo = storeCommonDao.getStoreBaseInfoByCode(dslContext, imageCode!!)
                ?: throw ErrorCodeException(
                    errorCode = StoreMessageCode.USER_IMAGE_NOT_EXIST,
                    params = arrayOf(imageName ?: imageCode)
                )
            // 如果镜像是公共镜像，则无需校验与模板的可见范围
            if (!storeBaseInfo.publicFlag) {
                handleInvalidStoreList(
                    storeDeptMap = templateImageDeptMap,
                    storeCode = imageCode,
                    templateDeptInfos = deptInfos,
                    invalidStoreList = invalidImageList,
                    storeName = imageName ?: imageCode
                )
            }
        }
    }

    private fun handleInvalidStoreList(
        storeDeptMap: Map<String, List<DeptInfo>?>?,
        storeCode: String,
        templateDeptInfos: List<DeptInfo>?,
        invalidStoreList: MutableList<String>,
        storeName: String
    ) {
        var flag = false
        val storeDeptInfos = storeDeptMap?.get(storeCode)
        run breaking@{
            storeDeptInfos?.forEach deptEach@{ deptInfo ->
                val storeDeptId = deptInfo.deptId
                val storeDeptName = deptInfo.deptName
                val storeDepts = storeDeptName.split("/")
                val storeDeptSize = storeDepts.size
                flag = handleTemplateDeptInfos(templateDeptInfos, storeDeptSize, storeDeptId)
                if (flag) return@breaking
            }
        }
        // 判断每个组件下的可见范围是否都在模板的可见范围之内
        if (!flag) {
            invalidStoreList.add(storeName)
        }
    }

    private fun handleTemplateDeptInfos(
        templateDeptInfos: List<DeptInfo>?,
        storeDeptSize: Int,
        storeDeptId: Int
    ): Boolean {
        templateDeptInfos?.forEach templateDeptEach@{ dept ->
            val templateDeptId = dept.deptId
            val templateDeptName = dept.deptName
            val templateDepts = templateDeptName.split("/")
            val templateDeptSize = templateDepts.size
            if (templateDeptSize < storeDeptSize) {
                return@templateDeptEach
            }
            return validateTemplateDept(
                storeDeptId = storeDeptId,
                templateDeptId = templateDeptId,
                templateDeptSize = templateDeptSize,
                storeDeptSize = storeDeptSize
            )
        }
        return false
    }

    private fun validateTemplateDept(
        storeDeptId: Int,
        templateDeptId: Int,
        templateDeptSize: Int,
        storeDeptSize: Int
    ): Boolean {
        if (storeDeptId == 0 || templateDeptId == storeDeptId) {
            return true // 组件在模板的可见范围内
        }
        // 判断模板的上级机构是否属于组件的可见范围
        return validateTemplateVisible(
            templateDeptSize = templateDeptSize,
            storeDeptSize = storeDeptSize,
            templateDeptId = templateDeptId,
            storeDeptId = storeDeptId
        )
    }

    private fun validateTemplateVisible(
        templateDeptSize: Int,
        storeDeptSize: Int,
        templateDeptId: Int,
        storeDeptId: Int
    ): Boolean {
        val gap = templateDeptSize - storeDeptSize
        val parentDeptInfoList = client.get(ServiceProjectOrganizationResource::class)
            .getParentDeptInfos(templateDeptId.toString(), gap + 1).data
        parentDeptInfoList?.forEach {
            if (it.id.toInt() == storeDeptId) {
                return true // 组件在模板的可见范围内
            }
        }
        return false
    }
}
