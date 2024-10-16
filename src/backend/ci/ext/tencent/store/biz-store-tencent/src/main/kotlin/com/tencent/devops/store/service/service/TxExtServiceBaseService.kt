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

package com.tencent.devops.store.service.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.MASTER
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.service.ServiceExtServiceBuildPipelineResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.pojo.pipeline.ExtServiceBuildPipelineReq
import com.tencent.devops.process.utils.KEY_PIPELINE_NAME
import com.tencent.devops.project.api.service.service.ServiceItemResource
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.common.configuration.StoreInnerPipelineConfig
import com.tencent.devops.store.common.dao.BusinessConfigDao
import com.tencent.devops.store.common.dao.StorePipelineBuildRelDao
import com.tencent.devops.store.common.dao.StorePipelineRelDao
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.extservice.constants.KEY_EXT_SERVICE_ITEMS_PREFIX
import com.tencent.devops.store.pojo.extservice.dto.ExtServiceBaseInfoDTO
import com.tencent.devops.store.pojo.extservice.dto.ExtServiceImageInfoDTO
import com.tencent.devops.store.pojo.extservice.dto.InitExtServiceDTO
import com.tencent.devops.store.pojo.extservice.enums.ExtServicePackageSourceTypeEnum
import com.tencent.devops.store.service.configuration.ExtServiceImageSecretConfig
import com.tencent.devops.store.service.dao.ExtServiceBuildInfoDao
import java.util.Base64
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

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
    private lateinit var businessConfigDao: BusinessConfigDao

    @Autowired
    private lateinit var storePipelineBuildRelDao: StorePipelineBuildRelDao

    @Autowired
    private lateinit var extServiceImageSecretConfig: ExtServiceImageSecretConfig

    @Autowired
    private lateinit var storeInnerPipelineConfig: StoreInnerPipelineConfig

    override fun handleServicePackage(
        extensionInfo: InitExtServiceDTO,
        userId: String,
        serviceCode: String
    ): Result<Map<String, String>?> {
        logger.info("handleServicePackage params:[$extensionInfo|$serviceCode|$userId]")
        extensionInfo.authType ?: return I18nUtil.generateResponseDataObject(
            messageCode = CommonMessageCode.PARAMETER_IS_NULL,
            params = arrayOf("authType"),
            data = null,
            language = I18nUtil.getLanguage(userId)
        )
        extensionInfo.visibilityLevel ?: return I18nUtil.generateResponseDataObject(
            messageCode = CommonMessageCode.PARAMETER_IS_NULL,
            params = arrayOf("visibilityLevel"),
            data = null,
            language = I18nUtil.getLanguage(userId)
        )
        val repositoryInfo: RepositoryInfo?
        if (extensionInfo.visibilityLevel == VisibilityLevelEnum.PRIVATE) {
            if (extensionInfo.privateReason.isNullOrBlank()) {
                return I18nUtil.generateResponseDataObject(
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
                )?.sampleProjectPath,
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
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_CREATE_REPOSITORY_FAIL,
                language = I18nUtil.getLanguage(userId))
        }
        if (null == repositoryInfo) {
            return I18nUtil.generateResponseDataObject(
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
        val buildInfo = extServiceBuildInfoDao.getServiceBuildInfo(context, serviceId)
        logger.info("service[$serviceCode] buildInfo is:$buildInfo")
        val script = buildInfo.value1()
        val username = Base64.getEncoder().encodeToString(extServiceImageSecretConfig.repoUsername.toByteArray())
        val password = Base64.getEncoder().encodeToString(extServiceImageSecretConfig.repoPassword.toByteArray())
        val extServiceImageInfo = ExtServiceImageInfoDTO(
            serviceName = serviceCode,
            imageTag = version,
            repoProjectCode = extServiceImageSecretConfig.imageRepoProject,
            repoName = extServiceImageSecretConfig.imageRepoName,
            username = username,
            password = password
        )
        // 未正式发布的扩展服务先部署到bcs灰度环境
        val deployApp = extServiceBcsService.generateDeployApp(
            userId = userId,
            namespaceName = extServiceBcsNameSpaceConfig.grayNamespaceName,
            serviceCode = serviceCode,
            version = version
        )
        val pipelineName = "EXT_SERVICE_PIPELINE_BUILD_PUBLIC"
        var publicPipelineId = redisOperation.get(pipelineName)
        if (publicPipelineId.isNullOrBlank()) {
            publicPipelineId = creatServicePipeline(
                context = context,
                userId = storeInnerPipelineConfig.innerPipelineUser,
                projectCode = storeInnerPipelineConfig.innerPipelineProject,
                pipelineName = pipelineName
            )
        }
        val extServiceFeature = extFeatureDao.getServiceByCode(context, serviceCode)!!
        val serviceBaseInfo = ExtServiceBaseInfoDTO(
            serviceId = serviceId,
            serviceCode = serviceCode,
            version = serviceRecord.version,
            extServiceImageInfo = extServiceImageInfo,
            extServiceDeployInfo = deployApp,
            branch = MASTER,
            codeSrc = extServiceFeature.codeSrc,
            repositoryPath = (buildInfo.value2() ?: "")
        )
        val extServiceBuildPipelineReq = ExtServiceBuildPipelineReq(
            script = script,
            extServiceBaseInfo = serviceBaseInfo
        )
        val pipelineId = when {
            servicePipelineRelRecord == null -> publicPipelineId
            servicePipelineRelRecord.pipelineId != publicPipelineId &&
                    servicePipelineRelRecord.projectCode == storeInnerPipelineConfig.innerPipelineProject ->
                servicePipelineRelRecord.pipelineId
            else -> publicPipelineId
        }

        val serviceMarketPipelineResp =
            client.get(ServiceExtServiceBuildPipelineResource::class).extServiceBuildPipeline(
                userId = storeInnerPipelineConfig.innerPipelineUser,
                projectCode = storeInnerPipelineConfig.innerPipelineProject,
                pipelineId = pipelineId,
                extServiceBuildPipelineReq = extServiceBuildPipelineReq
            ).data
        logger.info("the serviceMarketPipelineResp is:$serviceMarketPipelineResp")
        if (null != serviceMarketPipelineResp) {
            if (servicePipelineRelRecord == null) {
                storePipelineRelDao.add(
                    dslContext = context,
                    storeCode = serviceCode,
                    storeType = StoreTypeEnum.ATOM,
                    pipelineId = pipelineId,
                    projectCode = storeInnerPipelineConfig.innerPipelineProject
                )
            } else if (servicePipelineRelRecord.pipelineId != pipelineId) {
                storePipelineRelDao.updateStorePipelineProject(
                    dslContext = context,
                    storeCode = serviceCode,
                    storeType = StoreTypeEnum.SERVICE,
                    projectCode = storeInnerPipelineConfig.innerPipelineProject,
                    pipelineId = pipelineId
                )
            }
            extServiceDao.setServiceStatusById(
                dslContext = context,
                serviceId = serviceId,
                serviceStatus = serviceMarketPipelineResp.extServiceStatus.status.toByte(),
                userId = userId,
                msg = null
            )
            val buildId = serviceMarketPipelineResp.buildId
            if (null != buildId) {
                storePipelineBuildRelDao.add(context, serviceId, serviceMarketPipelineResp.pipelineId, buildId)
            }
        }
        return true
    }

    fun creatServicePipeline(
        context: DSLContext,
        userId: String,
        projectCode: String,
        pipelineName: String
    ): String {
        var pipelineId: String?
        val lock = RedisLock(redisOperation, "creat-service-pipeline-$projectCode", 60L)
        try {
            lock.lock()
            pipelineId = redisOperation.get(pipelineName)
            if (!pipelineId.isNullOrBlank()) {
                return pipelineId
            }
            val pipelineModelConfig = businessConfigDao.get(
                dslContext = context,
                business = StoreTypeEnum.SERVICE.name,
                feature = "initBuildPipeline",
                businessValue = "PIPELINE_MODEL"
            )
            val pipelineModel =
                pipelineModelConfig!!.configValue.replace("#{$KEY_PIPELINE_NAME}", pipelineName)
            val model = JsonUtil.to(pipelineModel, Model::class.java)
            pipelineId = client.get(ServicePipelineResource::class).create(
                userId = userId,
                projectId = projectCode,
                pipeline = model,
                channelCode = ChannelCode.AM
            ).data!!.id
            redisOperation.set(
                key = pipelineName,
                value = pipelineId,
                expired = false
            )
            return pipelineId
        } finally {
            lock.unlock()
        }
    }
}
