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

package com.tencent.devops.store.service.atom.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.store.dao.common.StoreIndexManageInfoDao
import com.tencent.devops.store.dao.common.StorePipelineRelDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.common.KEY_INDEX_CODE
import com.tencent.devops.store.pojo.common.enums.IndexExecuteTimeTypeEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StorePipelineBusTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.AtomIndexTriggerCalService
import com.tencent.devops.store.utils.VersionUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class AtomIndexTriggerCalServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val storeIndexManageInfoDao: StoreIndexManageInfoDao,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val storePipelineRelDao: StorePipelineRelDao
) : AtomIndexTriggerCalService {

    private val logger = LoggerFactory.getLogger(AtomIndexTriggerCalServiceImpl::class.java)

    private val executors = Executors.newFixedThreadPool(3)
    override fun upgradeTriggerCalculate(
        userId: String,
        atomCode: String,
        version: String,
        releaseType: ReleaseTypeEnum
    ): Boolean {
        logger.info("upgradeTriggerCalculate params:[$userId|$atomCode|$version|$releaseType]")
        // 异步执行计算指标任务逻辑
        executors.submit {
            doCalculateBus(
                atomCode = atomCode,
                releaseType = releaseType,
                userId = userId,
                version = version
            )
        }
        return true
    }

    private fun doCalculateBus(
        atomCode: String,
        releaseType: ReleaseTypeEnum,
        userId: String,
        version: String
    ) {
        // 查询该插件关联的指标数据
        val indexCodes = storeIndexManageInfoDao.getIndexCodesByAtomCode(
            dslContext = dslContext,
            storeType = StoreTypeEnum.ATOM,
            atomCode = atomCode,
            executeTimeType = IndexExecuteTimeTypeEnum.COMPONENT_UPGRADE
        )
        if (indexCodes.isEmpty()) {
            logger.info("atom[$atomCode] no need to calculate index")
            return
        }
        val initProjectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
            dslContext = dslContext,
            storeCode = atomCode,
            storeType = StoreTypeEnum.ATOM.type.toByte()
        )
        val pipelineId = storePipelineRelDao.getStorePipelineRel(
            dslContext = dslContext,
            storeCode = atomCode,
            storeType = StoreTypeEnum.ATOM,
            busType = StorePipelineBusTypeEnum.INDEX
        )?.id
        if (initProjectCode == null || pipelineId == null) {
            throw ErrorCodeException(errorCode = CommonMessageCode.SYSTEM_ERROR)
        }
        // 如果是非兼容性升级需要修改流水线插件的版本号
        if (releaseType == ReleaseTypeEnum.INCOMPATIBILITY_UPGRADE) {
            // 获取插件对应的指标计算流水线模型
            val model = client.get(ServicePipelineResource::class).get(
                userId = userId,
                projectId = initProjectCode,
                pipelineId = pipelineId,
                channelCode = ChannelCode.AM
            ).data ?: throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_CLIENT_REST_ERROR)
            // 修改插件的版本号
            updateModelVersion(model, atomCode, version)
            // 修改插件对应的指标计算流水线模型
            client.get(ServicePipelineResource::class).edit(
                userId = userId,
                projectId = initProjectCode,
                pipelineId = pipelineId,
                pipeline = model,
                channelCode = ChannelCode.AM
            )
        }
        // 启动指标计算流水线
        indexCodes.forEach { indexCode ->
            client.get(ServiceBuildResource::class).manualStartupNew(
                userId = userId,
                projectId = initProjectCode,
                pipelineId = pipelineId,
                values = mapOf(KEY_INDEX_CODE to indexCode),
                channelCode = ChannelCode.AM,
                startType = StartType.SERVICE
            )
        }
    }

    private fun updateModelVersion(model: Model, atomCode: String, version: String) {
        model.stages.forEach { stage ->
            stage.containers.forEach { container ->
                container.elements.forEach { element ->
                    setAtomVersion(element, atomCode, version)
                }
            }
        }
    }

    private fun setAtomVersion(element: Element, atomCode: String, version: String) {
        if (element.getAtomCode() == atomCode) {
            element.version = VersionUtils.convertLatestVersion(version)
        }
    }
}
