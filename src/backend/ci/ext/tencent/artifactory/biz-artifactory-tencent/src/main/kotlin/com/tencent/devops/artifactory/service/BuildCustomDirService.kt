package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.pojo.CombinationPath
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.PathList

interface BuildCustomDirService {
    fun list(projectId: String, path: String): List<FileInfo>
    fun show(projectId: String, path: String): FileDetail
    fun mkdir(projectId: String, path: String)
    fun rename(projectId: String, fromPath: String, toPath: String)
    fun copy(projectId: String, combinationPath: CombinationPath)
    fun move(projectId: String, combinationPath: CombinationPath)
    fun delete(projectId: String, pathList: PathList)
}