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

package com.tencent.devops.store.service.image

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.store.dao.common.BusinessConfigDao
import com.tencent.devops.store.dao.container.BuildResourceDao
import com.tencent.devops.store.pojo.common.enums.BusinessEnum
import com.tencent.devops.store.pojo.image.response.BaseImageInfo
import com.tencent.devops.store.service.container.impl.BuildResourceServiceImpl
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

/**
 * 含镜像的构建资源扩展逻辑类
 *
 * since: 2018-12-20
 */
@Suppress("ALL")
@Service
@Primary
class ImageBuildResourceServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val buildResourceDao: BuildResourceDao,
    private val businessConfigDao: BusinessConfigDao
) : BuildResourceServiceImpl(dslContext, buildResourceDao) {

    private val logger = LoggerFactory.getLogger(ImageBuildResourceServiceImpl::class.java)

    override fun getDefaultBuildResource(buildType: BuildType): Any? {
        logger.info("getDefaultBuildResource buildType=${buildType.name}")
        if (buildType.name == BuildType.DOCKER.name ||
            buildType.name == BuildType.PUBLIC_DEVCLOUD.name ||
            buildType.name == BuildType.KUBERNETES.name ||
            buildType.name == BuildType.PUBLIC_BCS.name) {
            val record = businessConfigDao.get(
                dslContext = dslContext,
                business = BusinessEnum.BUILD_TYPE.name,
                feature = "defaultBuildResource",
                businessValue = buildType.name
            )
            if (record == null) {
                logger.warn("defaultBuildResource of ${buildType.name} not configed, plz config in op")
                return null
            } else {
                return try {
                    logger.info("configValue=${record.configValue}")
                    val baseImageInfo = JsonUtil.to(record.configValue, BaseImageInfo::class.java)
                    if (baseImageInfo.imageType == null) {
                        baseImageInfo.imageType = ImageType.BKSTORE.name
                    }
                    if (baseImageInfo.imageType.equals(ImageType.BKSTORE.name)) {
                        baseImageInfo.value = baseImageInfo.code
                    }
                    baseImageInfo
                } catch (ignored: Throwable) {
                    logger.error("BKSystemErrorMonitor|getDefaultBuildResource|${record.configValue}|" +
                        "error=${ignored.message}", ignored)
                    null
                }
            }
        } else {
            logger.info("BuildType ${buildType.name} does not need image")
            return null
        }
    }
}
