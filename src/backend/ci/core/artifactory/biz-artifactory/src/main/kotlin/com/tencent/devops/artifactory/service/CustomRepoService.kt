package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.pojo.DirNode
import com.tencent.devops.common.api.pojo.Page

/**
 * 自定义仓库服务接口
 */
interface CustomRepoService {

    /**
     * 目录树
     * @param userId 用户id
     * @param projectId 项目id
     * @param path 路径
     * @param name 模糊查询的目录名
     */
    fun dirTree(userId: String, projectId: String, path: String?, name: String?): DirNode

    /**
     * 分页查询目录树
     * @param userId 用户id
     * @param projectId 项目id
     * @param path 路径
     * @param name 模糊查询的目录名
     * @param page 页数
     * @param pageSize 页大小
     */
    fun dirTreePage(
        userId: String,
        projectId: String,
        path: String?,
        name: String?,
        page: Int,
        pageSize: Int
    ): Page<DirNode>
}
