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
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.TimeoutException

/**
 * 权限服务装饰器 (Decorator Pattern)
 */
class DelegatingPermissionServiceDecorator(
    private val rbacPermissionService: RbacPermissionService,
    private val bkInternalPermissionService: BkInternalPermissionService,
    private val routingStrategy: PermissionRoutingStrategy,
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val rbacCommonService: RbacCommonService,
    private val meterRegistry: MeterRegistry
) : PermissionService {
    fun circuitBreakerCounter(): Counter {
        return Counter.builder("permission.circuit.breaker.counter")
            .description("Counts the circuit breaker")
            .tag("method", "circuit breaker")
            .register(meterRegistry)
    }

    override fun validateUserActionPermission(userId: String, action: String): Boolean {
        // 此方法未使用过，暂不处理
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

    override fun validateUserResourcePermission(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String?
    ): Boolean {
        val actionInfo = rbacCommonService.getActionInfo(action)
        val relatedType = actionInfo.relatedResourceType

        val (resourceCode, finalResourceType) = if (relatedType == AuthResourceType.PROJECT.value) {
            projectCode to AuthResourceType.PROJECT.value
        } else {
            "*" to resourceType!!
        }

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

    override fun getUserProjectsByPermission(
        userId: String,
        action: String,
        resourceType: String?
    ): List<String> {
        return executeWithRouting(
            context = this::getUserProjectsByPermission.name,
            externalCall = {
                rbacPermissionService.getUserProjectsByPermission(
                    userId = userId,
                    action = action,
                    resourceType = resourceType
                )
            },
            internalCall = {
                val finalResourceType = if (resourceType == null) {
                    AuthResourceType.PROJECT
                } else {
                    AuthResourceType.get(resourceType)
                }
                val useAction = resolveInternalAction(action = action, resourceType = finalResourceType.value)
                bkInternalPermissionService.getUserProjectsByAction(
                    userId = userId,
                    action = useAction
                )
            }
        )
    }

    /**
     * 根据路由模式执行权限调用。
     * @param projectCode 项目ID，用于决定路由模式。
     * @param context 调用上下文（如方法名），用于日志记录。
     * @param externalCall 外部（新系统，如RBAC）的调用逻辑。
     * @param internalCall 内部调用逻辑，用于后台验证。
     */
    private fun <T> executeWithRouting(
        projectCode: String? = null,
        context: String,
        externalCall: () -> T,
        internalCall: () -> T
    ): T {
        val mode = projectCode?.let { routingStrategy.getModeForProject(it) } ?: routingStrategy.getDefaultMode()
        logger.debug(
            "execute with routing ,projectCode={},mode={},context={},externalCall={},internalCall={}",
            projectCode, context, mode, externalCall, internalCall
        )
        return when (mode) {
            RoutingMode.NORMAL -> externalCall()
            RoutingMode.INTERNAL -> internalCall()
            RoutingMode.CIRCUIT_BREAKER -> doWithCircuitBreaker(externalCall, internalCall, context)
        }
    }

    fun resolveInternalAction(action: String, resourceType: String): String {
        return if (!action.contains("_")) {
            RbacAuthUtils.buildAction(AuthPermission.get(action), AuthResourceType.get(resourceType))
        } else {
            action
        }
    }

    private fun <T> doWithCircuitBreaker(
        externalCall: () -> T,
        fallbackCall: () -> T,
        context: String
    ): T {
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker(AUTH_CIRCUIT_BREAKER_NAME)
        return try {
            circuitBreaker.executeCallable(externalCall)
        } catch (e: CallNotPermittedException) {
            logger.warn(
                "[AUTH_CIRCUIT_BREAKER_OPEN] Circuit breaker '{}' for context '{}' is open." +
                    " Falling back to internal call. Error: {}",
                e.causingCircuitBreakerName,
                context,
                e.message
            )
            circuitBreakerCounter().increment()
            fallbackCall()
        } catch (e: TimeoutException) {
            logger.error("[AUTH_TIMEOUT] External call timeout for context '$context'", e)
            fallbackCall()
        } catch (e: IOException) {
            logger.error("[AUTH_NETWORK_ERROR] Network issue for context '$context'", e)
            fallbackCall()
        } catch (e: Exception) {
            logger.error(
                "[AUTH_CALL_FAILED] Failed to execute external call for context '{}'. " +
                    "Falling back to internal call. Error: {}",
                context,
                e.message,
                e
            )
            fallbackCall()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DelegatingPermissionServiceDecorator::class.java)
        private const val AUTH_CIRCUIT_BREAKER_NAME = "AUTH_CIRCUIT_BREAKER"
    }
}
