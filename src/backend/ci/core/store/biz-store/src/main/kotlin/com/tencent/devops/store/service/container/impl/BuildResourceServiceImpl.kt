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

package com.tencent.devops.store.service.container.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.dao.container.BuildResourceDao
import com.tencent.devops.store.pojo.container.BuildResource
import com.tencent.devops.store.service.container.BuildResourceService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 构建资源逻辑类
 *
 * since: 2018-12-20
 */
@Service
class BuildResourceServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val buildResourceDao: BuildResourceDao
) : BuildResourceService {

    private val logger = LoggerFactory.getLogger(BuildResourceServiceImpl::class.java)

    override fun getDefaultBuildResource(buildType: BuildType): Any? {
        logger.info("Input(${buildType.name})")
        return null
    }

    /**
     * 获取所有构建资源信息
     */
    override fun getAllPipelineBuildResource(): Result<List<BuildResource>> {
        val pipelineBuildResourceList =
            buildResourceDao.getAllBuildResource(dslContext).map { buildResourceDao.convert(it) }
        return Result(pipelineBuildResourceList)
    }

    /**
     * 根据id获取构建资源信息
     */
    override fun getPipelineBuildResource(id: String): Result<BuildResource?> {
        val pipelineBuildResourceRecord = buildResourceDao.getBuildResource(dslContext, id)
        return Result(
            if (pipelineBuildResourceRecord == null) {
                null
            } else {
                buildResourceDao.convert(pipelineBuildResourceRecord)
            }
        )
    }

    /**
     * 根据构建资源编码查询数据库记录数
     */
    override fun getCountByCode(buildResourceCode: String): Int {
        val recordList = buildResourceDao.countByCode(dslContext, buildResourceCode)
        var result = 0
        if (recordList != null) {
            result = recordList.get(0) as Int
        }
        return result
    }

    /**
     * 保存构建资源信息
     */
    override fun savePipelineBuildResource(
        defaultFlag: Boolean,
        buildResourceCode: String,
        buildResourceName: String
    ): Result<Boolean> {
        // 判断构建资源代码是否存在
        val count = getCountByCode(buildResourceCode)
        if (count > 0) {
            // 抛出错误提示
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(buildResourceCode),
                data = false,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }
        buildResourceDao.add(
            dslContext = dslContext,
            id = UUIDUtil.generate(),
            defaultFlag = defaultFlag,
            buildResourceCode = buildResourceCode,
            buildResourceName = buildResourceName
        )
        return Result(true)
    }

    /**
     * 更新构建资源信息
     */
    override fun updatePipelineBuildResource(
        id: String,
        defaultFlag: Boolean,
        buildResourceCode: String,
        buildResourceName: String
    ): Result<Boolean> {
        // 判断构建资源代码是否存在
        val count = getCountByCode(buildResourceCode)
        if (count > 0) {
            // 判断更新的构建资源代码是否属于自已
            val pipelineBuildResource = buildResourceDao.getBuildResource(dslContext, id)
            if (null != pipelineBuildResource && buildResourceCode != pipelineBuildResource.buildResourceCode) {
                // 抛出错误提示
                return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                    params = arrayOf(buildResourceCode),
                    data = false
                )
            }
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            // 如果要更改为默认资源，先把表里面所有资源更新为非默认资源，然后再将该资源更新为默认的
            if (defaultFlag) {
                buildResourceDao.setAllBuildResourceDefaultFlag(context, false)
            }
            buildResourceDao.update(
                dslContext = context,
                id = id,
                defaultFlag = defaultFlag,
                buildResourceCode = buildResourceCode,
                buildResourceName = buildResourceName
            )
        }
        return Result(true)
    }

    /**
     * 删除构建资源信息
     */
    override fun deletePipelineBuildResource(id: String): Result<Boolean> {
        buildResourceDao.delete(dslContext, id)
        return Result(true)
    }
}
