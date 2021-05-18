package com.tencent.bk.codecc.task.utils


import com.tencent.bk.codecc.task.model.TaskInfoEntity
import com.tencent.bk.codecc.task.vo.BatchRegisterVO
import com.tencent.bk.codecc.task.vo.MetadataVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.CodePullStrategy
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.*
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.devcloud.PublicDevCloudDispathcType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.plugin.codecc.pojo.coverity.ProjectLanguage
import net.sf.json.JSONArray
import org.slf4j.LoggerFactory

object PipelineUtils {

    private val logger = LoggerFactory.getLogger(PipelineUtils::class.java)

    const val CODECC_ATOM_CODE = "CodeccCheckAtomDebug"
    const val GIT_ATOM_CODE = "gitCodeRepo"
    const val GITLAB_ATOM_CODE = "GitLab"
    const val GITHUB_ATOM_CODE = "PullFromGithub"
    const val SVN_ATOM_CODE = "svnCodeRepo"
    const val GIT_SCM_TYPE = "CODE_GIT"
    const val GITLAB_SCM_TYPE = "CODE_GITLAB"
    const val GITHUB_SCM_TYPE = "GITHUB"
    const val SVN_SCM_TYPE = "CODE_SVN"

    /**
     * 创建流水线
     */
    fun createPipeline(
            registerVO: BatchRegisterVO,
            taskInfoEntity: TaskInfoEntity,
            relPath: String?,
            imageName: String,
            dispatchType: DispatchType
    ): Model {
        /**
         * 第一个stage的内容
         */
        val elementFirst = ManualTriggerElement(
                name = "手动触发",
                id = null,
                status = null,
                canElementSkip = false,
                useLatestParameters = false
        )

        val containerFirst = TriggerContainer(
                id = null,
                name = "demo",
                elements = arrayListOf(elementFirst),
                status = null,
                startEpoch = null,
                systemElapsed = null,
                elementElapsed = null,
                params = emptyList(),
                templateParams = null,
                buildNo = null,
                canRetry = null,
                containerId = null
        )

        val stageFirst = Stage(
                containers = arrayListOf(containerFirst),
                id = null
        )

        /**
         * 第二个stage
         */
        val elementThird = getNewCodeElement(CodeElementData(
                scmType = registerVO.scmType,
                branch = registerVO.branch,
                repoHashId = registerVO.repoHashId,
                relPath = relPath))

        val elementFourth: Element = MarketBuildAtomElement(
                name = "执行扫描脚本",
                id = null,
                status = null,
                atomCode = CODECC_ATOM_CODE,
                version = "4.*",
                data = mapOf("input" to mapOf<String, String>())
        )

        val containerSecond = VMBuildContainer(
                id = null,
                name = "demo",
                elements = listOf(elementThird, elementFourth),
                status = null,
                startEpoch = null,
                systemElapsed = null,
                elementElapsed = null,
                baseOS = if (!registerVO.osType.isNullOrBlank()) VMBaseOS.valueOf(registerVO.osType) else VMBaseOS.valueOf("LINUX"),
                vmNames = emptySet(),
                maxQueueMinutes = null,
                maxRunningMinutes = 480,
                buildEnv = registerVO.buildEnv,
                customBuildEnv = null,
                thirdPartyAgentId = null,
                thirdPartyAgentEnvId = null,
                thirdPartyWorkspace = null,
                dockerBuildVersion = imageName,
                tstackAgentId = null,
                dispatchType = dispatchType,
                canRetry = null,
                enableExternal = null,
                containerId = null,
                jobControlOption = JobControlOption(
                        enable = true,
                        timeout = 900,
                        runCondition = JobRunCondition.STAGE_RUNNING,
                        customVariables = null,
                        customCondition = null
                ),
                mutexGroup = null
        )
        val stageSecond = Stage(
                containers = arrayListOf(containerSecond),
                id = null
        )

        logger.info("assemble pipeline parameter successfully! task id: ${taskInfoEntity.taskId}")
        /**
         * 总流水线拼装
         */
        return Model(
                name = taskInfoEntity.taskId.toString(),
                desc = taskInfoEntity.projectName,
                stages = arrayListOf(stageFirst, stageSecond),
                labels = emptyList(),
                instanceFromTemplate = null,
                pipelineCreator = null,
                srcTemplateId = null
        )
    }

