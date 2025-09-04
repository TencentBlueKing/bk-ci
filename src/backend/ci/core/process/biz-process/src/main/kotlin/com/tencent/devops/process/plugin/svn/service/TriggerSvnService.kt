package com.tencent.devops.process.plugin.svn.service

import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.webhook.pojo.code.svn.SvnCommitEvent
import com.tencent.devops.common.webhook.pojo.code.svn.SvnCommitEventFile
import com.tencent.devops.process.dao.PipelineWebhookRevisionDao
import com.tencent.devops.process.engine.dao.PipelineWebhookDao
import com.tencent.devops.process.utils.Credential
import com.tencent.devops.process.utils.CredentialUtils
import com.tencent.devops.process.webhook.CodeWebhookEventDispatcher
import com.tencent.devops.process.webhook.pojo.event.commit.SvnWebhookEvent
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.api.scm.ServiceSvnResource
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.scm.exception.ScmException
import com.tencent.devops.scm.pojo.SvnRevisionInfo
import com.tencent.devops.ticket.api.ServiceCredentialResource
import org.jooq.DSLContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.function.StreamBridge
import java.time.LocalDateTime
import java.util.Base64
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Suppress("TooManyFunctions", "MagicNumber")
class TriggerSvnService(
    private val client: Client,
    private val pipelineWebhookRevisionDao: PipelineWebhookRevisionDao,
    private val pipelineWebhookDao: PipelineWebhookDao,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val streamBridge: StreamBridge
) {
    /**
     *  触发流水线用
     */
    private val triggerTaskExecutor = Executors.newWorkStealingPool(10)

    private val cache = CacheBuilder.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(8, TimeUnit.HOURS)
        .build<String, CodeSvnRepository>()

    fun start(interval: Long) {
        pollingSvnRepoTask(interval)
    }

    private fun getRevisionList(
        url: String,
        privateKey: String,
        passPhrase: String?,
        userName: String,
        current: String?
    ): Pair<Long, List<SvnRevisionInfo>>? {
        val svnService = client.get(ServiceSvnResource::class)
        return svnService.getSvnRevisionList(
            url = url,
            privateKey = privateKey,
            passPhrase = passPhrase,
            username = userName,
            branchName = "",
            currentVersion = current
        ).data
    }

    /**
     *  轮询 SVN 仓库
     */
    private fun pollingSvnRepoTask(interval: Long) {
        logger.info("SVN repositroy pooling, now time: {}", LocalDateTime.now())
        // 使用 redis, 防止多节点同时执行
        val lock = RedisLock(redisOperation, REDIS_LOCK_KEY, LOCK_TIME_SEC)
        try {
            if (!lock.tryLock()) {
                logger.info("The other process is processing polling job, ignore")
                return
            }
            if (!checkLastPollTime(interval)) {
                return
            }
            // 开始分页查询
            pagingLoopSvnWebhook()
        } finally {
            lock.unlock()
        }
    }

    private fun pagingLoopSvnWebhook() {
        var start = 0
        loop@ while (true) {
            try {
                // 获取svn触发器
                val svnTriggerList =
                    pipelineWebhookDao.getPipelineWebHooksByRepositoryType(
                        dslContext = dslContext,
                        repositoryType = ScmType.CODE_SVN.name,
                        offset = start,
                        limit = LIMIT
                    )
                if (svnTriggerList.isEmpty()) {
                    logger.info("list timer pipeline finish|start=$start")
                    break@loop
                }
                // 通过触发器获取所有仓库id
                val repoHashIds = svnTriggerList.map { it.repoHashId!! }.toSet()
                // 由hashId获取所有的仓库信息
                // map<projectName, url+凭证信息>
                val svnRepositoryMap = getSvnRepositoryMap(repoHashIds)
                // 获取仓库的projectName数据
                val svnProjectNameList = svnRepositoryMap.keys
                // 由projectName数据获取流水线触发器的仓库版本信息
                val svnRevisionMap = getWebhookSvnRevisionMap(svnProjectNameList)
                pollAllWebhook(
                    svnProjectNameList = svnProjectNameList,
                    svnRepositoryMap = svnRepositoryMap,
                    svnRevisionMap = svnRevisionMap
                )
                start += LIMIT
            } catch (ignore: Throwable) {
                logger.info("loop err, error message : ${ignore.message}")
            }
        }
    }

    private fun findRevisionList(
        svnRevision: String?,
        repoInfoList: List<SvnRepoInfo>
    ): Pair<Long, List<SvnRevisionInfo>>? {
        var result: Pair<Long, List<SvnRevisionInfo>>? = null
        repoInfoList.forEach {
            try {
                result = getRevisionList(
                    url = it.url,
                    privateKey = it.privateKey,
                    passPhrase = it.passPhrase,
                    userName = it.userName,
                    current = svnRevision
                )
                if (result != null) {
                    // 获取到一个可用的svn记录时，跳出循环
                    logger.info("the repositroy : ${it.url} get revision info success")
                    return result
                }
            } catch (err: Throwable) {
                logger.error("the repository ${it.url} get revision fail，error message is${err.message}")
            }
        }
        return result
    }

    private fun pollAllWebhook(
        svnProjectNameList: Set<String>,
        svnRepositoryMap: Map<String, List<SvnRepoInfo>>,
        svnRevisionMap: Map<String, String>
    ) {
        svnProjectNameList.forEach {
            try {
                // 仓库的projectName
                val projectName = it
                // svn仓库目前的提交版本,可能为空，为空无条件运行触发器
                val svnRevision = svnRevisionMap[projectName]
                val repoInfoList = svnRepositoryMap[projectName]
                // 如果获取svn仓库最新数据失败，则不触发
                if (repoInfoList.isNullOrEmpty()) {
                    logger.warn("repository:$it get certificate info fail")
                    return@forEach
                }
                // 获取最新的version数据
                val svnRevisonList = findRevisionList(
                    svnRevision = svnRevision,
                    repoInfoList = repoInfoList
                )
                if (svnRevisonList == null) {
                    logger.error("$projectName get revison list fail")
                    return@forEach
                }
                val revision = svnRevisonList.first
                if (svnRevision == null || revision > svnRevision.toLong()) {
                    // 如果commit库中无commit信息，则存储一份，并且触发流水线
                    // 如果仓库版本大于commit版本，则触发流水线
                    // 获取最新的信息
                    // 保存最新的提交版本号
                    pipelineWebhookRevisionDao.saveOrUpdateRevision(
                        dslContext = dslContext,
                        projectName = projectName,
                        revision = revision.toString()
                    )
                    // 触发流水线
                    triggerPipelines(projectName, repoInfoList[0].url, svnRevisonList.second)
                    logger.info("$projectName repository trigger pipeline success")
                }
            } catch (err: Throwable) {
                logger.error("trigger pipeline with repositroy:$it fail，error message is${err.message}")
            }
        }
    }

    private fun checkLastPollTime(interval: Long): Boolean {
        val value = redisOperation.get(REDIS_KEY)
        val currentTimeMillis = System.currentTimeMillis()
        if (value != null) {
            val time = JsonUtil.to(value, Long::class.java)
            // 如果 redis 中的时间比间隔小, 说明是别的节点执行不久, 终止本次执行
            logger.info(
                "[last svn polling timestamp is: $time|" +
                        "now is: $currentTimeMillis|${currentTimeMillis - time}]"
            )
            // 增加三秒误差
            if (currentTimeMillis - time < (interval - THREE) * 1000) {
                logger.info("[this time is ignore ${currentTimeMillis - time} ${(interval - THREE) * 1000}]")
                return false
            }
        }
        redisOperation.set(REDIS_KEY, JsonUtil.toJson(currentTimeMillis))
        return true
    }

    private fun getWebhookSvnRevisionMap(
        projectNameSet: Set<String>
    ): Map<String, String> {
        // 根据projectName获取revision信息，此时为二元组<projectName,revision>
        val svnRevisionList =
            pipelineWebhookRevisionDao.getRevisonByProjectNames(dslContext, projectNameSet.toList())
        val result = mutableMapOf<String, String>()
        svnRevisionList.map {
            result.put(it.value1(), it.value2())
        }
        return result
    }

    private fun getSvnRepositoryMap(
        repoHashIds: Set<String>
    ): Map<String, List<SvnRepoInfo>> {
        val result = mutableMapOf<String, MutableList<SvnRepoInfo>>()
        // 获取所有的仓库
        val repos = listSvnRepoByIds(
            repoHashIds = repoHashIds
        )
        repos.map {
            val projectName = getProjectName(it.url)
            val credential = getCredential(
                it.projectId!!,
                it
            )
            val repoInfo = SvnRepoInfo(
                url = it.url,
                privateKey = credential.privateKey,
                passPhrase = credential.passPhrase,
                userName = credential.username
            )
            var repoInfoList = result[projectName]
            if (repoInfoList != null) {
                repoInfoList.add(repoInfo)
            } else {
                repoInfoList = mutableListOf(repoInfo)
            }
            result.put(projectName, repoInfoList)
        }
        return result
    }

    private fun listSvnRepoByIds(
        repoHashIds: Set<String>
    ): List<CodeSvnRepository> {
        val hashIdWithoutCatch = mutableSetOf<String>()
        val result = mutableListOf<CodeSvnRepository>()
        repoHashIds.forEach {
            val repo = cache.getIfPresent(it)
            if (repo != null) {
                // 获取缓存中有的仓库
                result.add(repo)
            } else {
                hashIdWithoutCatch.add(it)
            }
        }
        val repositoryService = client.get(ServiceRepositoryResource::class)
        val repos = repositoryService.listRepoByIds(
            repositoryIds = hashIdWithoutCatch
        ).data ?: emptyList()
        repos.forEach {
            // 将缓存中没有的加入缓存，并且返回
            if (it is CodeSvnRepository) {
                result.add(it)
                cache.put(it.repoHashId!!, it)
            }
        }
        return result
    }

    private fun triggerPipelines(projectName: String, url: String, revisionMessage: List<SvnRevisionInfo>) {
        revisionMessage.forEach {
            triggerTaskExecutor.execute {
                try {
                    val event = buildSvnCommitEvent(projectName, url, it)
                    logger.info("SvnCommitEvent: {}", event)
                    CodeWebhookEventDispatcher.dispatchEvent(
                        streamBridge,
                        SvnWebhookEvent(requestContent = JsonUtil.toJson(event))
                    )
                } catch (e: Exception) {
                    logger.warn("trigger pipeline fail")
                }
            }
        }
    }

    private fun buildSvnCommitEvent(
        projectName: String,
        url: String,
        svnRevisionInfo: SvnRevisionInfo
    ): SvnCommitEvent {
        val paths = svnRevisionInfo.paths.map { p ->
            val trimStart = p.trimStart('/', '\\')
            "${
                getRelativePath(url).trim(
                    '/',
                    '\\'
                )
            }/${
                removePrefixFromUrl(
                    url,
                    trimStart
                ).trim('/', '\\')
            }".trim('/', '\\')
        }
        val files = paths.map {
            SvnCommitEventFile(
                type = "U",
                file = it,
                fileFlag = true,
                size = 0
            )
        }
        return SvnCommitEvent(
            svnRevisionInfo.authorName,
            0,
            "",
            projectName, // projectName
            svnRevisionInfo.revision.toInt(),
            paths,
            files,
            svnRevisionInfo.commitTime,
            files.size,
            null
        )
    }

    private fun removePrefixFromUrl(url: String, path: String): String {
        var urlVar = url.trim('/', '\\')
        if (urlVar.contains("://")) {
            urlVar = urlVar.substring(urlVar.indexOf("://") + 3)
        }
        val ulist = urlVar.split("/")
        val plist = path.trim('/', '\\').split("/")
        // 交集
        val intersect = ulist.intersect(plist.toSet())
        val prepareToRemove = mutableListOf<String>()
        for (p in plist) {
            if (p !in intersect) {
                break
            }
            prepareToRemove.add(p)
        }
        return path.removePrefix(prepareToRemove.joinToString("/"))
    }

    private fun getRelativePath(url: String): String {
        val urlArray = url.split("//")
        if (urlArray.size < 2) {
            return ""
        }

        val path = urlArray[1]
        val repoSplit = path.split("/")
        if (repoSplit.size < 4) {
            return ""
        }
        val domain = repoSplit[0]
        val first = repoSplit[1]
        val second = repoSplit[2]

        return path.removePrefix("$domain/$first/$second").removePrefix("/")
    }

    private fun getProjectName(url: String): String {
        val urlArray = url.split("//")
        if (urlArray.size < 2) {
            return ""
        }
        val path = urlArray[1]
        val pathArray = path.split("/")
        // 与svnUtil不同，我们是//划分，所以划分出来的结果里面[0]的位置是svn服务器的地址，删除不要
        if (pathArray.size < 3) {
            throw ScmException("Invalid svn url($url)", ScmType.CODE_SVN.name)
        }
        return if (pathArray.size >= 4 && pathArray[3].endsWith("_proj")) {
            // 这个分支先保留吧，腾讯开发环境测试时要用
            "${pathArray[1]}/${pathArray[2]}/${pathArray[3]}"
        } else {
            "${pathArray[1]}/${pathArray[2]}"
        }
    }

    private fun getCredential(
        projectId: String,
        repository: CodeSvnRepository
    ): Credential {
        val credentialId = repository.credentialId
        val pair = DHUtil.initKey()
        val encoder = Base64.getEncoder()
        val decoder = Base64.getDecoder()
        val credentialResult = client.get(ServiceCredentialResource::class).get(
            projectId = projectId,
            credentialId = credentialId,
            publicKey = encoder.encodeToString(pair.publicKey),
            padding = true
        )
        if (credentialResult.isNotOk() || credentialResult.data == null) {
            logger.error(
                "Fail to get the credential($credentialId) of project($projectId) " +
                        "because of ${credentialResult.message}"
            )
            throw RuntimeException("Fail to get the credential($credentialId) of project($projectId)")
        }

        val credential = credentialResult.data!!

        val privateKey = String(
            DHUtil.decrypt(
                decoder.decode(credential.v1),
                decoder.decode(credential.publicKey),
                pair.privateKey
            )
        )

        val passPhrase = if (credential.v2.isNullOrBlank()) "" else String(
            DHUtil.decrypt(
                decoder.decode(credential.v2),
                decoder.decode(credential.publicKey),
                pair.privateKey
            )
        )

        val list = if (passPhrase.isBlank()) {
            listOf(privateKey)
        } else {
            listOf(privateKey, passPhrase)
        }

        return CredentialUtils.getCredential(
            repository,
            list,
            credentialResult.data!!.credentialType
        )
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(TriggerSvnService::class.java.name)
        private const val REDIS_KEY: String = "repository_svn_polling"
        private const val REDIS_LOCK_KEY: String = "repository_svn_polling_redis_lock"
        private const val THREE = 3L
        private const val LIMIT = 200
        private val LOCK_TIME_SEC = TimeUnit.MINUTES.toSeconds(30) // 30 minutes
    }

    data class SvnRepoInfo(
        val url: String,
        val privateKey: String,
        val passPhrase: String?,
        val userName: String
    )
}
