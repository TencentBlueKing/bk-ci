package com.tencent.bkrepo.git.service

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.toArtifactFile
import com.tencent.bkrepo.common.artifact.hash.sha1
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.git.artifact.GitContentArtifactInfo
import com.tencent.bkrepo.git.artifact.GitRepositoryArtifactInfo
import com.tencent.bkrepo.git.config.GitProperties
import com.tencent.bkrepo.git.constant.DOT_GIT
import com.tencent.bkrepo.git.constant.GIT_NODE_LIST_PAGE_NUMBER
import com.tencent.bkrepo.git.constant.GIT_NODE_LIST_PAGE_SIZE
import com.tencent.bkrepo.git.constant.GitMessageCode
import com.tencent.bkrepo.git.constant.R_HEADS
import com.tencent.bkrepo.git.constant.R_REMOTE_ORIGIN
import com.tencent.bkrepo.git.util.FileUtil
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.util.FS
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

@Service
class GitCommonService {

    val logger: Logger = LoggerFactory.getLogger(GitCommonService::class.java)

    @Autowired
    lateinit var nodeClient: NodeClient

    @Autowired
    lateinit var storageManager: StorageManager

    @Autowired
    lateinit var gitProperties: GitProperties

    @Autowired
    lateinit var properties: StorageProperties

    @Autowired
    lateinit var storageService: StorageService

    fun generateWorkDir(artifactContext: ArtifactContext): File {
        with(artifactContext) {
            val sha1 = "$projectId$repoName".sha1()
            val dirName = "${gitProperties.locationDir}/${sha1.substring(0, 2)}/${sha1.substring(2, 40)}"
            val directory = File(dirName)
            if (!directory.isDirectory && !directory.mkdirs()) {
                throw IOException("failed to create work directory ${directory.canonicalPath}")
            }
            logger.debug("create tmp git work dir ${directory.canonicalPath}")
            return directory
        }
    }

    /**
     * 创建git工作目录
     * */
    fun createGit(artifactContext: ArtifactContext, directory: File): Git {
        with(artifactContext) {
            if (RepositoryCache.FileKey
                .isGitRepository(File(directory, DOT_GIT), FS.DETECTED)
            )
                return Git(
                    RepositoryCache
                        .open(
                            RepositoryCache.FileKey
                                .lenient(directory, FS.DETECTED)
                        )
                )

            // 一般情况不会走到这里，除非服务器本地文件被清理了
            logger.info("acquire git file from storage")
            val nodeListOption = NodeListOption(
                pageNumber = GIT_NODE_LIST_PAGE_NUMBER,
                pageSize = GIT_NODE_LIST_PAGE_SIZE,
                includeFolder = false,
                includeMetadata = true,
                deep = true,
                sort = false
            )
            val response = nodeClient.listNodePage(projectId, repoName, DOT_GIT, nodeListOption)
            if (response.data == null || response.data!!.records.isEmpty()) {
                throw ErrorCodeException(GitMessageCode.GIT_REPO_NOT_SYNC)
            }
            return buildGit(response.data!!.records, directory, storageCredentials)
        }
    }

    /**
     * 根据存储文件，创建临时git目录
     * */
    fun buildGit(nodes: List<NodeInfo>, dir: File, storageCredentials: StorageCredentials?): Git {
        for (node in nodes) {
            val inputStream = storageManager.loadArtifactInputStream(NodeDetail(node), storageCredentials)
                ?: throw ErrorCodeException(GitMessageCode.GIT_ORIGINAL_FILE_MISS, node.fullPath)
            val file = File(dir.canonicalPath + node.fullPath)
            if (!file.parentFile.isDirectory && !file.parentFile.mkdirs()) {
                throw IOException("failed to create directory ${file.parentFile.canonicalPath}")
            }
            logger.debug("success create file ${file.canonicalPath}")
            inputStream.use { i ->
                FileOutputStream(file).use { o ->
                    i.copyTo(o)
                }
            }
        }

        if (!RepositoryCache.FileKey
            .isGitRepository(File(dir, DOT_GIT), FS.DETECTED)
        )
            throw ErrorCodeException(GitMessageCode.GIT_REPO_NOT_SYNC)
        return Git(
            RepositoryCache
                .open(
                    RepositoryCache.FileKey
                        .lenient(dir, FS.DETECTED)
                )
        )
    }

