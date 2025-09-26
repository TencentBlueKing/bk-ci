package com.tencent.devops.repository.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.repository.dao.RepositoryConfigVisibilityDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
@Service
class TencentRepositoryConfigVisibilityService @Autowired constructor(
    override val dslContext: DSLContext,
    override val repositoryConfigVisibilityDao: RepositoryConfigVisibilityDao,
    override val client: Client
) : RepositoryConfigVisibilityService(
    dslContext = dslContext,
    repositoryConfigVisibilityDao = repositoryConfigVisibilityDao,
    client = client
) {

    override fun getUserDeptList(userId: String): List<Int> {
        val userInfo = client.get(ServiceTxUserResource::class).get(userId).data
        return if (userInfo == null) {
            listOf(0, 0, 0, 0)
        } else {
            val list = mutableListOf(
                userInfo.bgId.toInt(),
                userInfo.deptId.toInt(),
                userInfo.centerId.toInt(),
                userInfo.groupId.toInt()
            )
            userInfo.businessLineId?.let { list.add(it.toInt()) }
            list
        }
    }
}