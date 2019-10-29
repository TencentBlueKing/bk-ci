package com.tencent.devops.store.service.atom

import com.tencent.devops.common.api.pojo.Result

interface AtomRepositoryService {

    /**
     * 更改插件代码库的用户信息
     * @param userId 移交的用户ID
     * @param projectCode 项目代码
     * @param atomCode 插件代码
     */
    fun updateAtomRepositoryUserInfo(userId: String, projectCode: String, atomCode: String): Result<Boolean>
}