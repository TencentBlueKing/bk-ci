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

package com.tencent.devops.gitci.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.gitci.dao.GitProjectConfDao
import com.tencent.devops.gitci.pojo.GitProjectConf
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class GitProjectConfService @Autowired constructor(
    private val dslContext: DSLContext,
    private val gitProjectConfDao: GitProjectConfDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitProjectConfService::class.java)
    }

    fun create(gitProjectId: Long, name: String, url: String, enable: Boolean): Boolean {
        logger.info("Create git project, id: $gitProjectId, name: $name, url: $url, enable: $enable")
        val record = gitProjectConfDao.get(dslContext, gitProjectId)
        if (null != record) {
            throw CustomException(Response.Status.BAD_REQUEST, "项目已存在")
        }
        gitProjectConfDao.create(dslContext, gitProjectId, name, url, enable)
        return true
    }

    fun update(gitProjectId: Long, name: String?, url: String?, enable: Boolean?): Boolean {
        logger.info("update git project, id: $gitProjectId, name: $name, url: $url, enable: $enable")
        gitProjectConfDao.get(dslContext, gitProjectId) ?: throw CustomException(Response.Status.BAD_REQUEST, "项目不存在")
        gitProjectConfDao.update(dslContext, gitProjectId, name, url, enable)
        return true
    }

    fun delete(gitProjectId: Long): Boolean {
        logger.info("Delete git project, id: $gitProjectId")
        gitProjectConfDao.delete(dslContext, gitProjectId)
        return true
    }

    fun list(gitProjectId: Long?, name: String?, url: String?, page: Int, pageSize: Int): List<GitProjectConf> {
        return gitProjectConfDao.getList(dslContext, gitProjectId, name, url, page, pageSize).map {
            GitProjectConf(
                    it.id,
                    it.name,
                    it.url,
                    it.enable,
                    it.createTime.timestamp(),
                    it.updateTime.timestamp()
            )
        }
    }

    fun count(gitProjectId: Long?, name: String?, url: String?): Int {
        return gitProjectConfDao.count(dslContext, gitProjectId, name, url)
    }

    fun isEnable(gitProjectId: Long): Boolean {
        val record = gitProjectConfDao.get(dslContext, gitProjectId)
        if (null != record && record.enable) {
            return true
        }
        return false
    }
}