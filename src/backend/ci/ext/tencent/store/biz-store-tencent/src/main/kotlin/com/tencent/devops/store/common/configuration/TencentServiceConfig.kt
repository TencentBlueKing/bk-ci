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

package com.tencent.devops.store.common.configuration

import com.tencent.devops.store.atom.service.impl.TxAtomCooperationServiceImpl
import com.tencent.devops.store.atom.service.impl.TxAtomMemberServiceImpl
import com.tencent.devops.store.atom.service.impl.TxAtomNotifyServiceImpl
import com.tencent.devops.store.atom.service.impl.TxAtomReleaseServiceImpl
import com.tencent.devops.store.atom.service.impl.TxAtomServiceImpl
import com.tencent.devops.store.atom.service.impl.TxMarketAtomServiceImpl
import com.tencent.devops.store.common.service.TxStoreGitRepositoryService
import com.tencent.devops.store.common.service.container.impl.TxContainerServiceImpl
import com.tencent.devops.store.common.service.impl.TxStoreCommonServiceImpl
import com.tencent.devops.store.common.service.impl.TxStoreLogoServiceImpl
import com.tencent.devops.store.common.service.impl.TxStoreNotifyServiceImpl
import com.tencent.devops.store.common.service.impl.TxStoreUserServiceImpl
import com.tencent.devops.store.template.service.impl.TxMarketTemplateServiceImpl
import com.tencent.devops.store.template.service.impl.TxTemplateNotifyServiceImpl
import com.tencent.devops.store.template.service.impl.TxTemplateReleaseServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TencentServiceConfig @Autowired constructor() {

    @Bean
    fun containerService() = TxContainerServiceImpl()

    @Bean
    fun storeUserService() = TxStoreUserServiceImpl()

    @Bean
    fun storeNotifyService() = TxStoreNotifyServiceImpl()

    @Bean
    fun atomService() = TxAtomServiceImpl()

    @Bean
    fun marketAtomService() = TxMarketAtomServiceImpl()

    @Bean
    fun atomMemberService(
        gitRepositoryService: TxStoreGitRepositoryService
    ) = TxAtomMemberServiceImpl(gitRepositoryService)

    @Bean
    fun atomReleaseService() = TxAtomReleaseServiceImpl()

    @Bean
    fun atomNotifyService() = TxAtomNotifyServiceImpl()

    @Bean
    fun atomCooperationService() = TxAtomCooperationServiceImpl()

    @Bean
    fun templateNotifyService() = TxTemplateNotifyServiceImpl()

    @Bean
    fun marketTemplateService() = TxMarketTemplateServiceImpl()

    @Bean
    fun templateReleaseService() = TxTemplateReleaseServiceImpl()

    @Bean
    fun storeLogoService() = TxStoreLogoServiceImpl()

    @Bean
    fun storeCommonService() = TxStoreCommonServiceImpl()
}
