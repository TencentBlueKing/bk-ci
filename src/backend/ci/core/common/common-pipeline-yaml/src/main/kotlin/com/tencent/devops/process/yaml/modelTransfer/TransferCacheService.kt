package com.tencent.devops.process.yaml.modelTransfer

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.process.api.service.ServicePipelineGroupResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.pojo.classify.PipelineGroup
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.api.image.service.ServiceStoreImageResource
import com.tencent.devops.store.pojo.atom.ElementThirdPartySearchParam
import com.tencent.devops.store.pojo.image.response.ImageRepoInfo
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
        .expireAfterWrite(1, TimeUnit.DAYS)
        .build<String, Map<String, String>> { key ->
            kotlin.runCatching {
                val (atomCode, version) = key.split("@")
                client.get(ServiceMarketAtomResource::class)
                    .getAtomsDefaultValue(ElementThirdPartySearchParam(atomCode, version)).data
            }.onFailure { logger.warn("get $key default value error.") }.getOrNull() ?: emptyMap()
        }
    private val storeImageInfoCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(1, TimeUnit.DAYS)
        .build<String, ImageRepoInfo?> { key ->
            kotlin.runCatching {
                val (imageCode, imageVersion) = key.split("@@")
                client.get(ServiceStoreImageResource::class)
                    .getImageInfoByCodeAndVersion(
                        imageCode = imageCode,
                        imageVersion = imageVersion
                    ).data
            }.onFailure { logger.warn("get $key ImageInfoByCodeAndVersion value error.") }.getOrNull()
        }
    private val projectGroupAndUsersCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, List<BkAuthGroupAndUserList>?> { key ->
            kotlin.runCatching {
                client.get(ServiceProjectAuthResource::class)
                    .getProjectGroupAndUserList(
                        token = tokenService.getSystemToken(null)!!,
                        projectCode = key
                    ).data
            }.onFailure { logger.warn("get $key ProjectGroupAndUserList error.") }.getOrNull()
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
            }.onFailure { logger.warn("get $key pipeline label value error.") }.getOrNull()
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
            }.onFailure { logger.warn("get $key remote token value error.") }.getOrNull()
        }

    fun getAtomDefaultValue(key: String) = atomDefaultValueCache.get(key) ?: emptyMap()

    fun getStoreImageInfo(imageCode: String, imageVersion: String?) =
        storeImageInfoCache.get("$imageCode@@${imageVersion ?: ""}")

    fun getProjectGroupAndUsers(projectId: String) = projectGroupAndUsersCache.get(projectId)

    fun getPipelineLabel(userId: String, projectId: String) = pipelineLabel.get("$userId@@$projectId")

    fun getGitRepository(projectId: String, repositoryType: RepositoryType, value: String) =
        gitRepository.get("$projectId@@${repositoryType.name}@@$value")

    fun getPipelineRemoteToken(userId: String, projectId: String, pipelineId: String) =
        pipelineRemoteToken.get("$userId@@$projectId@@$pipelineId")
}
