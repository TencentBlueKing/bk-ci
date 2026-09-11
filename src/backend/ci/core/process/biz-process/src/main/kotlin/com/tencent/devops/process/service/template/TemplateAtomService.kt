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

package com.tencent.devops.process.service.template

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.utils.ModelUtils
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.atom.AtomUtils
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.service.AtomPropVersionOsService
import com.tencent.devops.store.api.atom.ServiceAtomResource
import com.tencent.devops.store.pojo.atom.AtomProp
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import jakarta.ws.rs.core.Response

@Service
class TemplateAtomService @Autowired constructor(
    private val templateDao: TemplateDao,
    private val dslContext: DSLContext,
    private val atomPropVersionOsService: AtomPropVersionOsService,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TemplateAtomService::class.java)
    }

    fun getTemplateAtomPropList(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long? = null
    ): Result<Map<String, AtomProp>?> {
        val srcTemplateObj = if (version != null) {
            templateDao.getTemplate(dslContext = dslContext, version = version)
        } else {
            // 版本号为空则默认查最新版本的模板
            templateDao.getLatestTemplate(dslContext, projectId, templateId)
        }
        val templateObj = if (srcTemplateObj?.type == TemplateType.CONSTRAINT.name) {
            templateDao.getTemplate(dslContext, srcTemplateObj.srcTemplateId)
        } else {
            srcTemplateObj
        }
        val modelStr = templateObj?.template
        if (modelStr.isNullOrBlank()) {
            logger.warn("The template is not exist [$projectId|$userId|$templateId]")
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_NOT_EXISTS
            )
        }
        val model = JsonUtil.to(modelStr, Model::class.java)
        // 获取流水线下插件标识集合
        val atomCodes = ModelUtils.getModelAtoms(model)
        return atomPropVersionOsService.fillVersionOsMap(
            // 约束模式的编排取自源模板，插件的项目可用性与调试版本须按编排所属项目解析，
            // 否则源模板里引用了本项目未安装的插件时，整批查询会失败而丢掉全部适用操作系统。
            // 研发商店的公共模板 PROJECT_ID 为空串，此时回退到当前项目
            projectId = templateObj.projectId?.takeIf { it.isNotBlank() } ?: projectId,
            atomVersions = AtomUtils.getModelAtomVersions(model),
            atomPropResult = client.get(ServiceAtomResource::class).getAtomProps(atomCodes),
            // 模板不归属某条流水线，无自身渠道记录可取，只能以请求渠道为准
            channelCode = ChannelCode.getRequestChannelCode()
        )
    }
}