    fun getNewCodeElement(codeElementData: CodeElementData): Element {
        return when (codeElementData.scmType) {
            GIT_SCM_TYPE -> MarketBuildAtomElement(
                    name = "下载代码",
                    id = null,
                    status = null,
                    atomCode = GIT_ATOM_CODE,
                    version = "4.*",
                    data = mapOf("input" to mapOf(
                            "repositoryType" to "ID",
                            "repositoryHashId" to codeElementData.repoHashId,
                            "repositoryName" to "",
                            "pullType" to "BRANCH",
                            "branchName" to (codeElementData.branch),
                            "tagName" to "",
                            "commitId" to "",
                            "localPath" to (codeElementData.relPath ?: ""),
                            "includePath" to "",
                            "excludePath" to "",
                            "fetchDepth" to "",
                            "strategy" to CodePullStrategy.FRESH_CHECKOUT,
                            "enableSubmodule" to true,
                            "enableSubmoduleRemote" to false,
                            "enableSubmoduleRecursive" to true,
                            "enableVirtualMergeBranch" to false,
                            "enableAutoCrlf" to false,
                            "enableGitLfs" to false,
                            "enableGitClean" to true,
                            "rebuildToNew" to "false",
                            "autoCrlf" to "false",
                            "scmType" to GIT_SCM_TYPE
                    )))
            SVN_SCM_TYPE -> MarketBuildAtomElement(
                    name = "下载代码",
                    id = null,
                    status = null,
                    atomCode = SVN_ATOM_CODE,
                    version = "4.*",
                    data = mapOf("input" to mapOf(
                            "repositoryType" to "ID",
                            "repositoryHashId" to codeElementData.repoHashId,
                            "repositoryName" to "",
                            "svnPath" to "",
                            "codePath" to (codeElementData.relPath ?: ""),
                            "strategy" to CodePullStrategy.FRESH_CHECKOUT,
                            "svnDepth" to "infinity",
                            "enableSubmodule" to true,
                            "specifyRevision" to false,
                            "reversion" to ""
                    )))
            GITHUB_SCM_TYPE -> MarketBuildAtomElement(
                    name = "下载代码",
                    id = null,
                    status = null,
                    atomCode = GITHUB_ATOM_CODE,
                    version = "4.*",
                    data = mapOf("input" to mapOf(
                            "repositoryType" to "ID",
                            "repositoryHashId" to codeElementData.repoHashId,
                            "aliasName" to "",
                            "pullType" to "BRANCH",
                            "branchName" to codeElementData.branch,
                            "tagName" to "",
                            "commitId" to "",
                            "localPath" to (codeElementData.relPath ?: ""),
                            "strategy" to CodePullStrategy.FRESH_CHECKOUT,
                            "enableSubmodule" to true,
                            "enableVirtualMergeBranch" to false
                    )))
            else -> CodeGitlabElement(
                    name = "下载代码",
                    id = null,
                    status = null,
                    repositoryHashId = codeElementData.repoHashId,
                    branchName = if (codeElementData.branch.isBlank()) "" else codeElementData.branch,
                    revision = null,
                    strategy = CodePullStrategy.FRESH_CHECKOUT,
                    path = codeElementData.relPath,
                    enableSubmodule = null,
                    gitPullMode = null,
                    repositoryType = null,
                    repositoryName = null
            )
        }
    }

    fun getOldCodeElement(registerVO: BatchRegisterVO?, relPath: String?): Element? {
        if (registerVO == null) return null

        return when (registerVO!!.scmType) {
            "CODE_GIT" -> CodeGitElement(
                    name = "下载代码",
                    id = null,
                    status = null,
                    repositoryHashId = registerVO.repoHashId,
                    branchName = if (registerVO.branch.isNullOrBlank()) "" else registerVO.branch,
                    revision = null,
                    strategy = CodePullStrategy.FRESH_CHECKOUT,
                    path = relPath,
                    enableSubmodule = null,
                    gitPullMode = null,
                    repositoryType = null,
                    repositoryName = null
            )
            "CODE_GITLAB" -> CodeGitlabElement(
                    name = "下载代码",
                    id = null,
                    status = null,
                    repositoryHashId = registerVO.repoHashId,
                    branchName = if (registerVO.branch.isNullOrBlank()) "" else registerVO.branch,
                    revision = null,
                    strategy = CodePullStrategy.FRESH_CHECKOUT,
                    path = relPath,
                    enableSubmodule = null,
                    gitPullMode = null,
                    repositoryType = null,
                    repositoryName = null
            )
            "GITHUB" -> GithubElement(
                    name = "下载代码",
                    id = null,
                    status = null,
                    repositoryHashId = registerVO.repoHashId,
                    strategy = CodePullStrategy.FRESH_CHECKOUT,
                    path = relPath,
                    enableSubmodule = null,
                    revision = null,
                    gitPullMode = null,
                    enableVirtualMergeBranch = null,
                    repositoryType = null,
                    repositoryName = null
            )
            else -> CodeSvnElement(
                    name = "下载代码",
                    id = null,
                    status = null,
                    repositoryHashId = registerVO.repoHashId,
                    revision = null,
                    strategy = CodePullStrategy.FRESH_CHECKOUT,
                    path = relPath,
                    enableSubmodule = null,
                    specifyRevision = null,
                    svnDepth = null,
                    svnPath = null,
                    svnVersion = null,
                    repositoryType = null,
                    repositoryName = null
            )
        }
    }

