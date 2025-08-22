package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.auth.pojo.enum.RoutingMode
import com.tencent.devops.auth.service.BkInternalPermissionService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.auth.api.pojo.AuthResourceInstance
import com.tencent.devops.common.auth.rbac.utils.RbacAuthUtils
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 权限服务装饰器 (Decorator Pattern)
 */
class DelegatingPermissionServiceDecorator(
    private val rbacPermissionService: RbacPermissionService,
    private val bkInternalPermissionService: BkInternalPermissionService,
    private val routingStrategy: PermissionRoutingStrategy,
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val rbacCommonService: RbacCommonService
) : PermissionService {
    companion object {
        private val logger = LoggerFactory.getLogger(DelegatingPermissionServiceDecorator::class.java)
        private const val AUTH_CIRCUIT_BREAKER_NAME = "AUTH_CIRCUIT_BREAKER"
        private val threadPoolExecutor = ThreadPoolExecutor(
            5,
            5,
            0,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(500),
            Executors.defaultThreadFactory()
        ) { r, executor ->
            logger.warn(
                "Auth validation task rejected. Task: {}. Pool status: {}",
                r.toString(),
                executor.toString()
            )
        }
    }

    override fun validateUserActionPermission(userId: String, action: String): Boolean {
        // 此方法逻辑简单，保持不变
        return rbacPermissionService.validateUserActionPermission(userId = userId, action = action)
    }

    override fun validateUserProjectPermission(
        userId: String,
        projectCode: String,
        permission: AuthPermission
    ): Boolean {
        return executeWithRouting(
            projectCode = projectCode,
            context = this::validateUserProjectPermission.name,
            externalCall = {
                rbacPermissionService.validateUserProjectPermission(
                    userId = userId,
                    projectCode = projectCode,
                    permission = permission
                )
            },
            internalCall = {
                val action = RbacAuthUtils.buildAction(permission, AuthResourceType.PROJECT)
                bkInternalPermissionService.validateUserResourcePermission(
                    userId = userId,
                    projectCode = projectCode,
                    resourceType = ResourceTypeId.PROJECT,
                    resourceCode = projectCode,
                    action = action,
                    enableSuperManagerCheck = false
                )
            }
        )
    }

    override fun checkProjectManager(userId: String, projectCode: String): Boolean {
        return validateUserProjectPermission(
            userId = userId,
            projectCode = projectCode,
            permission = AuthPermission.MANAGE
        )
    }

    // 优化点 4: 简化方法调用链，移除了 validateUserResourcePermissionByRelation
    override fun validateUserResourcePermission(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String?
    ): Boolean {
        val actionInfo = rbacCommonService.getActionInfo(action)
        // 如果action关联的资源是项目,则直接查询项目的权限
        val resourceCode = if (actionInfo.relatedResourceType == AuthResourceType.PROJECT.value) {
            projectCode
        } else {
            "*"
        }
        val finalResourceType = resourceType ?: AuthResourceType.PROJECT.value

        val resource = rbacPermissionService.buildAuthResourceInstance(
            userId = userId,
            projectCode = projectCode,
            resourceCode = resourceCode,
            resourceType = finalResourceType
        )

        return validateUserResourcePermissionByInstance(
            userId = userId,
            action = action,
            projectCode = projectCode,
            resource = resource
        )
    }

    override fun validateUserResourcePermissionByRelation(
        userId: String,
        action: String,
        projectCode: String,
        resourceCode: String,
        resourceType: String,
        relationResourceType: String?
    ): Boolean {
        val resource = rbacPermissionService.buildAuthResourceInstance(
            userId = userId,
            projectCode = projectCode,
            resourceCode = resourceCode,
            resourceType = resourceType
        )
        return validateUserResourcePermissionByInstance(
            userId = userId,
            action = action,
            projectCode = projectCode,
            resource = resource
        )
    }

    override fun validateUserResourcePermissionByInstance(
        userId: String,
        action: String,
        projectCode: String,
        resource: AuthResourceInstance
    ): Boolean {
        return executeWithRouting(
            projectCode = projectCode,
            context = this::validateUserResourcePermissionByInstance.name,
            externalCall = {
                rbacPermissionService.validateUserResourcePermissionByInstance(
                    userId = userId,
                    action = action,
                    projectCode = projectCode,
                    resource = resource
                )
            },
            internalCall = {
                bkInternalPermissionService.validateUserResourcePermission(
                    userId = userId,
                    projectCode = projectCode,
                    resourceType = resource.resourceType,
                    resourceCode = resource.resourceCode,
                    action = resolveInternalAction(action, resource.resourceType),
                    enableSuperManagerCheck = true
                )
            }
        )
    }

    override fun batchValidateUserResourcePermission(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceCode: String,
        resourceType: String
    ): Map<String, Boolean> {
        val resource = rbacPermissionService.buildAuthResourceInstance(
            userId = userId,
            projectCode = projectCode,
            resourceCode = resourceCode,
            resourceType = resourceType
        )

        return batchValidateUserResourcePermissionByInstance(
            userId = userId,
            actions = actions,
            projectCode = projectCode,
            resource = resource
        )
    }

    override fun batchValidateUserResourcePermissionByInstance(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resource: AuthResourceInstance
    ): Map<String, Boolean> {
        return executeWithRouting(
            projectCode = projectCode,
            context = this::batchValidateUserResourcePermissionByInstance.name,
            externalCall = {
                rbacPermissionService.batchValidateUserResourcePermissionByInstance(
                    userId = userId,
                    projectCode = projectCode,
                    actions = actions,
                    resource = resource
                )
            },
            internalCall = {
                bkInternalPermissionService.batchValidateUserResourcePermission(
                    userId = userId,
                    projectCode = projectCode,
                    resourceType = resource.resourceType,
                    resourceCode = resource.resourceCode,
                    actions = actions
                )
            }
        )
    }

    override fun getUserResourceByAction(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): List<String> {
        return executeWithRouting(
            projectCode = projectCode,
            context = this::getUserResourceByAction.name,
            externalCall = {
                rbacPermissionService.getUserResourceByAction(
                    userId = userId,
                    projectCode = projectCode,
                    action = action,
                    resourceType = resourceType
                )
            },
            internalCall = {
                bkInternalPermissionService.getUserResourceByAction(
                    userId = userId,
                    projectCode = projectCode,
                    action = resolveInternalAction(action, resourceType),
                    resourceType = resourceType
                )
            }
        )
    }

    override fun getUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String
    ): Map<AuthPermission, List<String>> {
        val batchResult = executeWithRouting(
            projectCode = projectCode,
            context = this::getUserResourcesByActions.name,
            externalCall = {
                rbacPermissionService.getUserResourcesByActions(
                    userId = userId,
                    actions = actions,
                    projectCode = projectCode,
                    resourceType = resourceType
                )
            },
            internalCall = {
                bkInternalPermissionService.getUserResourcesByActions(
                    userId = userId,
                    actions = actions,
                    projectCode = projectCode,
                    resourceType = resourceType
                )
            }
        )
        return batchResult
    }

    override fun filterUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String,
        resources: List<AuthResourceInstance>
    ): Map<AuthPermission, List<String>> {
        return executeWithRouting(
            projectCode = projectCode,
            context = this::filterUserResourcesByActions.name,
            externalCall = {
                rbacPermissionService.filterUserResourcesByActions(
                    userId = userId,
                    projectCode = projectCode,
                    actions = actions,
                    resourceType = resourceType,
                    resources = resources
                )
            },
            internalCall = {
                bkInternalPermissionService.filterUserResourcesByActions(
                    userId = userId,
                    actions = actions,
                    projectCode = projectCode,
                    resourceType = resourceType,
                    resourceCodes = resources.map { it.resourceCode }
                )
            }
        )
    }

    override fun getUserResourceAndParentByPermission(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): Map<String, List<String>> {
        // 该方法目前没有调用，不做熔断
        return rbacPermissionService.getUserResourceAndParentByPermission(
            userId = userId,
            projectCode = projectCode,
            action = action,
            resourceType = resourceType
        )
    }

    /**
     * 根据路由模式执行权限调用。
     * @param projectCode 项目ID，用于决定路由模式。
     * @param context 调用上下文（如方法名），用于日志记录。
     * @param externalCall 外部（新系统，如RBAC）的调用逻辑，作为“真理之源”。
     * @param internalCall 内部（旧系统，如IAM）的调用逻辑，用于后台验证。
     */
    private fun <T> executeWithRouting(
        projectCode: String,
        context: String,
        externalCall: () -> T,
        internalCall: () -> T
    ): T {
        val mode = routingStrategy.getModeForProject(projectCode)
        return when (mode) {
            RoutingMode.NORMAL -> externalCall()
            RoutingMode.INTERNAl -> internalCall()
            RoutingMode.VALIDATION -> {
                val externalResult = externalCall()
                threadPoolExecutor.submit {
                    try {
                        val internalResult = internalCall()
                        // 比较基准（外部）结果与验证（内部）结果。
                        compareAndLogDifference(projectCode, context, externalResult, internalResult)
                    } catch (e: Exception) {
                        logger.warn(
                            "[AUTH_VALIDATION_ERROR] 后台验证内部调用失败。项目: '{}', 上下文: {}",
                            projectCode,
                            context,
                            e
                        )
                    }
                }
                externalResult
            }

            RoutingMode.CIRCUIT_BREAKER -> doWithCircuitBreaker(externalCall, internalCall)
        }
    }

    /**
     * 比较基准结果和验证结果，如果不一致则记录警告日志。
     * @param benchmarkResult 基准结果（在验证模式下是 externalResult）。
     * @param validationResult 用于验证的结果（在验证模式下是 internalResult）。
     */
    private fun <T> compareAndLogDifference(
        projectCode: String,
        context: String,
        benchmarkResult: T,
        validationResult: T
    ) {
        val isMismatch = when (benchmarkResult) {
            is List<*> -> benchmarkResult.toSet() != (validationResult as? List<*>)?.toSet()
            is Map<*, *>, is Boolean -> benchmarkResult != validationResult
            else -> benchmarkResult.toString() != validationResult.toString() // 兜底比较
        }

        if (isMismatch) {
            logger.warn(
                """
                [AUTH_VALIDATION_MISMATCH] 
                Permission validation mismatch found for project:'$projectCode' in context: '$context'
                - Benchmark Result (External): $benchmarkResult
                - Validation Result (Internal): $validationResult
                """.trimIndent()
            )
        }
    }

    private fun resolveInternalAction(action: String, resourceType: String): String {
        return if (!action.contains("_")) {
            RbacAuthUtils.buildAction(AuthPermission.get(action), AuthResourceType.get(resourceType))
        } else {
            action
        }
    }

    private fun <T> doWithCircuitBreaker(
        externalCall: () -> T,
        fallbackCall: () -> T
    ): T {
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker(AUTH_CIRCUIT_BREAKER_NAME)
        return try {
            circuitBreaker.executeCallable(externalCall)
        } catch (e: CallNotPermittedException) {
            logger.warn(
                "[AUTH]|AUTH_SERVER_ERROR|Circuit breaker '{}' is open. Falling back.", e.causingCircuitBreakerName
            )
            fallbackCall()
        }
    }
}
