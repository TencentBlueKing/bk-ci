/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.process.engine.interceptor

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.RepositoryTypeNew
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.GitPullModeType
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitlabElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeSvnElement
import com.tencent.devops.common.pipeline.pojo.element.agent.GithubElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.pipeline.pojo.git.GitPullMode
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils.buildConfig
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode.BK_NON_TIMED_TRIGGER_SKIP
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_TIMER_SCM_NO_CHANGE
import com.tencent.devops.process.constant.ProcessMessageCode.OK
import com.tencent.devops.process.engine.pojo.Response
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.repository.api.ServiceCommitResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * 定时触发的锁定
 * @version 1.0
 */
@Suppress("ALL")
@Component
class TimerTriggerScmChangeInterceptor @Autowired constructor(
    private val scmProxyService: ScmProxyService,
    private val client: Client
) : PipelineInterceptor {

    override fun execute(task: InterceptData): Response<BuildStatus> {
        // 非定时触发的直接跳过
        if (task.startType != StartType.TIME_TRIGGER) {
            return Response(OK, I18nUtil.getCodeLanMessage(BK_NON_TIMED_TRIGGER_SKIP))
        }

        val pipelineId = task.pipelineInfo.pipelineId
        val projectId = task.pipelineInfo.projectId
        val model = task.model ?: return Response(
            ERROR_PIPELINE_MODEL_NOT_EXISTS.toInt(),
            I18nUtil.getCodeLanMessage(ERROR_PIPELINE_MODEL_NOT_EXISTS)
        )

        var noScm = false
        var hasCodeChange = false
        var hasScmElement = false
        val variables = HashMap<String, String>()
        run outer@{
            model.stages.forEach { stage ->
                stage.containers.forEach { container ->
                    if (container is TriggerContainer) {
                        container.elements.forEach { ele ->
                            if (ele is TimerTriggerElement) {
                                noScm = ele.noScm ?: false
                                if (!noScm) {
                                    return@outer
                                }
                            }
                        }
                        // 解析变量
                        container.params.forEach { param ->
                            variables[param.id] = param.defaultValue.toString()
                        }
                    } else if (noScm && container is VMBuildContainer) {
                        container.elements.forEach ele@{ ele ->
                            // 插件没有启用或者是post action不需要比较变更
                            if (!ele.isElementEnable() || ele.additionalOptions?.elementPostInfo != null) {
                                return@ele
                            }
                            val (existScmElement, codeChange) = scmElementCheck(
                                ele = ele,
                                projectId = projectId,
                                pipelineId = pipelineId,
                                variables = variables
                            )
                            val (existScmElementNew, codeChangeNew) = scmElementCheckNew(
                                ele = ele,
                                projectId = projectId,
                                pipelineId = pipelineId,
                                variables = variables
                            )
                            val bothExistScmElement = existScmElement || existScmElementNew
                            val bothCodeChange = codeChange || codeChangeNew
                            hasScmElement = bothExistScmElement || hasScmElement // 只要有一个拉代码插件的，即标识为存在
                            hasCodeChange = (bothExistScmElement && bothCodeChange) || hasCodeChange // 当前库有更新
                            if (hasCodeChange) { // 只要有一个库有更新 就返回
                                return@outer
                            }
                        }
                    }
                }
            }
        }

        return when {
            !noScm -> Response(OK) // 没有开启【源代码未更新时不触发构建】, 则允许执行
            hasCodeChange -> Response(OK) //  有代码变更，【源代码未更新时不触发构建】不成立，允许执行
            !hasScmElement -> Response(OK) // 没有任何拉代码的插件，【源代码未更新时不触发构建】无效，允许执行
            else -> Response(
                ERROR_PIPELINE_TIMER_SCM_NO_CHANGE.toInt(),
                I18nUtil.getCodeLanMessage(ERROR_PIPELINE_TIMER_SCM_NO_CHANGE)
            )
        }
    }

    private fun scmElementCheckNew(
        ele: Element,
        projectId: String,
        pipelineId: String,
        variables: HashMap<String, String>
    ): Pair<Boolean, Boolean> {

        // 默认没有拉代码的原子
        var existScmElement = false
        var codeChange = false

        if (ele !is MarketBuildAtomElement) return Pair(first = false, second = false)

        when {
            ele.getAtomCode() == "svnCodeRepo" -> {
                existScmElement = true
                codeChange = checkSvnChangeNew(projectId, pipelineId, ele, variables)
            }
            ele.getAtomCode() in setOf("gitCodeRepo", "PullFromGithub", "Gitlab", "atomtgit", "checkout") -> {
                existScmElement = true
                codeChange = checkGitChangeNew(
                    variables,
                    projectId,
                    pipelineId,
                    ele
                )
            }
        }
        // 没有拉代码原子的直接通过|有拉代码原子则一定要有代码变更
        return existScmElement to codeChange
    }

    private fun scmElementCheck(
        ele: Element,
        projectId: String,
        pipelineId: String,
        variables: HashMap<String, String>
    ): Pair<Boolean, Boolean> {

        // 默认没有拉代码的原子
        var existScmElement = false
        var codeChange = false

        when (ele) {
            is CodeSvnElement -> {
                existScmElement = true
                codeChange = checkSvnChange(projectId, pipelineId, ele, variables)
            }
            is CodeGitElement -> {
                existScmElement = true
                codeChange = checkGitChange(
                    ele.branchName,
                    ele.gitPullMode,
                    buildConfig(ele),
                    variables,
                    projectId,
                    pipelineId,
                    ele
                )
            }
            is CodeGitlabElement -> {
                existScmElement = true
                codeChange = checkGitChange(
                    ele.branchName,
                    ele.gitPullMode,
                    buildConfig(ele),
                    variables,
                    projectId,
                    pipelineId,
                    ele
                )
            }
            is GithubElement -> {
                existScmElement = true
                codeChange = checkGitChange(
                    null,
                    ele.gitPullMode,
                    buildConfig(ele),
                    variables,
                    projectId,
                    pipelineId,
                    ele
                )
            }
        }
        // 没有拉代码原子的直接通过|有拉代码原子则一定要有代码变更
        return existScmElement to codeChange
    }

    private fun checkSvnChange(
        projectId: String,
        pipelineId: String,
        ele: CodeSvnElement,
        variables: Map<String, String>
    ): Boolean {
        val repositoryConfig = buildConfig(ele)
        // 如果没有初始化，就初始化一下
        if (ele.revision.isNullOrBlank()) {

            val latestRevision =
                scmProxyService.recursiveFetchLatestRevision(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    repositoryConfig = repositoryConfig,
                    branchName = ele.svnPath,
                    variables = variables
                )

            if (latestRevision.isNotOk() || latestRevision.data == null) {
                LOG.warn("[$pipelineId] get svn latestRevision fail!")
                return false
            }
            ele.revision = latestRevision.data!!.revision
            ele.specifyRevision = true
        }
        val latestCommit = try {
            client.get(ServiceCommitResource::class).getLatestCommit(
                projectId,
                pipelineId,
                ele.id!!,
                repositoryConfig.getRepositoryId(),
                repositoryConfig.repositoryType
            )
        } catch (e: Exception) {
            LOG.warn("[$pipelineId] scmService.getLatestRevision fail", e)
            return false
        }
        if (latestCommit.isOk() && (latestCommit.data == null || latestCommit.data!!.commit != ele.revision)) {
            LOG.info("$pipelineId|${ele.id}|svn lastCommit=${latestCommit.data?.commit}, newCommitId=${ele.revision}")
            return true
        }
        return false
    }

    private fun checkSvnChangeNew(
        projectId: String,
        pipelineId: String,
        ele: MarketBuildAtomElement,
        variables: Map<String, String>
    ): Boolean {
        val input = ele.data["input"]
        if (input !is Map<*, *>) return false

        val repositoryConfig = getMarketBuildRepoConfig(input, variables) ?: return false

        // get pre commit
        val svnPath = EnvUtils.parseEnv(input["svnPath"] as String?, variables)
        val preCommit =
            scmProxyService.recursiveFetchLatestRevision(projectId, pipelineId, repositoryConfig, svnPath, variables)
        if (preCommit.isNotOk() || preCommit.data == null) {
            LOG.warn("[$pipelineId] get svn latestRevision fail!")
            return false
        }

        // get latest commit
        val latestCommit = try {
            client.get(ServiceCommitResource::class).getLatestCommit(
                projectId,
                pipelineId,
                ele.id!!,
                repositoryConfig.getRepositoryId(),
                repositoryConfig.repositoryType
            )
        } catch (e: Exception) {
            LOG.warn("[$pipelineId] scmService.getLatestRevision fail", e)
            return false
        }

        // start check
        return if (latestCommit.isOk() && (latestCommit.data == null ||
                latestCommit.data!!.commit != preCommit.data!!.revision)) {
            LOG.info("[$pipelineId] [${ele.id}] scm svn change: lastCommitId=" +
                "${if (latestCommit.data != null) latestCommit.data!!.commit else null}, newCommitId=$preCommit")
            true
        } else {
            LOG.info("[$pipelineId] [${ele.id}] svn not change")
            false
        }
    }

    private fun checkGitChange(
        oldBranchName: String?,
        gitPullMode: GitPullMode?,
        repositoryConfig: RepositoryConfig,
        variables: HashMap<String, String>,
        projectId: String,
        pipelineId: String,
        ele: Element
    ): Boolean {

        val branchName = when {
            gitPullMode != null -> EnvUtils.parseEnv(gitPullMode.value, variables)
            !oldBranchName.isNullOrBlank() -> EnvUtils.parseEnv(oldBranchName, variables)
            else -> return false
        }
        if (branchName.isBlank()) {
            return false
        }
        val gitPullModeType = gitPullMode?.type ?: GitPullModeType.BRANCH
//        val latestRevision =
//        // 如果是commit id ,则直接比对就可以了，不需要再拉commit id
//            if (gitPullModeType == GitPullModeType.COMMIT_ID) {
//                Result(RevisionInfo(gitPullMode!!.value, "", branchName))
//            } else {
//                recursiveFetchLatestRevision(scmService, projectId, pipelineId, repositoryHashId, branchName)
//            }
//
//        if (latestRevision.isNotOk() || latestRevision.data == null) {
//            logger.warn("[$pipelineId] get git latestRevision empty! msg=${latestRevision.message}")
//            return false
//        }
        // 如果是commit id ,则gitPullModeType直接比对就可以了，不需要再拉commit id
        val latestRevision =
            if (gitPullModeType == GitPullModeType.COMMIT_ID) {
                gitPullMode!!.value
            } else {
                val t = when (ele) {
                    is CodeGitElement -> ele.revision
                    is CodeGitlabElement -> ele.revision
                    is GithubElement -> ele.revision
                    else -> return false // 非法类型退出
                }
                // 如果没有初始化，就初始化一下
                if (t.isNullOrBlank()) {
                    val result =
                        scmProxyService.recursiveFetchLatestRevision(
                            projectId,
                            pipelineId,
                            repositoryConfig,
                            branchName,
                            variables
                        )

                    if (result.isNotOk() || result.data == null) {
                        LOG.warn("[$pipelineId] get git latestRevision empty! msg=${result.message}")
                        return false
                    }
                    result.data!!.revision
                } else t
            }
        val latestCommit = try {
            client.get(ServiceCommitResource::class).getLatestCommit(
                projectId,
                pipelineId,
                ele.id!!,
                EnvUtils.parseEnv(repositoryConfig.getRepositoryId(), variables),
                repositoryConfig.repositoryType
            )
        } catch (e: Exception) {
            LOG.warn("[$pipelineId] scmService.getLatestRevision fail", e)
            return false
        }
        if (latestCommit.isOk() && (latestCommit.data == null || latestCommit.data!!.commit != latestRevision)) {
            LOG.info("[$pipelineId] [${ele.id}] ${ele.getClassType()} change: lastCommitId=" +
                "${if (latestCommit.data != null) latestCommit.data!!.commit else null}, newCommitId=$latestRevision")
            return true
        }
        return false
    }

    private fun checkGitChangeNew(
        variables: HashMap<String, String>,
        projectId: String,
        pipelineId: String,
        ele: MarketBuildAtomElement
    ): Boolean {
        val input = ele.data["input"]
        if (input !is Map<*, *>) return false

        // checkout插件[按仓库URL输入]不校验代码变更
        if (ele.getAtomCode() == "checkout" && input["repositoryType"] == RepositoryTypeNew.URL.name) return true
        val repositoryConfig = getMarketBuildRepoConfig(input, variables) ?: return false

        val gitPullMode = EnvUtils.parseEnv(input["pullType"] as String?, variables)
        val branchName = if (ele.getAtomCode() == "checkout") {
            EnvUtils.parseEnv(input["refName"] as String?, variables)
        } else {
            when (gitPullMode) {
                GitPullModeType.BRANCH.name -> EnvUtils.parseEnv(input["branchName"] as String?, variables)
                GitPullModeType.TAG.name -> EnvUtils.parseEnv(input["tagName"] as String?, variables)
                GitPullModeType.COMMIT_ID.name -> EnvUtils.parseEnv(input["commitId"] as String?, variables)
                else -> return false
            }
        }
        // 如果分支是变量形式,默认值为空,那么解析后值就为空,导致调接口失败
        if (branchName.isBlank()) {
            return false
        }

        // 如果是commit id ,则gitPullModeType直接比对就可以了，不需要再拉commit id
        // get pre vision
        val preCommit =
            if (gitPullMode == GitPullModeType.COMMIT_ID.name) {
                branchName
            } else {
                val result =
                    scmProxyService.recursiveFetchLatestRevision(
                        projectId,
                        pipelineId,
                        repositoryConfig,
                        branchName,
                        variables
                    )
                if (result.isNotOk() || result.data == null) {
                    LOG.warn("[$pipelineId] get git latestRevision empty! msg=${result.message}")
                    return false
                }
                result.data!!.revision
            }

        // get latest commit
        val latestCommit = try {
            client.get(ServiceCommitResource::class).getLatestCommit(
                projectId,
                pipelineId,
                ele.id!!,
                EnvUtils.parseEnv(repositoryConfig.getRepositoryId(), variables),
                repositoryConfig.repositoryType
            )
        } catch (e: Exception) {
            LOG.warn("[$pipelineId] scmService.getLatestRevision fail", e)
            return false
        }

        // start check
        return if (latestCommit.isOk() && (latestCommit.data == null || latestCommit.data!!.commit != preCommit)) {
            LOG.info("[$pipelineId] [${ele.id}] ${ele.getClassType()} " +
                "change: lastCommitId=${latestCommit.data?.commit}, newCommitId=$preCommit")
            true
        } else {
            LOG.info("[$pipelineId] [${ele.id}] ${ele.getAtomCode()} scm not change")
            false
        }
    }

    private fun getMarketBuildRepoConfig(input: Map<*, *>, variables: Map<String, String>): RepositoryConfig? {
        val repositoryType = RepositoryType.parseType(input["repositoryType"] as String?)
        val repositoryId = when (repositoryType) {
            RepositoryType.ID -> EnvUtils.parseEnv(input["repositoryHashId"] as String?, variables)
            RepositoryType.NAME -> EnvUtils.parseEnv(input["repositoryName"] as String?, variables)
            else -> return null
        }
        return buildConfig(repositoryId, repositoryType)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PipelineInterceptor::class.java)
    }
}
