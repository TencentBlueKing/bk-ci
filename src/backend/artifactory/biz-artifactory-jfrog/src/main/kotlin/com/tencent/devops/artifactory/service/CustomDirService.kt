package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.pojo.CombinationPath
import com.tencent.devops.artifactory.pojo.FileChecksums
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.PathList
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.util.JFrogUtil
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.api.JFrogPropertiesApi
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_NAME
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.ArtifactoryAuthServiceCode
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.ws.rs.BadRequestException

@Service
class CustomDirService @Autowired constructor(
    private val authProjectApi: AuthProjectApi,
    private val jFrogPropertiesApi: JFrogPropertiesApi,
    private val pipelineService: PipelineService,
    private val jFrogService: JFrogService,
    private val artifactoryAuthServiceCode: ArtifactoryAuthServiceCode
) {
    fun list(userId: String, projectId: String, argPath: String): List<FileInfo> {
        validatePermission(userId, projectId)
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        val realPath = JFrogUtil.getCustomDirPath(projectId, path)
        val jFrogFileInfoList = jFrogService.list(realPath, false, 1)

        val fileInfoList = jFrogFileInfoList.map {
            val name = it.uri.removePrefix("/")
            val fullPath = JFrogUtil.compose(path, name, it.folder)
            FileInfo(
                name,
                fullPath,
                it.uri,
                fullPath,
                it.size,
                it.folder,
                LocalDateTime.parse(it.lastModified, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                ArtifactoryType.CUSTOM_DIR
            )
        }
        return JFrogUtil.sort(fileInfoList)
    }

    fun show(userId: String, projectId: String, argPath: String): FileDetail {
        validatePermission(userId, projectId)
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        // 项目目录不存在时，创建根目录
        val realPath = JFrogUtil.getCustomDirPath(projectId, path)
        if (JFrogUtil.isRoot(path) && !jFrogService.exist(realPath)) {
            jFrogService.mkdir(realPath)
        }

        val jFrogFileInfo = jFrogService.file(realPath)
        val jFrogProperties = jFrogPropertiesApi.getProperties(realPath)
        val jFrogPropertiesMap = mutableMapOf<String, String>()
        jFrogProperties.map {
            jFrogPropertiesMap[it.key] = it.value.joinToString(",")
        }
        if (jFrogProperties.containsKey(ARCHIVE_PROPS_PIPELINE_ID)) {
            val pipelineId = jFrogProperties[ARCHIVE_PROPS_PIPELINE_ID]!!.first()
            val pipelineName = pipelineService.getPipelineName(projectId, pipelineId)
            jFrogPropertiesMap[ARCHIVE_PROPS_PIPELINE_NAME] = pipelineName
        }

        return if (jFrogFileInfo.checksums == null) {
            FileDetail(
                name = JFrogUtil.getFileName(path),
                path = path,
                fullName = path,
                fullPath = path,
                size = jFrogFileInfo.size,
                createdTime = LocalDateTime.parse(jFrogFileInfo.created, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                modifiedTime = LocalDateTime.parse(
                    jFrogFileInfo.lastModified,
                    DateTimeFormatter.ISO_DATE_TIME
                ).timestamp(),
                checksums = FileChecksums("", "", ""),
                meta = jFrogPropertiesMap
            )
        } else {
            FileDetail(
                name = JFrogUtil.getFileName(path),
                path = path,
                fullName = path,
                fullPath = path,
                size = jFrogFileInfo.size,
                createdTime = LocalDateTime.parse(jFrogFileInfo.created, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                modifiedTime = LocalDateTime.parse(
                    jFrogFileInfo.lastModified,
                    DateTimeFormatter.ISO_DATE_TIME
                ).timestamp(),
                checksums = FileChecksums(
                    jFrogFileInfo.checksums.sha256,
                    jFrogFileInfo.checksums.sha1,
                    jFrogFileInfo.checksums.md5
                ),
                meta = jFrogPropertiesMap
            )
        }
    }

    fun deploy(
        userId: String,
        projectId: String,
        argPath: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ) {
        validatePermission(userId, projectId)
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        val folderPath = JFrogUtil.getCustomDirPath(projectId, path)
        if (!(JFrogUtil.isRoot(path) || jFrogService.exist(folderPath))) {
            logger.error("Destination path $path doesn't exist")
            throw BadRequestException("文件夹($path)不存在")
        }

        val fileName = String(disposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        val relativePath = JFrogUtil.compose(path, fileName)
        val realPath = JFrogUtil.getCustomDirPath(projectId, relativePath)
        if (jFrogService.exist(realPath)) {
            val detail = jFrogService.file(realPath)
            if (detail.checksums == null) {
                logger.error("Destination path $path has same name folder")
                throw BadRequestException("文件($fileName)已存在同名文件夹")
            }
        }

        val properties = mapOf(
            "userId" to userId,
            "projectId" to projectId
        )
        jFrogService.deploy(realPath, inputStream, properties)
    }

    fun mkdir(userId: String, projectId: String, argPath: String) {
        validatePermission(userId, projectId)
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        val name = JFrogUtil.getFileName(path)
        val folderPath = JFrogUtil.getCustomDirPath(projectId, path)
        if (jFrogService.exist(folderPath)) {
            val detail = jFrogService.file(folderPath)
            if (detail.checksums != null) {
                logger.error("Destination path $path has same name file")
                throw BadRequestException("文件($name)已存在同名文件")
            } else {
                logger.error("Destination path $path has same name folder")
                throw BadRequestException("文件($name)已存在同名文件夹")
            }
        }

        jFrogService.mkdir(folderPath)
    }

    fun rename(userId: String, projectId: String, argSrcPath: String, argDestPath: String) {
        validatePermission(userId, projectId)
        val srcPath = JFrogUtil.normalize(argSrcPath)
        val destPath = JFrogUtil.normalize(argDestPath)
        if (!JFrogUtil.isValid(srcPath) || !JFrogUtil.isValid(destPath)) {
            logger.error("Path $srcPath or $destPath is not valid")
            throw BadRequestException("非法路径")
        }

        val name = JFrogUtil.getFileName(destPath)
        val realSrcPath = JFrogUtil.getCustomDirPath(projectId, srcPath)
        val realDestPath = JFrogUtil.getCustomDirPath(projectId, destPath)
        if (jFrogService.exist(realDestPath)) {
            logger.error("Destination path $destPath already exist")
            throw OperationException("文件或者文件夹($name)已经存在")
        }

        jFrogService.move(realSrcPath, realDestPath)
    }

    fun copy(userId: String, projectId: String, bkCombinationPath: CombinationPath) {
        validatePermission(userId, projectId)
        val destPath = JFrogUtil.normalize(bkCombinationPath.destPath)
        if (!JFrogUtil.isValid(destPath)) {
            logger.error("Path $destPath is not valid")
            throw BadRequestException("非法路径")
        }

        val folderPath = JFrogUtil.getCustomDirPath(projectId, destPath)
        if (!jFrogService.exist(folderPath)) {
            logger.error("Destination path $destPath doesn't exist")
            throw BadRequestException("文件夹($destPath)不存在")
        }

        bkCombinationPath.srcPaths.map {
            val srcPath = JFrogUtil.normalize(it)
            if (!JFrogUtil.isValid(srcPath)) {
                logger.error("Path $srcPath is not valid")
                throw BadRequestException("非法路径")
            }

            if (JFrogUtil.getParentFolder(srcPath) == destPath) {
                logger.error("Cannot copy in same path ($srcPath, $destPath)")
                throw BadRequestException("不能在拷贝到当前目录")
            }

            val realSrcPath = JFrogUtil.getCustomDirPath(projectId, srcPath)
            if (!jFrogService.exist(realSrcPath)) {
                logger.error("Path $srcPath is not valid")
                throw BadRequestException("文件($srcPath)不存在")
            }

            val realDestPath = JFrogUtil.getCustomDirPath(projectId, destPath)
            jFrogService.copy(realSrcPath, realDestPath)
        }
    }

    fun move(userId: String, projectId: String, bkCombinationPath: CombinationPath) {
        validatePermission(userId, projectId)
        val destPath = JFrogUtil.normalize(bkCombinationPath.destPath)
        if (!JFrogUtil.isValid(destPath)) {
            logger.error("Path $destPath is not valid")
            throw BadRequestException("非法路径")
        }

        bkCombinationPath.srcPaths.map {
            val srcPath = JFrogUtil.normalize(it)
            if (!JFrogUtil.isValid(srcPath)) {
                logger.error("Path $srcPath is not valid")
                throw BadRequestException("非法路径")
            }

            if (srcPath == destPath || JFrogUtil.getParentFolder(srcPath) == destPath) {
                logger.error("Cannot move in same path ($srcPath, $destPath)")
                throw BadRequestException("不能移动到当前目录")
            }

            if (destPath.startsWith(srcPath)) {
                logger.error("Cannot move parent path to sub path ($srcPath, $destPath)")
                throw BadRequestException("不能将父目录移动到子目录")
            }

            val realSrcPath = JFrogUtil.getCustomDirPath(projectId, srcPath)
            val realDestPath = JFrogUtil.getCustomDirPath(projectId, destPath)
            jFrogService.move(realSrcPath, realDestPath)
        }
    }

    fun delete(userId: String, projectId: String, bkPathList: PathList) {
        validatePermission(userId, projectId)
        bkPathList.paths.map {
            val path = JFrogUtil.normalize(it)
            if (!JFrogUtil.isValid(path)) {
                logger.error("Path $path is not valid")
                throw BadRequestException("非法路径")
            }

            val realPath = JFrogUtil.getCustomDirPath(projectId, path)
            jFrogService.delete(realPath)
        }
    }

    fun validatePermission(userId: String, projectId: String) {
        if (!isProjectUser(userId, projectId)) {
            throw PermissionForbiddenException("用户($userId)不是工程($projectId)成员")
        }
    }

    fun isProjectUser(user: String, projectId: String): Boolean {
        return authProjectApi.getProjectUsers(artifactoryAuthServiceCode, projectId).contains(user)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CustomDirService::class.java)
    }
}