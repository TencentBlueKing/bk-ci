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

package com.tencent.devops.prebuild.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class PreBuildConfig {

    @Value("\${codeCC.clientImage:#{null}}")
    val codeCCSofwareClientImage: String? = null

    @Value("\${codeCC.softwarePath:#{null}}")
    val codeCCSofwarePath: String? = null

    @Value("\${registry.host:#{null}}")
    val registryHost: String? = null

    @Value("\${registry.userName:#{null}}")
    val registryUserName: String? = null

    @Value("\${registry.password:#{null}}")
    val registryPassword: String? = null

    @Value("\${registry.image:#{null}}")
    val registryImage: String? = null

    @Value("\${devCloud.cpu:16}")
    val cpu: Int = 16

    @Value("\${devCloud.memory:32767M}")
    val memory: String = "32767M"

    @Value("\${devCloud.disk:50G}")
    val disk: String = "50G"

    @Value("\${devCloud.volume:100}")
    val volume: Int = 100

    @Value("\${devCloud.activeDeadlineSeconds:86400}")
    val activeDeadlineSeconds: Int = 86400

    @Value("\${devCloud.appId:#{null}}")
    val devCloudAppId: String = ""

    @Value("\${devCloud.token:#{null}}")
    val devCloudToken: String = ""

    @Value("\${devCloud.url:#{null}}")
    val devCloudUrl: String = ""
}
