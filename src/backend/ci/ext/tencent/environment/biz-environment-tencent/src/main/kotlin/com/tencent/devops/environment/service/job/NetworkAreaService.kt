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

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.environment.dao.job.NetworkAreaDao
import com.tencent.devops.environment.pojo.networkarea.NetworkAreaResult
import com.tencent.devops.environment.pojo.networkarea.NetworkInfo
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service("NetworkAreaService")
class NetworkAreaService @Autowired constructor(
    private val dslContext: DSLContext,
    private val networkAreaDao: NetworkAreaDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(NetworkAreaService::class.java)

        private const val DEFAULT_PAGE = 1
        private const val DEFAULT_PAGE_SIZE = 20

        private const val SUCCESSFUL_CODE = 0
        private const val SUCCESSFUL_RESULT = true

        private const val ADD_SUCCESSFUL_NUM = 1
        private const val ADD_FAILED_NUM = 0L

        private const val DELETE_SUCCESSFUL_NUM = 1
        private const val DELETE_FAILED_NUM = 0L
    }

    /**
     * 查询所有网络区域
     * @param page 页数，从1开始
     * @param pageSize 页大小，>0
     * @param keyword 查询关键词，支持网络区域名称模糊查询
     * @return 查询结果
     */
    fun queryAllNetworkArea(page: Int?, pageSize: Int?, keyword: String?): NetworkAreaResult {
        val allNetworkArea = networkAreaDao.getAllNetworkArea(
            dslContext, page ?: DEFAULT_PAGE, pageSize ?: DEFAULT_PAGE_SIZE, keyword
        )
        if (logger.isDebugEnabled) logger.debug("[queryAllNetworkArea] $allNetworkArea")
        return NetworkAreaResult(
            code = SUCCESSFUL_CODE,
            result = SUCCESSFUL_RESULT,
            msg = "[queryAllNetworkArea] Query all network area successfully.",
            networkNum = allNetworkArea.size.toLong(),
            networkInfo = allNetworkArea.map {
                NetworkInfo(netAreaId = it.netAreaId, netAreaName = it.netArea, netSegment = it.netSegment)
            }
        )
    }

    /**
     * 创建新网络区域
     * @param netArea 新网络区域名称，使用大写下划线命名
     * @param netSegment 新网络区域网段信息，网段之间用英文逗号分隔
     * @return 创建结果
     */
    fun createNewNetworkArea(netArea: String, netSegment: String): NetworkAreaResult {
        checkNetworkSegment(netSegment)
        val creatNewNetworkAreaNum = networkAreaDao.insertNetworkArea(dslContext, netArea, netSegment)
        return NetworkAreaResult(
            code = SUCCESSFUL_CODE,
            result = SUCCESSFUL_RESULT,
            msg = "[createNewNetworkArea] Create $creatNewNetworkAreaNum new network area successfully.",
            networkNum = creatNewNetworkAreaNum.toLong(),
            networkInfo = null
        )
    }

    /**
     * 更新某网络区域信息（replace）
     * @param netArea 要更新的网络区域名称
     * @param netSegment 要完整替换的网段信息
     * @return 更新结果
     */
    fun replaceNetworkArea(netArea: String, netSegment: String?): NetworkAreaResult {
        checkNetworkSegment(netSegment)
        val replaceNetworkAreaNum = networkAreaDao.replaceNetworkAreaSegment(dslContext, netArea, netSegment!!)
        return NetworkAreaResult(
            code = SUCCESSFUL_CODE,
            result = SUCCESSFUL_RESULT,
            msg = "[replaceNetworkAreaSegment] Replace $replaceNetworkAreaNum network area successfully.",
            networkNum = replaceNetworkAreaNum.toLong(),
            networkInfo = null
        )
    }

    /**
     * 为某网络区域添加网段
     * @param netArea 要添加网段的网络区域名称
     * @param netSegment 要添加的网段信息，网段之间用英文逗号分隔
     * @return 添加结果
     */
    fun addSegmentToNetworkArea(netArea: String, netSegment: String?): NetworkAreaResult {
        if (netSegment.isNullOrEmpty()) {
            throw ParamBlankException("Blank net segment!")
        }
        checkNetworkSegment(netSegment)
        val segmentList: MutableList<String> = netSegment.replace(" ", "").split(",").toMutableList()
        val networkAreaInfo = networkAreaDao.getAllNetworkArea(
            dslContext, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, netArea
        )
        val nonRepetitiveSegmentList = segmentList.mapNotNull {
            if (networkAreaInfo[0].netSegment.contains(it)) null else it
        }
        val nonRepetitiveSegmentString = "," + nonRepetitiveSegmentList.joinToString(separator = ",")
        val addResInt = networkAreaDao.addNetWorkSegment(dslContext, netArea, nonRepetitiveSegmentString)
        val addSegmentNum = if (ADD_SUCCESSFUL_NUM == addResInt) {
            nonRepetitiveSegmentList.size.toLong()
        } else ADD_FAILED_NUM
        return NetworkAreaResult(
            code = SUCCESSFUL_CODE,
            result = SUCCESSFUL_RESULT,
            msg = "[addSegmentToNetworkArea] Add $addSegmentNum network area successfully: $nonRepetitiveSegmentString",
            networkNum = addSegmentNum,
            networkInfo = null
        )
    }

    /**
     * 为某网络区域删除网段
     * @param netArea 要删除网段的网络区域名称
     * @param netSegment 要删除的网段信息，网段之间用英文逗号分隔
     * @return 部分删除结果
     */
    fun deleteSegmentFromNetworkArea(netArea: String, netSegment: String?): NetworkAreaResult {
        if (netSegment.isNullOrEmpty()) {
            throw ParamBlankException("Blank net segment!")
        }
        checkNetworkSegment(netSegment)
        val networkAreaInfo = networkAreaDao.getAllNetworkArea(
            dslContext, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, netArea
        )
        val segmentList: MutableList<String> = netSegment.replace(" ", "").split(",").toMutableList()
        val notExistSegment: MutableList<String> = mutableListOf()
        var afterDeleteSegment = networkAreaInfo[0].netSegment
        val canDeleteSegmentList = segmentList.mapNotNull {
            if (networkAreaInfo[0].netSegment.contains(it)) {
                afterDeleteSegment = if (networkAreaInfo[0].netSegment.contains("$it,")) {
                    afterDeleteSegment.replace(it, "$it,")
                } else {
                    afterDeleteSegment.replace(it, "")
                }
                it
            } else {
                notExistSegment.add(it)
                null
            }
        }
        val deleteInt = networkAreaDao.replaceNetworkAreaSegment(dslContext, netArea, afterDeleteSegment)
        val deleteNum = if (DELETE_SUCCESSFUL_NUM == deleteInt) {
            canDeleteSegmentList.size.toLong()
        } else DELETE_FAILED_NUM
        return NetworkAreaResult(
            code = SUCCESSFUL_CODE,
            result = SUCCESSFUL_RESULT,
            msg = "[deleteSegmentFromNetworkArea] Delete $deleteNum network area successfully. Delete segment List:" +
                { canDeleteSegmentList.joinToString(separator = ",") },
            networkNum = deleteNum,
            networkInfo = null
        )
    }

    /**
     * 删除某网络区域
     * @param netArea 要删除的网络区域名称
     * @return 删除结果
     */
    fun deleteNetworkArea(netArea: String): NetworkAreaResult {
        val deleteNetworkAreaNum = networkAreaDao.deleteNetworkArea(dslContext, netArea)
        return NetworkAreaResult(
            code = SUCCESSFUL_CODE,
            result = SUCCESSFUL_RESULT,
            msg = "[deleteNetworkArea] delete $deleteNetworkAreaNum network area.",
            networkNum = deleteNetworkAreaNum.toLong(),
            networkInfo = null
        )
    }

    /**
     * 判断一个字符串去掉所有空格后是不是用英文逗号连接的英文字符串（允许大小写，新CMDB接口通过userId查询大小写不敏感）
     * @param netSegment 网段信息（用英文逗号分隔，允许有空格）
     */
    private fun checkNetworkSegment(netSegment: String?) {
        val isWellFormedNetSegment = netSegment?.replace(" ", "")
            ?.matches(Regex("^([a-zA-Z]+(?:,[a-zA-Z]+)*)?$"))
            ?: false
        if (!isWellFormedNetSegment) {
            throw CustomException(
                Response.Status.BAD_REQUEST,
                "Invalid net segment! " +
                    "Please check whether the network segment(s) is separated by commas(,) or an empty string"
            )
        }
    }
}
