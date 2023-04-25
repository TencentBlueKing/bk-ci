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
import com.tencent.devops.common.pipeline.type.StoreDispatchType
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.api.template.ServicePTemplateResource
import com.tencent.devops.project.api.service.ServiceProjectOrganizationResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.common.AbstractStoreCommonDao
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.DeptInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import com.tencent.devops.store.service.common.StoreDeptService
import com.tencent.devops.store.service.common.StoreVisibleDeptService
import com.tencent.devops.store.service.template.TemplateVisibleDeptService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Optional
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Response

/**
 * 模板可见范围逻辑类
 * since: 2019-01-08
 */
@Service
class TemplateVisibleDeptServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val atomDao: AtomDao,
    private val storeDeptService: StoreDeptService,
    private val storeVisibleDeptService: StoreVisibleDeptService
) : TemplateVisibleDeptService {

    private val logger = LoggerFactory.getLogger(TemplateVisibleDeptServiceImpl::class.java)

    private val cache = CacheBuilder.newBuilder().maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(object : CacheLoader<String, Optional<Boolean>>() {
            override fun load(atomCode: String): Optional<Boolean> {
                val atomStatusList = listOf(
                    AtomStatusEnum.RELEASED.status.toByte(),
                    AtomStatusEnum.UNDERCARRIAGING.status.toByte(),
                    AtomStatusEnum.UNDERCARRIAGED.status.toByte()
                )
                val atomRecord = atomDao.getPipelineAtom(
                    dslContext = dslContext,
                    atomCode = atomCode,
                    atomStatusList = atomStatusList
                )
                return if (atomRecord != null) {
                    Optional.of(atomRecord.defaultFlag)
                } else {
                    Optional.empty()
                }
            }
        })

    /**
     * 设置模板可见范围
     */
    override fun addVisibleDept(userId: String, templateCode: String, deptInfos: List<DeptInfo>): Result<Boolean> {
        logger.info("addVisibleDept userId is :$userId,templateCode is :$templateCode,deptInfos is :$deptInfos")
        val validateResult = validateTemplateVisibleDept(templateCode, deptInfos)
        logger.info("the validateResult is :$validateResult")
        if (validateResult.isNotOk()) {
            return validateResult
        }
        return storeVisibleDeptService.addVisibleDept(userId, templateCode, deptInfos, StoreTypeEnum.TEMPLATE)
    }

    private fun getTemplateModel(templateCode: String): Result<Model?> {
        val result = client.get(ServicePTemplateResource::class).getTemplateDetailInfo(templateCode)
        val templateDetailInfo = result.data
        val templateModel = templateDetailInfo?.templateModel
        return Result(templateModel)
    }

    override fun validateTemplateVisibleDept(
        templateCode: String,
        validImageCodes: List<String>?,
        validAtomCodes: List<String>?
    ): Result<Boolean> {
        logger.info("validateTemplateVisibleDept templateCode:$templateCode")
        return validateTemplateVisibleDept(
            templateCode = templateCode,
            validImageCodes = validImageCodes,
            validAtomCodes = validAtomCodes
        )
    }

    override fun validateTemplateVisibleDept(
        templateCode: String,
        deptInfos: List<DeptInfo>?,
        validImageCodes: List<String>?,
        validAtomCodes: List<String>?
    ): Result<Boolean> {
        logger.info("validateTemplateVisibleDept templateCode:$templateCode,deptInfos:$deptInfos")
        val templateModelResult = getTemplateModel(templateCode)
        if (templateModelResult.isNotOk()) {
            // 抛出错误提示
            throw ErrorCodeException(
                statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                errorCode = templateModelResult.status.toString(),
                defaultMessage = templateModelResult.message
            )
        }
        val templateModel = templateModelResult.data
            ?: throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(templateCode)
            )
        return validateTemplateVisibleDept(
            templateModel = templateModel,
            deptInfos = deptInfos,
            validImageCodes = validImageCodes,
            validAtomCodes = validAtomCodes
        )
    }

    override fun validateTemplateVisibleDept(
        templateModel: Model,
        deptInfos: List<DeptInfo>?,
        validImageCodes: List<String>?,
        validAtomCodes: List<String>?
    ): Result<Boolean> {
        logger.info("validateTemplateVisibleDept deptInfos is :$deptInfos")
        val invalidImageList = mutableListOf<String>()
        val invalidAtomList = mutableListOf<String>()
        val stageList = templateModel.stages
        // 获取模板下镜像的机构信息
        val templateImageDeptMap = storeDeptService.getTemplateImageDeptMap(stageList)
        // 获取模板下插件的机构信息
        val templateAtomDeptMap = storeDeptService.getTemplateAtomDeptMap(stageList)
        stageList.forEach { stage ->
            val containerList = stage.containers
            containerList.forEach { container ->
                // 判断镜像的可见范围是否在模板的可见范围之内
                handleImageVisible(
                    container = container,
                    templateImageDeptMap = templateImageDeptMap,
                    deptInfos = deptInfos,
                    invalidImageList = invalidImageList,
                    validImageCodes = validImageCodes
                )
                val elementList = container.elements
                elementList.forEach { element ->
                    // 判断插件的可见范围是否在模板的可见范围之内
                    handleAtomVisible(
                        element = element,
                        templateAtomDeptMap = templateAtomDeptMap,
                        deptInfos = deptInfos,
                        invalidAtomList = invalidAtomList,
                        validAtomCodes = validAtomCodes
                    )
                }
            }
        }
        logger.info("validateTemplateVisibleDept invalidImageList:$invalidImageList")
        if (invalidImageList.isNotEmpty()) {
            // 存在不在插件的可见范围内的模板，给出错误提示
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_TEMPLATE_IMAGE_VISIBLE_DEPT_IS_INVALID,
                params = arrayOf(JsonUtil.toJson(invalidImageList))
            )
        }
        logger.info("validateTemplateVisibleDept invalidAtomList:$invalidAtomList")
        if (invalidAtomList.isNotEmpty()) {
            // 存在不在插件的可见范围内的模板，给出错误提示
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_TEMPLATE_ATOM_VISIBLE_DEPT_IS_INVALID,
                params = arrayOf(JsonUtil.toJson(invalidAtomList))
            )
        }
        return Result(true)
    }

    private fun handleAtomVisible(
        element: Element,
        templateAtomDeptMap: Map<String, List<DeptInfo>?>?,
        deptInfos: List<DeptInfo>?,
        invalidAtomList: MutableList<String>,
        validAtomCodes: List<String>?
    ) {
        val atomCode = element.getAtomCode()
        val atomName = element.name
        // 判断插件是否为默认插件
        val defaultFlagOptional = cache.get(atomCode)
        val defaultFlag = if (defaultFlagOptional.isPresent) {
            defaultFlagOptional.get()
        } else {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_TEMPLATE_ATOM_IS_INVALID,
                params = arrayOf(element.name)
            )
        }
        // 如果插件是默认插件或者在可用插件列表，则无需校验与模板的可见范围
        if (!defaultFlag && validAtomCodes?.contains(atomCode) != true) {
            handleInvalidStoreList(
                storeDeptMap = templateAtomDeptMap,
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
        invalidImageList: MutableList<String>,
        validImageCodes: List<String>?
    ) {
        val storeType = StoreTypeEnum.IMAGE.name
        if (container is VMBuildContainer && container.dispatchType is StoreDispatchType) {
            val dispatchType = container.dispatchType as StoreDispatchType
            val imageCode = dispatchType.imageCode
            val imageName = dispatchType.imageName
            if (!imageCode.isNullOrBlank()) {
                handleInvalidImageList(
                    storeType = storeType,
                    imageCode = imageCode,
                    imageName = imageName,
                    templateImageDeptMap = templateImageDeptMap,
                    deptInfos = deptInfos,
                    invalidImageList = invalidImageList,
                    validImageCodes = validImageCodes
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
        invalidImageList: MutableList<String>,
        validImageCodes: List<String>?
    ) {
        val storeCommonDao = try {
            SpringContextUtil.getBean(AbstractStoreCommonDao::class.java, "${storeType}_COMMON_DAO")
        } catch (e: Exception) {
            logger.warn("StoreCommonDao is not exist")
            null
        }
        if (storeCommonDao != null) {
            val storeBaseInfo = storeCommonDao.getNewestStoreBaseInfoByCode(
                dslContext = dslContext,
                storeCode = imageCode,
                storeStatus = ImageStatusEnum.RELEASED.status.toByte()
            )
                ?: throw ErrorCodeException(
                    errorCode = StoreMessageCode.USER_IMAGE_NOT_EXIST,
                    params = arrayOf(imageName ?: imageCode)
                )
            // 如果镜像是公共镜像或者在可用镜像列表，则无需校验与模板的可见范围
            if (!storeBaseInfo.publicFlag && validImageCodes?.contains(imageCode) != true) {
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
        var flag = false
        run breaking@{
            parentDeptInfoList?.forEach {
                flag = it.id.toInt() == storeDeptId // 组件在模板的可见范围内
                if (flag) return@breaking
            }
        }
        return flag
    }
}
