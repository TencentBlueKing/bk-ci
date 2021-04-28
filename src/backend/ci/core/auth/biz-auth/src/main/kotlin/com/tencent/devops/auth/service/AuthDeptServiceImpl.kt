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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.auth.entity.SearchDeptEntity
import com.tencent.devops.auth.entity.SearchDeptUserEntity
import com.tencent.devops.auth.pojo.vo.DeptInfoVo
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.redis.RedisOperation
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

class AuthDeptServiceImpl @Autowired constructor(
    val redisOperation: RedisOperation,
    val objectMapper: ObjectMapper
) : DeptService {

    @Value("\${esb.code:#{null}}")
    val appCode: String? = null

    @Value("\${esb.secret:#{null}}")
    val appSecret: String? = null

    @Value("\${bk.paas.host:#{null}}")
    val iamHost: String? = null

    override fun getDeptByLevel(level: Int): DeptInfoVo {
        val search = SearchDeptEntity(
            bk_app_code = appCode!!,
            bk_app_secret = appSecret!!,
            bk_username = "",
            fields = "id,name,parent",
            lookupField = "level",
            exactLookups = level,
            fuzzyLookups = null
        )
        return getDeptInfo(search)
    }

    override fun getDeptByParent(parentId: Int): DeptInfoVo {
        val search = SearchDeptEntity(
            bk_app_code = appCode!!,
            bk_app_secret = appSecret!!,
            bk_username = "",
            fields = "id,name,parent",
            lookupField = "parent",
            exactLookups = parentId,
            fuzzyLookups = null
        )
        return getDeptInfo(search)
    }

    override fun getDeptUser(deptId: Int): List<String> {
        val search = SearchDeptUserEntity(
            id = deptId,
            recursive = true,
            bk_app_code = appCode!!,
            bk_app_secret = appSecret!!,
            bk_username = ""
        )
        val url = getAuthRequestUrl(LIST_DEPARTMENT_PROFILES)
        val content = objectMapper.writeValueAsString(search)
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val requestBody = RequestBody.create(mediaType, content)
        val request = Request.Builder().url(url)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                // 请求错误
                throw RemoteServiceException("get dept user fail, response: ($it)")
            }
            val responseStr = it.body()!!.string()
            val deptInfos = objectMapper.readValue<Map<String, Any>>(responseStr)
            if (deptInfos["code"] != 0 || deptInfos["result"] == false) {
                // 请求错误
                throw RemoteServiceException(
                    "get dept user fail: $responseStr"
                )
            }
            val data = deptInfos["data"].toString()
            val dataMap = objectMapper.readValue<Map<String, Any>>(data)
            val userInfos = objectMapper.readValue<List<Map<String,Any>>>(dataMap["result"].toString())
            val users = mutableListOf<String>()
            userInfos.forEach {
                users.add(it["username"].toString())
            }

            return users
        }
    }

    private fun getDeptInfo(search: SearchDeptEntity): DeptInfoVo {
        val url = getAuthRequestUrl(LIST_DEPARTMENTS)
        val content = objectMapper.writeValueAsString(search)
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val requestBody = RequestBody.create(mediaType, content)
        val request = Request.Builder().url(url)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                // 请求错误
                throw RemoteServiceException("get dept fail, response: ($it)")
            }
            val responseStr = it.body()!!.string()
            val deptInfos = objectMapper.readValue<Map<String, Any>>(responseStr)
            if (deptInfos["code"] != 0 || deptInfos["result"] == false) {
                // 请求错误
                throw RemoteServiceException(
                    "get dept fail: $responseStr"
                )
            }
            return objectMapper.readValue<DeptInfoVo>(deptInfos["data"].toString())
        }
    }

    fun findUserName(str: String): List<String> {
        val deptInfos = objectMapper.readValue<Map<String, Any>>(str)
        if (deptInfos["code"] != 0 || deptInfos["result"] == false) {
            // 请求错误
            throw RemoteServiceException(
                "get dept user fail: $str"
            )
        }
        val data = deptInfos["data"].toString()
        val dataMap = objectMapper.readValue<Map<String, Any>>(data)
        val userInfos = objectMapper.readValue<List<Map<String,Any>>>(dataMap["result"].toString())
        val users = mutableListOf<String>()
        userInfos.forEach {
            users.add(it["username"].toString())
        }

        return users
    }

    /**
     * 生成请求url
     */
    private fun getAuthRequestUrl(uri: String): String {
        return if (iamHost?.endsWith("/")!!) {
            iamHost + uri
        } else {
            "$iamHost/$uri"
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(AuthDeptServiceImpl::class.java)
        const val LIST_DEPARTMENTS = "api/c/compapi/v2/usermanage/list_departments/"
        const val LIST_DEPARTMENT_PROFILES = "api/c/compapi/v2/usermanage/list_department_profiles/"
    }
}
