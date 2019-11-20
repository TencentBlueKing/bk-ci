package com.tencent.devops.plugin.worker.task.codecc

import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.env.BuildEnv
import com.tencent.devops.worker.common.utils.WorkspaceUtils
import java.io.File

object LinuxCodeccConstants {

    const val SVN_USER = "SVN_USER"
    const val SVN_PASSWORD = "SVN_PASSWORD"
    val COV_TOOLS = listOf("COVERITY", "KLOCWORK", "GOCILINT")

    // 1. 公共构建机参数
    private val CODECC_FOLDER = File("/data/bkdevops/apps/coverity")

    // 2. 第三方构建机相关参数
    val THIRD_CODECC_FOLDER = WorkspaceUtils.getLandun().canonicalPath + File.separator + "codecc"

    // 2.1 第三方构建机需要下载的文件
    val THIRD_PYTHON2_TAR_FILE = File(THIRD_CODECC_FOLDER, "Python-2.7.12.tgz")
    val THIRD_PYTHON3_TAR_FILE = File(THIRD_CODECC_FOLDER, "Python-3.5.1.tgz")
    val THIRD_COVERITY_FILE = File(
        THIRD_CODECC_FOLDER, if (AgentEnv.getOS() == OSType.LINUX) {
        if (AgentEnv.is32BitSystem()) "cov-analysis-linux-2018.03.tar.gz"
        else "cov-analysis-linux64-2018.03.tar.gz"
    } else {
        "cov-analysis-macosx-2018.06.tar.gz"
    })
    val THIRD_KLOCWORK_FILE = File(
        THIRD_CODECC_FOLDER, if (AgentEnv.getOS() == OSType.LINUX) {
        if (AgentEnv.is32BitSystem()) "kw-analysis-linux-12.3.tar.gz"
        else "kw-analysis-linux64-12.3.tar.gz"
    } else {
        "kw-analysis-macosx-12.3.tar.gz"
    })
    val THIRD_PYLINT2_FILE = File(THIRD_CODECC_FOLDER, "pylint_2.7.zip")
    val THIRD_PYLINT3_FILE = File(THIRD_CODECC_FOLDER, "pylint_3.5.zip")
    val THIRD_GOROOT_FILE = File(
        THIRD_CODECC_FOLDER, if (AgentEnv.getOS() == OSType.LINUX) {
        if (AgentEnv.is32BitSystem()) "go1.9.2.linux-386.tar.gz"
        else "go1.9.2.linux-amd64.tar.gz"
    } else {
        "go1.9.2.darwin-amd64.tar.gz"
    })
    val THIRD_JDK_FILE = if (AgentEnv.getOS() == OSType.LINUX) {
        if (AgentEnv.is32BitSystem()) File(THIRD_CODECC_FOLDER, "jdk-8u191-linux-x64.tar.gz")
        else File(THIRD_CODECC_FOLDER, "jdk-8u191-linux-i586.tar.gz")
    } else {
        File(THIRD_CODECC_FOLDER, "jdk-8u191-macosx-x64.dmg")
    }
    val THIRD_NODE_FILE = File(THIRD_CODECC_FOLDER, "node-v8.9.0-linux-x64_eslint.tar.gz")
    val THIRD_GOMETALINTER_FILE = if (AgentEnv.getOS() == OSType.MAC_OS) {
        File(THIRD_CODECC_FOLDER, "gometalinter_macos.zip")
    } else {
        File(THIRD_CODECC_FOLDER, "gometalinter_linux.zip")
    }

    val COVRITY_HOME = if (BuildEnv.isThirdParty()) {
        THIRD_COVERITY_FILE.canonicalPath.removeSuffix(".tar.gz")
    } else {
        File(CODECC_FOLDER, if (AgentEnv.getOS() == OSType.MAC_OS) "cov-analysis-macosx" else "cov-analysis-linux").canonicalPath
    }

    val KLOCWORK_PATH = if (BuildEnv.isThirdParty()) {
        File(THIRD_CODECC_FOLDER, THIRD_KLOCWORK_FILE.name)
    } else {
        File("/data/bkdevops/apps/codecc/kw-analysis/bin")
    }

    val PYTHON2_PATH = if (BuildEnv.isThirdParty()) {
        if (AgentEnv.getOS() == OSType.MAC_OS) {
            File("/usr/bin")
        } else {
            File(THIRD_CODECC_FOLDER, "Python-2.7.12/bin")
        }
    } else {
        if (AgentEnv.getOS() == OSType.MAC_OS) {
            File("/usr/bin")
        } else {
            File("/data/bkdevops/apps/python/2.7.12/bin")
        }
    }

