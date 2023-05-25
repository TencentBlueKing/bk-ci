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

package com.tencent.devops.process.template.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.type.StoreDispatchType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.pojo.template.TemplateDetailInfo
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.store.api.image.service.ServiceStoreImageResource
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils

@Suppress("ALL")
@Service
class PipelineTemplateService @Autowired constructor(
    private val dslContext: DSLContext,
    private val templateDao: TemplateDao,
    private val client: Client
) {

    fun getTemplateDetailInfo(templateCode: String): Result<TemplateDetailInfo?> {
        logger.info("getTemplateDetailInfo templateCode is:$templateCode")
        var templateRecord = templateDao.getLatestTemplate(dslContext, templateCode)
        if (templateRecord.srcTemplateId != null && templateRecord.type == TemplateType.CONSTRAINT.name) {
            templateRecord = templateDao.getLatestTemplate(dslContext, templateRecord.srcTemplateId)
        }
        return Result(
            TemplateDetailInfo(
                templateCode = templateRecord.id,
                templateName = templateRecord.templateName,
                templateModel = if (!StringUtils.isEmpty(templateRecord.template)) JsonUtil.to(
                    templateRecord.template,
                    Model::class.java
                ) else null
            )
        )
    }

    fun checkImageReleaseStatus(userId: String, templateCode: String): Result<String?> {
        logger.info("start checkImageReleaseStatus templateCode is:$templateCode")
        val templateModel = getTemplateDetailInfo(templateCode).data?.templateModel
            ?: return I18nUtil.generateResponseDataObject(
                CommonMessageCode.SYSTEM_ERROR,
                language = I18nUtil.getLanguage(userId)
            )
        var code: String? = null
        val images = mutableSetOf<String>()
        run releaseStatus@{
            templateModel.stages.forEach { stage ->
                stage.containers.forEach imageInfo@{ container ->
                    if (container is VMBuildContainer && container.dispatchType is StoreDispatchType) {
                        val imageCode = (container.dispatchType as StoreDispatchType).imageCode
                        val imageVersion = (container.dispatchType as StoreDispatchType).imageVersion
                        val image = imageCode + imageVersion
                        if (imageCode.isNullOrBlank() || imageVersion.isNullOrBlank()) {
                            return@imageInfo
                        } else {
                            if (images.contains(image)) {
                                return@imageInfo
                            } else {
                                images.add(image)
                            }
                            if (!isRelease(imageCode, imageVersion)) {
                                code = imageCode
                            }
                            return@releaseStatus
                        }
                    } else {
                        return@imageInfo
                    }
                }
            } }
        return Result(code)
    }

    private fun isRelease(imageCode: String, imageVersion: String): Boolean {
        val imageStatus = client.get(ServiceStoreImageResource::class)
            .getImageStatusByCodeAndVersion(imageCode, imageVersion).data
        return ImageStatusEnum.RELEASED.name == imageStatus
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateService::class.java)
    }
}
