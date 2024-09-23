package com.tencent.devops.auth.service.self

import com.tencent.devops.auth.pojo.UserOauthInfo
import com.tencent.devops.auth.pojo.enum.OauthType
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.repository.pojo.RepoOauthRefVo

/**
 * 用户OAUTH资源接口
 */
interface OauthService {
    /**
     * 获取授权信息
     */
    fun get(userId: String, projectId: String): UserOauthInfo?

    /**
     * 授权代码库列表
     */
    fun relRepo(userId: String, projectId: String, page: Int, pageSize: Int): Page<RepoOauthRefVo>

    /**
     * 删除授权
     */
    fun delete(userId: String, projectId: String)

    /**
     * 重置授权
     */
    fun reOauth(userId: String)
}