    val PYTHON3_PATH = if (BuildEnv.isThirdParty()) {
        if (AgentEnv.getOS() == OSType.MAC_OS) {
            File("/data/bkdevops/apps/python/3.5/IDLE.app/Contents/MacOS")
        } else {
            File(THIRD_CODECC_FOLDER, "Python-3.5.1/bin")
        }
    } else {
        if (AgentEnv.getOS() == OSType.MAC_OS) {
            File("/data/bkdevops/apps/python/3.5/IDLE.app/Contents/MacOS")
        } else {
            File("/data/bkdevops/apps/python/3.5.1/bin")
        }
    }

    val JDK_PATH = if (BuildEnv.isThirdParty()) {
        File(THIRD_CODECC_FOLDER, "jdk1.8.0_191/bin").canonicalPath
    } else {
        if (AgentEnv.getOS() == OSType.MAC_OS) {
            "/data/soda/apps/jdk/1.8.0_161/Contents/Home/bin"
        } else {
            "/data/bkdevops/apps/jdk/1.8.0_161/bin"
        }
    }
    val NODE_PATH = if (BuildEnv.isThirdParty()) {
        File(THIRD_CODECC_FOLDER, "node-v8.9.0-linux-x64_eslint/bin").canonicalPath
    } else {
        "/data/bkdevops/apps/codecc/node-v8.9.0-linux-x64/bin"
    }
    val GOMETALINTER_PATH = if (BuildEnv.isThirdParty()) {
        File(THIRD_CODECC_FOLDER, "gometalinter/bin").canonicalPath
    } else {
        "/data/bkdevops/apps/codecc/gometalinter/bin"
    }
    val PYLINT2_PATH = if (BuildEnv.isThirdParty()) {
        File(THIRD_CODECC_FOLDER, "mypylint_2.7").canonicalPath
    } else {
        "/data/bkdevops/apps/codecc/pylint_2.7"
    }
    val PYLINT3_PATH = if (BuildEnv.isThirdParty()) {
        File(THIRD_CODECC_FOLDER, "mypylint_3.5").canonicalPath
    } else {
        "/data/bkdevops/apps/codecc/pylint_3.5"
    }
    val GOROOT_PATH = if (BuildEnv.isThirdParty()) {
        File(THIRD_CODECC_FOLDER, "go/bin").canonicalPath
    } else {
        "/data/bkdevops/apps/codecc/go/bin"
    }
    val STYLE_TOOL_PATH = if (BuildEnv.isThirdParty()) {
        File(THIRD_CODECC_FOLDER, "").canonicalPath // 暂时不支持第三方机器
    } else {
        "/data/bkdevops/apps/codecc/mono/bin"
    }
    val PHPCS_TOOL_PATH = if (BuildEnv.isThirdParty()) {
        File(THIRD_CODECC_FOLDER, "").canonicalPath // 暂时不支持第三方机器
    } else {
        "/data/bkdevops/apps/codecc/php/bin"
    }
    val GOROOT_PATH_12 = if (BuildEnv.isThirdParty()) {
        File(THIRD_CODECC_FOLDER, "golang1.12.9/bin").canonicalPath
    } else {
        "/data/bkdevops/apps/codecc/golang1.12.9/bin"
    }
    val GO_CI_LINT_PATH = if (BuildEnv.isThirdParty()) {
        File(THIRD_CODECC_FOLDER, "gocilint1.17.1").canonicalPath
    } else {
        "/data/bkdevops/apps/codecc/gocilint1.17.1"
    }

    fun getCovPyFile(): File {
        val covPyFile = when {
            AgentEnv.isDev() -> "build_external_dev.py"
            AgentEnv.isTest() -> "build_external_test.py"
            else -> "build_external_prod.py"
        }
        return if (BuildEnv.isThirdParty()) File(THIRD_CODECC_FOLDER, covPyFile)
        else File(CODECC_FOLDER, covPyFile)
    }

    fun getToolPyFile(): File {
        val toolPyFile =  when {
            AgentEnv.isDev() -> "build_tool_external_dev.py"
            AgentEnv.isTest() -> "build_tool_external_test.py"
            else -> "build_tool_external_prod.py"
        }
        return if (BuildEnv.isThirdParty()) File(THIRD_CODECC_FOLDER, toolPyFile)
        else File(CODECC_FOLDER, toolPyFile)
    }
}