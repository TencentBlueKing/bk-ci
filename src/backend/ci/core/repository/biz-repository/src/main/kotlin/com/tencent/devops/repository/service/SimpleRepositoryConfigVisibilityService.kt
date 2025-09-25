package com.tencent.devops.repository.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.dao.RepositoryConfigVisibilityDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SimpleRepositoryConfigVisibilityService @Autowired constructor(
    override val dslContext: DSLContext,
    override val repositoryConfigVisibilityDao: RepositoryConfigVisibilityDao,
    override val client: Client
) : RepositoryConfigVisibilityService(
    dslContext = dslContext,
    repositoryConfigVisibilityDao = repositoryConfigVisibilityDao,
    client = client
) {
    /**
     * 开源版没有组织信息
     */
    override fun getUserDeptList(userId: String): List<Int> = listOf()
}