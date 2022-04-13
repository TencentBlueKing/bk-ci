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
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.CiYamlUtils
import com.tencent.devops.common.ci.yaml.CIBuildYaml
import com.tencent.devops.stream.dao.GitCIServicesConfDao
import com.tencent.devops.stream.dao.GitCISettingDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.pojo.EnvironmentVariables
import com.tencent.devops.stream.pojo.v2.V2BuildYaml
import com.tencent.devops.stream.v2.dao.StreamBasicSettingDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.StringReader
import javax.ws.rs.core.Response

@Service
class StreamYamlService @Autowired constructor(
    private val dslContext: DSLContext,
    private val streamSettingDao: StreamBasicSettingDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamYamlService::class.java)
    }

    fun getYamlV2(gitProjectId: Long, buildId: String): V2BuildYaml? {
        logger.info("get yaml by buildId:($buildId), gitProjectId: $gitProjectId")
        streamSettingDao.getSetting(dslContext, gitProjectId) ?: throw CustomException(
            Response.Status.FORBIDDEN,
            "项目未开启Stream，无法查询"
        )
        val eventBuild = gitRequestEventBuildDao.getByBuildId(dslContext, buildId) ?: return null
        // 针对V2版本做替换
        val parsed = eventBuild.parsedYaml.replaceFirst("triggerOn:", "on:")
        return V2BuildYaml(parsedYaml = parsed, originYaml = eventBuild.originYaml)
    }
}
