/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.git.artifact.repository

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.util.HumanReadable.time
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteConfiguration
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.redis.RedisOperation
import com.tencent.bkrepo.git.artifact.GitContentArtifactInfo
import com.tencent.bkrepo.git.constant.GitMessageCode
import com.tencent.bkrepo.git.constant.REDIS_SET_REPO_TO_UPDATE
import com.tencent.bkrepo.git.constant.R_REMOTE_ORIGIN
import com.tencent.bkrepo.git.service.GitCommonService
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.TagOpt
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.measureNanoTime

@Component
class GitRemoteRepository : RemoteRepository() {

    val logger: Logger = LoggerFactory.getLogger(GitRemoteRepository::class.java)

    @Autowired
    lateinit var redisOperation: RedisOperation

    @Autowired
    lateinit var gitCommonService: GitCommonService

    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        val gitContentArtifactInfo = context.artifactInfo as GitContentArtifactInfo
        val ref = gitContentArtifactInfo.ref
        val directory = gitCommonService.generateWorkDir(context)
        val git = gitCommonService.createGit(context, directory)
        git.repository.use {
            if (gitContentArtifactInfo.objectId == null) {
                val objectId = objectId(git, gitContentArtifactInfo, ref)
                gitContentArtifactInfo.objectId = objectId.name
            }
            with(context) {
                val node = nodeClient.getNodeDetail(
                    projectId, repoName,
                    artifactInfo.getArtifactFullPath()
                ).data ?: let {
                    gitCommonService.checkoutFileAndCreateNode(git, gitContentArtifactInfo, context)
                }
                val inputStream = storageManager.loadArtifactInputStream(node, storageCredentials) ?: return null
                val responseName = artifactInfo.getResponseName()
                return ArtifactResource(inputStream, responseName, node, ArtifactChannel.PROXY, useDisposition)
            }
        }
    }

    private fun objectId(git: Git, gitContentArtifactInfo: GitContentArtifactInfo, ref: String): ObjectId {
        return git.repository
            .resolve("${R_REMOTE_ORIGIN}${gitContentArtifactInfo.ref}") ?: let {
            git.repository.resolve(gitContentArtifactInfo.ref) ?: let {
                throw ErrorCodeException(GitMessageCode.GIT_REF_NOT_FOUND, ref)
            }
        }
    }

    /**
     * 同步仓库
     * */
    fun sync(context: ArtifactContext) {
        logger.info("start sync project ${context.projectId} repo ${context.repoName}")
        val remoteConfiguration = context.getRemoteConfiguration()
        var credentialsProvider: CredentialsProvider? = null
        if (remoteConfiguration.credentials.username != null &&
            remoteConfiguration.credentials.password != null
        ) {
            credentialsProvider = UsernamePasswordCredentialsProvider(
                remoteConfiguration.credentials.username, remoteConfiguration.credentials.password
            )
        }
        with(context) {
            val directory = gitCommonService.generateWorkDir(this)
            /*
            * 1. 先在本地根据配置的upload.location和project,repoName的sha1值，构建工作目录
            * 2. 执行clone或fetch操作
            * 3. 上传git文件
            *
            * 因为是本地已经有了文件内容，并且不会清除，
            * 所以在node上能查找到git相关文件时，则表明本地文件已处理完整
            * */

            // 检查是否存在.git目录
            if (nodeClient.checkExist(projectId, repoName, artifactInfo.getArtifactFullPath()).data!!) {
                updateRepo(directory, credentialsProvider, null)
            } else {
                // 克隆仓库
                cloneRepo(directory, remoteConfiguration, credentialsProvider)
            }
        }
    }

    private fun ArtifactContext.cloneRepo(
        directory: File,
        remoteConfiguration: RemoteConfiguration,
        credentialsProvider: CredentialsProvider?
    ) {
        if (StringUtils.isEmpty(remoteConfiguration.url)) {
            throw ErrorCodeException(GitMessageCode.GIT_URL_NOT_CONFIG)
        }
        directory.listFiles()?.isEmpty().let {
            logger.info("start clone ${remoteConfiguration.url}")

            val command = Git.cloneRepository()
            command.setDirectory(directory)
            command.setURI(remoteConfiguration.url)
            command.setNoCheckout(true)
            credentialsProvider.let {
                command.setCredentialsProvider(credentialsProvider)
            }
            var git: Git? = null
            val nanoTime = measureNanoTime { git = command.call() }
            logger.info(
                "end clone ${remoteConfiguration.url}, " +
                    "clone call spend ${time(nanoTime)}"
            )
            RepositoryCache.register(git?.repository)
            val toUpdate = redisOperation
                .getSetMembers(REDIS_SET_REPO_TO_UPDATE)?.contains(artifactInfo.getArtifactName())
            if (toUpdate == true)
                updateRepo(directory, credentialsProvider, git)
            else
                gitCommonService.storeGitDir(git!!.repository, this)
        }
    }

    private fun ArtifactContext.updateRepo(
        directory: File,
        credentialsProvider: CredentialsProvider?,
        gitv: Git?
    ) {
        // 更新仓库,拉取新的数据，并且上传。完成后清理掉原来的ref缓存
        val git = gitv ?: gitCommonService.createGit(this, directory)
        val fetchCommand = git.fetch()
            .setTagOpt(TagOpt.FETCH_TAGS)
        credentialsProvider.let {
            fetchCommand.setCredentialsProvider(credentialsProvider)
        }
        do {
            logger.info("start fetch ${getRemoteConfiguration().url}")
            val nanoTime = measureNanoTime { fetchCommand.call() }
            logger.info(
                "end fetch ${getRemoteConfiguration().url}, " +
                    "fetch call spend ${time(nanoTime)}"
            )
            val toUpdate = redisOperation
                .getSetMembers(REDIS_SET_REPO_TO_UPDATE)?.contains(artifactInfo.getArtifactName())
            if (toUpdate != true) {
                gitCommonService.storeGitDir(git.repository, this)
                logger.info("update ${artifactInfo.getArtifactName()}   state: ${git.repository.repositoryState}")
                return
            }
            // 为防止频繁的触发更新，每隔3s轮训一次
            logger.info("check toUpdate OK : ${artifactInfo.getArtifactName()} 3s later fetch")
            redisOperation.sremove(REDIS_SET_REPO_TO_UPDATE, artifactInfo.getArtifactName())
            logger.info("remove $REDIS_SET_REPO_TO_UPDATE ${artifactInfo.getArtifactName()}")
            TimeUnit.SECONDS.sleep(3)
        } while (true)
    }
}
