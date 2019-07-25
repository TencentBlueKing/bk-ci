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

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.api.ServiceProjectResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.pojo.common.InstalledProjRespItem
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreProjectService
import com.tencent.devops.store.service.common.StoreUserService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

/**
 * store项目通用业务逻辑类
 *
 * since: 2019-03-22
 */
@Service
class StoreProjectServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val storeStatisticDao: StoreStatisticDao,
    private val storeUserService: StoreUserService
) : StoreProjectService {

    private val logger = LoggerFactory.getLogger(StoreProjectServiceImpl::class.java)

    /**
     * 根据商城组件标识获取已安装的项目列表
     */
    override fun getInstalledProjects(
        accessToken: String,
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum
    ): Result<List<InstalledProjRespItem>> {
        logger.info("accessToken is :$accessToken, userId is :$userId, storeCode is :$storeCode, storeType is :$storeType")
        // 获取用户有权限的项目列表
        val projectList = client.get(ServiceProjectResource::class).list(userId).data
        logger.info("projectList is :$projectList")
        if (projectList?.count() == 0) {
            return Result(mutableListOf())
        }
        val projectCodeMap = projectList?.map { it.projectCode to it }?.toMap()!!
        val records =
            storeProjectRelDao.getInstalledProject(dslContext, storeCode, storeType.type.toByte(), projectCodeMap.keys)
        val result = mutableListOf<InstalledProjRespItem>()
        val df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        records?.forEach {
            result.add(
                InstalledProjRespItem(
                    projectCode = it.projectCode,
                    projectName = projectCodeMap[it.projectCode]?.projectName,
                    creator = it.creator,
                    createTime = df.format(it.createTime)
                )
            )
        }
        return Result(result)
    }

    override fun installStoreComponent(
        accessToken: String,
        userId: String,
        projectCodeList: ArrayList<String>,
        storeId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        publicFlag: Boolean
    ): Result<Boolean> {
        logger.info("accessToken is :$accessToken, userId is :$userId, projectCodeList is :$projectCodeList, storeId is :$storeId, storeCode is :$storeCode, storeType is :$storeType")
        val installFlag = storeUserService.isCanInstallStoreComponent(publicFlag, userId, storeCode, storeType) // 是否能安装
        // 判断用户是否有权限安装
        if (!installFlag) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED, false)
        }
        // 获取用户有权限的项目列表
        val projectList = client.get(ServiceProjectResource::class).list(userId).data
        logger.info("projectList is :$projectList")
        // 判断用户是否有权限安装到对应的项目
        val privilegeProjectCodeList = mutableListOf<String>()
        projectList?.map {
            privilegeProjectCodeList.add(it.projectCode)
        }
        val dataList = mutableListOf<String>()
        dataList.addAll(projectCodeList)
        dataList.removeAll(privilegeProjectCodeList)
        if (dataList.isNotEmpty()) {
            // 存在用户没有安装权限的项目，抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(
                StoreMessageCode.USER_PROJECT_IS_NOT_ALLOW_INSTALL,
                arrayOf(dataList.toString()),
                false
            )
        }
        var increment = 0
        dslContext.transaction { t ->
            val context = DSL.using(t)
            for (projectCode in projectCodeList) {
                // 判断是否已安装
                val relCount =
                    storeProjectRelDao.countInstalledProject(context, projectCode, storeCode, storeType.type.toByte())
                logger.info("relCount is :$relCount")
                if (relCount > 0) {
                    continue
                }
                // 未安装则入库
                storeProjectRelDao.addStoreProjectRel(
                    context,
                    userId,
                    storeCode,
                    projectCode,
                    1,
                    storeType.type.toByte()
                )
                increment += 1
            }
            logger.info("increment: $increment")
            // 更新安装量
            if (increment > 0) {
                storeStatisticDao.updateDownloads(
                    context,
                    userId,
                    storeId,
                    storeCode,
                    storeType.type.toByte(),
                    increment
                )
            }
        }
        return Result(true)
    }

    override fun uninstall(storeType: StoreTypeEnum, templateCode: String, projectCode: String): Result<Boolean> {
        storeProjectRelDao.deleteRel(dslContext, templateCode, storeType.type.toByte())
        return Result(true)
    }
}
