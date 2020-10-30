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

package com.tencent.devops.process.engine.atom

import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_ATOM_NOT_FOUND
import com.tencent.devops.process.engine.exception.BuildTaskException
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.store.api.atom.ServiceAtomResource
import com.tencent.devops.store.api.atom.ServiceMarketAtomEnvResource
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import java.util.concurrent.TimeUnit

object AtomUtils {

    private val atomCache = CacheBuilder.newBuilder()
            .maximumSize(10000).expireAfterWrite(24, TimeUnit.HOURS).build<String, String>()

    fun <T> parseAtomBeanName(task: Class<T>): String {
        val taskAtomClass = task.simpleName
        return taskAtomClass[0].toLowerCase() + taskAtomClass.substring(1)
    }

    /**
     * 解析出Container中的市场插件，如果市场插件相应版本找不到就抛出异常
     */
    fun parseContainerMarketAtom(
        container: Container,
        task: PipelineBuildTask,
        client: Client,
        buildLogPrinter: BuildLogPrinter
    ): MutableMap<String, String> {
        val atoms = mutableMapOf<String, String>()
        val serviceMarketAtomEnvResource = client.get(ServiceMarketAtomEnvResource::class)
        container.elements.forEach { element ->
            if (element is MarketBuildAtomElement || element is MarketBuildLessAtomElement) {
                var version = element.version
                if (version.isBlank()) {
                    version = "1.*"
                }
                val atomCode = element.getAtomCode()
                val atomEnvResult = serviceMarketAtomEnvResource.getAtomEnv(task.projectId, atomCode, version)
                val atomEnv = atomEnvResult.data
                if (atomEnvResult.isNotOk() || atomEnv == null) {
                    val message = "Can not found task($atomCode):${element.name}| ${atomEnvResult.message}"
                    throw BuildTaskException(
                        errorType = ErrorType.SYSTEM,
                        errorCode = ERROR_ATOM_NOT_FOUND.toInt(),
                        errorMsg = message,
                        pipelineId = task.pipelineId,
                        buildId = task.buildId,
                        taskId = task.taskId
                    ) }

                buildLogPrinter.addLine(
                    buildId = task.buildId,
                    message = "Prepare ${element.name}(${atomEnv.atomName})",
                    tag = task.taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
                )
                atoms[atomCode] = atomEnv.projectCode!!
            }
        }
        return atoms
    }

    fun isAtomExist(atomCode: String, client: Client): Boolean {
        if (atomCache.getIfPresent(atomCode) != null) {
            return true
        }
        val atomResult = client.get(ServiceMarketAtomResource::class).getAtomByCode(atomCode, "") ?: return false
        if (atomResult.isNotOk()) {
            return false
        }
        val atomInfo = atomResult.data ?: return false
        atomCache.put(atomInfo.atomCode, atomInfo.name)
        return true
    }

    fun isProjectInstallAtom(atomCodes: List<String>, projectCode: String, client: Client): List<String> {
        val atomInfos = client.get(ServiceAtomResource::class).getInstalledAtoms(projectCode).data ?: return atomCodes
        val projectInstallAtoms = atomInfos.map { it.atomCode }
        val unInstallAtom = mutableListOf<String>()
        atomCodes.forEach {
            if (!projectInstallAtoms.contains(it)) {
                unInstallAtom.add(it)
            }
        }
        val unDefaultAtoms = client.get(ServiceAtomResource::class).findUnDefaultAtom(unInstallAtom).data
        if (unDefaultAtoms != null && unDefaultAtoms.isNotEmpty()) {
            return unDefaultAtoms
        }

        return emptyList()
    }
}
