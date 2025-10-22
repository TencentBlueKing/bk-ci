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

package com.tencent.devops.dockerhost.common

enum class DockerExitCodeEnum(
    val exitCode: Long,
    val errorCodeEnum: ErrorCodeEnum
) {
    DOCKER_INIT_DOWNLOAD_FAIL_CODE(20, ErrorCodeEnum.DOCKER_INIT_DOWNLOAD_ERROR),
    DOCKER_JAR_DOWNLOAD_FAIL_CODE(21, ErrorCodeEnum.DOCKER_JAR_DOWNLOAD_ERROR),
    DOCKER_INIT_CURL_FAIL_CODE(50, ErrorCodeEnum.DOCKER_INIT_CURL_ERROR),
    DOCKER_INIT_JDK_FAIL_CODE(51, ErrorCodeEnum.DOCKER_INIT_JDK_ERROR),
    DOCKER_INIT_FAIL_CODE(52, ErrorCodeEnum.DOCKER_INIT_ERROR);

    companion object {
        fun getValue(exitCode: Long): DockerExitCodeEnum? {
            values().forEach {
                if (it.exitCode == exitCode) {
                    return it
                }
            }

            return null
        }
    }
}
