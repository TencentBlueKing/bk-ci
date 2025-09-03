package com.tencent.devops.repository.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.dao.RepositoryConfigDeptDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SimpleRepositoryConfigPermissionService @Autowired constructor(
    override val dslContext: DSLContext,
    override val repositoryConfigDeptDao: RepositoryConfigDeptDao,
    override val client: Client
) : RepositoryConfigDeptService(
    dslContext = dslContext,
    repositoryConfigDeptDao = repositoryConfigDeptDao,
    client = client
) {
    /**
     * 开源版没有组织信息
     */
    override fun getUserDeptList(userId: String): List<Int> = listOf()
}