package com.tencent.bkrepo.git.service

import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.util.HumanReadable.time
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactService
import com.tencent.bkrepo.common.redis.RedisLock
import com.tencent.bkrepo.common.redis.RedisOperation
import com.tencent.bkrepo.common.service.util.LocaleMessageUtils
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.git.artifact.GitContentArtifactInfo
import com.tencent.bkrepo.git.artifact.GitRepositoryArtifactInfo
import com.tencent.bkrepo.git.artifact.repository.GitRemoteRepository
import com.tencent.bkrepo.git.constant.DOT_GIT
import com.tencent.bkrepo.git.constant.GitMessageCode
import com.tencent.bkrepo.git.constant.REDIS_SET_REPO_TO_UPDATE
import com.tencent.bkrepo.git.constant.convertorLockKey
import com.tencent.bkrepo.git.server.BkrepoReceivePackFactory
import com.tencent.bkrepo.git.server.SmartOutputStream
import org.eclipse.jgit.errors.CorruptObjectException
import org.eclipse.jgit.errors.PackProtocolException
import org.eclipse.jgit.errors.UnpackException
import org.eclipse.jgit.http.server.GitSmartHttpTools
import org.eclipse.jgit.http.server.GitSmartHttpTools.UPLOAD_PACK_REQUEST_TYPE
import org.eclipse.jgit.http.server.GitSmartHttpTools.UPLOAD_PACK_RESULT_TYPE
import org.eclipse.jgit.http.server.GitSmartHttpTools.sendError
import org.eclipse.jgit.http.server.HttpServerText
import org.eclipse.jgit.http.server.ServletUtils
import org.eclipse.jgit.http.server.ServletUtils.consumeRequestBody
import org.eclipse.jgit.http.server.resolver.DefaultUploadPackFactory
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.InternalHttpServerGlue
import org.eclipse.jgit.transport.PacketLineOut
import org.eclipse.jgit.transport.ReceivePack
import org.eclipse.jgit.transport.RefAdvertiser
import org.eclipse.jgit.transport.ServiceMayNotContinueException
import org.eclipse.jgit.transport.UploadPack
import org.eclipse.jgit.transport.UploadPackInternalServerErrorException
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException
import org.eclipse.jgit.util.HttpSupport
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.lang.Exception
import java.text.MessageFormat
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponse.SC_FORBIDDEN
import javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR
import javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED
import javax.servlet.http.HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE
import kotlin.system.measureNanoTime

