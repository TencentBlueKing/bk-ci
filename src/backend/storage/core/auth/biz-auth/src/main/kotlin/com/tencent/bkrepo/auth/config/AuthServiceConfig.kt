/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.auth.config

import com.tencent.bkrepo.auth.repository.AccountRepository
import com.tencent.bkrepo.auth.repository.OauthTokenRepository
import com.tencent.bkrepo.auth.repository.PermissionRepository
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.auth.service.AccountService
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.auth.service.RoleService
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.auth.service.bkauth.BkAuthPermissionServiceImpl
import com.tencent.bkrepo.auth.service.bkauth.BkAuthPipelineService
import com.tencent.bkrepo.auth.service.bkauth.BkAuthProjectService
import com.tencent.bkrepo.auth.service.bkiam.BkiamPermissionServiceImpl
import com.tencent.bkrepo.auth.service.bkiam.BkiamService
import com.tencent.bkrepo.auth.service.local.AccountServiceImpl
import com.tencent.bkrepo.auth.service.local.PermissionServiceImpl
import com.tencent.bkrepo.auth.service.local.RoleServiceImpl
import com.tencent.bkrepo.auth.service.local.UserServiceImpl
import com.tencent.bkrepo.repository.api.ProjectClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.core.Ordered
import org.springframework.data.mongodb.core.MongoTemplate

@Configuration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class AuthServiceConfig {

    @Autowired
    @Lazy
    private lateinit var repositoryClient: RepositoryClient

    @Autowired
    @Lazy
    private lateinit var projectClient: ProjectClient

    @Bean
    @ConditionalOnMissingBean(AccountService::class)
    fun accountService(
        accountRepository: AccountRepository,
        oauthTokenRepository: OauthTokenRepository,
        userService: UserService,
        mongoTemplate: MongoTemplate
    ) = AccountServiceImpl(accountRepository, oauthTokenRepository, userService, mongoTemplate)

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = "local", matchIfMissing = true)
    fun permissionService(
        userRepository: UserRepository,
        roleRepository: RoleRepository,
        permissionRepository: PermissionRepository,
        mongoTemplate: MongoTemplate
    ): PermissionService {
        logger.debug("init PermissionServiceImpl")
        return PermissionServiceImpl(
            userRepository,
            roleRepository,
            permissionRepository,
            mongoTemplate,
            repositoryClient,
            projectClient
        )
    }

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = "bkiam")
    fun bkiamPermissionService(
        userRepository: UserRepository,
        roleRepository: RoleRepository,
        permissionRepository: PermissionRepository,
        mongoTemplate: MongoTemplate,
        bkiamService: BkiamService
    ): PermissionService {
        logger.debug("init BkiamPermissionServiceImpl")
        return BkiamPermissionServiceImpl(
            userRepository,
            roleRepository,
            permissionRepository,
            mongoTemplate,
            repositoryClient,
            projectClient,
            bkiamService
        )
    }

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = "devops")
    fun bkAuthPermissionService(
        userRepository: UserRepository,
        roleRepository: RoleRepository,
        permissionRepository: PermissionRepository,
        mongoTemplate: MongoTemplate,
        bkAuthConfig: BkAuthConfig,
        bkAuthPipelineService: BkAuthPipelineService,
        bkAuthProjectService: BkAuthProjectService
    ): PermissionService {
        logger.debug("init BkAuthPermissionServiceImpl")
        return BkAuthPermissionServiceImpl(
            userRepository,
            roleRepository,
            permissionRepository,
            mongoTemplate,
            repositoryClient,
            projectClient,
            bkAuthConfig,
            bkAuthPipelineService,
            bkAuthProjectService
        )
    }

    @Bean
    @ConditionalOnMissingBean(RoleService::class)
    fun roleService(
        roleRepository: RoleRepository,
        userService: UserService,
        userRepository: UserRepository,
        mongoTemplate: MongoTemplate
    ) = RoleServiceImpl(roleRepository, userService, userRepository, mongoTemplate)

    @Bean
    @ConditionalOnMissingBean(UserService::class)
    fun userService(
        userRepository: UserRepository,
        roleRepository: RoleRepository,
        mongoTemplate: MongoTemplate
    ) = UserServiceImpl(userRepository, roleRepository, mongoTemplate)

    companion object {
        private val logger = LoggerFactory.getLogger(AuthServiceConfig::class.java)
    }
}
