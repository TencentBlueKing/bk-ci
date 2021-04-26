/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.repository.service.repo.impl

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.api.DefaultArtifactInfo
import com.tencent.bkrepo.common.artifact.constant.PRIVATE_PROXY_REPO_NAME
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode.REPOSITORY_NOT_FOUND
import com.tencent.bkrepo.common.artifact.path.PathUtils.ROOT
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.configuration.RepositoryConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.composite.CompositeConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.composite.ProxyChannelSetting
import com.tencent.bkrepo.common.artifact.pojo.configuration.local.LocalConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.virtual.VirtualConfiguration
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.service.util.SpringContextUtils.Companion.publishEvent
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.repository.config.RepositoryProperties
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import com.tencent.bkrepo.repository.dao.RepositoryDao
import com.tencent.bkrepo.repository.listener.event.repo.RepoCreatedEvent
import com.tencent.bkrepo.repository.listener.event.repo.RepoDeletedEvent
import com.tencent.bkrepo.repository.listener.event.repo.RepoUpdatedEvent
import com.tencent.bkrepo.repository.model.TRepository
import com.tencent.bkrepo.repository.pojo.project.RepoRangeQueryRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoDeleteRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoUpdateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import com.tencent.bkrepo.repository.service.node.NodeService
import com.tencent.bkrepo.repository.service.repo.ProjectService
import com.tencent.bkrepo.repository.service.repo.ProxyChannelService
import com.tencent.bkrepo.repository.service.repo.RepositoryService
import com.tencent.bkrepo.repository.service.repo.StorageCredentialService
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 仓库服务实现类
 */
