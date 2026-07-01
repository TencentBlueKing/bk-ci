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

package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.store.common.dao.StoreBaseExtQueryDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureExtQueryDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureQueryDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreProjectRelDao
import com.tencent.devops.store.common.dao.StoreVersionLogDao
import com.tencent.devops.store.common.service.CategoryService
import com.tencent.devops.store.common.service.ClassifyService
import com.tencent.devops.store.common.service.StoreCommentService
import com.tencent.devops.store.common.service.StoreComponentDetailQueryService
import com.tencent.devops.store.common.service.StoreHonorService
import com.tencent.devops.store.common.service.StoreIndexManageService
import com.tencent.devops.store.common.service.StoreLabelService
import com.tencent.devops.store.common.service.StoreTotalStatisticService
import com.tencent.devops.store.common.service.StoreUserService
import com.tencent.devops.store.common.service.action.StoreDecorateFactory
import com.tencent.devops.store.common.utils.StoreExtFieldUtil
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.pojo.common.BK_STORE_CREATIVE_STREAM_MANUAL_TRIGGER
import com.tencent.devops.store.pojo.common.KEY_HTML_TEMPLATE_VERSION
import com.tencent.devops.store.pojo.common.StoreDetailInfo
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.VersionModel
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreComponentDetailQueryServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseFeatureQueryDao: StoreBaseFeatureQueryDao,
    private val storeBaseExtQueryDao: StoreBaseExtQueryDao,
    private val storeBaseFeatureExtQueryDao: StoreBaseFeatureExtQueryDao,
    private val storeVersionLogDao: StoreVersionLogDao,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val storeUserService: StoreUserService,
    private val storeLabelService: StoreLabelService,
    private val storeCommentService: StoreCommentService,
    private val classifyService: ClassifyService,
    private val storeTotalStatisticService: StoreTotalStatisticService,
    private val categoryService: CategoryService,
    private val storeHonorService: StoreHonorService,
    private val storeIndexManageService: StoreIndexManageService
) : StoreComponentDetailQueryService {

    @Suppress("LongMethod")
    override fun getComponentDetailInfoById(
        userId: String,
        storeType: StoreTypeEnum,
        storeId: String
    ): StoreDetailInfo? {
        val storeBaseRecord = storeBaseQueryDao.getComponentById(dslContext, storeId) ?: return null
        val storeCode = storeBaseRecord.storeCode
        val storeFeatureRecord = storeBaseFeatureQueryDao.getBaseFeatureByCode(
            dslContext = dslContext,
            storeCode = storeBaseRecord.storeCode,
            storeType = storeType
        ) ?: throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(storeCode))
        // 用户是否可安装组件
        val installFlag = storeUserService.isCanInstallStoreComponent(
            defaultFlag = storeFeatureRecord.publicFlag,
            userId = userId,
            storeCode = storeCode,
            storeType = storeType
        )
        val labels = storeLabelService.getLabelsByStoreId(storeBaseRecord.id)
        val userCommentInfo = storeCommentService.getStoreUserCommentInfo(userId, storeCode, storeType)
        val baseExtRecords = storeBaseExtQueryDao.getBaseExtByIds(dslContext, listOf(storeId))
        val baseFeatureExtRecords = storeBaseFeatureExtQueryDao.queryStoreBaseFeatureExt(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType
        )
        // 版本级扩展：跟随版本(如 installType、installParams、urlScheme 等)
        val versionExtData = baseExtRecords.associateBy(
            { it.fieldName }, { StoreExtFieldUtil.formatJson(it.fieldValue) }
        ).takeIf { it.isNotEmpty() }
        // 组件级共享扩展：所有版本共享(如 installPath、os、yamlFlag 等)
        val featureExtData = baseFeatureExtRecords.associateBy(
            { it.fieldName }, { StoreExtFieldUtil.formatJson(it.fieldValue) }
        ).takeIf { it.isNotEmpty() }
        // 向后兼容：保持 extData 为组件级+版本级合并，旧客户端仍可读取
        val mergedExt = mutableMapOf<String, Any>()
        featureExtData?.let { mergedExt.putAll(it) }
        versionExtData?.let { mergedExt.putAll(it) }
        val extData = mergedExt.ifEmpty { null }
        val htmlTemplateVersion = extData?.get(KEY_HTML_TEMPLATE_VERSION)
        val initProjectCode =
            if (htmlTemplateVersion != null && htmlTemplateVersion == FrontendTypeEnum.HISTORY.typeVersion) {
                ""
            } else {
                storeProjectRelDao.getInitProjectCodeByStoreCode(dslContext, storeCode, storeType.type.toByte())
            }
        val classify = classifyService.getClassify(storeBaseRecord.classifyId).data
        val versionLog =
            storeVersionLogDao.getStoreVersions(dslContext, listOf(storeId), true)?.firstOrNull()?.let {
                VersionModel(
                    publisher = storeBaseRecord.publisher,
                    releaseType = ReleaseTypeEnum.getReleaseTypeObj(it.releaseType.toInt())!!,
                    version = storeBaseRecord.version,
                    versionContent = it.content
                )
            }
        val statistic = storeTotalStatisticService.getStatisticByCode(userId, storeType.type.toByte(), storeCode)
        val newestComponentRecord = storeBaseQueryDao.getNewestComponentByCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType
        )
        val logoUrl = storeBaseRecord.logoUrl
        return StoreDetailInfo(
            storeId = storeId,
            storeCode = storeCode,
            storeType = storeType.name,
            name = storeBaseRecord.name,
            version = storeBaseRecord.version,
            status = storeBaseRecord.status,
            classify = classify,
            logoUrl = logoUrl?.let {
                StoreDecorateFactory.get(StoreDecorateFactory.Kind.HOST)?.decorate(logoUrl) as? String
            },
            versionInfo = versionLog,
            downloads = statistic.downloads,
            score = statistic.score,
            summary = storeBaseRecord.summary,
            description = storeBaseRecord.description?.let {
                StoreDecorateFactory.get(StoreDecorateFactory.Kind.HOST)?.decorate(it) as? String
            },
            testProjectCode = storeProjectRelDao.getUserStoreTestProjectCode(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeType
            ),
            initProjectCode = initProjectCode,
            categoryList = categoryService.getByRelStoreId(storeId),
            labelList = labels,
            latestFlag = storeBaseRecord.latestFlag,
            installFlag = installFlag,
            publicFlag = storeFeatureRecord.publicFlag,
            recommendFlag = storeFeatureRecord.recommendFlag,
            certificationFlag = storeFeatureRecord.certificationFlag,
            type = storeFeatureRecord.type,
            rdType = storeFeatureRecord.rdType,
            userCommentInfo = userCommentInfo,
            editFlag = StoreUtils.checkEditCondition(newestComponentRecord!!.status),
            honorInfos = storeHonorService.getStoreHonor(userId, storeType, storeCode),
            indexInfos = storeIndexManageService.getStoreIndexInfosByStoreCode(storeType, storeCode),
            extData = extData
        )
    }

    override fun getComponentDetailInfoByCode(
        userId: String,
        storeType: String,
        storeCode: String,
        version: String?,
        ownerStoreCode: String?
    ): StoreDetailInfo? {
        return getComponent(
            version = version,
            storeCode = if (storeCode == ManualTriggerElement.classType) {
                // 兼容手动触发器
                BK_STORE_CREATIVE_STREAM_MANUAL_TRIGGER
            } else {
                storeCode
            },
            storeType = storeType,
            ownerStoreCode = ownerStoreCode
        )?.let {
            getComponentDetailInfoById(userId, StoreTypeEnum.valueOf(storeType), it.id)
        }
    }

    override fun getComponentDataInfoByCode(
        storeType: String,
        storeCode: String,
        version: String?,
        ownerStoreCode: String?,
        status: StoreStatusEnum?
    ): StoreDetailInfo? {
        return getComponent(
            version = version,
            storeCode = storeCode,
            storeType = storeType,
            ownerStoreCode = ownerStoreCode,
            status = status
        )?.let {
            val finalStoreType = StoreTypeEnum.getStoreTypeObj(it.storeType.toInt())
            StoreDetailInfo(
                storeCode = it.storeCode,
                storeType = finalStoreType.name,
                storeId = it.id,
                name = it.name,
                description = it.description,
                extData = getComponentExtData(
                    storeId = it.id,
                    storeType = finalStoreType,
                    storeCode = it.storeCode
                ),
                ownerStoreCode = it.ownerStoreCode
            )
        }
    }

    private fun getComponentExtData(
        storeId: String,
        storeCode: String,
        storeType: StoreTypeEnum
    ): Map<String, Any> {
        val baseExtRecords = storeBaseExtQueryDao.getBaseExtByIds(dslContext, listOf(storeId))
        var extData = baseExtRecords.associateBy({ it.fieldName }, { StoreExtFieldUtil.formatJson(it.fieldValue) })
        val baseFeatureExtRecords = storeBaseFeatureExtQueryDao.queryStoreBaseFeatureExt(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType
        )
        extData = extData.plus(
            baseFeatureExtRecords.associateBy({ it.fieldName },
                { StoreExtFieldUtil.formatJson(it.fieldValue) })
        )
        return extData
    }

    private fun getComponent(
        version: String?,
        storeCode: String,
        storeType: String,
        ownerStoreCode: String? = null,
        status: StoreStatusEnum? = null
    ) = if (version.isNullOrBlank()) {
        storeBaseQueryDao.getLatestComponentByCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = StoreTypeEnum.valueOf(storeType),
            ownerStoreCode = ownerStoreCode,
            storeStatus = status
        )
    } else {
        storeBaseQueryDao.getComponent(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = StoreTypeEnum.valueOf(storeType),
            version = version,
            ownerStoreCode = ownerStoreCode,
            status = status
        )
    }
}
