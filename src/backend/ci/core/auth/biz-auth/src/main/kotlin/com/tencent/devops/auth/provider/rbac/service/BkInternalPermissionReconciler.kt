package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.auth.dao.AuthResourceDao
import com.tencent.devops.auth.service.BkInternalPermissionCache
import com.tencent.devops.auth.service.BkInternalPermissionService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupSyncService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.util.CacheHelper
import com.tencent.devops.project.api.service.ServiceProjectResource
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 权限协调器，协调实际状态与期望状态。
 *
 * 核心职责：
 * 1. **权限一致性检查**：异步比对本地与外部权限服务的校验结果。
 * 2. **双层修复机制**：
 *    - **即时修复**：检测到单点不一致时，立即清除相关缓存，强制下次请求重新计算。
 *    - **累积自愈**：当项目不一致报告累积达到阈值时，自动触发全量同步，修复深层数据问题。
 * 3. **监控指标上报**：通过Micrometer上报一致性校验结果指标。
 **/
@Service
class BkInternalPermissionReconciler(
    val bkInternalPermissionService: BkInternalPermissionService,
    val meterRegistry: MeterRegistry,
    val client: Client,
    val redisOperation: RedisOperation,
    val authResourceService: AuthResourceDao,
    val dslContext: DSLContext,
    val permissionResourceGroupService: PermissionResourceGroupSyncService,
    val permissionResourceGroupPermissionService: PermissionResourceGroupPermissionService
) {

    private val project2StatusCache = CacheHelper.createCache<String, Boolean>(duration = 60)

    @Value("\${permission.comparator.pool.size:#{null}}")
    private val corePoolSize: Int? = null

    // 不一致性阈值，
    @Value("\${permission.comparator.inconsistency.threshold:100}")
    private val inconsistencyThreshold: Long = 100L

    private fun consistencyCounter(method: String, isConsistent: Boolean): Counter {
        return Counter.builder("rbac.permission.consistency.check")
            .description("Counts the consistency checks between iam and internal permission services")
            .tag("method", method) // 标签：区分是哪个方法
            .tag("result", if (isConsistent) "consistent" else "inconsistent") // 标签：区分结果
            .register(meterRegistry)
    }

    private fun threadPoolTasksRejectedCounter(): Counter {
        return Counter.builder("permission.thread.pool.tasks.rejected.count")
            .description("Counts the thread pool tasks rejected")
            .register(meterRegistry)
    }

    private val threadPoolExecutor = ThreadPoolExecutor(
        corePoolSize ?: 5,
        corePoolSize ?: 5,
        0,
        TimeUnit.SECONDS,
        LinkedBlockingQueue(500),
        Executors.defaultThreadFactory()
    ) { _, executor ->
        threadPoolTasksRejectedCounter().increment()
        logger.warn("Permission post processor task rejected. Pool status: {}", executor.toString())
    }

    private fun postProcess(action: () -> Unit) {
        try {
            val enabled = redisOperation.get(PERMISSION_POST_PROCESSOR_CONTROL)?.toBooleanStrictOrNull() ?: false
            if (!enabled) {
                return
            }
            threadPoolExecutor.execute { action() }
        } catch (ex: Exception) {
            logger.warn("Permission Post Processor failed", ex)
        }
    }

    /**
     * [核心] 统一处理不一致情况的核心方法 (累积自愈逻辑)
     */
    private fun handleInconsistency(
        projectCode: String,
        isConsistent: Boolean,
        methodName: String,
        reportIndicators: Boolean = true,
        details: () -> String
    ) {
        if (reportIndicators) {
            consistencyCounter(methodName, isConsistent).increment()
        }

        if (isConsistent) {
            return
        }

        logger.warn(details())

        val inconsistencyCountKey = "$INCONSISTENCY_COUNT_KEY_PREFIX$projectCode"
        val currentCount = redisOperation.increment(inconsistencyCountKey, 1)

        if (currentCount != null && currentCount >= inconsistencyThreshold) {
            logger.warn(
                "Project $projectCode inconsistency count reached threshold($inconsistencyThreshold). " +
                    "Triggering project permission sync."
            )
            try {
                permissionResourceGroupService.syncByCondition(
                    projectConditionDTO = ProjectConditionDTO(projectCodes = listOf(projectCode))
                )
                permissionResourceGroupPermissionService.syncPermissionsByCondition(
                    projectConditionDTO = ProjectConditionDTO(projectCodes = listOf(projectCode))
                )
                redisOperation.delete(inconsistencyCountKey)
                logger.info("Successfully synced project $projectCode and reset inconsistency count.")
            } catch (e: Exception) {
                logger.warn("Failed to sync project permissions for $projectCode after threshold reached", e)
            }
        }
    }

    fun validateUserResourcePermission(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        action: String,
        enableSuperManagerCheck: Boolean,
        expectedResult: Boolean
    ) {
        postProcess {
            val localCheckResult = bkInternalPermissionService.validateUserResourcePermission(
                userId = userId,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                action = action,
                enableSuperManagerCheck = enableSuperManagerCheck
            )
            val isConsistent = (localCheckResult == expectedResult)

            if (!isConsistent) {
                // 步骤1: 立即清理缓存 (即时修复)
                val (fixResourceType, fixResourceCode) = bkInternalPermissionService.buildFixResourceTypeAndCode(
                    projectCode = projectCode,
                    resourceType = resourceType,
                    resourceCode = resourceCode
                )
                BkInternalPermissionCache.invalidatePermission(
                    projectCode = projectCode,
                    resourceType = fixResourceType,
                    resourceCode = fixResourceCode,
                    userId = userId,
                    action = action
                )
            }

            // 步骤2: 调用统一处理器记录不一致，并累积计数 (累积自愈)
            handleInconsistency(
                projectCode = projectCode,
                isConsistent = isConsistent,
                methodName = ::validateUserResourcePermission.name
            ) {
                "Verification results are inconsistent: $userId|$projectCode|$resourceType" +
                    "|$resourceCode|$action|external=$expectedResult|local=$localCheckResult"
            }
        }
    }

    fun batchValidateUserResourcePermission(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        actions: List<String>,
        expectedResult: Map<String, Boolean>
    ) {
        expectedResult.forEach { (action, verify) ->
            validateUserResourcePermission(
                userId = userId,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                action = action,
                expectedResult = verify,
                enableSuperManagerCheck = false
            )
        }
    }

    fun getUserResourceByAction(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String,
        expectedResult: List<String>
    ) {
        postProcess {
            val localResult = bkInternalPermissionService.getUserResourceByAction(
                userId = userId,
                projectCode = projectCode,
                resourceType = resourceType,
                action = action
            )
            val expectedSet = authResourceService.listByResourceCodes(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCodes = expectedResult.filterNot { it == "##NONE##" }.distinct(),
            ).map { it.resourceCode }.toSet()
            val localSet = authResourceService.listByResourceCodes(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCodes = localResult,
            ).map { it.resourceCode }.toSet()

            val isConsistent = (expectedSet == localSet)

            if (!isConsistent) {
                // 步骤1: 立即清理缓存
                BkInternalPermissionCache.invalidateUserResources(
                    userId = userId,
                    projectCode = projectCode,
                    resourceType = resourceType,
                    action = action
                )
            }

            // 步骤2: 调用统一处理器
            handleInconsistency(
                projectCode = projectCode,
                isConsistent = isConsistent,
                methodName = ::getUserResourceByAction.name
            ) {
                val externalOnly = expectedSet - localSet
                val localOnly = localSet - expectedSet
                """
                Get user resource by action results are inconsistent:
                userId=$userId|projectCode=$projectCode|resourceType=$resourceType|action=$action
                ===== 差异项详情 =====
                external独有项: ${externalOnly.joinToString()}
                local独有项: ${localOnly.joinToString()}
                """.trimIndent()
            }
        }
    }

    fun getUserProjectsByAction(
        userId: String,
        action: String,
        expectedResult: List<String>
    ) {
        postProcess {
            val localResult = bkInternalPermissionService.getUserProjectsByAction(userId, action)
            val localSet = localResult.toSet()
            val expectedSet = expectedResult.toSet()

            if (expectedSet == localSet) {
                consistencyCounter(::getUserProjectsByAction.name, true).increment()
                return@postProcess
            }

            val diffProjects = expectedSet - localSet
            if (diffProjects.isEmpty()) {
                consistencyCounter(::getUserProjectsByAction.name, true).increment()
                return@postProcess
            }

            val trulyInconsistentProjects = diffProjects.filterNot { projectCode ->
                val isEnabled = CacheHelper.getOrLoad(project2StatusCache, projectCode) {
                    client.get(ServiceProjectResource::class).get(projectCode).data?.enabled ?: false
                }
                !isEnabled || bkInternalPermissionService.listMemberGroupIdsInProjectWithCache(
                    projectCode = projectCode,
                    userId = userId,
                    enableTemplateInvalidationOnUserExpiry = true
                ).isEmpty()
            }

            val isOverallConsistent = trulyInconsistentProjects.isEmpty()
            consistencyCounter(::getUserProjectsByAction.name, isOverallConsistent).increment()

            if (!isOverallConsistent) {
                // 步骤1: 立即清理用户项目列表缓存
                BkInternalPermissionCache.invalidateUserProjects(userId = userId, action = action)

                // 步骤2: 对每个不一致的项目累积计数
                trulyInconsistentProjects.forEach { inconsistentProjectCode ->
                    handleInconsistency(
                        projectCode = inconsistentProjectCode,
                        isConsistent = false,
                        methodName = ::getUserProjectsByAction.name,
                        reportIndicators = false
                    ) {
                        "Project '$inconsistentProjectCode' identified as inconsistent in getUserProjectsByAction " +
                            "for user $userId. Initial diff: $diffProjects"
                    }
                }
            }
        }
    }

    fun filterUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String,
        resourceCodes: List<String>,
        expectedResult: Map<AuthPermission, List<String>>
    ) {
        postProcess {
            val localResult = bkInternalPermissionService.filterUserResourcesByActions(
                userId = userId,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCodes = resourceCodes,
                actions = actions
            )

            var isOverallConsistent = true
            val inconsistentDetails = StringBuilder()

            expectedResult.forEach { (permission, resources) ->
                val localResources = localResult[permission]?.toSet() ?: emptySet()
                val externalApiResources = resources.toSet()
                val isPermissionConsistent = localResources == externalApiResources
                consistencyCounter(::filterUserResourcesByActions.name, isPermissionConsistent).increment()

                if (!isPermissionConsistent) {
                    isOverallConsistent = false
                    // 步骤1: 对每个不一致的permission/action清理缓存
                    val action = "${resourceType}_${permission.value}"
                    BkInternalPermissionCache.invalidateUserResources(
                        userId = userId,
                        projectCode = projectCode,
                        resourceType = resourceType,
                        action = action
                    )

                    val externalOnly = externalApiResources - localResources
                    val localOnly = localResources - externalApiResources
                    inconsistentDetails.append(
                        """
                        |
                        |Permission '$permission' is inconsistent:
                        |  external独有项: ${externalOnly.joinToString()}
                        |  local独有项: ${localOnly.joinToString()}
                        """.trimMargin()
                    )
                }
            }

            // 步骤2: 对整个项目的对比结果进行一次统一处理
            handleInconsistency(
                projectCode = projectCode,
                isConsistent = isOverallConsistent,
                reportIndicators = false,
                methodName = ::filterUserResourcesByActions.name
            ) {
                """
                Filter user resources by actions are inconsistent:
                userId=$userId|projectCode=$projectCode|resourceType=$resourceType
                ===== 差异项详情 =====
                $inconsistentDetails
                """.trimIndent()
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkInternalPermissionReconciler::class.java)
        private const val PERMISSION_POST_PROCESSOR_CONTROL = "permission:post:processor:control"
        private const val INCONSISTENCY_COUNT_KEY_PREFIX = "auth:permission:inconsistent:count:"
    }
}