@Service
class RepositoryServiceImpl(
    private val repositoryDao: RepositoryDao,
    private val nodeService: NodeService,
    private val projectService: ProjectService,
    private val storageCredentialService: StorageCredentialService,
    private val proxyChannelService: ProxyChannelService,
    private val repositoryProperties: RepositoryProperties
) : RepositoryService {

    override fun getRepoInfo(projectId: String, name: String, type: String?): RepositoryInfo? {
        val tRepository = repositoryDao.findByNameAndType(projectId, name, type)
        return convertToInfo(tRepository)
    }

    override fun getRepoDetail(projectId: String, name: String, type: String?): RepositoryDetail? {
        val tRepository = repositoryDao.findByNameAndType(projectId, name, type)
        val storageCredentials = tRepository?.credentialsKey?.let { storageCredentialService.findByKey(it) }
        return convertToDetail(tRepository, storageCredentials)
    }

    override fun updateStorageCredentialsKey(projectId: String, repoName: String, storageCredentialsKey: String) {
        repositoryDao.findByNameAndType(projectId, repoName, null)?.run {
            credentialsKey = storageCredentialsKey
            repositoryDao.save(this)
        }
    }

    override fun listRepo(projectId: String, name: String?, type: String?): List<RepositoryInfo> {
        val query = buildListQuery(projectId, name, type)
        return repositoryDao.find(query).map { convertToInfo(it)!! }
    }

    override fun listRepoPage(
        projectId: String,
        pageNumber: Int,
        pageSize: Int,
        name: String?,
        type: String?
    ): Page<RepositoryInfo> {
        val query = buildListQuery(projectId, name, type)
        val pageRequest = Pages.ofRequest(pageNumber, pageSize)
        val totalRecords = repositoryDao.count(query)
        val records = repositoryDao.find(query.with(pageRequest)).map { convertToInfo(it)!! }
        return Pages.ofResponse(pageRequest, totalRecords, records)
    }

    override fun rangeQuery(request: RepoRangeQueryRequest): Page<RepositoryInfo?> {
        val limit = request.limit
        val skip = request.offset
        val projectId = request.projectId

        val criteria = if (request.repoNames.isEmpty()) {
            where(TRepository::projectId).isEqualTo(projectId)
        } else {
            where(TRepository::projectId).isEqualTo(projectId).and(TRepository::name).inValues(request.repoNames)
        }
        val totalCount = repositoryDao.count(Query(criteria))
        val records = repositoryDao.find(Query(criteria).limit(limit).skip(skip))
            .map { convertToInfo(it) }
        return Page(0, limit, totalCount, records)
    }

    override fun checkExist(projectId: String, name: String, type: String?): Boolean {
        return repositoryDao.findByNameAndType(projectId, name, type) != null
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun createRepo(repoCreateRequest: RepoCreateRequest): RepositoryDetail {
        with(repoCreateRequest) {
            Preconditions.matchPattern(name, REPO_NAME_PATTERN, this::name.name)
            Preconditions.checkArgument(description?.length ?: 0 <= REPO_DESCRIPTION_MAX_LENGTH, this::description.name)
            // 确保项目一定存在
            if (!projectService.checkExist(projectId)) {
                throw ErrorCodeException(ArtifactMessageCode.PROJECT_NOT_FOUND, name)
            }
            // 确保同名仓库不存在
            if (checkExist(projectId, name)) {
                throw ErrorCodeException(ArtifactMessageCode.REPOSITORY_EXISTED, name)
            }
            // 确保存储凭证Key一定存在
            val credentialsKey = storageCredentialsKey ?: repositoryProperties.defaultStorageCredentialsKey
            val storageCredential = credentialsKey?.takeIf { it.isNotBlank() }?.let {
                storageCredentialService.findByKey(it) ?: throw ErrorCodeException(
                    CommonMessageCode.RESOURCE_NOT_FOUND,
                    it
                )
            }
            // 初始化仓库配置
            val repoConfiguration = configuration ?: buildRepoConfiguration(this)
            // 创建仓库
            val repository = TRepository(
                name = name,
                type = type,
                category = category,
                public = public,
                description = description,
                configuration = repoConfiguration.toJsonString(),
                credentialsKey = credentialsKey,
                projectId = projectId,
                createdBy = operator,
                createdDate = LocalDateTime.now(),
                lastModifiedBy = operator,
                lastModifiedDate = LocalDateTime.now()
            )
            return try {
                if (repoConfiguration is CompositeConfiguration) {
                    updateCompositeConfiguration(repoConfiguration, null, repository, operator)
                }
                repositoryDao.insert(repository)
                publishEvent(RepoCreatedEvent(repoCreateRequest))
                logger.info("Create repository [$repoCreateRequest] success.")
                convertToDetail(repository, storageCredential)!!
            } catch (exception: DuplicateKeyException) {
                logger.warn("Insert repository[$projectId/$name] error: [${exception.message}]")
                getRepoDetail(projectId, name, type.name)!!
            }
        }
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun updateRepo(repoUpdateRequest: RepoUpdateRequest) {
        repoUpdateRequest.apply {
            Preconditions.checkArgument(description?.length ?: 0 < REPO_DESCRIPTION_MAX_LENGTH, this::description.name)
            val repository = checkRepository(projectId, name)
            val oldConfiguration = repository.configuration.readJsonString<RepositoryConfiguration>()
            repository.public = public ?: repository.public
            repository.description = description ?: repository.description
            repository.lastModifiedBy = operator
            repository.lastModifiedDate = LocalDateTime.now()
            configuration?.let {
                updateRepoConfiguration(it, oldConfiguration, repository, operator)
                repository.configuration = it.toJsonString()
            }
            repositoryDao.save(repository)
        }
        publishEvent(RepoUpdatedEvent(repoUpdateRequest))
        logger.info("Update repository[$repoUpdateRequest] success.")
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun deleteRepo(repoDeleteRequest: RepoDeleteRequest) {
        repoDeleteRequest.apply {
            val repository = checkRepository(projectId, name)
            if (repoDeleteRequest.forced) {
                nodeService.deleteByPath(projectId, name, ROOT, operator)
            } else {
                val artifactInfo = DefaultArtifactInfo(projectId, name, ROOT)
                nodeService.countFileNode(artifactInfo).takeIf { it == 0L } ?: throw ErrorCodeException(
                    ArtifactMessageCode.REPOSITORY_CONTAINS_FILE
                )
                nodeService.deleteByPath(projectId, name, ROOT, operator)
            }
            repositoryDao.deleteById(repository.id)
            // 删除关联的库
            if (repository.category == RepositoryCategory.COMPOSITE) {
                val configuration = repository.configuration.readJsonString<CompositeConfiguration>()
                configuration.proxy.channelList.filter { !it.public }.forEach {
                    deleteProxyRepo(repository.projectId, repository.name, it.name!!)
                }
            }
        }
        publishEvent(RepoDeletedEvent(repoDeleteRequest))
        logger.info("Delete repository [$repoDeleteRequest] success.")
    }

    /**
     * 检查仓库是否存在，不存在则抛异常
     */
    private fun checkRepository(projectId: String, repoName: String, repoType: String? = null): TRepository {
        return repositoryDao.findByNameAndType(projectId, repoName, repoType)
            ?: throw ErrorCodeException(REPOSITORY_NOT_FOUND, repoName)
    }

    /**
     * 构造list查询条件
     */
    private fun buildListQuery(projectId: String, repoName: String? = null, repoType: String? = null): Query {
        val criteria = where(TRepository::projectId).isEqualTo(projectId)
        criteria.and(TRepository::display).ne(false)
        repoName?.takeIf { it.isNotBlank() }?.apply { criteria.and(TRepository::name).regex("^$this") }
        repoType?.takeIf { it.isNotBlank() }?.apply { criteria.and(TRepository::type).isEqualTo(this.toUpperCase()) }
        return Query(criteria).with(Sort.by(Sort.Direction.DESC, TRepository::createdDate.name))
    }

    /**
     * 构造仓库初始化配置
     */
    private fun buildRepoConfiguration(request: RepoCreateRequest): RepositoryConfiguration {
        return when (request.category) {
            RepositoryCategory.LOCAL -> LocalConfiguration()
            RepositoryCategory.REMOTE -> RemoteConfiguration()
            RepositoryCategory.VIRTUAL -> VirtualConfiguration()
            RepositoryCategory.COMPOSITE -> CompositeConfiguration()
        }
    }

    /**
     * 更新仓库配置
     */
    private fun updateRepoConfiguration(
        new: RepositoryConfiguration,
        old: RepositoryConfiguration,
        repository: TRepository,
        operator: String
    ) {
        val newType = new::class.simpleName
        val oldType = old::class.simpleName
        if (newType != oldType) {
            throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "configuration type")
        }
        if (new is CompositeConfiguration && old is CompositeConfiguration) {
            updateCompositeConfiguration(new, old, repository, operator)
        }
    }

    /**
     * 更新Composite类型仓库配置
     *
     * 创建private代理仓库
     */
    private fun updateCompositeConfiguration(
        new: CompositeConfiguration,
        old: CompositeConfiguration? = null,
        repository: TRepository,
        operator: String
    ) {
        // 校验
        new.proxy.channelList.forEach {
            if (it.public) {
                Preconditions.checkArgument(
                    proxyChannelService.checkExistById(it.channelId!!, repository.type),
                    "channelId"
                )
            } else {
                Preconditions.checkNotBlank(it.name, "name")
                Preconditions.checkNotBlank(it.url, "url")
            }
        }
        val newPrivateProxyRepos = new.proxy.channelList.filter { !it.public }
        val existPrivateProxyRepos = old?.proxy?.channelList?.filter { !it.public }.orEmpty()

        val newPrivateProxyRepoMap = newPrivateProxyRepos.map { it.name!! to it }.toMap()
        val existPrivateProxyRepoMap = existPrivateProxyRepos.map { it.name!! to it }.toMap()
        Preconditions.checkArgument(newPrivateProxyRepoMap.size == newPrivateProxyRepos.size, "channelList")

        val toCreateList = mutableListOf<ProxyChannelSetting>()
        val toDeleteList = mutableListOf<ProxyChannelSetting>()

        // 查找要添加的代理库
        newPrivateProxyRepoMap.forEach { (name, channel) ->
            existPrivateProxyRepoMap[name]?.let {
                // 确保用户未修改name和url，以及添加同名channel
                if (channel.url != it.url) {
                    throw ErrorCodeException(CommonMessageCode.RESOURCE_EXISTED, channel.name.orEmpty())
                }
            } ?: run { toCreateList.add(channel) }
        }
        // 查找要删除的代理库
        existPrivateProxyRepoMap.forEach { (name, channel) ->
            if (!newPrivateProxyRepoMap.containsKey(name)) {
                toDeleteList.add(channel)
            }
        }
        // 创建新的代理库
        toCreateList.forEach {
            val proxyRepoName = PRIVATE_PROXY_REPO_NAME.format(repository.name, it.name)
            if (checkExist(repository.projectId, proxyRepoName, null)) {
                logger.error("[$proxyRepoName] exist in project[${repository.projectId}], skip creating proxy repo.")
            }
            createProxyRepo(repository, proxyRepoName, operator)
        }
        // 删除旧的代理库
        toDeleteList.forEach {
            deleteProxyRepo(repository.projectId, repository.name, it.name!!)
        }
    }

    /**
     * 删除关联的代理仓库
     */
    private fun deleteProxyRepo(projectId: String, repoName: String, channelName: String) {
        val proxyRepoName = PRIVATE_PROXY_REPO_NAME.format(repoName, channelName)
        val proxyRepo = repositoryDao.findByNameAndType(projectId, proxyRepoName, null)
        proxyRepo?.let { repo ->
            // 删除仓库
            nodeService.deleteByPath(repo.projectId, repo.name, ROOT, SYSTEM_USER)
            repositoryDao.deleteById(repo.id)
            logger.info("Success to delete private proxy repository[$proxyRepo]")
        }
    }

    private fun createProxyRepo(repository: TRepository, proxyRepoName: String, operator: String) {
        // 创建仓库
        val proxyRepository = TRepository(
            name = proxyRepoName,
            type = repository.type,
            category = RepositoryCategory.REMOTE,
            public = false,
            description = null,
            configuration = RemoteConfiguration().toJsonString(),
            credentialsKey = repository.credentialsKey,
            display = false,
            projectId = repository.projectId,
            createdBy = operator,
            createdDate = LocalDateTime.now(),
            lastModifiedBy = operator,
            lastModifiedDate = LocalDateTime.now()
        )
        repositoryDao.insert(proxyRepository)
        logger.info("Success to create private proxy repository[$proxyRepository]")
    }

    override fun listRepoPageByType(type: String, pageNumber: Int, pageSize: Int): Page<RepositoryDetail> {
        val query = Query(TRepository::type.isEqualTo(type)).with(Sort.by(TRepository::name.name))
        val count = repositoryDao.count(query)
        val pageQuery = query.with(PageRequest.of(pageNumber, pageSize))
        val data = repositoryDao.find(pageQuery).map { convertToDetail(it)!! }

        return Page(pageNumber, pageSize, count, data)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryServiceImpl::class.java)
        private const val REPO_NAME_PATTERN = "[a-zA-Z_][a-zA-Z0-9\\-_]{1,31}"
        private const val REPO_DESCRIPTION_MAX_LENGTH = 200

        private fun convertToDetail(
            tRepository: TRepository?,
            storageCredentials: StorageCredentials? = null
        ): RepositoryDetail? {
            return tRepository?.let {
                RepositoryDetail(
                    name = it.name,
                    type = it.type,
                    category = it.category,
                    public = it.public,
                    description = it.description,
                    configuration = it.configuration.readJsonString(),
                    storageCredentials = storageCredentials,
                    projectId = it.projectId,
                    createdBy = it.createdBy,
                    createdDate = it.createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    lastModifiedBy = it.lastModifiedBy,
                    lastModifiedDate = it.lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME)
                )
            }
        }

        private fun convertToInfo(tRepository: TRepository?): RepositoryInfo? {
            return tRepository?.let {
                RepositoryInfo(
                    name = it.name,
                    type = it.type,
                    category = it.category,
                    public = it.public,
                    description = it.description,
                    configuration = it.configuration.readJsonString(),
                    storageCredentialsKey = it.credentialsKey,
                    projectId = it.projectId,
                    createdBy = it.createdBy,
                    createdDate = it.createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    lastModifiedBy = it.lastModifiedBy,
                    lastModifiedDate = it.lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME)
                )
            }
        }
    }
}
