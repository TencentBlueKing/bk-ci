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

package com.tencent.devops.stream.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.stream.dao.GitCIServicesConfDao
import com.tencent.devops.stream.pojo.GitCIServicesConf
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import javax.ws.rs.core.Response

@Service
class GitCIServicesConfService @Autowired constructor(
    private val dslContext: DSLContext,
    private val gitCIServicesConfDao: GitCIServicesConfDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitCIServicesConfService::class.java)
    }

    fun create(userId: String, gitCIServicesConf: GitCIServicesConf): Boolean {
        logger.info("GitCIServicesConfService|Create|user|$userId|gitCIServicesConf|$gitCIServicesConf")
        try {
            gitCIServicesConfDao.create(
                dslContext = dslContext,
                imageName = gitCIServicesConf.imageName,
                imageTag = gitCIServicesConf.imageTag,
                repoUrl = gitCIServicesConf.repoUrl,
                repoUsername = gitCIServicesConf.repoUsername,
                repoPwd = gitCIServicesConf.repoPwd,
                enable = gitCIServicesConf.enable,
                env = gitCIServicesConf.env,
                createUser = gitCIServicesConf.createUser,
                updateUser = gitCIServicesConf.updateUser
            )
            return true
        } catch (e: Exception) {
            logger.warn("GitCIServicesConfService|Create|error=${e.message}")
            throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "Create git service failed.")
        }
    }

    fun update(userId: String, id: Long, enable: Boolean?): Boolean {
        logger.info("GitCIServicesConfService|update|user|$userId|id|$id")
        try {
            gitCIServicesConfDao.update(dslContext, id, userId, enable)
            return true
        } catch (e: Exception) {
            logger.warn("GitCIServicesConfService|update|error=${e.message}")
            throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "update git service failed.")
        }
    }

    fun delete(userId: String, id: Long): Boolean {
        logger.info("GitCIServicesConfService|Delete|user|$userId|id|$id")
        try {
            gitCIServicesConfDao.delete(dslContext, id)
            return true
        } catch (e: Exception) {
            logger.warn("GitCIServicesConfService|Delete|error=${e.message}")
            throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "Delete git service failed.")
        }
    }

    fun list(userId: String): List<GitCIServicesConf> {
        try {
            val list = gitCIServicesConfDao.list(dslContext)
            val resultList = mutableListOf<GitCIServicesConf>()
            list.forEach {
                resultList.add(
                    GitCIServicesConf(
                        id = it.id,
                        imageName = it.imageName,
                        imageTag = it.imageTag,
                        repoUrl = it.repoUrl,
                        repoUsername = it.repoUsername,
                        repoPwd = it.repoPwd,
                        enable = it.enable,
                        env = it.env,
                        createUser = it.createUser,
                        updateUser = it.updateUser,
                        createTime = it.gmtCreate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        updateTime = it.gmtModified.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    )
                )
            }
            return resultList
        } catch (e: Exception) {
            logger.warn("GitCIServicesConfService|List|error=${e.message}")
            throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "List git service failed.")
        }
    }
}