@Service
class GitService(
    private val redisOperation: RedisOperation,
    private val gitCommonService: GitCommonService
) : ArtifactService() {
    val uploadPackFactory: DefaultUploadPackFactory = DefaultUploadPackFactory()
    val receivePackFactory: BkrepoReceivePackFactory = BkrepoReceivePackFactory()
    val executor: ThreadPoolExecutor = ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors() * 2,
        200, 60, TimeUnit.SECONDS, LinkedBlockingQueue(10000)
    )

    private val logger = LoggerFactory.getLogger(GitService::class.java)

    companion object {
        /*
        * 暂时没有看门狗的锁机制存在，所以暂时设置五分钟同步请求锁时间。
        * 这个时间主要是做请求消峰使用，控制频繁的向代理源频繁的发起网络请求
        * 如果同步过程超过五分钟，锁释放了
        * clone: 因为本地目录不为空，所以再次发起clone请求会被忽略
        * fetch: 每个不同的fetch都有自己的incoming pack文件，不会互相影响
        * */
        private const val expiredTimeInSeconds: Long = 300L
    }

    fun sync(infoRepository: GitRepositoryArtifactInfo) {
        val context = ArtifactDownloadContext()
        val task = {
            val name = context.artifactInfo.getArtifactName()
            val key = convertorLockKey(name)
            val lock = RedisLock(redisOperation, key, expiredTimeInSeconds)
            if (lock.tryLock()) {
                lock.use {
                    doSync(context)
                }
            } else {
                logger.info("not acquire lock $key")
                redisOperation.addSetValue(REDIS_SET_REPO_TO_UPDATE, name)
                logger.info("add $REDIS_SET_REPO_TO_UPDATE $name")
            }
        }
        executor.submit(task)
        context.response.contentType = MediaTypes.APPLICATION_JSON
        context.response.writer.println(ResponseBuilder.success().toJsonString())
    }

    private fun doSync(context: ArtifactContext) {
        try {
            val repository = ArtifactContextHolder.getRepository(RepositoryCategory.REMOTE)
            val nanoTime = measureNanoTime { (repository as GitRemoteRepository).sync(context) }
            logger.info("Success to sync ${context.getRemoteConfiguration().url}, ${time(nanoTime)}")
        } catch (e: Exception) {
            logger.error("Failed to sync ${context.getRemoteConfiguration().url}", e)
        }
    }

    fun getContent(gitContentArtifactInfo: GitContentArtifactInfo) {
        repository.download(ArtifactDownloadContext())
    }

    fun infoRefs(infoRepository: GitRepositoryArtifactInfo, svc: String) {
        val context = ArtifactContext()
        val (db, req, res) = prepare(context)
        var up: UploadPack? = null
        val buf = SmartOutputStream(req, res, true)
        try {
            up = uploadPackFactory.create(req, db)
            InternalHttpServerGlue.setPeerUserAgent(
                up,
                req.getHeader(HttpSupport.HDR_USER_AGENT)
            )
            res.contentType = infoRefsResultType(svc)
            up.isBiDirectionalPipe = false
            buf.use {
                val out = PacketLineOut(buf)
                up.sendAdvertisedRefs(RefAdvertiser.PacketLineOutRefAdvertiser(out), svc)
            }
        } catch (e: ServiceNotAuthorizedException) {
            res.sendError(SC_UNAUTHORIZED, e.message)
        } catch (e: ServiceNotEnabledException) {
            sendError(req, res, SC_FORBIDDEN, e.message)
        } catch (e: ServiceMayNotContinueException) {
            if (e.isOutput) buf.close() else sendError(req, res, e.statusCode, e.message)
        } finally {
            up?.revWalk?.close()
        }
    }

    fun gitUploadPack() {
        val context = ArtifactContext()
        val (db, req, rsp) = prepare(context)
        var up: UploadPack? = null
        if (UPLOAD_PACK_REQUEST_TYPE != req.contentType) {
            rsp.sendError(SC_UNSUPPORTED_MEDIA_TYPE)
            return
        }
        val out = object : SmartOutputStream(req, rsp, false) {
            override fun flush() {
                doFlush()
            }
        }
        try {
            up = uploadPackFactory.create(req, db)
            up.isBiDirectionalPipe = false
            rsp.contentType = UPLOAD_PACK_RESULT_TYPE
            out.use {
                up.uploadWithExceptionPropagation(
                    ServletUtils.getInputStream(req),
                    out,
                    null
                )
            }
        } catch (e: ServiceMayNotContinueException) {
            if (e.isOutput) {
                consumeRequestBody(req)
            }
            throw e
        } catch (e: UploadPackInternalServerErrorException) {
            logger.error(
                MessageFormat.format(
                    HttpServerText.get().internalErrorDuringUploadPack,
                    identify(db)
                ),
                e
            )
            consumeRequestBody(req)
        } catch (e: ServiceMayNotContinueException) {
            if (!e.isOutput && !rsp.isCommitted) {
                rsp.reset()
                sendError(req, rsp, e.statusCode, e.message)
            }
        } catch (e: Throwable) {
            logger.error(
                MessageFormat.format(
                    HttpServerText.get().internalErrorDuringUploadPack,
                    identify(db)
                ),
                e
            )
            if (!rsp.isCommitted) {
                rsp.reset()
                val msg = (e as? PackProtocolException)?.message
                sendError(req, rsp, SC_INTERNAL_SERVER_ERROR, msg)
            }
        } finally {
            up?.revWalk?.close()
        }
    }

    fun gitReceivePack() {
        val context = ArtifactContext()
        val (db, req, rsp) = prepare(context)
        if (context.repositoryDetail.category == RepositoryCategory.REMOTE) {
            rsp.sendError(
                SC_FORBIDDEN,
                LocaleMessageUtils.getLocalizedMessage(
                    GitMessageCode.GIT_REMOTE_REPO_PUSH_NOT_SUPPORT
                )
            )
            logger.info("refuse git push request for remote repository ${context.repoName}")
            return
        }
        var rp: ReceivePack? = null
        val out = object : SmartOutputStream(req, rsp, false) {
            override fun flush() {
                doFlush()
            }
        }
        try {
            rp = receivePackFactory.create(req, db, context)
            rp.isBiDirectionalPipe = false
            rsp.contentType = GitSmartHttpTools.RECEIVE_PACK_RESULT_TYPE
            out.use {
                rp.receive(ServletUtils.getInputStream(req), out, null)
            }
        } catch (e: ServiceNotAuthorizedException) {
            rsp.sendError(SC_UNAUTHORIZED, e.message)
            return
        } catch (e: ServiceNotEnabledException) {
            sendError(req, rsp, SC_FORBIDDEN, e.message)
            return
        } catch (e: CorruptObjectException) {
            logger.error(
                MessageFormat.format(
                    HttpServerText.get().receivedCorruptObject,
                    e.message,
                    identify(db)
                )
            )
            consumeRequestBody(req)
        } catch (e: Throwable) {
            logger.error(
                MessageFormat.format(
                    HttpServerText.get().internalErrorDuringReceivePack,
                    identify(db)
                ),
                e
            )
            when (e) {
                is UnpackException, is PackProtocolException -> {
                    consumeRequestBody(req)
                }
            }
            if (!rsp.isCommitted) {
                rsp.reset()
                sendError(req, rsp, SC_INTERNAL_SERVER_ERROR)
            }
        } finally {
            rp?.revWalk?.close()
        }
        gitCommonService.storeGitDir(db, context)
    }

    private fun prepare(context: ArtifactContext): Triple<Repository, HttpServletRequest, HttpServletResponse> {
        val dir = gitCommonService.generateWorkDir(context)
        var db: Repository
        try {
            db = gitCommonService.createGit(context, dir).repository
        } catch (e: ErrorCodeException) {
            db = FileRepository("${dir.canonicalPath}/$DOT_GIT")
            db.create()
        }
        val req = context.request
        val rsp = context.response
        return Triple(db, req, rsp)
    }

    fun infoRefsResultType(svc: String): String? {
        return "application/x-$svc-advertisement"
    }

    fun identify(git: Repository): String? {
        return git.identifier ?: return "unknown"
    }
}
