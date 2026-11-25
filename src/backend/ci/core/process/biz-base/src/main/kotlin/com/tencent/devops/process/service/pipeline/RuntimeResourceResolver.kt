/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.service.pipeline

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.setting.RuntimeResource
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.environment.api.thirdpartyagent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.process.utils.NODE_ENV_HASH_ID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 把流水线绑定的运行资源解析成一台可执行的构建机。
 *
 * 底层按 agentHashId 派发（见 `CreateAgentIdDispatchType`），而运行资源有两类：
 * 单体资源（数字分身 / 云桌面）绑定的就是一台机器，平台可以直接由工作空间名换出 agentHashId；
 * 环境资源（数字分身环境 / 云桌面环境）只圈定了范围，具体在哪台机器上执行由触发方选定后随启动参数传入，平台不代选。
 */
@Service
class RuntimeResourceResolver(
    private val client: Client
) {

    /**
     * 解析单体运行资源对应的 agentHashId；环境资源返回 null，因为节点应由触发方指定。
     *
     * 解析不出来时同样返回 null，由调用方决定是报错还是沿用已有的节点参数。
     */
    fun resolveAgentHashId(
        projectId: String,
        userId: String,
        resource: RuntimeResource
    ): String? = resolveSingleAgent(projectId, userId, resource).agentHashId

    /**
     * 同 [resolveAgentHashId]，但把「这台机器确实没了」和「这次没问到」区分开，
     * 判定资源是否已释放必须用它：查询失败时 [SingleAgentResolveResult.queried] 为 false，
     * 不能据此断定机器不存在，否则环境服务抖动会把正常的发布挡下来。
     */
    fun resolveSingleAgent(
        projectId: String,
        userId: String,
        resource: RuntimeResource
    ): SingleAgentResolveResult {
        if (resource.group) return SingleAgentResolveResult(queried = true, agentHashId = null)
        val workspaceName = resource.workspaceName
            ?: return SingleAgentResolveResult(queried = true, agentHashId = null)
        return try {
            SingleAgentResolveResult(
                queried = true,
                agentHashId = client.get(ServiceNodeResource::class).getNodeAgentDetailByWorkspaceName(
                    userId = userId,
                    projectId = projectId,
                    workspaceName = workspaceName
                ).data?.agentHashId?.takeIf(String::isNotBlank)
            )
        } catch (ignored: Exception) {
            logger.warn("resolve agent by workspace name failed|$projectId|$workspaceName", ignored)
            SingleAgentResolveResult(queried = false, agentHashId = null)
        }
    }

    /** 单体运行资源的解析结果，[queried] 为 false 表示这次没问到结论 */
    data class SingleAgentResolveResult(
        val queried: Boolean,
        val agentHashId: String?
    )

    /**
     * 数字分身环境 / 云桌面环境：列出环境下该用户可用的节点，供触发方挑选要执行的那一台。
     *
     * 结果按节点使用权限过滤：选出来的节点是要拿去启动构建的，挑一台用不了的机器只会在启动时失败。
     * 判断某台机器是不是还属于这个环境请用 [agentStillInEnvOrNull]，两者口径不同。
     *
     * 默认取全量，只在定时触发要逐台启动时才需要。只要一台时请传页码，不要为挑一台拉回整个环境。
     */
    fun listEnvAgentHashIds(
        projectId: String,
        userId: String,
        envHashId: String,
        page: Int = ALL_PAGE,
        pageSize: Int? = null
    ): List<String> = try {
        client.get(ServiceEnvironmentResource::class).listNodesByEnvIdsNew(
            projectId = projectId,
            envHashIds = listOf(envHashId),
            userId = userId,
            page = page,
            pageSize = pageSize
        ).data?.records?.mapNotNull { it.agentHashId?.takeIf(String::isNotBlank) }.orEmpty()
    } catch (ignored: Exception) {
        logger.warn("list env node failed|$projectId|$envHashId", ignored)
        emptyList()
    }

    /**
     * 列出环境下的全部节点，不按节点使用权限过滤，把「环境是空的」和「这次问不到」区分开：
     * 返回空列表代表环境确实没有节点（环境被清空，或环境本身已被删除），
     * 返回 null 代表查询本身失败，拿不到任何结论。
     *
     * 发布校验用它：一次要同时判断环境有没有节点、以及定时触发勾选的那几台还在不在，
     * 拿一份列表比逐台去问更划算。发布是低频操作，不在启动热路径上。
     * 启动链路只判断一台机器的归属，用 [agentStillInEnvOrNull]。
     *
     * 这里不能用 [listEnvAgentHashIds]：归属是客观事实，与查询人有没有这台机器的使用权无关，
     * 用带权限过滤的列表反推，会把「授权人没有该节点使用权」误判成「节点已被移出环境」。
     */
    fun listEnvAgentHashIdsForCheckOrNull(
        projectId: String,
        userId: String,
        envHashId: String
    ): List<String>? = try {
        client.get(ServiceNodeResource::class).listRawEnvAgentHashIds(
            userId = userId,
            projectId = projectId,
            envHashId = envHashId
        ).data?.filter { it.isNotBlank() }
    } catch (ignored: RemoteServiceException) {
        // 跨服务调用：环境服务的错误码被网关转成了数值
        envNotExistsOrNull(ignored.errorCode == ENV_NOT_EXISTS_CODE, projectId, envHashId, ignored)
    } catch (ignored: ErrorCodeException) {
        // 同进程直接调用到环境服务的实现时，拿到的是原始异常
        envNotExistsOrNull(
            ignored.errorCode == EnvironmentMessageCode.ERROR_ENV_NOT_EXISTS, projectId, envHashId, ignored
        )
    } catch (ignored: Exception) {
        logger.warn("list env node failed|$projectId|$envHashId", ignored)
        null
    }

    /**
     * 判断某台构建机是否仍属于这个环境，与 [listEnvAgentHashIdsForCheckOrNull] 同口径，
     * 但只问这一台的归属，不把环境下的全部节点拉回来再在内存里找。
     *
     * 启动链路每次构建都要判一次，定时触发还会按勾选的节点逐台启动，
     * 用全量列表反推的代价会随环境规模线性放大，这里固定只查这一台。
     *
     * 返回 false 代表这台机器确实不属于该环境（含环境已被删除），必须拒绝执行；
     * 返回 null 代表查询本身失败，拿不到结论，由调用方放行。
     */
    fun agentStillInEnvOrNull(
        projectId: String,
        userId: String,
        envHashId: String,
        agentHashId: String
    ): Boolean? = try {
        client.get(ServiceNodeResource::class).checkEnvAgentExists(
            userId = userId,
            projectId = projectId,
            envHashId = envHashId,
            agentHashId = agentHashId
        ).data
    } catch (ignored: RemoteServiceException) {
        // 跨服务调用：环境服务的错误码被网关转成了数值
        agentNotInEnvOrNull(ignored.errorCode == ENV_NOT_EXISTS_CODE, projectId, envHashId, ignored)
    } catch (ignored: ErrorCodeException) {
        // 同进程直接调用到环境服务的实现时，拿到的是原始异常
        agentNotInEnvOrNull(
            ignored.errorCode == EnvironmentMessageCode.ERROR_ENV_NOT_EXISTS, projectId, envHashId, ignored
        )
    } catch (ignored: Exception) {
        logger.warn("check env agent failed|$projectId|$envHashId|$agentHashId", ignored)
        null
    }

    /**
     * 取环境名用于报错展示。环境可能来自历史构建参数，流水线设置里的名字未必对得上，只能按 hashId 现查。
     * 查不到就返回 null，由调用方退回展示 hashId。
     */
    fun getEnvName(
        projectId: String,
        userId: String,
        envHashId: String
    ): String? = try {
        client.get(ServiceEnvironmentResource::class).get(
            userId = userId,
            projectId = projectId,
            envHashId = envHashId,
            checkPermission = false
        ).data?.name?.takeIf(String::isNotBlank)
    } catch (ignored: Exception) {
        logger.warn("get env name failed|$projectId|$envHashId", ignored)
        null
    }

    /**
     * 判断一次历史构建记录的运行资源，是否仍是流水线当前绑定的那个。
     *
     * 只拦环境被换掉这一种：环境资源的节点是从环境里选出来的，换了环境之后原来那台机器
     * 就不再属于这条流水线该跑的范围，重跑没有意义，调用方按「已不满足最新版本的触发条件」拦下。
     *
     * 单体资源不拦。绑的是哪台机器是那一次构建的既成事实，流水线后来改绑了别的机器，
     * 不影响把原来那次在原机器上重跑一遍（见 PipelineBuildService.initNodeAgentParams）；
     * 原机器确实没了的情况由启动流程报「找不到可用的执行机器」，比笼统的「触发条件不满足」准确。
     *
     * 同理，机器还在原环境里、只是被移出环境或被删除，这里也放行，交给启动流程报「不在环境内」。
     * 没绑运行资源、启动参数里没有环境时同样放行：没有证据不做判定。
     */
    fun stillMatchRuntimeResource(
        projectId: String,
        userId: String,
        setting: PipelineSetting?,
        startParameters: Map<String, String>
    ): Boolean {
        val resource = setting?.runtimeResource() ?: return true
        if (!resource.group) return true
        // 环境类资源只比环境：原构建记下的环境和当前绑定的环境是不是同一个
        val originEnvHashId = startParameters[NODE_ENV_HASH_ID]?.takeIf { it.isNotBlank() } ?: return true
        return originEnvHashId == resource.envHashId
    }

    /**
     * 取节点名用于报错展示。节点由 agentHashId 认定，先换出它所属的节点再取别名，别名为空时退回节点名。
     * 节点已被删除等查不到的情况返回 null，由调用方退回展示 agentHashId。
     */
    fun getNodeName(
        projectId: String,
        userId: String,
        agentHashId: String
    ): String? = try {
        client.get(ServiceThirdPartyAgentResource::class)
            .getAgentById(projectId, agentHashId).data
            ?.nodeId?.takeIf(String::isNotBlank)
            ?.let { nodeHashId ->
                client.get(ServiceNodeResource::class).listByHashIds(
                    userId = userId,
                    projectId = projectId,
                    nodeHashIds = listOf(nodeHashId),
                    checkPermission = false
                ).data?.firstOrNull()
            }
            ?.let { it.displayName?.takeIf(String::isNotBlank) ?: it.name.takeIf(String::isNotBlank) }
    } catch (ignored: Exception) {
        logger.warn("get node name failed|$projectId|$agentHashId", ignored)
        null
    }

    /**
     * 环境已被删除时按「环境里没有这台机器」处理（空列表），其余错误按查询失败处理（null）。
     * 删掉环境和清空环境对「这个节点还能不能跑」是同一个结论，不能因为前者报错就放行。
     */
    private fun envNotExistsOrNull(
        envNotExists: Boolean,
        projectId: String,
        envHashId: String,
        cause: Exception
    ): List<String>? = if (envNotExists) {
        logger.warn("env not exists|$projectId|$envHashId")
        emptyList()
    } else {
        logger.warn("list env node failed|$projectId|$envHashId", cause)
        null
    }

    /**
     * 环境已被删除时按「这台机器不属于该环境」处理，口径同 [envNotExistsOrNull]；其余错误按查询失败处理（null）。
     */
    private fun agentNotInEnvOrNull(
        envNotExists: Boolean,
        projectId: String,
        envHashId: String,
        cause: Exception
    ): Boolean? = if (envNotExists) {
        logger.warn("env not exists|$projectId|$envHashId")
        false
    } else {
        logger.warn("check env agent failed|$projectId|$envHashId", cause)
        null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RuntimeResourceResolver::class.java)

        /** [listEnvAgentHashIds] 取全量节点时的页码约定，见 EnvService.listAllEnvNodesNew */
        const val ALL_PAGE = -1

        /** 环境服务返回的「环境不存在」错误码，经网关后以数值形式出现在 [RemoteServiceException] 上 */
        private val ENV_NOT_EXISTS_CODE = EnvironmentMessageCode.ERROR_ENV_NOT_EXISTS.toInt()
    }
}
