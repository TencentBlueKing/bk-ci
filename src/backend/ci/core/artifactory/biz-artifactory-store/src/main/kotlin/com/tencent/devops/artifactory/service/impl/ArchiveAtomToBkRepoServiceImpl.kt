package com.tencent.devops.artifactory.service.impl

import com.tencent.devops.artifactory.constant.BKREPO_DEFAULT_USER
import com.tencent.devops.artifactory.constant.BK_CI_ATOM_DIR
import com.tencent.devops.artifactory.constant.BK_CI_PLUGIN_FE_DIR
import com.tencent.devops.artifactory.constant.REPO_NAME_STATIC
import com.tencent.devops.artifactory.util.DefaultPathUtils
import com.tencent.devops.common.api.constant.STATIC
import com.tencent.devops.common.api.exception.RemoteServiceException
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import javax.ws.rs.NotFoundException

abstract class ArchiveAtomToBkRepoServiceImpl : ArchiveAtomServiceImpl() {

    override fun getAtomArchiveBasePath(): String {
        return System.getProperty("java.io.tmpdir")
    }

    override fun handleArchiveFile(
        disposition: FormDataContentDisposition,
        inputStream: InputStream,
        projectCode: String,
        atomCode: String,
        version: String
    ) {
        unzipFile(
            disposition = disposition,
            inputStream = inputStream,
            projectCode = projectCode,
            atomCode = atomCode,
            version = version
        )
        val atomArchivePath = buildAtomArchivePath(projectCode, atomCode, version)
        val frontendDir = buildAtomFrontendPath(atomCode, version)
        logger.info("atom plugin: $atomArchivePath, $frontendDir")

        directoryIteration(
            directoryFile = File(atomArchivePath),
            prefix = "${getAtomArchiveBasePath()}/$BK_CI_ATOM_DIR",
            directoryPath = atomArchivePath,
            repoName = getBkRepoName()
        )
        directoryIteration(
            directoryFile = File(frontendDir),
            prefix = "${getAtomArchiveBasePath()}/$STATIC/$BK_CI_PLUGIN_FE_DIR",
            directoryPath = frontendDir,
            repoName = REPO_NAME_STATIC
        )
    }

    private fun directoryIteration(directoryFile: File, prefix: String, directoryPath: String, repoName: String) {
        directoryFile.walk().filter { it.path != directoryPath }.forEach {
            if (it.isDirectory) {
                directoryIteration(
                    directoryFile = it,
                    prefix = prefix,
                    directoryPath = it.path,
                    repoName = repoName
                )
            } else {
                val path = it.path.removePrefix(prefix)
                logger.debug("uploadLocalFile fileName=${it.name}|path=$path")

                bkRepoClient.uploadLocalFile(
                    userId = BKREPO_DEFAULT_USER,
                    projectId = getBkRepoProjectId(),
                    repoName = repoName,
                    path = path,
                    file = it
                )
            }
        }
    }

    override fun getAtomFileContent(filePath: String): String {
        val tmpFile = DefaultPathUtils.randomFile()
        return try {
            bkRepoClient.downloadFile(
                userId = BKREPO_DEFAULT_USER,
                projectId = getBkRepoProjectId(),
                repoName = getBkRepoName(),
                fullPath = filePath,
                destFile = tmpFile
            )
            tmpFile.readText(Charsets.UTF_8)
        } catch (ignored: NotFoundException) {
            logger.warn("file[$filePath] not exists")
            ""
        } catch (ignored: RemoteServiceException) {
            logger.warn("download file[$filePath] error: $ignored")
            ""
        } finally {
            tmpFile.delete()
        }
    }

    override fun clearServerTmpFile(projectCode: String, atomCode: String, version: String) {
        val atomArchivePath = buildAtomArchivePath(projectCode, atomCode, version)
        val frontendDir = buildAtomFrontendPath(atomCode, version)
        File(atomArchivePath).deleteRecursively()
        File(frontendDir).deleteRecursively()
    }

    abstract fun getBkRepoProjectId(): String

    abstract fun getBkRepoName(): String

    companion object {
        private val logger = LoggerFactory.getLogger(ArchiveAtomToBkRepoServiceImpl::class.java)
    }
}
