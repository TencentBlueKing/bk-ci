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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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