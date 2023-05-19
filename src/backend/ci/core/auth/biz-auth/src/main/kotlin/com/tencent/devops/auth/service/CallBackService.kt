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

package com.tencent.devops.auth.service

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthIamCallBackDao
import com.tencent.devops.auth.pojo.IamCallBackInfo
import com.tencent.devops.auth.pojo.IamCallBackInterfaceDTO
import com.tencent.devops.common.api.exception.ErrorCodeException
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CallBackService @Autowired constructor(
    private val iamCallBackDao: AuthIamCallBackDao,
    private val dslContext: DSLContext
) {

    fun createOrUpdate(resourceMap: Map<String, IamCallBackInterfaceDTO>): Boolean {
        logger.info("createOrUpdate $resourceMap")
        if (!checkParams(resourceMap)) {
            return false
        }

        resourceMap.forEach { (key, resource) ->
            if (resource.relatedResource != null &&
                resource.relatedResource!!.isNotEmpty() &&
                resource.relatedFlag!!) {
                checkRelatedResource(resource.relatedResource!!, resourceMap.keys)
            }
            checkPath(resource.path)
            checkGateway(resource.gateway)
            val resourceInfo = IamCallBackInfo(
                system = resource.system,
                gateway = resource.gateway,
                path = resource.path,
                deleteFlag = false,
                resource = resource.resource,
                id = null
            )
            val resourceOldInfo = iamCallBackDao.get(dslContext, resource.resource)

            if (resourceOldInfo == null) {
                logger.info("resource ${resource.resource} not exist, create. $resource")
                iamCallBackDao.create(dslContext, resourceInfo)
            } else {
                iamCallBackDao.update(dslContext, resourceInfo, resourceOldInfo.id)
            }
        }
        logger.info("init iam callback resource success")
        return true
    }

    fun getResource(resource: String): IamCallBackInfo? {
        val resourceRecord = iamCallBackDao.get(dslContext, resource) ?: return null
        return IamCallBackInfo(
            id = resourceRecord.id,
            system = resourceRecord.system,
            path = resourceRecord.path,
            resource = resourceRecord.resource,
            deleteFlag = resourceRecord.deleteFlag,
            gateway = resourceRecord.gateway
        )
    }

    fun list(): List<IamCallBackInfo>? {
        val resourceRecords = iamCallBackDao.list(dslContext) ?: return emptyList()
        val iamResourceList = mutableListOf<IamCallBackInfo>()
        resourceRecords.forEach {
            iamResourceList.add(IamCallBackInfo(
                id = it.id,
                system = it.system,
                path = it.path,
                resource = it.resource,
                deleteFlag = it.deleteFlag,
                gateway = it.gateway
            ))
        }
        return iamResourceList
    }

    fun refreshGateway(oldToNewMap: Map<String, String>): Boolean {
        return try {
            iamCallBackDao.refreshGateway(dslContext, oldToNewMap)
            true
        } catch (ignore: Throwable) {
            logger.error("AUTH|refreshGateway failed with error: ", ignore)
            false
        }
    }

    private fun checkRelatedResource(relatedResource: List<String>, resourceList: Set<String>) {
        relatedResource.forEach {
            if (!resourceList.contains(it)) {
                val relatedResourceRecord = iamCallBackDao.get(dslContext, it)
                if (relatedResourceRecord == null) {
                    logger.warn("resource[$it] related not exist")
                    throw ErrorCodeException(errorCode = AuthMessageCode.RELATED_RESOURCE_CHECK_FAIL)
                }
            }
        }
    }

    private fun checkPath(path: String) {
        // auth回调接口通道校验
        if (!path.contains("api/open")) {
            throw ErrorCodeException(errorCode = AuthMessageCode.PATH_CHECK_FAIL)
        }
    }

    private fun checkGateway(gateway: String) {
        // gateway校验https,http
        if (!gateway.contains("http://") && !gateway.contains("https://")) {
            throw ErrorCodeException(errorCode = AuthMessageCode.HOST_CHECKOU_FAIL)
        }
    }

    private fun checkParams(resourceMap: Map<String, IamCallBackInterfaceDTO>): Boolean {
        // 校验入参map不能为空
        if (resourceMap.isEmpty()) {
            return false
        }
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CallBackService::class.java)
    }
}
