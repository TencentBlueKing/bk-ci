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

package com.tencent.devops.environment.service.job

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.service.utils.BkServiceUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.environment.dao.ProjectDao
import com.tencent.devops.environment.pojo.job.req.OpOperateReq
import com.tencent.devops.environment.pojo.job.resp.OpOperateResult
import com.tencent.devops.environment.pojo.job.resp.ProjectOpInfo
import org.jooq.DSLContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("OpService")
class OpService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val projectDao: ProjectDao
) {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(OpService::class.java)

        const val SUCCESSFUL_CODE = 0
        const val SUCCESSFUL_RESULT = true
        const val SUCCESSFUL_SADD_MSG = "Operation executed successfully. %d new projects have been added."
        const val SUCCESSFUL_SREM_MSG = "Operation executed successfully. %d new projects have been removed."
        const val SUCCESSFUL_SMEMBERS_MSG = "Query all gray projects successfully."
        const val SUCCESSFUL_DELETE_KEY_MSG = "Clear all gray projects successfully."

        const val INVALID_OPERATION_TYPE_CODE = 10001
        const val INVALID_OPERATION_TYPE_RESULT = false
        const val INVALID_OPERATION_TYPE_MSG = "Invalid operation type."

        const val INVALID_PROJECT_CODE = 10002
        const val INVALID_PROJECT_RESULT = false
        const val INVALID_PROJECT_MSG = "Operation not execute. Invalid project: "

        const val EMPTY_PROJ_CODE = 10003
        const val EMPTY_PROJ_RESULT = false
        const val EMPTY_PROJ_MSG = "Passed project list is empty."

        const val OPERATE_FAILED_CODE = 10004
        const val OPERATE_FAILED_RESULT = false
        const val OPERATE_FAILED_MSG = "Executed failed."

        const val KEY_NOT_EXIST_CODE = 10005
        const val KEY_NOT_EXIST_RESULT = false
        const val KEY_NOT_EXIST_MSG = "No gray projects."
    }

    private val opKey = getOpKey()

    fun operateOpProject(userId: String, opOperateReq: OpOperateReq): OpOperateResult {
        return when (opOperateReq.operateFlag) {
            1 -> queryAllGrayProjs()
            2 -> queryProjsGrayStatus(opOperateReq.projectCodeList)
            3 -> setProjsGrayStatus(opOperateReq.projectCodeList)
            4 -> cancelProjsGrayStatus(opOperateReq.projectCodeList)
            5 -> clearAllGrayProjs()
            else -> {
                OpOperateResult(
                    code = INVALID_OPERATION_TYPE_CODE,
                    result = INVALID_OPERATION_TYPE_RESULT,
                    msg = INVALID_OPERATION_TYPE_MSG,
                    grayProjNumber = grayProjsTotalNum()
                )
            }
        }
    }

    /**
     * operateFlag == 1：查询所有 灰度项目
     * SMEMBERS
     */
    private fun queryAllGrayProjs(): OpOperateResult {
        val allGrayProjs = redisOperation.smembers(opKey)
        return OpOperateResult(
            code = SUCCESSFUL_CODE,
            result = SUCCESSFUL_RESULT,
            msg = SUCCESSFUL_SMEMBERS_MSG,
            grayProjNumber = grayProjsTotalNum(),
            grayProjList = allGrayProjs
        )
    }

    /**
     * operateFlag == 2：查询某些 项目灰度状态
     * SISMEMBER
     */
    private fun queryProjsGrayStatus(projectCodeList: List<String>?): OpOperateResult {
        return if (!projectCodeList.isNullOrEmpty()) {
            queryProjExist(projectCodeList) ?: run {
                val projectCodeSet = projectCodeList.toSet()
                val projsGrayStatus = redisOperation.sismember(opKey, *projectCodeSet.toTypedArray())
                OpOperateResult(
                    code = EMPTY_PROJ_CODE,
                    result = EMPTY_PROJ_RESULT,
                    msg = EMPTY_PROJ_MSG,
                    grayProjNumber = grayProjsTotalNum(),
                    projGrayStatus = projsGrayStatus?.map {
                        ProjectOpInfo(englishName = it.key.toString(), projGrayStatus = it.value)
                    }
                )
            }
        } else {
            OpOperateResult(
                code = EMPTY_PROJ_CODE,
                result = EMPTY_PROJ_RESULT,
                msg = EMPTY_PROJ_MSG,
                grayProjNumber = grayProjsTotalNum()
            )
        }
    }

    /**
     * operateFlag == 3：设置某些 项目灰度状态
     * SADD
     */
    private fun setProjsGrayStatus(projectCodeList: List<String>?): OpOperateResult {
        return if (!projectCodeList.isNullOrEmpty()) {
            queryProjExist(projectCodeList) ?: run {
                val projectCodeSet = projectCodeList.toSet()
                val addProjsNumber = redisOperation.sadd(opKey, *projectCodeSet.toTypedArray())
                if (null != addProjsNumber && addProjsNumber >= 0) {
                    OpOperateResult(
                        code = SUCCESSFUL_CODE,
                        result = SUCCESSFUL_RESULT,
                        msg = String.format(SUCCESSFUL_SADD_MSG, addProjsNumber),
                        grayProjNumber = grayProjsTotalNum()
                    )
                } else {
                    OpOperateResult(
                        code = OPERATE_FAILED_CODE,
                        result = OPERATE_FAILED_RESULT,
                        msg = OPERATE_FAILED_MSG,
                        grayProjNumber = grayProjsTotalNum()
                    )
                }
            }
        } else {
            OpOperateResult(
                code = EMPTY_PROJ_CODE,
                result = EMPTY_PROJ_RESULT,
                msg = EMPTY_PROJ_MSG,
                grayProjNumber = grayProjsTotalNum()
            )
        }
    }

    /**
     * operateFlag == 4：取消某些 项目灰度状态
     * SREM
     */
    private fun cancelProjsGrayStatus(projectCodeList: List<String>?): OpOperateResult {
        return if (!projectCodeList.isNullOrEmpty()) {
            queryProjExist(projectCodeList) ?: run {
                val projectCodeSet = projectCodeList.toSet()
                val removeProjsNumber = redisOperation.sremove(opKey, *projectCodeSet.toTypedArray())
                if (null != removeProjsNumber && removeProjsNumber > 0) {
                    OpOperateResult(
                        code = SUCCESSFUL_CODE,
                        result = SUCCESSFUL_RESULT,
                        msg = String.format(SUCCESSFUL_SREM_MSG, removeProjsNumber),
                        grayProjNumber = grayProjsTotalNum()
                    )
                } else if (0L == removeProjsNumber) {
                    OpOperateResult(
                        code = KEY_NOT_EXIST_CODE,
                        result = KEY_NOT_EXIST_RESULT,
                        msg = KEY_NOT_EXIST_MSG,
                        grayProjNumber = grayProjsTotalNum()
                    )
                } else {
                    OpOperateResult(
                        code = OPERATE_FAILED_CODE,
                        result = OPERATE_FAILED_RESULT,
                        msg = OPERATE_FAILED_MSG,
                        grayProjNumber = grayProjsTotalNum()
                    )
                }
            }
        } else {
            OpOperateResult(
                code = EMPTY_PROJ_CODE,
                result = EMPTY_PROJ_RESULT,
                msg = EMPTY_PROJ_MSG,
                grayProjNumber = grayProjsTotalNum()
            )
        }
    }

    /**
     * operateFlag == 5：清空所有 灰度项目
     * delete key
     */
    private fun clearAllGrayProjs(): OpOperateResult {
        val deleteKeyResult = redisOperation.delete(opKey)
        return if (deleteKeyResult) {
            OpOperateResult(
                code = SUCCESSFUL_CODE,
                result = SUCCESSFUL_RESULT,
                msg = SUCCESSFUL_DELETE_KEY_MSG,
                grayProjNumber = grayProjsTotalNum()
            )
        } else {
            OpOperateResult(
                code = OPERATE_FAILED_CODE,
                result = OPERATE_FAILED_RESULT,
                msg = OPERATE_FAILED_MSG,
                grayProjNumber = grayProjsTotalNum()
            )
        }
    }

    /**
     * @return 灰度项目总数
     * SCARD
     */
    private fun grayProjsTotalNum(): Long {
        return redisOperation.scard(opKey) ?: 0
    }

    /**
     * @return redis中存储op项目的key
     */
    private fun getOpKey(): String {
        val profileName = getProfileName()
        val serviceName = BkServiceUtil.findServiceName()
        return "ENV:$profileName:SERVICE:$serviceName:OP_PROJECT"
    }

    /**
     * @return 微服务的profile名称
     */
    private fun getProfileName(): String {
        val profile = SpringContextUtil.getBean(Profile::class.java)
        return profile.getActiveProfiles().joinToString().trim()
    }

    /**
     * 查询项目在数据库中是否存在
     */
    private fun queryProjExist(projectCodeList: List<String>?): OpOperateResult? {
        return if (!projectCodeList.isNullOrEmpty()) {
            val existedProject = projectDao.getExistedEnglishName(dslContext, projectCodeList)
            val notExistedProject = projectCodeList.filterNot { existedProject.contains(it) }
            if (notExistedProject.isNotEmpty()) {
                OpOperateResult(
                    code = INVALID_PROJECT_CODE,
                    result = INVALID_PROJECT_RESULT,
                    msg = INVALID_PROJECT_MSG + notExistedProject.joinToString(", "),
                    grayProjNumber = grayProjsTotalNum()
                )
            } else null
        } else null
    }
}