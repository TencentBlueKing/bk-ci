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

package com.tencent.devops.artifactory.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 研发商店仓库配置
 */
@Component
class BkRepoStoreConfig {

    // 蓝盾新仓库研发商店项目名称
    @Value("\${bkrepo.store.projectName:bk-store}")
    val bkrepoStoreProjectName: String = "bk-store"

    // 蓝盾新仓库研发商店用户名
    @Value("\${bkrepo.store.userName:g_bkstore}")
    val bkrepoStoreUserName: String = "g_bkstore"

    // 蓝盾新仓库研发商店密码
    @Value("\${bkrepo.store.password:}")
    val bkrepoStorePassword: String = ""

    // 蓝盾新仓库微扩展项目名称
    @Value("\${bkrepo.extService.projectName:bk-extension}")
    val bkrepoExtServiceProjectName: String = "bk-extension"

    // 蓝盾新仓库微扩展用户名
    @Value("\${bkrepo.extService.userName:bk_extension}")
    val bkrepoExtServiceUserName: String = "bk_extension"

    // 蓝盾新仓库微扩展密码
    @Value("\${bkrepo.extService.password:}")
    val bkrepoExtServicePassword: String = ""
}
