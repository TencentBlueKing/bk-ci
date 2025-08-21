package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.auth.dao.AuthResourceDao
import com.tencent.devops.auth.service.BkInternalPermissionCache
import com.tencent.devops.auth.service.BkInternalPermissionService
import com.tencent.devops.common.auth.api.AuthPermission
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
 * 权限协调器，协调实际状态与期望状态，用于熔断前置准备
 *
 * 核心职责：
 * 1. **权限一致性检查**：比对本地缓存与外部权限服务（如IAM）的权限校验结果
 * 2. **自动修复机制**：检测到不一致时自动清除缓存触发重建
 * 3. **监控指标上报**：通过Micrometer上报一致性校验结果指标
 *
 * 关键特性：
 * - 异步处理：通过线程池隔离校验任务，避免阻塞主流程
 * - 缓存优化：采用多级缓存策略减少外部调用
 * - 差异分析：记录不一致的详细差异信息
 **/
@Service
class BkInternalPermissionReconciler(
    val bkInternalPermissionService: BkInternalPermissionService,
    val meterRegistry: MeterRegistry,
    val client: Client,
    val redisOperation: RedisOperation,
    val authResourceService: AuthResourceDao,
    val dslContext: DSLContext
) {

    private val project2StatusCache = CacheHelper.createCache<String, Boolean>(duration = 60)

    @Value("\${permission.comparator.pool.size:#{null}}")
    private val corePoolSize: Int? = null

    private fun consistencyCounter(method: String, isConsistent: Boolean): Counter {
        return Counter.builder("rbac.permission.consistency.check")
            .description("Counts the consistency checks between iam and internal permission services")
            .tag("method", method) // 标签：区分是哪个方法
            .tag("result", if (isConsistent) "consistent" else "inconsistent") // 标签：区分结果
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
            consistencyCounter(::validateUserResourcePermission.name, isConsistent).increment()
            if (!isConsistent) {
                val (fixResourceType, fixResourceCode) = bkInternalPermissionService.buildFixResourceTypeAndCode(
                    projectCode = projectCode,
                    resourceType = resourceType,
                    resourceCode = resourceCode
                )
                // 缓存不一致，进行销毁
                BkInternalPermissionCache.invalidatePermission(
                    projectCode = projectCode,
                    resourceType = fixResourceType,
                    resourceCode = fixResourceCode,
                    userId = userId,
                    action = action
                )
                logger.warn(
                    "Verification results are inconsistent: $userId|$projectCode|$resourceType" +
                        "|$resourceCode|$action|external=$expectedResult|local=$localCheckResult"
                )
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
            consistencyCounter(::getUserResourceByAction.name, isConsistent).increment()
            if (!isConsistent) {
                // 计算差异项
                val externalOnly = expectedSet - localSet  // external有但local无的项
                val localOnly = localSet - expectedSet     // local有但external无的项
                BkInternalPermissionCache.invalidateUserResources(
                    userId = userId,
                    projectCode = projectCode,
                    resourceType = resourceType,
                    action = action
                )
                logger.warn(
                    """
                get user resource by action results are inconsistent: 
                userId=$userId|projectCode=$projectCode|resourceType=$resourceType|action=$action
                ===== 差异项详情 =====
                external独有项: ${externalOnly.joinToString()}
                local独有项: ${localOnly.joinToString()}
                ===== 完整数据 =====
                external=$expectedResult
                local=$localResult
                """.trimIndent()
                )
            }
        }
    }

    fun getUserProjectsByAction(
        userId: String,
        action: String,
        expectedResult: List<String>
    ) {
        postProcess {
            val localResult = bkInternalPermissionService.getUserProjectsByAction(
                userId = userId,
                action = action
            )
            val method = ::getUserProjectsByAction.name

            val localResultSet = localResult.toSet()
            val expectedResultSet = expectedResult.toSet()

            if (expectedResultSet == localResultSet) {
                consistencyCounter(method, true).increment()
                return@postProcess // 提前返回，后续代码无需执行
            }

            val diffProjects = expectedResult.filterNot { localResultSet.contains(it) }
            if (diffProjects.isEmpty()) {
                consistencyCounter(method, true).increment()
                return@postProcess
            }

            val inconsistentProjectCodes = diffProjects.filterNot { projectCode ->
                val isEnabled = CacheHelper.getOrLoad(project2StatusCache, projectCode) {
                    client.get(ServiceProjectResource::class).get(projectCode).data?.enabled ?: false
                }
                val hasNoActiveMembership by lazy {
                    bkInternalPermissionService.listMemberGroupIdsInProjectWithCache(
                        projectCode = projectCode,
                        userId = userId,
                        enableTemplateInvalidationOnUserExpiry = true
                    ).isEmpty()
                }
                !isEnabled || hasNoActiveMembership
            }

            val isConsistent = inconsistentProjectCodes.isEmpty()
            if (!isConsistent) {
                BkInternalPermissionCache.invalidateUserProjects(
                    userId = userId,
                    action = action
                )
                logger.warn(
                    "get user projects by action results are inconsistent: " +
                        "userId=$userId, action=$action, initialDiff=$diffProjects, " +
                        "finalInconsistent=$inconsistentProjectCodes"
                )
            }
            consistencyCounter(method, isConsistent).increment()
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

            // 整体比较可能复杂，可以比较每个permission的结果
            expectedResult.forEach { (permission, resources) ->
                val localResources = localResult[permission]?.toSet() ?: emptySet()
                val externalApiResources = resources.toSet()

                val isConsistent = localResources == externalApiResources
                // 可以在标签中加入更详细的维度，比如 permission
                consistencyCounter(::filterUserResourcesByActions.name, isConsistent).increment()

                if (!isConsistent) {
                    // 计算差异项
                    val externalOnly = externalApiResources - localResources  // external有但local无的项
                    val localOnly = localResources - externalApiResources     // local有但external无的项
                    val action = "${resourceType}_${permission.value}"
                    BkInternalPermissionCache.invalidateUserResources(
                        userId = userId,
                        projectCode = projectCode,
                        resourceType = resourceType,
                        action = action
                    )
                    logger.warn(
                        """
                        filter user resources by actions are inconsistent: 
                        userId=$userId|projectCode=$projectCode|resourceType=$resourceType|action=$permission
                        ===== 差异项详情 =====
                        external独有项: ${externalOnly.joinToString()}
                        local独有项: ${localOnly.joinToString()}
                        ===== 完整数据 =====
                        external=$expectedResult
                        local=$localResult
                        """.trimIndent()
                    )
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkInternalPermissionReconciler::class.java)
        private const val PERMISSION_POST_PROCESSOR_CONTROL = "permission:post:processor:control"
    }
}
