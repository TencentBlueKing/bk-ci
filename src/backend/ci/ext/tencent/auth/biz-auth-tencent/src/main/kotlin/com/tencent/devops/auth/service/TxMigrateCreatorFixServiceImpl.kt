package com.tencent.devops.auth.service

import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.auth.service.iam.MigrateCreatorFixService
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.enums.AuthSystemType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.service.BkTag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class TxMigrateCreatorFixServiceImpl @Autowired constructor(
    private val deptService: DeptService,
    private val client: Client,
    private val tokenService: ClientTokenService,
    private val bkTag: BkTag
) : MigrateCreatorFixService {
    @Value("\${tag.prod:#{null}}")
    private val prodTag: String? = null

    override fun getProjectCreator(
        projectCode: String,
        authSystemType: AuthSystemType,
        projectCreator: String,
        projectUpdator: String?
    ): String? {
        return when {
            isUserExist(projectCreator) -> projectCreator
            projectUpdator != null && isUserExist(projectUpdator) -> {
                logger.warn(
                    "project creator has left, use project updater to migrate|" +
                        "$projectCode|${authSystemType.value}|$projectCreator|$projectUpdator"
                )
                projectUpdator
            }

            authSystemType == AuthSystemType.V0_AUTH_TYPE -> {
                val managers = bkTag.invokeByTag(prodTag) {
                    client.getGateway(ServiceProjectAuthResource::class).getProjectUsers(
                        token = tokenService.getSystemToken(null)!!,
                        projectCode = projectCode,
                        group = BkAuthGroup.MANAGER
                    ).data
                }
                logger.info("get project($projectCode) managers $managers")
                managers?.find { isUserExist(it) }.also {
                    logger.warn(
                        "project creator and updater has left, use project manager to migrate|" +
                            "$projectCode|${authSystemType.value}|$projectCreator|$projectUpdator|$it"
                    )
                }
            }

            else -> {
                logger.warn(
                    "project creator and updater has left, not found project manager to migrate|" +
                        "$projectCode|${authSystemType.value}|$projectCreator|$projectUpdator"
                )
                null
            }
        }
    }

    override fun getResourceCreator(
        projectCreator: String,
        resourceCreator: String
    ): String {
        return if (isUserExist(resourceCreator)) {
            resourceCreator
        } else {
            logger.warn(
                "resource creator has left, use project creator to migrate|$projectCreator|$resourceCreator"
            )
            projectCreator
        }
    }

    private fun isUserExist(name: String): Boolean =
        deptService.getUserInfo(userId = "admin", name = name) != null

    companion object {
        private val logger = LoggerFactory.getLogger(TxMigrateCreatorFixServiceImpl::class.java)
    }
}
