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

import com.tencent.devops.common.pipeline.enums.DockerVersion
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_CODE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_FEATURE_RECOMMEND_FLAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_NAME
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_VERSION
import com.tencent.devops.store.dao.image.ImageDao
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import com.tencent.devops.store.pojo.image.response.SimpleImageInfo
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ImageHistoryDataService @Autowired constructor(
    private val dslContext: DSLContext,
    private val imageDao: ImageDao
) {
    private val logger = LoggerFactory.getLogger(ImageHistoryDataService::class.java)

    /**
     * 根据历史model数据获取其在商店中对应的镜像数据记录
     */
    fun tranferHistoryImage(
        userId: String,
        projectId: String,
        agentType: ImageAgentTypeEnum,
        value: String?,
        interfaceName: String? = "Anon Interface"
    ): SimpleImageInfo {
        logger.info("$interfaceName:tranferHistoryImage:Input($userId,$projectId,$agentType,$value)")
        var realImageNameTag = when (agentType) {
            ImageAgentTypeEnum.DOCKER, ImageAgentTypeEnum.IDC -> {
                if (value == DockerVersion.TLINUX1_2.value) {
                    "paas$TLINUX1_2_IMAGE"
                } else if (value == DockerVersion.TLINUX2_2.value) {
                    "paas$TLINUX2_2_IMAGE"
                } else {
                    "paas/bkdevops/$value"
                }
            }
            ImageAgentTypeEnum.PUBLIC_DEVCLOUD -> {
                "devcloud/$value"
            }
            ImageAgentTypeEnum.KUBERNETES -> {
                throw Exception("not support !")
            }
        }
        realImageNameTag = realImageNameTag.removePrefix("/")
        // 拆分repoName与tag
        val (repoName, tag) = if (realImageNameTag.contains(":")) {
            val nameAndTag = realImageNameTag.split(":")
            Pair(nameAndTag[0], nameAndTag[1])
        } else {
            Pair(realImageNameTag, "latest")
        }
        val imageRecords = imageDao.listByRepoNameAndTag(
            dslContext = dslContext,
            projectId = projectId,
            repoName = repoName,
            tag = tag
        )
        if (imageRecords != null && imageRecords.size > 0) {
            logger.info("$interfaceName:tranferHistoryImage:Inner:imageRecords.size={$imageRecords.size}")
            return SimpleImageInfo(
                code = imageRecords[0].get(KEY_IMAGE_CODE) as String,
                name = imageRecords[0].get(KEY_IMAGE_NAME) as String,
                version = imageRecords[0].get(KEY_IMAGE_VERSION) as String,
                recommendFlag = imageRecords[0].get(KEY_IMAGE_FEATURE_RECOMMEND_FLAG) as Boolean
            )
        } else {
            // 不存在这样的镜像
            return SimpleImageInfo(
                code = "",
                name = "",
                version = "",
                recommendFlag = false
            )
        }
    }

    companion object {
        private const val TLINUX1_2_IMAGE = "/bkdevops/docker-builder1.2:v1"
        private const val TLINUX2_2_IMAGE = "/bkdevops/docker-builder2.2:v1"
    }
}
