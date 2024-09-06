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
package com.tencent.devops.store.devx.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.KEY_FILE_SHA_CONTENT
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.model.store.tables.records.TStoreBaseEnvRecord
import com.tencent.devops.process.api.service.ServiceVarResource
import com.tencent.devops.store.common.configuration.StoreInnerPipelineConfig
import com.tencent.devops.store.common.dao.StoreBaseEnvExtManageDao
import com.tencent.devops.store.common.dao.StoreBaseEnvManageDao
import com.tencent.devops.store.common.dao.StoreBaseEnvQueryDao
import com.tencent.devops.store.common.dao.StoreBaseManageDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.service.AbstractStoreHandleBuildResultService
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.publication.StoreBaseEnvDataPO
import com.tencent.devops.store.pojo.common.publication.StoreBaseEnvExtDataPO
import com.tencent.devops.store.pojo.common.publication.StoreBuildResultRequest
import com.tencent.devops.store.pojo.common.publication.UpdateStoreBaseDataPO
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("DEVX_HANDLE_BUILD_RESULT")
@Suppress("LongParameterList")
class DevxHandleBuildResultService @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseManageDao: StoreBaseManageDao,
    private val storeBaseEnvQueryDao: StoreBaseEnvQueryDao,
    private val storeBaseEnvManageDao: StoreBaseEnvManageDao,
    private val storeBaseEnvExtManageDao: StoreBaseEnvExtManageDao,
    private val client: Client,
    private val storeInnerPipelineConfig: StoreInnerPipelineConfig
) : AbstractStoreHandleBuildResultService() {

    private val logger = LoggerFactory.getLogger(DevxHandleBuildResultService::class.java)

    override fun handleStoreBuildResult(
        pipelineId: String,
        buildId: String,
        storeBuildResultRequest: StoreBuildResultRequest
    ): Result<Boolean> {
        logger.info("handleStoreBuildResult storeBuildResultRequest is:$storeBuildResultRequest")
        val storeId = storeBuildResultRequest.storeId
        val baseRecord = storeBaseQueryDao.getComponentById(dslContext, storeId)
            ?: throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(storeId))
        // 防止重复的mq消息造成的状态异常
        if (baseRecord.status != StoreStatusEnum.BUILDING.name) {
            return Result(true)
        }
        var status = StoreStatusEnum.TESTING
        if (BuildStatus.SUCCEED != storeBuildResultRequest.buildStatus) {
            status = StoreStatusEnum.BUILD_FAIL
        }
        val baseEnvRecords = storeBaseEnvQueryDao.getBaseEnvsByStoreId(
            dslContext = dslContext,
            storeId = storeId
        )
        val keys = mutableSetOf<String>()
        val baseEnvMap = mutableMapOf<String, TStoreBaseEnvRecord>()
        baseEnvRecords?.forEach { baseEnvRecord ->
            val osName = baseEnvRecord.osName
            val osArch = baseEnvRecord.osArch ?: ""
            keys.add("${osName}_${osArch}_signResult")
            baseEnvMap["${osName}_${osArch}"] = baseEnvRecord
        }
        // 批量获取key在构建变量表的值
        val varMap = if (keys.isNotEmpty()) {
            client.get(ServiceVarResource::class).getBuildVars(
                projectId = storeInnerPipelineConfig.innerPipelineProject,
                pipelineId = pipelineId,
                buildId = buildId,
                keys = keys
            ).data
        } else {
            null
        }
        val userId = storeBuildResultRequest.userId
        // 处理环境信息业务逻辑
        val storeBaseEnvDataPOs: MutableList<StoreBaseEnvDataPO> = mutableListOf()
        val storeBaseEnvExtDataPOs: MutableList<StoreBaseEnvExtDataPO> = mutableListOf()
        handleStoreBaseEnvBus(
            userId = userId,
            varMap = varMap,
            baseEnvMap = baseEnvMap,
            storeBaseEnvDataPOs = storeBaseEnvDataPOs,
            storeBaseEnvExtDataPOs = storeBaseEnvExtDataPOs
        )
        dslContext.transaction { t ->
            val context = DSL.using(t)
            storeBaseManageDao.updateStoreBaseInfo(
                dslContext = context,
                updateStoreBaseDataPO = UpdateStoreBaseDataPO(
                    id = storeId,
                    status = status,
                    modifier = userId
                )
            )
            if (storeBaseEnvDataPOs.isNotEmpty()) {
                storeBaseEnvManageDao.batchSave(context, storeBaseEnvDataPOs)
            }
            if (storeBaseEnvExtDataPOs.isNotEmpty()) {
                storeBaseEnvExtManageDao.batchSave(context, storeBaseEnvExtDataPOs)
            }
        }
        return Result(true)
    }

    private fun handleStoreBaseEnvBus(
        userId: String,
        varMap: Map<String, String>?,
        baseEnvMap: MutableMap<String, TStoreBaseEnvRecord>,
        storeBaseEnvDataPOs: MutableList<StoreBaseEnvDataPO>,
        storeBaseEnvExtDataPOs: MutableList<StoreBaseEnvExtDataPO>
    ) {
        varMap?.forEach { (key, value) ->
            val filedNames = key.split("_")
            // 获取操作系统名称
            val osName = filedNames[0]
            val osArch = filedNames[1]
            val signMap = JsonUtil.toMap(value)
            val baseEnvRecord = baseEnvMap["${osName}_${osArch}"]
            baseEnvRecord?.let {
                doStoreSignBus(
                    signMap = signMap,
                    storeBaseEnvDataPOs = storeBaseEnvDataPOs,
                    baseEnvRecord = baseEnvRecord,
                    userId = userId,
                    storeBaseEnvExtDataPOs = storeBaseEnvExtDataPOs
                )
            }
        }
    }

    private fun doStoreSignBus(
        signMap: Map<String, Any>,
        storeBaseEnvDataPOs: MutableList<StoreBaseEnvDataPO>,
        baseEnvRecord: TStoreBaseEnvRecord,
        userId: String,
        storeBaseEnvExtDataPOs: MutableList<StoreBaseEnvExtDataPO>
    ) {
        val pkgFileName = baseEnvRecord.pkgName
        signMap.forEach { (fileName, shaContent) ->
            // 判断文件名是否是软件包文件名称
            if (fileName == pkgFileName) {
                storeBaseEnvDataPOs.add(
                    StoreBaseEnvDataPO(
                        id = baseEnvRecord.id,
                        storeId = baseEnvRecord.storeId,
                        shaContent = shaContent.toString(),
                        creator = baseEnvRecord.creator,
                        modifier = userId,
                        createTime = baseEnvRecord.createTime
                    )
                )
            } else {
                storeBaseEnvExtDataPOs.add(
                    StoreBaseEnvExtDataPO(
                        id = UUIDUtil.generate(),
                        envId = baseEnvRecord.id,
                        storeId = baseEnvRecord.storeId,
                        fieldName = "${KEY_FILE_SHA_CONTENT}_${fileName.substringAfterLast("/")}",
                        fieldValue = shaContent.toString(),
                        creator = userId,
                        modifier = userId
                    )
                )
            }
        }
    }
}
