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

package com.tencent.devops.plugin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.model.plugin.tables.TPluginWetestTask
import com.tencent.devops.plugin.client.WeTestClient
import com.tencent.devops.plugin.dao.WetestTaskDao
import com.tencent.devops.plugin.pojo.wetest.WetestCloud
import com.tencent.devops.plugin.pojo.wetest.WetestTask
import com.tencent.devops.plugin.pojo.wetest.WetestTestType
import com.tencent.devops.plugin.utils.CommonUtils
import org.jooq.DSLContext
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WetestTaskService @Autowired constructor(
    private val wetestTaskDao: WetestTaskDao,
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WetestTaskService::class.java)
    }

    private val TOP50 = "TOP50"
    private val TOP100 = "TOP100"

    fun createTask(projectId: String, name: String, mobileCategory: String, mobileCategoryId: String, mobileModel: String, mobileModelId: String, description: String?): Int {
        logger.info("create wetest task projectId:$projectId, name:$name, mobileCategory: $mobileCategory, mobileCategoryId: $mobileCategoryId, " +
                "mobileModel: $mobileModel, mobileModelId: $mobileModelId, description: $description")
        return wetestTaskDao.insert(dslContext, projectId, name, mobileCategory, mobileCategoryId, mobileModel, mobileModelId, description)
    }

    fun updateTask(
        projectId: String,
        id: Int,
        name: String,
        mobileCategory: String,
        mobileCategoryId: String,
        mobileModel: String,
        mobileModelId: String,
        description: String?
    ) {
        logger.info("update wetest task projectId:$projectId, id: $id, name:$name, mobileCategory: $mobileCategory, mobileCategoryId: $mobileCategoryId, " +
                "mobileModel: $mobileModel, mobileModelId: $mobileModelId, description: $description")
        return wetestTaskDao.update(dslContext, projectId, id, name, mobileCategory, mobileCategoryId, mobileModel, mobileModelId, description)
    }

    fun getTask(projectId: String, id: Int): WetestTask? {
        val record = wetestTaskDao.getRecord(dslContext, projectId, id)
        if (null != record) {
            with(TPluginWetestTask.T_PLUGIN_WETEST_TASK) {
                return WetestTask(
                        record.id,
                        record.projectId,
                        record.name,
                        record.mobileCategory,
                        record.mobileCategoryId,
                        record.mobileModel,
                        record.mobileModelId,
                        record.description,
                        record.ticketsId,
                        record.createdTime.toString(),
                        record.updatedTime.toString()
                )
            }
        }
        return null
    }

    fun deleteTask(projectId: String, id: Int) {
        logger.info("delete weTest task: id: $id, projectId: $projectId")
        return wetestTaskDao.delete(dslContext, projectId, id)
    }

    fun getList(projectId: String, page: Int, pageSize: Int): List<WetestTask> {
        val recordList = wetestTaskDao.getList(dslContext, projectId, page, pageSize)
        val result = mutableListOf<WetestTask>()
        if (recordList != null) {
            with(TPluginWetestTask.T_PLUGIN_WETEST_TASK) {
                for (item in recordList) {
                    result.add(
                            WetestTask(
                                    id = item.get(ID),
                                    projectId = item.get(PROJECT_ID),
                                    name = item.get(NAME),
                                    mobileCategory = item.get(MOBILE_CATEGORY),
                                    mobileCategoryId = item.get(MOBILE_CATEGORY_ID),
                                    mobileModel = item.get(MOBILE_MODEL),
                                    mobileModelId = item.get(MOBILE_MODEL_ID),
                                    description = item.get(DESCRIPTION),
                                    ticketsId = item.get(TICKETS_ID),
                                    createTime = item.get(CREATED_TIME).toString(),
                                    updateTime = item.get(UPDATED_TIME).toString()
                            )
                    )
                }
            }
        }
        return result
    }

    fun getCount(projectId: String): Int {
        return wetestTaskDao.getCount(dslContext, projectId)
    }

    fun getByName(projectId: String, name: String): WetestTask? {
        val record = wetestTaskDao.getByName(dslContext, projectId, name)
        if (null != record) {
            with(TPluginWetestTask.T_PLUGIN_WETEST_TASK) {
                return WetestTask(
                        record.id,
                        record.projectId,
                        record.name,
                        record.mobileCategory,
                        record.mobileCategoryId,
                        record.mobileModel,
                        record.mobileModelId,
                        record.description,
                        record.ticketsId,
                        record.createdTime.toString(),
                        record.updatedTime.toString()
                )
            }
        }
        return null
    }

    fun getMyCloud(userId: String, projectId: String): Map<String, String> {
        val (secretId, secretKey) = CommonUtils.getCredential(userId)

        val client = WeTestClient(secretId, secretKey)
        val myCloudJson = client.getMyCloud()
        val ret = myCloudJson.optInt("ret")
        if (ret != 0) {
            val msg = myCloudJson.optString("msg")
            logger.error("fail to get myCloud info from weTest, retCode: $ret, msg: $msg")
            throw OperationException("WeTest拉取私有云配置失败，返回码: $ret, 错误消息: $msg")
        }

        val result = mutableMapOf<String, String>()
        result[TOP50] = "TOP 50"
        result[TOP100] = "TOP 100"
        myCloudJson.optJSONArray("clouds")?.forEach { it ->
            val item = it as JSONObject
            result[item.optString("id")] = item.optString("name")
        }

        return result
    }

    fun getPrivateCloudDevice(userId: String, projectId: String, cloudIds: String, online: String, free: String): List<WetestCloud> {
        val (secretId, secretKey) = CommonUtils.getCredential(userId)

        if (TOP50 == cloudIds || TOP100 == cloudIds) {
            logger.info("TOP50 or TOP100, model is empty.")
            return mutableListOf()
        }
        val client = WeTestClient(secretId, secretKey)
        val cloudDevice = client.getCloudDevices(cloudIds, online, free)
        val ret = cloudDevice.optInt("ret")
        if (ret != 0) {
            val msg = cloudDevice.optString("msg")
            logger.error("fail to get getPrivateCloudDevice info from weTest, retCode: $ret, msg: $msg")
            throw OperationException("WeTest拉获取私有云设备列表失败，返回码: $ret, 错误消息: $msg")
        }

        val result = mutableListOf<WetestCloud>()
        cloudDevice.optJSONArray("clouds")?.forEach { it ->
            val item = it as JSONObject
            result.add(objectMapper.readValue(item.toString()))
        }

        return result
    }

    fun getPrivateTestInfo(createUser: String, projectId: String, testId: String): JSONObject {
        val (secretId, secretKey) = CommonUtils.getCredential(createUser)

        val client = WeTestClient(secretId, secretKey)
        val testInfo = client.getTestInfo(testId)
        val ret = testInfo.optInt("ret")
        if (ret != 0) {
            val msg = testInfo.optString("msg")
            logger.error("fail to get getPrivateCloudDevice info from weTest, retCode: $ret, msg: $msg")
            throw OperationException("WeTest根据测试ID拉取测试相关的信息失败，返回码: $ret, 错误消息: $msg")
        }

        return testInfo
    }

    fun getTestDevicePerfError(createUser: String, projectId: String, testId: String, deviceId: String, needPerf: String, needError: String): JSONObject {

        val (secretId, secretKey) = CommonUtils.getCredential(createUser)

        val client = WeTestClient(secretId, secretKey)
        val testInfo = client.getTestDevicePerfError(testId, deviceId, ("1" == needError), ("1" == needPerf))
        val ret = testInfo.optInt("ret")
        if (ret != 0) {
            val msg = testInfo.optString("msg")
            logger.error("fail to get getPrivateCloudDevice info from weTest, retCode: $ret, msg: $msg")
            throw OperationException("WeTest通过测试ID和设备ID批量获取性能和错误信息失败，返回码: $ret, 错误消息: $msg")
        }

        return testInfo
    }

    fun getTestDeviceImageLog(createUser: String, projectId: String, testId: String, deviceId: String, needImage: String, needLog: String): JSONObject {
        val (secretId, secretKey) = CommonUtils.getCredential(createUser)

        val client = WeTestClient(secretId, secretKey)
        val testInfo = client.getTestDeviceImageLog(testId, deviceId, ("1" == needImage), ("1" == needLog))
        val ret = testInfo.optInt("ret")
        if (ret != 0) {
            val msg = testInfo.optString("msg")
            logger.error("fail to get getPrivateCloudDevice info from weTest, retCode: $ret, msg: $msg")
            throw OperationException("WeTest通过测试ID和手机ID截图和日志信息失败，返回码: $ret, 错误消息: $msg")
        }

        return testInfo
    }

    fun getTestTypeScriptType(createUser: String, projectId: String): List<WetestTestType> {
        val (secretId, secretKey) = CommonUtils.getCredential(createUser)

        val client = WeTestClient(secretId, secretKey)
        val testTypeInfo = client.testTypes()
        val ret = testTypeInfo.optInt("ret")
        if (ret != 0) {
            val msg = testTypeInfo.optString("msg")
            logger.error("fail to get getPrivateCloudDevice info from weTest, retCode: $ret, msg: $msg")
            throw OperationException("WeTest获取测试类型和脚本类型失败，返回码: $ret, 错误消息: $msg")
        }

        val result = mutableListOf<WetestTestType>()
        val testTypes = testTypeInfo.optJSONObject("testtypes")
        testTypes.optJSONArray("android")?.forEach {
            val item = it as JSONObject
            result.add(objectMapper.readValue(item.toString()))
        }

        testTypes.optJSONArray("ios")?.forEach {
            val item = it as JSONObject
            result.add(objectMapper.readValue(item.toString()))
        }

        return result
    }

    fun getTestLogContent(createUser: String, projectId: String, testId: String, deviceId: String, level: String, startLine: Int, lineCnt: Int): JSONObject {
        val (secretId, secretKey) = CommonUtils.getCredential(createUser)

        val client = WeTestClient(secretId, secretKey)
        val logInfo = client.getLogContent(testId, deviceId, level, startLine, lineCnt)
        val ret = logInfo.optInt("ret")
        if (ret != 0) {
            val msg = logInfo.optString("msg")
            logger.error("fail to get getTestLogContent info from weTest, retCode: $ret, msg: $msg")
            throw OperationException("WeTest根据测试ID拉取测试相关的日志信息失败，返回码: $ret, 错误消息: $msg")
        }

        return logInfo
    }
}