    /**
     * 存储.gir目录的文件
     * */
    fun storeGitDir(repository: Repository, context: ArtifactContext) {
        with(context) {
            val gitDir = repository.directory.canonicalPath
            logger.info("start store $gitDir")
            val filesToArchive: MutableList<File> = ArrayList()
            // 获取.git目录下所有的文件
            FileUtil.getAllFiles(File(gitDir), filesToArchive, true)

            for (file in filesToArchive) {
                val (artifactFile, nodeCreateRequest) = buildFileAndNodeCreateRequest(
                    file,
                    gitDir, this
                )
                storageManager.storeArtifactFile(nodeCreateRequest, artifactFile, storageCredentials)
            }
            // 把远程分支更新到refs/heads,因为git clone默认取refs/heads的下的分支
            val remotes = Git(repository).branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call()
            for (remote in remotes) {
                val name = remote.name.replace(R_REMOTE_ORIGIN, R_HEADS)
                val updateRef = repository.updateRef(name)
                updateRef.setNewObjectId(remote.objectId)
                updateRef.forceUpdate()
                val rHeadFile = File("$gitDir/$name")
                val (artifactHeadFile, nodeCreateHeadRequest) = buildFileAndNodeCreateRequest(
                    rHeadFile,
                    gitDir, this
                )
                storageManager.storeArtifactFile(nodeCreateHeadRequest, artifactHeadFile, storageCredentials)
                logger.debug("success store head file ${rHeadFile.canonicalPath}")
            }
        }
    }

    /**
     * 检出文件
     * 先从缓存中查找文件，如果没有找到则从git中检出
     * */
    fun checkoutFileAndCreateNode(
        git: Git,
        gitContentArtifactInfo: GitContentArtifactInfo,
        context: ArtifactContext
    ): NodeDetail {
        with(context) {
            try {
                git.checkout()
                    .setStartPoint(gitContentArtifactInfo.objectId)
                    .addPath(gitContentArtifactInfo.path)
                    .call()
            } catch (e: Exception) {
                throw ErrorCodeException(
                    GitMessageCode.GIT_PATH_NOT_FOUND,
                    gitContentArtifactInfo.path!!, gitContentArtifactInfo.ref
                )
            }
            val workDir = git.repository.directory.parentFile.canonicalPath
            val filePath = "$workDir/${gitContentArtifactInfo.path}"
            val checkoutFile = File(filePath)
            if (!checkoutFile.exists()) {
                throw ErrorCodeException(
                    GitMessageCode.GIT_PATH_NOT_FOUND,
                    gitContentArtifactInfo.path!!, gitContentArtifactInfo.ref
                )
            }
            val (artifactFile, nodeCreateRequest) = buildFileAndNodeCreateRequest(checkoutFile, workDir, this)
            return storageManager.storeArtifactFile(nodeCreateRequest, artifactFile, context.storageCredentials)
        }
    }

    fun buildFileAndNodeCreateRequest(
        file: File,
        workDir: String,
        context: ArtifactContext
    ): Pair<ArtifactFile, NodeCreateRequest> {
        with(context) {
            // 因为目前生产上配置的CacheStorageService会move掉文件，所以这里拷贝一份用于CacheStorageService issues/446
            val cacheEnabled = properties.defaultStorageCredentials().cache.enabled
            val artifactFile: ArtifactFile = if (cacheEnabled && !storageService.exist(
                file.toArtifactFile()
                    .getFileSha256(),
                storageCredentials
            )
            ) {
                val dst = File(file.canonicalPath + "_tmp_#446")
                Files.copy(FileInputStream(file), Paths.get(dst.canonicalPath), StandardCopyOption.REPLACE_EXISTING)
                dst.toArtifactFile()
            } else {
                file.toArtifactFile()
            }

            val gitArtifactInfo = artifactInfo as GitRepositoryArtifactInfo
            // .git目录的文件，root path为src/.git，其他文件为src/。计算出文件的相对src的路径
            gitArtifactInfo.path = FileUtil.entryName(file, workDir)

            val nodeCreateRequest = NodeCreateRequest(
                projectId = projectId,
                repoName = repoName,
                folder = false,
                fullPath = artifactInfo.getArtifactFullPath(),
                size = artifactFile.getSize(),
                sha256 = artifactFile.getFileSha256(),
                md5 = artifactFile.getFileMd5(),
                overwrite = true,
                operator = userId
            )
            return Pair(artifactFile, nodeCreateRequest)
        }
    }
}
