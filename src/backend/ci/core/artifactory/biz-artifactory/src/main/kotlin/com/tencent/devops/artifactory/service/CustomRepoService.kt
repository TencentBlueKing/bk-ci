package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.pojo.DirNode

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
}
