/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

val MOUNT_PATH = System.getProperty("devops.codecc.script.path", "/data/devops/codecc")
val SCRIPT_PATH = System.getProperty("devops.codecc.script.path", "$MOUNT_PATH/script")
val SOFTWARE_PATH = System.getProperty("devops.codecc.software.path", "$MOUNT_PATH/software")

val TOOL_SCRIT_PATH = System.getProperty("evops.codecc.script.tool.path", "build.py")
val PYLINT2_PATH = System.getProperty("devops.codecc.software.pylint2.path", "pylint2")
val PYLINT3_PATH = System.getProperty("devops.codecc.software.pylint3.path", "pylint3")
val NODE_PATH = System.getProperty("devops.codecc.software.node.path", "node/bin")
val JDK_PATH = System.getProperty("devops.codecc.software.jdk.path", "jdk/bin")
val GO_PATH = System.getProperty("devops.codecc.software.go.path", "go/bin")
val GOMETALINTER_PATH = System.getProperty("devops.codecc.software.gometalinter.path", "gometalinter/bin")
val LIBZIP_PATH = System.getProperty("devops.codecc.software.libzip.path", "libzip/bin")
val PHP_PATH = System.getProperty("devops.codecc.software.php.path", "php/bin")
val PYTHON2_PATH = System.getProperty("devops.codecc.software.python2.path", "python2/bin")
val PYTHON3_PATH = System.getProperty("devops.codecc.software.python3.path", "python3/bin")
val MONO_PATH = System.getProperty("devops.codecc.software.mono.path", "mono/bin")

val GRADLE_PATH = System.getProperty("devops.codecc.software.gradle.path", "gradle/bin")
val MAVEN_PATH = System.getProperty("devops.codecc.software.maven.path", "maven/bin")
val ANT_PATH = System.getProperty("devops.codecc.software.ant.path", "ant/bin")
