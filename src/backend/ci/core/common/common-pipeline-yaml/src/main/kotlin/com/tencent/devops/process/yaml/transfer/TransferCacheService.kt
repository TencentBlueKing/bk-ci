package com.tencent.devops.process.yaml.transfer

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.dispatch.docker.api.service.ServiceDockerResourceConfigResource
import com.tencent.devops.dispatch.docker.pojo.resource.UserDockerResourceOptionsVO
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.thirdpartyagent.ServiceThirdPartyAgentResource
import com.tencent.devops.process.api.service.ServicePipelineGroupResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.pojo.classify.PipelineGroup
import com.tencent.devops.process.pojo.classify.PipelineLabel
import com.tencent.devops.process.yaml.v3.models.job.JobRunsOnPoolType
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.api.image.ServiceStoreImageResource
import com.tencent.devops.store.pojo.atom.ElementThirdPartySearchParam
import com.tencent.devops.store.pojo.image.response.ImageDetail
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class TransferCacheService @Autowired constructor(
    private val client: Client,
    private val tokenService: ClientTokenService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TransferCacheService::class.java)
    }

    private val atomDefaultValueCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, JSONObject> { key ->
            kotlin.runCatching {
                val (atomCode, version) = key.split("@")
                val res = client.get(ServiceMarketAtomResource::class)
                    .getAtomsDefaultValue(ElementThirdPartySearchParam(atomCode, version)).data ?: emptyMap()
                JSONObject(res)
            }.onFailure { logger.warn("get $key default value error.", it) }.getOrNull() ?: JSONObject()
        }
    private val storeImageInfoCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, ImageDetail?> { key ->
            kotlin.runCatching {
                val (userId, imageCode, imageVersion) = key.split("@@")
                client.get(ServiceStoreImageResource::class)
                    .getImagesByCodeAndVersion(
                        userId = userId,
                        imageCode = imageCode,
                        version = imageVersion
                    ).data
            }.onFailure { logger.warn("get $key ImageInfoByCodeAndVersion value error.", it) }.getOrNull()
        }
    private val projectGroupAndUsersCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, List<BkAuthGroupAndUserList>?> { key ->
            kotlin.runCatching {
                client.get(ServiceProjectAuthResource::class)
                    .getProjectGroupAndUserList(
                        token = tokenService.getSystemToken(),
                        projectCode = key
                    ).data
            }.onFailure { logger.warn("get $key ProjectGroupAndUserList error.", it) }.getOrNull()
        }
    private val pipelineLabel = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, List<PipelineGroup>?> { key ->
            kotlin.runCatching {
                val (userId, projectId) = key.split("@@")
                client.get(ServicePipelineGroupResource::class)
                    .getGroups(userId, projectId)
                    .data
            }.onFailure { logger.warn("get $key pipeline label value error.", it) }.getOrNull()
        }
    private val pipelineLabelIdCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, PipelineLabel?> { key ->
            runCatching {
                val (projectId, labelId) = key.split("@@")
                client.get(ServicePipelineGroupResource::class)
                    .getLabel(projectId, labelId)
                    .data
            }.onFailure { logger.warn("get $key pipeline label id value error.", it) }.getOrNull()
        }
    private val gitRepository = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, Repository?> { key ->
            kotlin.runCatching {
                val (projectId, type, value) = key.split("@@")
                val repositoryType = RepositoryType.valueOf(type)
                val config = RepositoryConfig(
                    if (repositoryType == RepositoryType.ID) value else null,
                    if (repositoryType == RepositoryType.NAME) value else null,
                    RepositoryType.valueOf(type)
                )
                client.get(ServiceRepositoryResource::class)
                    .get(projectId, config.getURLEncodeRepositoryId(), config.repositoryType)
                    .data
            }.onFailure { logger.warn("get $key git repository error.") }.getOrNull()
        }
    private val pipelineRemoteToken = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, String?> { key ->
            kotlin.runCatching {
                val (userId, projectId, pipelineId) = key.split("@@")
                client.get(ServicePipelineResource::class)
                    .generateRemoteToken(userId, projectId, pipelineId)
                    .data?.token
            }.onFailure { logger.warn("get $key remote token value error.", it) }.getOrNull()
        }
    private val thirdPartyAgent = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, String?> { key ->
            kotlin.runCatching {
                val (poolType, userId, projectId, value) = key.split("@@")
                when (poolType) {
                    JobRunsOnPoolType.ENV_ID.name -> {
                        client.get(ServiceEnvironmentResource::class)
                            .get(userId, projectId, value)
                            .data?.name
                    }

                    JobRunsOnPoolType.AGENT_ID.name -> {
                        client.get(ServiceThirdPartyAgentResource::class)
                            .getAgentDetail(userId, projectId, value)
                            .data?.displayName
                    }

                    else -> null
                }
            }.onFailure { logger.warn("get $key thirdPartyAgent value error.", it) }.getOrNull()
        }

    private val dockerResource = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, UserDockerResourceOptionsVO?> { key ->
            kotlin.runCatching {
                val (userId, projectId, buildType) = key.split("@@")
                client.get(ServiceDockerResourceConfigResource::class)
                    .getDockerResourceConfigList(userId, projectId, buildType)
                    .data
            }.onFailure { logger.warn("get $key dockerResource value error.", it) }.getOrNull()
        }

    fun getAtomDefaultValue(key: String) = atomDefaultValueCache.get(key) ?: JSONObject()

    fun getStoreImageDetail(userId: String, imageCode: String, imageVersion: String?) =
        storeImageInfoCache.get("$userId@@$imageCode@@${imageVersion ?: ""}")

    fun getProjectGroupAndUsers(projectId: String) = projectGroupAndUsersCache.get(projectId)

    fun getPipelineLabel(userId: String, projectId: String) = pipelineLabel.get("$userId@@$projectId")

    fun getPipelineLabelById(projectId: String, labelId: String): PipelineLabel? =
        pipelineLabelIdCache.get("$projectId@@$labelId")

    fun getGitRepository(projectId: String, repositoryType: RepositoryType, value: String) =
        gitRepository.get("$projectId@@${repositoryType.name}@@$value")

    fun getPipelineRemoteToken(userId: String, projectId: String, pipelineId: String) =
        pipelineRemoteToken.get("$userId@@$projectId@@$pipelineId")

    fun getThirdPartyAgent(poolType: JobRunsOnPoolType, userId: String, projectId: String, value: String?): String? {
        if (value == null) return null
        return thirdPartyAgent.get("${poolType.name}@@$userId@@$projectId@@$value")
    }

    fun getDockerResource(userId: String, projectId: String, buildType: BuildType) =
        dockerResource.get("$userId@@$projectId@@${buildType.name}")
}
