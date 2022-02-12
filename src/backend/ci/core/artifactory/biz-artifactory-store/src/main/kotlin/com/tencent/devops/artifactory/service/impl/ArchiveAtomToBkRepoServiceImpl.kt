package com.tencent.devops.artifactory.service.impl

import com.tencent.devops.artifactory.client.bkrepo.DefaultBkRepoClient
import com.tencent.devops.artifactory.constant.BK_CI_ATOM_DIR
import com.tencent.devops.artifactory.constant.BK_CI_PLUGIN_FE_DIR
import com.tencent.devops.artifactory.constant.REALM_BK_REPO
import com.tencent.devops.artifactory.util.BkRepoUtils.BKREPO_DEFAULT_USER
import com.tencent.devops.artifactory.util.BkRepoUtils.BKREPO_STORE_PROJECT_ID
import com.tencent.devops.artifactory.util.BkRepoUtils.REPO_NAME_PLUGIN
import com.tencent.devops.artifactory.util.BkRepoUtils.REPO_NAME_STATIC
import com.tencent.devops.artifactory.util.DefaultPathUtils
import com.tencent.devops.common.api.constant.STATIC
import com.tencent.devops.common.api.exception.RemoteServiceException
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import javax.ws.rs.NotFoundException

@Service
@ConditionalOnProperty(prefix = "artifactory", name = ["realm"], havingValue = REALM_BK_REPO)
class ArchiveAtomToBkRepoServiceImpl(
    private val bkRepoClient: DefaultBkRepoClient
) : ArchiveAtomServiceImpl() {

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
        File(atomArchivePath).walk().filter { it.path != atomArchivePath }.forEach {
            val path = it.path.removePrefix("${getAtomArchiveBasePath()}/$BK_CI_ATOM_DIR")
            bkRepoClient.uploadLocalFile(
                userId = BKREPO_DEFAULT_USER,
                projectId = BKREPO_STORE_PROJECT_ID,
                repoName = REPO_NAME_PLUGIN,
                path = path,
                file = it
            )
        }
        File(frontendDir).walk().filter { it.path != frontendDir }.forEach {
            val path = it.path.removePrefix("${getAtomArchiveBasePath()}/$STATIC/$BK_CI_PLUGIN_FE_DIR")
            bkRepoClient.uploadLocalFile(
                userId = BKREPO_DEFAULT_USER,
                projectId = BKREPO_STORE_PROJECT_ID,
                repoName = REPO_NAME_STATIC,
                path = path,
                file = it
            )
        }
    }

    override fun getAtomFileContent(filePath: String): String {
        val tmpFile = DefaultPathUtils.randomFile()
        return try {
            bkRepoClient.downloadFile(
                userId = BKREPO_DEFAULT_USER,
                projectId = BKREPO_STORE_PROJECT_ID,
                repoName = REPO_NAME_PLUGIN,
                fullPath = filePath,
                destFile = tmpFile
            )
            tmpFile.readText(Charsets.UTF_8)
        } catch (e: NotFoundException) {
            logger.warn("file[$filePath] not exists")
            ""
        } catch (e: RemoteServiceException) {
            logger.warn("download file[$filePath] error: $e")
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

    companion object {
        private val logger = LoggerFactory.getLogger(ArchiveAtomToBkRepoServiceImpl::class.java)
    }
}
