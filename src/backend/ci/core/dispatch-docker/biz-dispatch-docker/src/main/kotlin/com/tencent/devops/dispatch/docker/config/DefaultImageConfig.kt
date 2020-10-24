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

package com.tencent.devops.dispatch.docker.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class DefaultImageConfig {
    // 编译环境，末尾不含bkdevops
    @Value("\${dispatch.dockerBuildImagePrefix:#{null}}")
    val dockerBuildImagePrefix: String? = null

    @Value("\${dispatch.imageTLinux1_2:bkci/ci:latest}")
    val imageTLinux1_2: String? = null

    @Value("\${dispatch.imageTLinux2_2:bkci/ci:latest}")
    val imageTLinux2_2: String? = null

    // 无编译环境，末尾需含bkdevops
    @Value("\${dispatch.dockerBuildLessImagePrefix:#{null}}")
    var dockerBuildLessImagePrefix: String? = null

    @Value("\${dispatch.imageBuildLessTLinux1_2:bkci/ci:alpine}")
    val imageBuildLessTLinux1_2: String? = null

    @Value("\${dispatch.imageBuildLessTLinux2_2:bkci/ci:alpine}")
    val imageBuildLessTLinux2_2: String? = null

    fun getBuildLessTLinux1_2CompleteUri(): String {
        return if (dockerBuildLessImagePrefix.isNullOrBlank()) {
            imageBuildLessTLinux1_2?.trim()?.removePrefix("/")
        } else {
            dockerBuildLessImagePrefix + imageBuildLessTLinux1_2?.trim()
        } ?: ""
    }

    fun getBuildLessTLinux2_2CompleteUri(): String {
        return if (dockerBuildLessImagePrefix.isNullOrBlank()) {
            imageBuildLessTLinux2_2?.trim()?.removePrefix("/")
        } else {
            dockerBuildLessImagePrefix + imageBuildLessTLinux2_2?.trim()
        } ?: ""
    }

    fun getBuildLessCompleteUriByImageName(imageName: String?): String {
        return if (dockerBuildLessImagePrefix.isNullOrBlank()) {
            imageName?.trim()?.removePrefix("/")
        } else {
            "$dockerBuildLessImagePrefix/${imageName?.trim()}"
        } ?: ""
    }

    fun getTLinux1_2CompleteUri(): String {
        return if (dockerBuildImagePrefix.isNullOrBlank()) {
            imageTLinux1_2?.trim()?.removePrefix("/")
        } else {
            dockerBuildImagePrefix + imageTLinux1_2
        } ?: ""
    }

    fun getTLinux2_2CompleteUri(): String {
        return if (dockerBuildImagePrefix.isNullOrBlank()) {
            imageTLinux2_2?.trim()?.removePrefix("/")
        } else {
            dockerBuildImagePrefix + imageTLinux2_2
        } ?: ""
    }

    fun getCompleteUriByImageName(imageName: String?): String {
        return if (dockerBuildImagePrefix.isNullOrBlank()) {
            imageName?.trim()?.removePrefix("/")
        } else {
            "$dockerBuildImagePrefix/$imageName"
        } ?: ""
    }
}