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

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.pojo.job.jobreq.OpOperateReq
import com.tencent.devops.environment.pojo.job.jobresp.OpOperateResult
import com.tencent.devops.environment.pojo.job.jobresp.ProjectOpInfo
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.DefaultTypedTuple
import org.springframework.stereotype.Service

@Service("OpService")
class OpService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val client: Client
) {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(OpService::class.java)

        const val OP_KEY = "environment:op_project"

        const val SUCCESSFUL_CODE = 0
        const val SUCCESSFUL_RESULT = true
        const val SUCCESSFUL_ZADD_MSG = "Operation executed successfully. %d new projects have been added."
        const val SUCCESSFUL_ZREM_MSG = "Operation executed successfully. %d new projects have been removed."
        const val SUCCESSFUL_QUERY_ALL_MSG = "Query all gray projects successfully."
        const val SUCCESSFUL_ZSCORE_MSG = "Query gray project(s) successfully."
        const val SUCCESSFUL_DELETE_KEY_MSG = "Clear all gray projects successfully."
        const val SUCCESSFUL_FUZZY_QUERY_MSG = "Fuzzy query successfully. %d gray project(s) in current page."

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

        const val INVALID_PAGE_CODE = 10006
        const val INVALID_PAGE_RESULT = false
        const val INVALID_PAGE_MSG = "Invalid page."

        const val INVALID_PAGE_SIZE_CODE = 10007
        const val INVALID_PAGE_SIZE_RESULT = false
        const val INVALID_PAGE_SIZE_MSG = "Invalid page size."

        private const val OPERATE_TAG_QUERY_ALL_GRAY_PROJS = 1
        private const val OPERATE_TAG_QUERY_PROJS_GRAY_STATUS = 2
        private const val OPERATE_TAG_SET_PROJS_GRAY_STATUS = 3
        private const val OPERATE_TAG_CANCEL_PROJS_GRAY_STATUS = 4
        private const val OPERATE_TAG_CLEAR_ALL_GRAY_PROJS = 5

        private const val DEFAULT_TRAVERSE_SIZE = 1000
        private const val DEFAULT_PAGE_VALUE = -1L
        private const val EMPTY_GRAY_PROJS_NUM = 0
    }

    fun operateOpProject(userId: String, opOperateReq: OpOperateReq): OpOperateResult {
        return when (opOperateReq.operateFlag) {
            OPERATE_TAG_QUERY_ALL_GRAY_PROJS -> queryAllGrayProjs(
                opOperateReq.page, opOperateReq.pageSize, opOperateReq.keyword
            )

            OPERATE_TAG_QUERY_PROJS_GRAY_STATUS -> queryProjsGrayStatus(opOperateReq.projectCodeList)
            OPERATE_TAG_SET_PROJS_GRAY_STATUS -> setProjsGrayStatus(opOperateReq.projectCodeList)
            OPERATE_TAG_CANCEL_PROJS_GRAY_STATUS -> cancelProjsGrayStatus(opOperateReq.projectCodeList)
            OPERATE_TAG_CLEAR_ALL_GRAY_PROJS -> clearAllGrayProjs()
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
     * ZREVRANGE
     */
    private fun queryAllGrayProjs(page: Long?, pageSize: Long?, keyword: String?): OpOperateResult {
        val grayProjNumber = grayProjsTotalNum()
        if (0L == grayProjNumber) {
            return OpOperateResult(
                code = KEY_NOT_EXIST_CODE,
                result = KEY_NOT_EXIST_RESULT,
                msg = KEY_NOT_EXIST_MSG,
                grayProjNumber = grayProjNumber
            )
        }
        var currentPage = page ?: DEFAULT_PAGE_VALUE
        var currentPageSize = pageSize ?: grayProjNumber
        if (DEFAULT_PAGE_VALUE == currentPage) {
            currentPage = 1
            currentPageSize = grayProjNumber
        } else {
            if (currentPage <= 0L) {
                return OpOperateResult(
                    code = INVALID_PAGE_CODE,
                    result = INVALID_PAGE_RESULT,
                    msg = INVALID_PAGE_MSG,
                    grayProjNumber = grayProjNumber
                )
            }
            if (currentPageSize <= 0L) {
                return OpOperateResult(
                    code = INVALID_PAGE_SIZE_CODE,
                    result = INVALID_PAGE_SIZE_RESULT,
                    msg = INVALID_PAGE_SIZE_MSG,
                    grayProjNumber = grayProjNumber
                )
            }
        }
        val msg: String
        val allGrayProjs = if (keyword.isNullOrBlank()) {
            msg = SUCCESSFUL_QUERY_ALL_MSG
            getGrayProjsByPage(currentPage, currentPageSize)
        } else {
            val fuzzyQueryGrayProjs = fuzzyQueryGrayProjsByPage(currentPage, currentPageSize, keyword)
            msg = String.format(SUCCESSFUL_FUZZY_QUERY_MSG, fuzzyQueryGrayProjs?.size ?: 0)
            fuzzyQueryGrayProjs
        }
        return OpOperateResult(
            code = SUCCESSFUL_CODE,
            result = SUCCESSFUL_RESULT,
            msg = msg,
            grayProjNumber = grayProjNumber,
            grayProjList = allGrayProjs
        )
    }

    private fun getGrayProjsByPage(currentPage: Long, currentPageSize: Long): Set<String>? {
        return if (currentPageSize <= DEFAULT_TRAVERSE_SIZE) {
            redisOperation.zrevrange(
                OP_KEY, (currentPage - 1) * currentPageSize, currentPage * currentPageSize - 1
            )
        } else {
            val innerPageNum = (currentPageSize / DEFAULT_TRAVERSE_SIZE).toInt() + 1
            val allGrayProjsSet: MutableSet<String> = mutableSetOf()
            for (i in 1..innerPageNum) {
                redisOperation.zrevrange(
                    OP_KEY,
                    (currentPage - 1) * currentPageSize + (i - 1) * DEFAULT_TRAVERSE_SIZE,
                    (currentPage - 1) * currentPageSize + i * DEFAULT_TRAVERSE_SIZE - 1
                )?.let {
                    allGrayProjsSet.addAll(it)
                }
            }
            allGrayProjsSet
        }
    }

    private fun fuzzyQueryGrayProjsByPage(
        currentPage: Long,
        currentPageSize: Long,
        keyword: String
    ): Set<String>? {
        val grayProjsList: MutableList<String> = mutableListOf()
        val allPage = (grayProjsTotalNum() / DEFAULT_TRAVERSE_SIZE).toInt() + 1
        for (i in 1..allPage) {
            redisOperation.zrevrange(
                OP_KEY, ((i - 1) * DEFAULT_TRAVERSE_SIZE).toLong(), (i * DEFAULT_TRAVERSE_SIZE - 1).toLong()
            )?.filter { keyword in it }?.let {
                grayProjsList.addAll(it)
            }
            if (grayProjsList.size >= currentPage * currentPageSize - 1) {
                break
            }
        }
        return if ((currentPage - 1) * currentPageSize > grayProjsList.size - 1) {
            emptySet()
        } else {
            val toIndex = if (currentPage * currentPageSize > grayProjsList.size) {
                grayProjsList.size
            } else {
                currentPage * currentPageSize
            }
            val grayProjsInCurPage = grayProjsList.subList(
                ((currentPage - 1) * currentPageSize).toInt(), toIndex.toInt()
            ).toSet()
            grayProjsInCurPage
        }
    }

    /**
     * operateFlag == 2：查询某些 项目灰度状态
     * ZSCORE
     */
    private fun queryProjsGrayStatus(projectCodeList: List<String>?): OpOperateResult {
        return if (!projectCodeList.isNullOrEmpty()) {
            queryProjExist(projectCodeList) ?: run {
                val projectCodeSet = projectCodeList.toSet()
                val projsGrayStatus = projectCodeSet.associateWith {
                    redisOperation.zscore(OP_KEY, it)
                }
                OpOperateResult(
                    code = SUCCESSFUL_CODE,
                    result = SUCCESSFUL_RESULT,
                    msg = SUCCESSFUL_ZSCORE_MSG,
                    grayProjNumber = grayProjsTotalNum(),
                    projGrayStatus = projsGrayStatus.map {
                        ProjectOpInfo(englishName = it.key, projGrayStatus = null != it.value)
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
     * ZADD NX
     */
    private fun setProjsGrayStatus(projectCodeList: List<String>?): OpOperateResult {
        return if (!projectCodeList.isNullOrEmpty()) {
            queryProjExist(projectCodeList) ?: run {
                val projectCodeTypedTuple = projectCodeList.map {
                    DefaultTypedTuple(it, System.currentTimeMillis().toDouble())
                }
                val addProjsNumber = redisOperation.zaddTuples(OP_KEY, projectCodeTypedTuple.toSet())
                if (null != addProjsNumber && addProjsNumber >= 0) {
                    OpOperateResult(
                        code = SUCCESSFUL_CODE,
                        result = SUCCESSFUL_RESULT,
                        msg = String.format(SUCCESSFUL_ZADD_MSG, addProjsNumber),
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
     * ZREM
     */
    private fun cancelProjsGrayStatus(projectCodeList: List<String>?): OpOperateResult {
        return if (!projectCodeList.isNullOrEmpty()) {
            queryProjExist(projectCodeList) ?: run {
                val projectCodeSet = projectCodeList.toSet()
                val removeProjsNumber = redisOperation.zremove(OP_KEY, *projectCodeSet.toTypedArray())
                if (null != removeProjsNumber && removeProjsNumber > 0) {
                    OpOperateResult(
                        code = SUCCESSFUL_CODE,
                        result = SUCCESSFUL_RESULT,
                        msg = String.format(SUCCESSFUL_ZREM_MSG, removeProjsNumber),
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
        val deleteKeyResult = redisOperation.delete(OP_KEY)
        return if (deleteKeyResult) { // 1 - ZET中有元素
            OpOperateResult(
                code = SUCCESSFUL_CODE,
                result = SUCCESSFUL_RESULT,
                msg = SUCCESSFUL_DELETE_KEY_MSG,
                grayProjNumber = grayProjsTotalNum()
            )
        } else { // 0 - ZET中没有元素
            OpOperateResult(
                code = KEY_NOT_EXIST_CODE,
                result = KEY_NOT_EXIST_RESULT,
                msg = KEY_NOT_EXIST_MSG,
                grayProjNumber = grayProjsTotalNum()
            )
        }
    }

    /**
     * @return 灰度项目总数
     * ZCARD
     */
    private fun grayProjsTotalNum(): Long {
        return redisOperation.zsize(OP_KEY) ?: 0
    }

    /**
     * 查询项目在数据库中是否存在
     */
    private fun queryProjExist(projectCodeList: List<String>?): OpOperateResult? {
        return if (!projectCodeList.isNullOrEmpty()) {
            val existedProject = client.get(ServiceProjectResource::class).getExistedEnglishName(projectCodeList).data
            val notExistedProject = projectCodeList.filterNot { existedProject?.contains(it) ?: false }
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