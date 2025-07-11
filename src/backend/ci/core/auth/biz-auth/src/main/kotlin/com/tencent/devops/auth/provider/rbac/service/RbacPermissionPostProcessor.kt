package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.auth.service.BkInternalPermissionService
import com.tencent.devops.auth.service.iam.PermissionPostProcessor
import com.tencent.devops.common.auth.api.AuthPermission
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
    val meterRegistry: MeterRegistry
) : PermissionPostProcessor {
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
            consistencyCounter("validateUserResourcePermission", isConsistent).increment()
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
            consistencyCounter("getUserResourceByAction", isConsistent).increment()
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
        TODO("Not yet implemented")
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
                consistencyCounter("filterUserResourcesByActions", isConsistent).increment()

                if (!isConsistent) {
                    logger.warn(
                        "filter user resources by actions results are inconsistent for permission '$permission': $userId|" +
                            "$projectCode|$resourceType|external=$externalApiResources|local=$localResources"
                    )
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionPostProcessor::class.java)
    }
}
