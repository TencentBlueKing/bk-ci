package com.tencent.devops.plugin.worker.task.codecc.util

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.plugin.codecc.pojo.coverity.CoverityProjectType
import com.tencent.devops.plugin.codecc.pojo.coverity.ProjectLanguage
import com.tencent.devops.plugin.worker.pojo.CodeccExecuteConfig
import com.tencent.devops.plugin.worker.task.codecc.LinuxCodeccConstants
import com.tencent.devops.plugin.worker.task.codecc.WindowsCodeccConstants
import com.tencent.devops.worker.common.CommonEnv
import com.tencent.devops.worker.common.api.utils.ThirdPartyAgentBuildInfoUtils
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.env.BuildEnv
import com.tencent.devops.worker.common.env.BuildType
import com.tencent.devops.worker.common.logger.LoggerService
import java.io.File

object CodeccParamsHelper {
    fun addCommonParams(list: MutableList<String>, codeccExecuteConfig: CodeccExecuteConfig) {
        val buildId = codeccExecuteConfig.buildVariables.buildId
        val taskParams = codeccExecuteConfig.buildTask.params ?: mapOf()
        val repoScmType = CodeccRepoHelper.getScmType(codeccExecuteConfig.repos)

        list.add(taskParams["codeCCTaskName"] ?: "")
        list.add("-DLANDUN_BUILDID=$buildId")
        list.add("-DCERT_TYPE=${CodeccRepoHelper.getCertType(codeccExecuteConfig.repos)}")
        list.add("-DSCM_TYPE=$repoScmType")

        val svnUerPassPair = CommonEnv.getSvnHttpCredential() ?: codeccExecuteConfig.repos.firstOrNull()?.svnUerPassPair
        if (svnUerPassPair != null) {
            list.add("-D${LinuxCodeccConstants.SVN_USER}=${svnUerPassPair.first}")
            list.add("-D${LinuxCodeccConstants.SVN_PASSWORD}='${svnUerPassPair.second}'")
        }

        if (repoScmType == "svn") {
            if (svnUerPassPair == null) list.add("-DSCM_SSH_ACCESS=$repoScmType")
        }

        list.add("-DREPO_URL_MAP='${getRepoUrlMap(codeccExecuteConfig)}'")
        list.add("-DREPO_RELPATH_MAP='${getRepoRealPathMap(codeccExecuteConfig)}'")
        list.add("-DREPO_SCM_RELPATH_MAP='${getRepoScmRelPathMap(codeccExecuteConfig)}'")
        list.add("-DSUB_CODE_PATH_LIST=${taskParams["path"] ?: ""}")
        list.add("-DLD_ENV_TYPE=${getEnvType()}")

        // 构建机信息
        if (BuildEnv.getBuildType() == BuildType.AGENT) {
            LoggerService.addNormalLine("检测到这是第三方构建机")
            list.add("-DDEVOPS_PROJECT_ID=${AgentEnv.getProjectId()}")
            list.add("-DDEVOPS_BUILD_TYPE=${BuildType.AGENT.name}")
            list.add("-DDEVOPS_AGENT_ID=${AgentEnv.getAgentId()}")
            list.add("-DDEVOPS_AGENT_SECRET_KEY=${AgentEnv.getAgentSecretKey()}")
            list.add("-DDEVOPS_AGENT_VM_SID=${ThirdPartyAgentBuildInfoUtils.getBuildInfo()!!.vmSeqId}")
        } else if (BuildEnv.getBuildType() == BuildType.DOCKER) {
            LoggerService.addNormalLine("检测到这是docker公共构建机")
            list.add("-DDEVOPS_PROJECT_ID=${AgentEnv.getProjectId()}")
            list.add("-DDEVOPS_BUILD_TYPE=${BuildType.DOCKER.name}")
            list.add("-DDEVOPS_AGENT_ID=${AgentEnv.getAgentId()}")
            list.add("-DDEVOPS_AGENT_SECRET_KEY=${AgentEnv.getAgentSecretKey()}")
            list.add("-DDEVOPS_AGENT_VM_SID=")
        }

        list.add("-DDEVOPS_PROJECT_ID=${codeccExecuteConfig.buildVariables.projectId}")
        list.add("-DDEVOPS_PIPELINE_ID=${codeccExecuteConfig.buildVariables.pipelineId}")
        list.add("-DDEVOPS_VMSEQ_ID=${codeccExecuteConfig.buildVariables.vmSeqId}")
    }

