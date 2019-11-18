package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.pojo.CombinationPath
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.PathList
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import java.io.InputStream

interface CustomDirService {
    fun list(userId: String, projectId: String, argPath: String): List<FileInfo>
    fun show(userId: String, projectId: String, argPath: String): FileDetail
    fun deploy(userId: String, projectId: String, argPath: String, inputStream: InputStream, disposition: FormDataContentDisposition)
    fun mkdir(userId: String, projectId: String, argPath: String)
    fun rename(userId: String, projectId: String, argSrcPath: String, argDestPath: String)
    fun copy(userId: String, projectId: String, combinationPath: CombinationPath)
    fun move(userId: String, projectId: String, combinationPath: CombinationPath)
    fun delete(userId: String, projectId: String, pathList: PathList)
    fun validatePermission(userId: String, projectId: String)
    fun isProjectUser(user: String, projectId: String): Boolean
}