    fun getDispatchType(buildType: String, imageName: String, imageVersion: String, imageCode: String, dockerBuildVersion: String): DispatchType {
        return when (buildType) {
            "DEVCLOUD" -> {
                PublicDevCloudDispathcType(
                        imageType = ImageType.BKSTORE,
                        image = imageName,
                        imageCode = imageCode,
                        imageVersion = imageVersion,
                        imageName = imageName,
                        performanceConfigId = "",
                        credentialId = ""
                )
            }
            else -> {
                DockerDispatchType(
                        imageType = ImageType.BKSTORE,
                        dockerBuildVersion = dockerBuildVersion,
                        imageCode = imageCode,
                        imageVersion = imageVersion,
                        imageName = imageName
                )
            }
        }
    }

    /**
     * 获取定时任务表达式
     *
     * @param executeTime
     * @param executeDateList
     * @return
     */
    fun getCrontabTimingStr(executeTime: String, executeDateList: List<String>?): String {
        if (executeTime.isBlank() || executeDateList.isNullOrEmpty()) {
            logger.error("execute date and time is empty!")
            throw CodeCCException(
                    errCode = CommonMessageCode.PARAMETER_IS_NULL,
                    msgParam = arrayOf("定时执行时间"),
                    errorCause = null
            )
        }
        val hour = executeTime.substring(0, executeTime.indexOf(":"))
        val min = executeTime.substring(executeTime.indexOf(":") + 1)

        val weekDayListStr = executeDateList.reduce { acc, s -> "$acc,$s" }
        return String.format("0 %s %s ? * %s", min, hour, weekDayListStr)
    }

    /**
     * 将codecc平台的项目语言转换为蓝盾平台的codecc原子语言
     */
    @Suppress("CAST_NEVER_SUCCEEDS")
    fun convertCodeLangToBs(metadataList: List<MetadataVO>?, langCode: Long): List<ProjectLanguage> {
        val languageList = metadataList?.filter { metadataVO ->
            (metadataVO.key.toLong() and langCode) != 0L
        }
                ?.map { metadataVO ->
                    ProjectLanguage.valueOf(JSONArray.fromObject(metadataVO.aliasNames)[0].toString())
                }
        return if (languageList.isNullOrEmpty()) listOf(ProjectLanguage.OTHERS) else languageList
    }

    fun isCodeElement(element: Element) = isOldCodeElement(element) || isNewCodeElement(element)

    fun isOldCodeElement(element: Element) = element is CodeGitElement ||
            element is CodeGitlabElement ||
            element is GithubElement ||
            element is CodeSvnElement

    fun isNewCodeElement(element: Element) = element is MarketBuildAtomElement &&
            (element.getAtomCode() in listOf(GIT_ATOM_CODE, GITHUB_ATOM_CODE, GITLAB_ATOM_CODE, SVN_ATOM_CODE))

    fun isOldCodeCCElement(element: Element) = element is LinuxCodeCCScriptElement

    fun getDevopsChannelCode(createFrom: String, nameEn: String) = when (createFrom) {
        ComConstants.BsTaskCreateFrom.BS_PIPELINE.value() -> ChannelCode.BS
        ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value() -> ChannelCode.GONGFENGSCAN
        else -> {
            if (nameEn.startsWith(ComConstants.OLD_CODECC_ENNAME_PREFIX)) {
                ChannelCode.CODECC
            } else {
                ChannelCode.CODECC_EE
            }
        }
    }

    fun transferOldCodeCCElementToNew(): Element {
        return MarketBuildAtomElement(
                name = "执行扫描脚本",
                id = null,
                status = null,
                atomCode = CODECC_ATOM_CODE,
                version = "4.*",
                data = mapOf("input" to mapOf<String, String>())
        )
    }

    data class CodeElementData(
            val scmType: String?,
            val branch: String,
            val repoHashId: String,
            val relPath: String?
    )
}