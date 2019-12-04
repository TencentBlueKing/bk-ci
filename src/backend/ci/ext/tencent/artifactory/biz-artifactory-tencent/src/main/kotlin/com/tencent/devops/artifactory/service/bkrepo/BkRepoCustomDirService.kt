/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.artifactory.service.bkrepo

import com.tencent.devops.artifactory.client.BkRepoClient
import com.tencent.devops.artifactory.pojo.CombinationPath
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.PathList
import com.tencent.devops.artifactory.service.CustomDirService
import com.tencent.devops.artifactory.util.JFrogUtil
import com.tencent.devops.artifactory.util.PathUtils
import com.tencent.devops.artifactory.util.RepoUtils
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.BSAuthProjectApi
import com.tencent.devops.common.auth.code.BSRepoAuthServiceCode
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.InputStream
import javax.ws.rs.BadRequestException
import javax.ws.rs.NotFoundException

@Service
class BkRepoCustomDirService @Autowired constructor(
    private val authProjectApi: BSAuthProjectApi,
    private val bkRepoClient: BkRepoClient,
    private val artifactoryAuthServiceCode: BSRepoAuthServiceCode
) : CustomDirService {
    override fun list(userId: String, projectId: String, path: String): List<FileInfo> {
        logger.info("list, userId: $userId, projectId: $projectId, path: $path")
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)
        val fileList = bkRepoClient.listFile(
            userId,
            projectId,
            RepoUtils.CUSTOM_REPO,
            normalizedPath,
            includeFolders = true,
            deep = false
        ).map {
            RepoUtils.toFileInfo(it)
        }
        return JFrogUtil.sort(fileList)
    }

    override fun show(userId: String, projectId: String, path: String): FileDetail {
        logger.info("show, userId: $userId, projectId: $projectId, path: $path")
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)
        val fileDetail = bkRepoClient.getFileDetail(
            userId,
            projectId,
            RepoUtils.CUSTOM_REPO,
            normalizedPath
        ) ?: throw NotFoundException("文件不存在")
        return RepoUtils.toFileDetail(fileDetail)
    }

    override fun deploy(userId: String, projectId: String, argPath: String, inputStream: InputStream, disposition: FormDataContentDisposition) {
        logger.info("deploy file, userId: $userId, projectId: $projectId, path: $argPath")
        bkRepoClient.uploadFile(userId, projectId, RepoUtils.CUSTOM_REPO, argPath, inputStream)
    }

    override fun mkdir(userId: String, projectId: String, path: String) {
        logger.info("mkdir, userId: $userId, projectId: $projectId, path: $path")
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)
        bkRepoClient.mkdir(userId, projectId, RepoUtils.CUSTOM_REPO, normalizedPath)
    }

    override fun rename(userId: String, projectId: String, fromPath: String, toPath: String) {
        logger.info("rename, userId: $userId, projectId: $projectId, srcPath: $fromPath, toPath: $toPath")
        val normalizedFromPath = PathUtils.checkAndNormalizeAbsPath(fromPath)
        val normalizedToPath = PathUtils.checkAndNormalizeAbsPath(toPath)
        bkRepoClient.rename(userId, projectId, RepoUtils.CUSTOM_REPO, normalizedFromPath, normalizedToPath)
    }

    override fun copy(userId: String, projectId: String, combinationPath: CombinationPath) {
        logger.info("copy, userId: $userId, projectId: $projectId, combinationPath: $combinationPath")
        val normalizeDestPath = PathUtils.checkAndNormalizeAbsPath(combinationPath.destPath)

        if (combinationPath.srcPaths.size > 1) {
            val destFileInfo = bkRepoClient.getFileDetail(userId, projectId, RepoUtils.CUSTOM_REPO, normalizeDestPath)
            if (destFileInfo != null && !destFileInfo.nodeInfo.folder) {
                throw OperationException("目标路径应为文件夹")
            }
        }

        combinationPath.srcPaths.map { srcPath ->
            val normalizedSrcPath = JFrogUtil.normalize(srcPath)
            if (JFrogUtil.getParentFolder(normalizedSrcPath) == normalizeDestPath) {
                logger.error("Cannot copy in same path ($normalizedSrcPath, $normalizeDestPath)")
                throw BadRequestException("不能在拷贝到当前目录")
            }

            bkRepoClient.copy(
                "",
                projectId,
                RepoUtils.CUSTOM_REPO,
                normalizedSrcPath,
                projectId,
                RepoUtils.CUSTOM_REPO,
                normalizeDestPath
            )
        }
    }

    override fun move(userId: String, projectId: String, combinationPath: CombinationPath) {
        logger.info("move, projectId: $projectId, combinationPath: $combinationPath")
        val normalizedDestPath = PathUtils.checkAndNormalizeAbsPath(combinationPath.destPath)

        combinationPath.srcPaths.map { srcPath ->
            val normalizedSrcPath = JFrogUtil.normalize(srcPath)

            if (normalizedSrcPath == normalizedDestPath ||
                JFrogUtil.getParentFolder(normalizedSrcPath) == normalizedDestPath) {
                logger.error("Cannot move in same path ($normalizedSrcPath, $normalizedDestPath)")
                throw BadRequestException("不能移动到当前目录")
            }

            if (normalizedDestPath.startsWith(normalizedSrcPath)) {
                logger.error("Cannot move parent path to sub path ($normalizedSrcPath, $normalizedDestPath)")
                throw BadRequestException("不能将父目录移动到子目录")
            }

            bkRepoClient.move(
                userId,
                projectId,
                RepoUtils.CUSTOM_REPO,
                normalizedSrcPath,
                normalizedDestPath
            )
        }
    }

    override fun delete(userId: String, projectId: String, pathList: PathList) {
        logger.info("delete, projectId: $projectId, pathList: $pathList")
        pathList.paths.map { path ->
            val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)
            bkRepoClient.delete(
                userId,
                projectId,
                RepoUtils.CUSTOM_REPO,
                normalizedPath
            )
        }
    }

    override fun validatePermission(userId: String, projectId: String) {
        if (!isProjectUser(userId, projectId)) {
            throw PermissionForbiddenException("用户($userId)不是工程($projectId)成员")
        }
    }

    override fun isProjectUser(user: String, projectId: String): Boolean {
        return authProjectApi.getProjectUsers(artifactoryAuthServiceCode, projectId).contains(user)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkRepoCustomDirService::class.java)
    }
}