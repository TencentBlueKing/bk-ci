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

package com.tencent.devops.store.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServiceExtServiceBuildPipelineInitResource
import com.tencent.devops.process.pojo.pipeline.ExtServiceBuildInitPipelineReq
import com.tencent.devops.project.api.service.service.ServiceItemResource
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.config.ExtServiceImageSecretConfig
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.ExtServiceBuildAppRelDao
import com.tencent.devops.store.dao.ExtServiceBuildInfoDao
import com.tencent.devops.store.dao.common.StorePipelineBuildRelDao
import com.tencent.devops.store.dao.common.StorePipelineRelDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.constants.KEY_EXT_SERVICE_ITEMS_PREFIX
import com.tencent.devops.store.pojo.dto.ExtServiceBaseInfoDTO
import com.tencent.devops.store.pojo.dto.ExtServiceImageInfoDTO
import com.tencent.devops.store.pojo.dto.InitExtServiceDTO
import com.tencent.devops.store.pojo.enums.ExtServicePackageSourceTypeEnum
import com.tencent.devops.store.pojo.enums.ExtServiceStatusEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class TxExtServiceBaseService : ExtServiceBaseService() {

    private val logger = LoggerFactory.getLogger(TxExtServiceBaseService::class.java)

    @Value("\${git.service.nameSpaceId}")
    private lateinit var serviceNameSpaceId: String

    @Autowired
    private lateinit var storePipelineRelDao: StorePipelineRelDao

    @Autowired
    private lateinit var extServiceBuildInfoDao: ExtServiceBuildInfoDao

    @Autowired
    private lateinit var extServiceBuildAppRelDao: ExtServiceBuildAppRelDao

    @Autowired
    private lateinit var storePipelineBuildRelDao: StorePipelineBuildRelDao

    @Autowired
    private lateinit var extServiceImageSecretConfig: ExtServiceImageSecretConfig

    override fun handleServicePackage(
        extensionInfo: InitExtServiceDTO,
        userId: String,
        serviceCode: String
    ): Result<Map<String, String>?> {
        logger.info("handleServicePackage params:[$extensionInfo|$serviceCode|$userId]")
        extensionInfo.authType ?: return MessageUtil.generateResponseDataObject(
            messageCode = CommonMessageCode.PARAMETER_IS_NULL,
            params = arrayOf("authType"),
            data = null,
            language = I18nUtil.getLanguage(userId)
        )
        extensionInfo.visibilityLevel ?: return MessageUtil.generateResponseDataObject(
            messageCode = CommonMessageCode.PARAMETER_IS_NULL,
            params = arrayOf("visibilityLevel"),
            data = null,
            language = I18nUtil.getLanguage(userId)
        )
        val repositoryInfo: RepositoryInfo?
        if (extensionInfo.visibilityLevel == VisibilityLevelEnum.PRIVATE) {
            if (extensionInfo.privateReason.isNullOrBlank()) {
                return MessageUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf("privateReason"),
                    data = null,
                    language = I18nUtil.getLanguage(userId)
                )
            }
        }
        // 把扩展服务对应的扩展点放入redis中（初始化扩展服务代码库的时候需将extension.json改成用户对应的模板）
        val itemInfoList = client.get(ServiceItemResource::class).getItemInfoByIds(extensionInfo.extensionItemList).data
        val itemCodeSet = mutableSetOf<String>()
        itemInfoList?.forEach {
            itemCodeSet.add(it.itemCode)
        }
        redisOperation.set(
            key = "$KEY_EXT_SERVICE_ITEMS_PREFIX:$serviceCode",
            value = JsonUtil.toJson(itemCodeSet),
            expiredInSecond = TimeUnit.DAYS.toSeconds(1)
        )
        // 远程调工蜂接口创建代码库
        try {
            val createGitRepositoryResult = client.get(ServiceGitRepositoryResource::class).createGitCodeRepository(
                userId = userId,
                projectCode = extensionInfo.projectCode,
                repositoryName = serviceCode,
                sampleProjectPath = storeBuildInfoDao.getStoreBuildInfoByLanguage(
                    dslContext,
                    extensionInfo.language!!,
                    StoreTypeEnum.SERVICE
                ).sampleProjectPath,
                namespaceId = serviceNameSpaceId.toInt(),
                visibilityLevel = extensionInfo.visibilityLevel,
                tokenType = TokenTypeEnum.PRIVATE_KEY
            )
            logger.info("the createGitRepositoryResult is :$createGitRepositoryResult")
            if (createGitRepositoryResult.isOk()) {
                repositoryInfo = createGitRepositoryResult.data
            } else {
                return Result(createGitRepositoryResult.status, createGitRepositoryResult.message, null)
            }
        } catch (ignored: Throwable) {
            logger.error("service[$serviceCode] createGitCodeRepository fail!", ignored)
            return MessageUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_CREATE_REPOSITORY_FAIL
            ,language = I18nUtil.getLanguage(userId))
        }
        if (null == repositoryInfo) {
            return MessageUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_CREATE_REPOSITORY_FAIL,
                language = I18nUtil.getLanguage(userId))
        }
        return Result(mapOf("repositoryHashId" to repositoryInfo.repositoryHashId!!, "codeSrc" to repositoryInfo.url))
    }

    override fun getExtServicePackageSourceType(serviceCode: String): ExtServicePackageSourceTypeEnum {
        // 内部版暂时只支持代码库打包的方式，后续支持用户传可执行包的方式
        return ExtServicePackageSourceTypeEnum.REPO
    }

    override fun getRepositoryInfo(projectCode: String?, repositoryHashId: String?): Result<Repository?> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun asyncHandleUpdateService(context: DSLContext, serviceId: String, userId: String) {
        runPipeline(context, serviceId, userId)
    }

    private fun runPipeline(context: DSLContext, serviceId: String, userId: String): Boolean {
        val serviceRecord = extServiceDao.getServiceById(context, serviceId) ?: return false
        val serviceCode = serviceRecord.serviceCode
        val version = serviceRecord.version
        val servicePipelineRelRecord = storePipelineRelDao.getStorePipelineRel(
            dslContext = context,
            storeCode = serviceCode,
            storeType = StoreTypeEnum.SERVICE
        )
        val projectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
            dslContext = context,
            storeCode = serviceCode,
            storeType = StoreTypeEnum.SERVICE.type.toByte()
        ) // 查找新增扩展服务时关联的项目
        val buildInfo = extServiceBuildInfoDao.getServiceBuildInfo(context, serviceId)
        logger.info("service[$serviceCode] buildInfo is:$buildInfo")
        val script = buildInfo.value1()
        val repoAddr = extServiceImageSecretConfig.repoRegistryUrl
        val imageName = "${extServiceImageSecretConfig.imageNamePrefix}$serviceCode"
        val extServiceImageInfo = ExtServiceImageInfoDTO(
            imageName = imageName,
            imageTag = version,
            repoAddr = repoAddr,
            username = extServiceImageSecretConfig.repoUsername,
            password = extServiceImageSecretConfig.repoPassword
        )
        // 未正式发布的扩展服务先部署到bcs灰度环境
        val deployApp = extServiceBcsService.generateDeployApp(
            userId = userId,
            namespaceName = extServiceBcsNameSpaceConfig.grayNamespaceName,
            serviceCode = serviceCode,
            version = version
        )
        if (null == servicePipelineRelRecord) {
            // 为用户初始化构建流水线并触发执行
            val serviceBaseInfo = ExtServiceBaseInfoDTO(
                serviceId = serviceId,
                serviceCode = serviceCode,
                version = serviceRecord.version,
                extServiceImageInfo = extServiceImageInfo,
                extServiceDeployInfo = deployApp
            )
            val serviceBuildAppInfoRecords = extServiceBuildAppRelDao.getExtServiceBuildAppInfo(context, serviceId)
            val buildEnv = mutableMapOf<String, String>()
            serviceBuildAppInfoRecords?.forEach {
                buildEnv[it["appName"] as String] = it["appVersion"] as String
            }
            val extServiceFeature = extFeatureDao.getServiceByCode(context, serviceCode)!!
            val extServiceBuildInitPipelineReq = ExtServiceBuildInitPipelineReq(
                repositoryHashId = extServiceFeature.repositoryHashId,
                repositoryPath = buildInfo.value2(),
                script = script,
                extServiceBaseInfo = serviceBaseInfo,
                buildEnv = buildEnv
            )
            val serviceMarketInitPipelineResp = client.get(ServiceExtServiceBuildPipelineInitResource::class)
                .initExtServiceBuildPipeline(userId, projectCode!!, extServiceBuildInitPipelineReq).data
            logger.info("the serviceMarketInitPipelineResp is:$serviceMarketInitPipelineResp")
            if (null != serviceMarketInitPipelineResp) {
                storePipelineRelDao.add(
                    dslContext = context,
                    storeCode = serviceCode,
                    storeType = StoreTypeEnum.SERVICE,
                    pipelineId = serviceMarketInitPipelineResp.pipelineId
                )
                extServiceDao.setServiceStatusById(
                    dslContext = context,
                    serviceId = serviceId,
                    serviceStatus = serviceMarketInitPipelineResp.extServiceStatus.status.toByte(),
                    userId = userId,
                    msg = null
                )
                val buildId = serviceMarketInitPipelineResp.buildId
                if (null != buildId) {
                    storePipelineBuildRelDao.add(context, serviceId, serviceMarketInitPipelineResp.pipelineId, buildId)
                }
            }
        } else {
            // 触发执行流水线
            val startParams = mutableMapOf<String, String>() // 启动参数
            startParams["serviceCode"] = serviceCode
            startParams["version"] = serviceRecord.version
            startParams["extServiceImageInfo"] = JsonUtil.toJson(extServiceImageInfo)
            startParams["extServiceDeployInfo"] = JsonUtil.toJson(deployApp)
            startParams["script"] = script
            val buildIdObj = client.get(ServiceBuildResource::class).manualStartup(
                userId, projectCode!!, servicePipelineRelRecord.pipelineId, startParams,
                ChannelCode.AM
            ).data
            logger.info("the buildIdObj is:$buildIdObj")
            if (null != buildIdObj) {
                storePipelineBuildRelDao.add(context, serviceId, servicePipelineRelRecord.pipelineId, buildIdObj.id)
                extServiceDao.setServiceStatusById(
                    dslContext = context,
                    serviceId = serviceId,
                    serviceStatus = ExtServiceStatusEnum.BUILDING.status.toByte(),
                    userId = userId,
                    msg = null
                ) // 构建中
            } else {
                extServiceDao.setServiceStatusById(
                    dslContext = context,
                    serviceId = serviceId,
                    serviceStatus = ExtServiceStatusEnum.BUILD_FAIL.status.toByte(),
                    userId = userId,
                    msg = null
                ) // 构建失败
            }
        }
        return true
    }
}