    private fun getRepoScmRelPathMap(codeccExecuteConfig: CodeccExecuteConfig): String {
        return toMapString(
            codeccExecuteConfig.repos.map {
                it.repoHashId to it.relativePath
            }.toMap()
        )
    }

    private fun getRepoRealPathMap(codeccExecuteConfig: CodeccExecuteConfig): String {
        return toMapString(
            codeccExecuteConfig.repos.map {
                it.repoHashId to it.relPath
            }.toMap()
        )
    }

    private fun getRepoUrlMap(codeccExecuteConfig: CodeccExecuteConfig): String {
        return toMapString(
            codeccExecuteConfig.repos.map {
                it.repoHashId to it.url
            }.toMap()
        )
    }

    private fun toMapString(toMap: Map<String, String>): String {
        val sb = StringBuilder()
        sb.append("{")
        toMap.entries.forEach {
            sb.append("\"${it.key}\":\"${it.value}\"")
            sb.append(",")
        }
        sb.deleteCharAt(sb.length - 1)
        sb.append("}")
        return sb.toString()
    }

    private fun getEnvType(): String {
        // 第三方机器
        return if (BuildEnv.getBuildType() == BuildType.AGENT) {
            when (AgentEnv.getOS()) {
                OSType.MAC_OS -> "MAC_THIRD_PARTY"
                OSType.WINDOWS -> "WIN_THIRD_PARTY"
                else -> "LINUX_THIRD_PARTY"
            }
        } else {
            "PUBLIC"
        }
    }
    private val map = mapOf(
        ProjectLanguage.C.name to CoverityProjectType.COMPILE,
        ProjectLanguage.C_PLUS_PLUSH.name to CoverityProjectType.COMPILE,
        ProjectLanguage.C_CPP.name to CoverityProjectType.COMPILE,
        ProjectLanguage.OBJECTIVE_C.name to CoverityProjectType.COMPILE,
        ProjectLanguage.OC.name to CoverityProjectType.COMPILE,
        ProjectLanguage.C_SHARP.name to CoverityProjectType.COMPILE,
        ProjectLanguage.JAVA.name to CoverityProjectType.COMPILE,
        ProjectLanguage.PYTHON.name to CoverityProjectType.UN_COMPILE,
        ProjectLanguage.JAVASCRIPT.name to CoverityProjectType.UN_COMPILE,
        ProjectLanguage.JS.name to CoverityProjectType.UN_COMPILE,
        ProjectLanguage.PHP.name to CoverityProjectType.UN_COMPILE,
        ProjectLanguage.RUBY.name to CoverityProjectType.UN_COMPILE,
        ProjectLanguage.LUA.name to CoverityProjectType.UN_COMPILE,
        ProjectLanguage.GOLANG.name to CoverityProjectType.COMBINE,
        ProjectLanguage.SWIFT.name to CoverityProjectType.COMBINE,
        ProjectLanguage.TYPESCRIPT.name to CoverityProjectType.UN_COMPILE,
        ProjectLanguage.KOTLIN.name to CoverityProjectType.COMPILE,
        ProjectLanguage.OTHERS.name to CoverityProjectType.UN_COMPILE
    )

    /**
     * C/C++                	编译型
     * Objective-C/C++			编译型
     * C#						编译型
     * Java 					编译型
     * Python					非编译型
     * JavaScript				非编译型
     * PHP						非编译型
     * Ruby					    非编译型
     */

    fun getProjectType(languagesStr: String): CoverityProjectType {
        // 此处为了新引擎兼容，新引擎传递的参数是真实类型json，而不是单纯的String
        // 而CodeCC是用x,y,z这种方式对待List，所以在这强转并写入params中供CodeCC读取
        val languages = JsonUtil.to(languagesStr, object : TypeReference<List<String>>() {})

        if (languages.isEmpty()) {
            return CoverityProjectType.UN_COMPILE
        }

        var type = map[languages[0]]

        languages.forEach {
            val currentType = map[it]
            if (type != null) {
                if (currentType != null && type != currentType) {
                    return CoverityProjectType.COMBINE
                }
            } else {
                type = currentType
            }
        }

        return type ?: CoverityProjectType.UN_COMPILE
    }

