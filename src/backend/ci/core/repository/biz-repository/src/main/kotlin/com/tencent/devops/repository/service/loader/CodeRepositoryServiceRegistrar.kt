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

package com.tencent.devops.repository.service.loader

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.CodeP4Repository
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.CodeTGitRepository
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.service.code.CodeRepositoryService
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

object CodeRepositoryServiceRegistrar {
    private val logger = LoggerFactory.getLogger(CodeRepositoryServiceRegistrar::class.java)

    private val repositoryServiceMap = ConcurrentHashMap<String, CodeRepositoryService<*>>()

    /**
     * 注册[CodeRepositoryService]代码库业务处理器
     */
    fun register(repositoryHandler: CodeRepositoryService<*>) {
        logger.info("[REGISTER]| ${repositoryHandler.javaClass} for ${repositoryHandler.repositoryType()}")
        repositoryServiceMap[repositoryHandler.repositoryType()] = repositoryHandler
    }

    /**
     * 读取指定[CodeRepositoryService]代码库业务处理器
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Repository> getService(repository: T): CodeRepositoryService<T> {
        val repositoryService = repositoryServiceMap[repository::class.java.name]
            ?: throw IllegalArgumentException("${repository::class.java.name} handler is not found")
        return (repositoryService as CodeRepositoryService<T>)
    }

    /**
     * 根据类型读取指定[CodeRepositoryService]代码库业务处理器
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getService(type: Class<out T>?): CodeRepositoryService<T> {
        if (type != null) {
            val repositoryService = repositoryServiceMap[type.name]
                ?: throw IllegalArgumentException("${type.name} handler is not found")
            return repositoryService as CodeRepositoryService<T>
        } else {
            throw IllegalArgumentException("Unknown repository type")
        }
    }

    /**
     * 读取指定[CodeRepositoryService]代码库业务处理器
     */
    @Suppress("UNCHECKED_CAST")
    fun getServiceByScmType(scmType: String): CodeRepositoryService<*> {
        val repositoryService = when (ScmType.valueOf(scmType)) {
            ScmType.CODE_SVN -> {
                getService(CodeSvnRepository::class.java)
            }
            ScmType.CODE_GIT -> {
                getService(CodeGitRepository::class.java)
            }
            ScmType.CODE_TGIT -> {
                getService(CodeTGitRepository::class.java)
            }
            ScmType.CODE_GITLAB -> {
                getService(CodeGitlabRepository::class.java)
            }
            ScmType.GITHUB -> {
                getService(GithubRepository::class.java)
            }
            ScmType.CODE_P4 -> {
                getService(CodeP4Repository::class.java)
            }
            else -> throw IllegalArgumentException("Unknown repository type")
        }
        return repositoryService
    }
}
