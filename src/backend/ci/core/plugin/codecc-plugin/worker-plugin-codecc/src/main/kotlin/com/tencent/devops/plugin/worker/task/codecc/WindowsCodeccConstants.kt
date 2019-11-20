package com.tencent.devops.plugin.worker.task.codecc

import java.io.File

object WindowsCodeccConstants {

    // windows公共构建机路径
    // windows不需要安装，直接配置路径即可
    val WINDOWS_CODECC_FOLDER = File("c:/software/codecc")
    val WINDOWS_COV_PY_FILE = File(WINDOWS_CODECC_FOLDER, "script/${LinuxCodeccConstants.getCovPyFile()}")
    val WINDOWS_TOOL_PY_FILE = File(WINDOWS_CODECC_FOLDER, "script/${LinuxCodeccConstants.getToolPyFile()}")
    val WINDOWS_COVRITY_HOME = File(WINDOWS_CODECC_FOLDER, "cov-analysis-win64-2018.06")
    val WINDOWS_KLOCWORK_HOME = File(WINDOWS_CODECC_FOLDER, "kw-analysis-win64-12.3")
    val WINDOWS_PYTHON2_PATH = File(WINDOWS_CODECC_FOLDER, "Python27")
    val WINDOWS_PYTHON3_PATH = File(WINDOWS_CODECC_FOLDER, "Python-3.5.2")
    val WINDOWS_PYLINT2_PATH = File(WINDOWS_CODECC_FOLDER, "pylint_2.7")
    val WINDOWS_PYLINT3_PATH = File(WINDOWS_CODECC_FOLDER, "pylint_3.5")
    val WINDOWS_GOROOT_PATH = File(WINDOWS_CODECC_FOLDER, "go1.10.3")
    val WINDOWS_JDK_PATH = File(WINDOWS_CODECC_FOLDER, "Java/jdk1.8.0_65/bin")
    val WINDOWS_NODE_PATH = File(WINDOWS_CODECC_FOLDER, "node-v8.9.0-win-x86_eslint")
    val WINDOWS_GOMETALINTER_PATH = File(WINDOWS_CODECC_FOLDER, "gometalinter/bin")
}