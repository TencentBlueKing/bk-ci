package com.tencent.bk.codecc.task.utils

import com.tencent.bk.codecc.task.model.TaskInfoEntity
import com.tencent.bk.codecc.task.vo.BatchRegisterVO
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.CodePullStrategy
import com.tencent.devops.common.pipeline.enums.GitPullModeType
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.SvnDepth
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitlabElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeSvnElement
import com.tencent.devops.common.pipeline.pojo.element.agent.GithubElement
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.git.GitPullMode
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import net.sf.json.JSONArray
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class PipelineUtils {

    private val logger = LoggerFactory.getLogger(PipelineUtils::class.java)

    @Value("\${pipeline.atomCode.codecc:CodeccCheckAtomDebug}")
    public lateinit var CODECC_ATOM_CODE: String
    @Value("\${pipeline.atomCode.codeccVersion:4.*}")
    public lateinit var CODECC_ATOM_VERSION: String

    @Value("\${pipeline.atomCode.git:gitCodeRepo}")
    public lateinit var GIT_ATOM_CODE: String
    @Value("\${pipeline.atomCode.gitVersion:4.*}")
    public lateinit var GIT_ATOM_CODE_VERSION: String
    @Value("\${pipeline.atomCode.gitlab:GitLab}")
    public lateinit var GITLAB_ATOM_CODE: String
    @Value("\${pipeline.atomCode.github:PullFromGithub}")
    public lateinit var GITHUB_ATOM_CODE: String
    @Value("\${pipeline.atomCode.githubVersion:4.*}")
    public lateinit var GITHUB_ATOM_CODE_VERSION: String
    @Value("\${pipeline.atomCode.svn:svnCodeRepo}")
    public lateinit var SVN_ATOM_CODE: String
    @Value("\${pipeline.atomCode.svnVersion:4.*}")
    public lateinit var SVN_ATOM_CODE_VERSION: String
    @Value("\${pipeline.scmType.git:CODE_GIT}")
    public lateinit var GIT_SCM_TYPE: String
    @Value("\${pipeline.scmType.gitlab:CODE_GITLAB}")
    public lateinit var GITLAB_SCM_TYPE: String
    @Value("\${pipeline.scmType.github:GITHUB}")
    public lateinit var GITHUB_SCM_TYPE: String
    @Value("\${pipeline.scmType.svn:CODE_SVN}")
    public lateinit var SVN_SCM_TYPE: String
    @Value("\${pipeline.scmType.gitUrl:GIT_URL_TYPE}")
    public lateinit var GIT_URL_TYPE: String

    @Value("\${pipeline.atomCode.gitCommon:gitCodeRepoCommon}")
    public lateinit var GIT_COMMON_ATOM_CODE: String
    @Value("\${pipeline.atomCode.gitCommonVersion:2 .*}")
    public lateinit var GIT_COMMON_ATOM_CODE_VERSION: String

    @Value("\${pipeline.scmType.github.old:false}")
    public var GITHUB_SCM_TYPE_OLD: Boolean = false

    @Value("\${pipeline.scmType.svn.old:false}")
    public var SVN_SCM_TYPE_OLD: Boolean = false

    @Value("\${pipeline.imageType:BKSTORE}")
    public var PIPELINE_IMAGE_TYPE: ImageType = ImageType.BKSTORE

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
            url = registerVO.repositoryUrl,
            userName = registerVO.userName,
            passWord = registerVO.passWord,
            scmType = registerVO.scmType,
            branch = registerVO.branch,
            repoHashId = registerVO.repoHashId ?: "",
            relPath = relPath))

        val elementFourth: Element = MarketBuildAtomElement(
            name = "执行扫描脚本",
            id = null,
            status = null,
            atomCode = CODECC_ATOM_CODE,
            version = CODECC_ATOM_VERSION,
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

        logger.info("assemble pipeline parameter successfully! task id: ${taskInfoEntity?.taskId}")
        /**
         * 总流水线拼装
         */
        return Model(
            name = if (0L != taskInfoEntity.taskId)taskInfoEntity.taskId.toString() else UUIDUtil.generate(),
            desc = taskInfoEntity.projectName ?: "",
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
                version = GIT_ATOM_CODE_VERSION,
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
            SVN_SCM_TYPE -> if (SVN_SCM_TYPE_OLD) {
                CodeSvnElement(
                    name = "下载代码",
                    id = null,
                    status = null,
                    repositoryHashId = codeElementData.repoHashId,
                    svnPath = "",
                    path = codeElementData.relPath ?: "",
                    strategy = CodePullStrategy.FRESH_CHECKOUT,
                    svnDepth = SvnDepth.infinity,
                    enableSubmodule = false,
                    specifyRevision = false,
                    revision = ""
                )
            } else {
                MarketBuildAtomElement(
                    name = "下载代码",
                    id = null,
                    status = null,
                    atomCode = SVN_ATOM_CODE,
                    version = SVN_ATOM_CODE_VERSION,
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
            }
            GITHUB_SCM_TYPE -> if (GITHUB_SCM_TYPE_OLD) {
                GithubElement(
                    name = "下载代码",
                    id = null,
                    status = null,
                    repositoryType = RepositoryType.ID,
                    repositoryHashId = codeElementData.repoHashId,
                    gitPullMode = GitPullMode(GitPullModeType.BRANCH, codeElementData.branch),
                    path = codeElementData.relPath ?: ",",
                    enableSubmodule = true,
                    enableVirtualMergeBranch = false
                )
            } else {
                MarketBuildAtomElement(
                    name = "下载代码",
                    id = null,
                    status = null,
                    atomCode = GITHUB_ATOM_CODE,
                    version = GITHUB_ATOM_CODE_VERSION,
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
            }
            GIT_URL_TYPE -> MarketBuildAtomElement(
                name = "拉取代码",
                atomCode = GIT_COMMON_ATOM_CODE,
                version = GIT_COMMON_ATOM_CODE_VERSION,
                data = mapOf(
                    "input" to
                            mapOf(
                                "username" to codeElementData.userName,
                                "password" to codeElementData.passWord,
                                "refName" to codeElementData.branch,
                                "commitId" to "",
                                "enableAutoCrlf" to false,
                                "enableGitClean" to true,
                                "enableSubmodule" to false,
                                "enableSubmoduleRemote" to false,
                                "enableVirtualMergeBranch" to false,
                                "excludePath" to "",
                                "fetchDepth" to "",
                                "includePath" to "",
                                "localPath" to "",
                                "paramMode" to "SIMPLE",
                                "pullType" to "BRANCH",
                                "repositoryUrl" to codeElementData.url,
                                "strategy" to "FRESH_CHECKOUT",
                                "tagName" to ""
                            ),
                    "output" to mapOf()
                )

            )
            else -> {
                CodeGitlabElement(
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

    fun getDispatchType(buildType: String, imageName: String, imageVersion: String): DispatchType {
        return when (buildType) {
            "DEVCLOUD" ->
            {
                getDevCloudDispatchType(imageName, imageName, imageVersion)!!
            }
            else ->
            {
                DockerDispatchType(
                    imageType = PIPELINE_IMAGE_TYPE,
                    dockerBuildVersion = imageName,
                    imageCode = imageName,
                    imageVersion = imageVersion,
                    imageName = imageName
                )
            }
        }
    }

    private fun getDevCloudDispatchType(imageName: String, imageCode: String, imageVersion: String): DispatchType? {
        // TODO("Not yet implemented")
        return null
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
            version = CODECC_ATOM_VERSION,
            data = mapOf("input" to mapOf<String, String>())
        )
    }

    companion object {
        data class CodeElementData(
            val scmType: String?,
            val url: String? = null,
            val branch: String,
            val repoHashId: String,
            val relPath: String?,
            val userName: String? = null,
            val passWord: String? = null
        )
    }
}
