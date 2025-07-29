package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.auth.dao.AuthResourceDao
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

@Service
class BkInternalPermissionComparator(
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
        expectedResult: Boolean
    ) {
        postProcess {
            val localCheckResult = bkInternalPermissionService.validateUserResourcePermission(
                userId = userId,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                action = action
            )
            val isConsistent = (localCheckResult == expectedResult)
            consistencyCounter(::validateUserResourcePermission.name, isConsistent).increment()
            if (!isConsistent) {
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
                expectedResult = verify
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
                        userId = userId
                    ).isEmpty()
                }
                !isEnabled || hasNoActiveMembership
            }

            val isConsistent = inconsistentProjectCodes.isEmpty()
            if (!isConsistent) {
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
        private val logger = LoggerFactory.getLogger(BkInternalPermissionComparator::class.java)
        private const val PERMISSION_POST_PROCESSOR_CONTROL = "permission:post:processor:control"
    }
}
