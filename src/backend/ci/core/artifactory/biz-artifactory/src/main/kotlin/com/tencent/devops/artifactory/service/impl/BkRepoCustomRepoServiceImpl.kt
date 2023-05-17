package com.tencent.devops.artifactory.service.impl

import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.devops.artifactory.pojo.DirNode
import com.tencent.devops.artifactory.service.CustomRepoService
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.constant.REPO_CUSTOM
import org.springframework.stereotype.Service
import java.util.StringTokenizer

@Service
class BkRepoCustomRepoServiceImpl(
    private val bkRepoClient: BkRepoClient
) : CustomRepoService {
    override fun dirTree(
        userId: String,
        projectId: String,
        path: String?,
        name: String?
    ): DirNode {
        return dirTreePage(userId, projectId, path, name, 1, 10000).records.first()
    }

    override fun dirTreePage(
        userId: String,
        projectId: String,
        path: String?,
        name: String?,
        page: Int,
        pageSize: Int
    ): Page<DirNode> {
        val queryData = bkRepoClient.listDir(
            userId = userId,
            projectId = projectId,
            repoName = REPO_CUSTOM,
            path = path,
            name = name,
            page = page,
            pageSize = pageSize
        )
        val dirList = queryData.records
        val rootDirNode = if (path.isNullOrBlank()) {
            DirNode("/", "/", mutableListOf())
        } else {
            DirNode(PathUtils.resolveName(path), PathUtils.normalizeFullPath(path), mutableListOf())
        }
        dirList.forEach { dir ->
            val s = StringTokenizer(dir.fullPath.removePrefix(rootDirNode.fullPath), "/")
            var current = rootDirNode
            while (s.hasMoreElements()) {
                val name = s.nextElement() as String
                var child = current.children.firstOrNull { it.name == name }
                if (child == null) {
                    child = DirNode(name, PathUtils.combineFullPath(current.fullPath, name), mutableListOf())
                    current.children.add(child)
                }
                current = child
            }
        }
        return Page(page, pageSize, queryData.totalRecords, listOf(rootDirNode))
    }
}