    fun getCovPyFile(scriptType: BuildScriptType, codeccWorkspace: File): String {
        return if (scriptType == BuildScriptType.SHELL) {
            val shareCoverityFile = LinuxCodeccConstants.getCovPyFile()
            if (!shareCoverityFile.exists()) {
                throw RuntimeException("The coverity file (${shareCoverityFile.canonicalPath}) is not exist")
            }
            val localCoverityFile = File(codeccWorkspace, shareCoverityFile.name)
            shareCoverityFile.copyTo(localCoverityFile, true)
            return localCoverityFile.canonicalPath
        } else {
            WindowsCodeccConstants.WINDOWS_COV_PY_FILE.canonicalPath
        }
    }

    fun getToolPyFile(scriptType: BuildScriptType, codeccWorkspace: File): String {
        return if (scriptType == BuildScriptType.SHELL) {
            val shareToolFile = LinuxCodeccConstants.getToolPyFile()
            if (AgentEnv.getOS() != OSType.MAC_OS && !shareToolFile.exists()) {
                throw RuntimeException("The mutli tool file (${shareToolFile.canonicalPath}) is not exist")
            }
            val localToolFile = File(codeccWorkspace, shareToolFile.name)
            shareToolFile.copyTo(localToolFile, true)
            return localToolFile.canonicalPath
        } else {
            WindowsCodeccConstants.WINDOWS_TOOL_PY_FILE.canonicalPath
        }
    }

    fun getPython3Path(scriptType: BuildScriptType): String {
        return if (scriptType == BuildScriptType.SHELL) {
            LinuxCodeccConstants.PYTHON3_PATH.canonicalPath
        } else {
            WindowsCodeccConstants.WINDOWS_PYTHON3_PATH.canonicalPath
        }
    }

    fun getPython2Path(scriptType: BuildScriptType): String {
        return if (scriptType == BuildScriptType.SHELL) {
            LinuxCodeccConstants.PYTHON2_PATH.canonicalPath
        } else {
            WindowsCodeccConstants.WINDOWS_PYTHON2_PATH.canonicalPath
        }
    }

    fun getCovToolPath(scriptType: BuildScriptType): String {
        return if (scriptType == BuildScriptType.SHELL) {
            LinuxCodeccConstants.COVRITY_HOME
        } else {
            WindowsCodeccConstants.WINDOWS_COVRITY_HOME.canonicalPath
        }
    }

    fun getKlocToolPath(scriptType: BuildScriptType): String {
        return if (scriptType == BuildScriptType.SHELL) {
            LinuxCodeccConstants.KLOCWORK_PATH.canonicalPath
        } else {
            WindowsCodeccConstants.WINDOWS_KLOCWORK_HOME.canonicalPath
        }
    }

    fun getPyLint2Path(scriptType: BuildScriptType): String {
        return if (scriptType == BuildScriptType.SHELL) {
            LinuxCodeccConstants.PYLINT2_PATH
        } else {
            WindowsCodeccConstants.WINDOWS_PYLINT2_PATH.canonicalPath
        }
    }

    fun getPyLint3Path(scriptType: BuildScriptType): String {
        return if (scriptType == BuildScriptType.SHELL) {
            LinuxCodeccConstants.PYLINT3_PATH
        } else {
            WindowsCodeccConstants.WINDOWS_PYLINT3_PATH.canonicalPath
        }
    }

    fun getGoRootPath(scriptType: BuildScriptType): String {
        return if (scriptType == BuildScriptType.SHELL) {
            LinuxCodeccConstants.GOROOT_PATH
        } else {
            WindowsCodeccConstants.WINDOWS_GOROOT_PATH.canonicalPath
        }
    }

    fun getJdkPath(scriptType: BuildScriptType): String {
        return if (scriptType == BuildScriptType.SHELL) {
            LinuxCodeccConstants.JDK_PATH
        } else {
            WindowsCodeccConstants.WINDOWS_JDK_PATH.canonicalPath
        }
    }

    fun getNodePath(scriptType: BuildScriptType): String {
        return if (scriptType == BuildScriptType.SHELL) {
            LinuxCodeccConstants.NODE_PATH
        } else {
            WindowsCodeccConstants.WINDOWS_NODE_PATH.canonicalPath
        }
    }

    fun getGoMetaLinterPath(scriptType: BuildScriptType): String {
        return if (scriptType == BuildScriptType.SHELL) {
            LinuxCodeccConstants.GOMETALINTER_PATH
        } else {
            WindowsCodeccConstants.WINDOWS_GOMETALINTER_PATH.canonicalPath
        }
    }
}