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

package com.tencent.devops.store.service.container.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.store.tables.records.TAppsRecord
import com.tencent.devops.store.dao.container.ContainerAppsDao
import com.tencent.devops.store.dao.container.ContainerAppsEnvDao
import com.tencent.devops.store.dao.container.ContainerAppsVersionDao
import com.tencent.devops.store.pojo.app.BuildEnv
import com.tencent.devops.store.pojo.app.BuildEnvParameters
import com.tencent.devops.store.pojo.app.ContainerApp
import com.tencent.devops.store.pojo.app.ContainerAppCreate
import com.tencent.devops.store.pojo.app.ContainerAppEnvCreate
import com.tencent.devops.store.pojo.app.ContainerAppInfo
import com.tencent.devops.store.pojo.app.ContainerAppRequest
import com.tencent.devops.store.pojo.app.ContainerAppVersion
import com.tencent.devops.store.pojo.app.ContainerAppVersionCreate
import com.tencent.devops.store.pojo.app.ContainerAppWithVersion
import com.tencent.devops.store.service.container.ContainerAppService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Collections
import javax.ws.rs.NotFoundException

/**
 * 编译环境业务逻辑类
 *
 * since: 2018-12-20
 */
@Service
class ContainerAppServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val containerAppsDao: ContainerAppsDao,
    private val containerAppsVersionDao: ContainerAppsVersionDao,
    private val containerAppsEnvDao: ContainerAppsEnvDao
) : ContainerAppService {

    private val logger = LoggerFactory.getLogger(ContainerAppServiceImpl::class.java)

    /**
     * 根据操作系统查找编译环境信息
     */
    override fun listApps(os: String): List<ContainerApp> {
        return containerAppsDao.listByOS(dslContext, os).map {
            containerAppsDao.convert(it)
        }
    }

    /**
     * 根据编译环境id查找编译环境版本信息
     */
    override fun listAppVersion(appId: Int): List<ContainerAppVersion> {
        return containerAppsVersionDao.listByAppId(dslContext, appId).map {
            containerAppsVersionDao.convert(it)
        }
    }

    /**
     * 根据操作系统查找环境变量列表及版本列表
     */
    override fun listAppsWithVersion(os: String): List<ContainerAppWithVersion> {
        return listApps(os).map {
            // 查找版本信息
            val versions = listAppVersion(it.id).filter { it.version != null && !it.version!!.trim().isEmpty() }
                .map { v ->
                    v.version!!
                }
            sortAppVersion(versions)
            val envRecords = containerAppsEnvDao.listByAppId(dslContext, it.id)
            ContainerAppWithVersion(it.name, versions, it.binPath, envRecords.map { env ->
                BuildEnvParameters(env.name, env.description, env.path)
            })
        }
    }

    /**
     * 根据操作系统查找构建机环境变量
     */
    override fun getApps(os: String): List<BuildEnv> {
        val apps = listApps(os)
        val buildEnvList = mutableListOf<BuildEnv>()
        for (app in apps) {
            val versions = listAppVersion(app.id).filter { it.version != null && !it.version!!.trim().isEmpty() }
                .map { v -> v.version!! }
            sortAppVersion(versions)
            for (version in versions) {
                buildEnvList.add(getBuildEnv(app.name, version, "linux")!!)
            }
        }
        return buildEnvList
    }

    /**
     * 添加编译环境信息
     */
    override fun addApp(app: ContainerAppCreate): Int {
        return containerAppsDao.add(dslContext, app.name, app.os, app.binPath)
    }

    /**
     * 添加编译环境版本信息
     */
    override fun addAppVersion(appVersion: ContainerAppVersionCreate) {
        dslContext.transaction { t ->
            val context = DSL.using(t)
            // Check if the app id exist
            if (!containerAppsDao.exist(context, appVersion.appId)) {
                throw NotFoundException("The appId(${appVersion.appId}) is not exist")
            }
            containerAppsVersionDao.add(context, appVersion.appId, appVersion.version)
        }
    }

    /**
     * 添加编译环境变量信息
     */
    override fun addAppEnv(appEnvCreate: ContainerAppEnvCreate) {
        dslContext.transaction { t ->
            val context = DSL.using(t)
            // Check if the app id exist
            if (!containerAppsDao.exist(context, appEnvCreate.appId)) {
                throw NotFoundException("The appId(${appEnvCreate.appId}) is not exist")
            }
            containerAppsEnvDao.add(
                context,
                appEnvCreate.appId,
                appEnvCreate.name,
                appEnvCreate.path,
                appEnvCreate.description
            )
        }
    }

    /**
     * 添加编译环境信息
     */
    override fun addContainerAppInfo(containerAppRequest: ContainerAppRequest): Result<Boolean> {
        logger.info("the containerAppRequest is {}", containerAppRequest)
        val containerApp = containerAppRequest.containerApp
        // 判断编译环境名称和操作系统组合是否存在系统
        val count = containerAppsDao.countByNameAndOs(dslContext, containerApp.name, containerApp.os)
        if (count > 0) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(containerApp.name + "+" + containerApp.os),
                false
            )
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val id = containerAppsDao.add(context, containerApp.name, containerApp.os, containerApp.binPath)
            logger.info("the id is {}", id)
            // 插入环境变量
            containerAppRequest.containerAppEnvList?.forEach {
                containerAppsEnvDao.add(context, id, it.name, it.path, it.description)
            }
            // 插入版本日志
            containerAppRequest.containerAppVersionList?.forEach {
                containerAppsVersionDao.add(context, id, it.version)
            }
        }
        return Result(true)
    }

    /**
     * 更新编译环境信息
     */
    override fun updateContainerAppInfo(id: Int, containerAppRequest: ContainerAppRequest): Result<Boolean> {
        logger.info("the update id is {} ,the containerAppRequest is {}", id, containerAppRequest)
        val containerApp = containerAppRequest.containerApp
        val name = containerApp.name
        val os = containerApp.os
        val count = containerAppsDao.countByNameAndOs(dslContext, name, os)
        // 判断更新的编译环境名称和操作系统组合是否存在系统
        if (count > 0) {
            val containerAppInfoRecord = containerAppsDao.getContainerAppInfo(dslContext, id)
            if (null != containerAppInfoRecord && name != containerAppInfoRecord.name && os != containerAppInfoRecord.os) {
                return MessageCodeUtil.generateResponseDataObject(
                    CommonMessageCode.PARAMETER_IS_EXIST,
                    arrayOf("$name+$os"),
                    false
                )
            }
        }
        // 判断编译环境名称是否存在
        dslContext.transaction { t ->
            val context = DSL.using(t)
            // 更新编译环境基本信息
            containerAppsDao.update(context, id, containerApp.name, containerApp.os, containerApp.binPath)
            // 更新版本日志信息
            containerAppsVersionDao.deleteByAppId(context, id)
            containerAppRequest.containerAppVersionList?.forEach {
                containerAppsVersionDao.add(context, id, it.version)
            }
            // 更新编译环境基本信息
            containerAppsEnvDao.deleteByAppId(context, id)
            containerAppRequest.containerAppEnvList?.forEach {
                containerAppsEnvDao.add(context, id, it.name, it.path, it.description)
            }
        }
        return Result(true)
    }

    /**
     * 删除编译环境信息
     */
    override fun deleteContainerAppInfo(id: Int): Result<Boolean> {
        logger.info("the delete id is {}", id)
        dslContext.transaction { t ->
            val context = DSL.using(t)
            // 删除版本日志信息
            containerAppsVersionDao.delete(context, id)
            // 删除环境变量信息
            containerAppsEnvDao.delete(context, id)
            // 删除编译环境基本信息
            containerAppsDao.delete(context, id)
        }
        logger.info("the $id delete success!")
        return Result(true)
    }

    /**
     * 根据id获取编译环境信息
     */
    override fun getContainerAppInfo(id: Int): Result<ContainerAppInfo?> {
        var containerAppInfo: ContainerAppInfo? = null
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val containerAppInfoRecord = containerAppsDao.getContainerAppInfo(context, id)
            logger.info("the containerAppInfoRecord is :{}", containerAppInfoRecord)
            if (null != containerAppInfoRecord) {
                val containerApp = containerAppInfoRecord.map { containerAppsDao.convert(it as TAppsRecord) }
                val containerAppEnvList =
                    containerAppsEnvDao.listByAppId(context, containerApp.id).map { containerAppsEnvDao.convert(it) }
                val containerAppVersionList = containerAppsVersionDao.listByAppId(context, containerApp.id)
                    .map { containerAppsVersionDao.convert(it) }
                containerAppInfo = ContainerAppInfo(containerApp, containerAppEnvList, containerAppVersionList)
            }
        }
        logger.info("the containerAppInfo is :{}", containerAppInfo)
        return Result(containerAppInfo)
    }

    /**
     * 获取所用编译环境信息
     */
    override fun getAllContainerAppInfos(): Result<List<ContainerApp>> {
        val containerAppList = containerAppsDao.getAllContainerApps(dslContext).map { containerAppsDao.convert(it) }
        return Result(containerAppList)
    }

    override fun getBuildEnv(name: String, version: String, os: String): BuildEnv? {
        return dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            val appRecord = containerAppsDao.get(context, name, os) ?: return@transactionResult null
            val versionRecord =
                containerAppsVersionDao.get(context, appRecord.id, version) ?: return@transactionResult null
            val envRecords = containerAppsEnvDao.listByAppId(context, appRecord.id)
            BuildEnv(
                appRecord.name,
                getAppVersion(versionRecord.version, appRecord.name),
                appRecord.binPath,
                envRecords.map {
                    it.name to it.path
                }.toMap()
            )
        }
    }

    private fun sortAppVersion(versions: List<String>) {
        // Sort the versions
        Collections.sort(versions) { v1, v2 ->
            compareVersion(v2, v1)
        }
    }

    private fun compareVersion(version1: String, version2: String): Int {
        val arr1 = version1.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val arr2 = version2.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        // same number of version "." dots
        if (arr1.size < arr2.size) {
            for (i in arr1.indices) {
                val v1 = arr1[i]
                val v2 = arr2[i]
                try {
                    if (Integer.parseInt(v1) < Integer.parseInt(v2))
                        return -1
                    if (Integer.parseInt(v1) > Integer.parseInt(v2))
                        return 1
                } catch (e: Exception) {
                    val compare = v1.compareTo(v2)
                    if (compare != 0) {
                        return compare
                    }
                }
            }
            return -1
        } else {
            for (i in arr2.indices) {
                val v1 = arr1[i]
                val v2 = arr2[i]
                try {
                    if (Integer.parseInt(v1) < Integer.parseInt(v2))
                        return -1
                    if (Integer.parseInt(v1) > Integer.parseInt(v2))
                        return 1
                } catch (e: Exception) {
                    val compare = v1.compareTo(v2)
                    if (compare != 0) {
                        return compare
                    }
                }
            }
            return if (arr1.size == arr2.size) {
                0
            } else {
                1
            }
        }
    }

    private fun getAppVersion(version: String, appName: String): String {
        if (appName == "xcode") {
            return "$version.app"
        }
        return version
    }

    override fun getAppVer(name: String, os: String): List<Map<String, String>> {
        val appRecord = containerAppsDao.get(dslContext, name, os) ?: return listOf()
        val versionRecord = containerAppsVersionDao.listByAppId(dslContext, appRecord.id)
        return versionRecord.map { mapOf(Pair("key", it.version)) }
    }
}
