package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.auth.service.BkInternalPermissionService
import com.tencent.devops.auth.service.iam.PermissionPostProcessor
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.util.CacheHelper
import com.tencent.devops.project.api.service.ServiceProjectResource
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Service
class RbacPermissionPostProcessor(
    val bkInternalPermissionService: BkInternalPermissionService,
    val meterRegistry: MeterRegistry,
    val client: Client
) : PermissionPostProcessor {

    private val project2StatusCache = CacheHelper.createCache<String, Boolean>(duration = 60)

    private fun consistencyCounter(method: String, isConsistent: Boolean): Counter {
        return Counter.builder("rbac.permission.consistency.check")
            .description("Counts the consistency checks between iam and internal permission services")
            .tag("method", method) // 标签：区分是哪个方法
            .tag("result", if (isConsistent) "consistent" else "inconsistent") // 标签：区分结果
            .register(meterRegistry)
    }

    val threadPoolExecutor = ThreadPoolExecutor(
        3,
        5,
        5,
        TimeUnit.SECONDS,
        LinkedBlockingQueue(1000),
        Executors.defaultThreadFactory(),
        ThreadPoolExecutor.DiscardPolicy()
    )

    override fun validateUserResourcePermission(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        action: String,
        externalApiResult: Boolean
    ) {
        threadPoolExecutor.execute {
            val localCheckResult = bkInternalPermissionService.validateUserResourcePermission(
                userId = userId,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                action = action
            )
            val isConsistent = (localCheckResult == externalApiResult)
            consistencyCounter(::validateUserResourcePermission.name, isConsistent).increment()
            if (!isConsistent) {
                logger.warn(
                    "Verification results are inconsistent: $userId|$projectCode|$resourceType" +
                        "|$resourceCode|$action|external=$externalApiResult|local=$localCheckResult"
                )
            }
        }
    }

    override fun batchValidateUserResourcePermission(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        actions: List<String>,
        externalApiResult: Map<String, Boolean>
    ) {
        externalApiResult.forEach { (action, verify) ->
            validateUserResourcePermission(
                userId = userId,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                action = action,
                externalApiResult = verify
            )
        }
    }

    override fun getUserResourceByAction(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String,
        externalApiResult: List<String>
    ) {
        threadPoolExecutor.execute {
            val localResult = bkInternalPermissionService.getUserResourceByAction(
                userId = userId,
                projectCode = projectCode,
                resourceType = resourceType,
                action = action
            )
            val isConsistent = (externalApiResult.toSet() == localResult.toSet())
            consistencyCounter(::getUserResourceByAction.name, isConsistent).increment()
            if (!isConsistent) {
                logger.warn(
                    "get user resource by action results are inconsistent: $userId|" +
                        "$projectCode|$resourceType|$action|external=$externalApiResult|local=$localResult"
                )
            }
        }
    }

    override fun getUserProjectsByAction(
        userId: String,
        action: String,
        externalApiResult: List<String>
    ) {
        threadPoolExecutor.execute {
            val localResult = bkInternalPermissionService.getUserProjectsByAction(
                userId = userId,
                action = action
            )
            val method = ::getUserProjectsByAction.name
            val diffProjects = externalApiResult.filterNot { localResult.contains(it) }
            if (diffProjects.isEmpty()) {
                consistencyCounter(method, true).increment()
            } else {
                // 由于只同步了未禁用的项目权限数据，所以需要对差异项目，进行项目是否禁用检查
                // 若差异项目中存在未被禁用项目，则说结果有差异
                val isExistProjectEnabled = diffProjects.any {
                    CacheHelper.getOrLoad(project2StatusCache, it) {
                        client.get(ServiceProjectResource::class).get(it).data?.enabled ?: false
                    }
                }
                if (isExistProjectEnabled){
                    logger.warn("get user projects by action results are inconsistent:$userId|$action|$diffProjects")
                }
                consistencyCounter(method, !isExistProjectEnabled).increment()
            }
        }
    }

    override fun filterUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String,
        resourceCodes: List<String>,
        externalApiResult: Map<AuthPermission, List<String>>
    ) {
        threadPoolExecutor.execute {
            val localResult = bkInternalPermissionService.filterUserResourcesByActions(
                userId = userId,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCodes = resourceCodes,
                actions = actions
            )

            // 整体比较可能复杂，可以比较每个permission的结果
            externalApiResult.forEach { (permission, resources) ->
                val localResources = localResult[permission]?.toSet()
                val externalApiResources = resources.toSet()

                val isConsistent = localResources == externalApiResources
                // 可以在标签中加入更详细的维度，比如 permission
                consistencyCounter(::filterUserResourcesByActions.name, isConsistent).increment()

                if (!isConsistent) {
                    logger.warn(
                        "filter user resources by actions results are inconsistent for permission:" +
                            "$permission|$userId|$projectCode|$resourceType" +
                            "|external=$externalApiResources|local=$localResources"
                    )
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionPostProcessor::class.java)
    }
}
