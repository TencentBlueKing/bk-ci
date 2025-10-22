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

package com.tencent.devops.dockerhost.services.generator

import com.github.dockerjava.api.model.Mount
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.dockerhost.services.container.ContainerHandlerContext
import com.tencent.devops.dockerhost.services.generator.annotation.MountGenerator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException

object DockerMountLoader {

    private val logger: Logger = LoggerFactory.getLogger(DockerMountLoader::class.java)

    @Suppress("UNCHECKED_CAST")
    fun loadMounts(handlerContext: ContainerHandlerContext): List<Mount> {

        val mountList = mutableListOf<Mount>()
        try {
            val generators: List<DockerMountGenerator> =
                SpringContextUtil.getBeansWithAnnotation(MountGenerator::class.java) as List<DockerMountGenerator>
            generators.forEach { generator ->
                mountList.addAll(generator.generateMounts(handlerContext))
            }
        } catch (notFound: BeansException) {
            logger.warn("${handlerContext.buildId}|${handlerContext.vmSeqId} not found mount generator.", notFound)
        } catch (ignored: Throwable) {
            logger.error("${handlerContext.buildId}|${handlerContext.vmSeqId} load mounts failed.", ignored)
        }

        return mountList
    }
}
