package com.tencent.devops.auth.service.self

import com.tencent.devops.auth.pojo.OauthRelResource
import com.tencent.devops.auth.pojo.OauthResetUrl
import com.tencent.devops.auth.pojo.UserOauthInfo
import com.tencent.devops.common.api.pojo.Page

/**
 * 用户OAUTH资源接口
 */
interface OauthService {
    /**
     * 获取授权信息
     */
    fun get(userId: String, projectId: String): UserOauthInfo?

    /**
     * 授权信息关联的资源列表
     */
    fun relSource(userId: String, projectId: String, page: Int, pageSize: Int): Page<OauthRelResource>

    /**
     * 删除授权
     */
    fun delete(userId: String, projectId: String)

    /**
     * 重置授权
     */
    fun reOauth(userId: String): OauthResetUrl
}