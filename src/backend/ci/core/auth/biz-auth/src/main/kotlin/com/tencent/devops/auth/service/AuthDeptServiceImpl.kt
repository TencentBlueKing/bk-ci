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

package com.tencent.devops.auth.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.cache.CacheBuilder
import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.response.ResponseDTO
import com.tencent.devops.auth.common.Constants.DEPT_LABEL
import com.tencent.devops.auth.common.Constants.HTTP_RESULT
import com.tencent.devops.auth.common.Constants.ID
import com.tencent.devops.auth.common.Constants.LEVEL
import com.tencent.devops.auth.common.Constants.NAME
import com.tencent.devops.auth.common.Constants.PARENT
import com.tencent.devops.auth.common.Constants.USERNAME
import com.tencent.devops.auth.common.Constants.USER_LABEL
import com.tencent.devops.auth.common.Constants.USER_NAME_AND_DISPLAY_NAME_LABEL
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.entity.SearchDeptUserEntity
import com.tencent.devops.auth.entity.SearchProfileDeptEntity
import com.tencent.devops.auth.entity.SearchRetrieveDeptEntity
import com.tencent.devops.auth.entity.SearchUserAndDeptEntity
import com.tencent.devops.auth.entity.UserDeptTreeInfo
import com.tencent.devops.auth.pojo.BkUserDeptInfo
import com.tencent.devops.auth.pojo.BkUserInfo
import com.tencent.devops.auth.pojo.DeptInfo
import com.tencent.devops.auth.pojo.vo.BkDeptDetailsVo
import com.tencent.devops.auth.pojo.vo.BkUserInfoVo
import com.tencent.devops.auth.pojo.vo.DeptInfoVo
import com.tencent.devops.auth.pojo.vo.UserAndDeptInfoVo
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.pojo.EsbBaseReq
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.util.concurrent.TimeUnit

class AuthDeptServiceImpl(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper,
    private val userManageService: UserManageService
) : DeptService {

    @Value("\${esb.code:#{null}}")
    val appCode: String? = null

    @Value("\${esb.secret:#{null}}")
    val appSecret: String? = null

    @Value("\${bk.user.host:#{null}}")
    val bkUserHost: String? = null

    private val deptUserCache = CacheBuilder.newBuilder()
        .maximumSize(500)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build<String/*deptId*/, List<String>>()

    private val userDeptCache = CacheBuilder.newBuilder()
        .maximumSize(500)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build<String/*userId*/, Set<String>>()

    private val userInfoCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build<String/*userId*/, UserAndDeptInfoVo>()

    private val memberInfoCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build<String/*userId*/, UserAndDeptInfoVo>()

    // 已离职或不存在成员
    private val departedMembersCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String/*userId*/, Boolean/*是否不存在/离职*/>()

    override fun getDeptByLevel(level: Int, accessToken: String?, userId: String): DeptInfoVo {
        val search = SearchUserAndDeptEntity(
            bk_app_code = appCode!!,
            bk_app_secret = appSecret!!,
            bk_username = userId,
            fields = null,
            lookupField = LEVEL,
            exactLookups = level,
            fuzzyLookups = null,
            accessToken = accessToken
        )
        return listDeptInfos(search)
    }

    override fun getDeptByParent(parentId: Int, accessToken: String?, userId: String, pageSize: Int?): DeptInfoVo {
        val search = SearchUserAndDeptEntity(
            bk_app_code = appCode!!,
            bk_app_secret = appSecret!!,
            bk_username = userId,
            fields = null,
            lookupField = PARENT,
            exactLookups = parentId,
            fuzzyLookups = null,
            accessToken = accessToken,
            pageSize = pageSize ?: null
        )
        return listDeptInfos(search)
    }

    @Suppress("ComplexMethod")
    override fun getUserAndDeptByName(
        name: String,
        accessToken: String?,
        userId: String,
        type: ManagerScopesEnum,
        exactLookups: Boolean?
    ): List<UserAndDeptInfoVo?> {
        val deptSearch = SearchUserAndDeptEntity(
            bk_app_code = appCode!!,
            bk_app_secret = appSecret!!,
            bk_username = userId,
            fields = null,
            lookupField = NAME,
            accessToken = accessToken
        )
        val userSearch = SearchUserAndDeptEntity(
            bk_app_code = appCode,
            bk_app_secret = appSecret,
            bk_username = userId,
            fields = USER_LABEL,
            lookupField = USERNAME,
            accessToken = accessToken
        )
        // 模糊搜索或者精准搜索方式
        if (exactLookups == null || exactLookups == false) {
            deptSearch.fuzzyLookups = name
            userSearch.fuzzyLookups = name
        } else {
            deptSearch.exactLookups = name
            userSearch.exactLookups = name
        }

        val userAndDeptInfos = mutableListOf<UserAndDeptInfoVo>()
        when (type) {
            ManagerScopesEnum.USER -> {
                val userInfos = listUserInfos(userSearch)
                userInfos.results.forEach {
                    userAndDeptInfos.add(
                        it.toUserAndDeptInfoVo()
                    )
                }
            }

            ManagerScopesEnum.DEPARTMENT -> {
                val deptInfos = listDeptInfos(deptSearch)
                deptInfos.results.forEach {
                    it.toUserAndDeptInfoVo()
                }
            }

            ManagerScopesEnum.ALL -> {
                val userInfos = listUserInfos(userSearch)
                userInfos.results.forEach {
                    userAndDeptInfos.add(
                        it.toUserAndDeptInfoVo()
                    )
                }
                val deptInfos = listDeptInfos(deptSearch)
                deptInfos.results.forEach {
                    userAndDeptInfos.add(
                        it.toUserAndDeptInfoVo()
                    )
                }
            }

            else -> {}
        }
        return userAndDeptInfos
    }

    override fun getDeptUser(deptId: Int, accessToken: String?): List<String> {
        return if (deptUserCache.getIfPresent(deptId.toString()) != null) {
            deptUserCache.getIfPresent(deptId.toString())!!
        } else {
            val deptUsers = getAndRefreshDeptUser(deptId, accessToken)
            deptUserCache.put(deptId.toString(), deptUsers)
            deptUsers
        }
    }

    override fun getUserParentDept(userId: String): Int {
        val deptSearchResponse = getUserDeptFamily(userId)
        val deptId = getUserLastDeptId(deptSearchResponse)
        val parentSearch = SearchRetrieveDeptEntity(
            id = deptId,
            bk_app_code = appCode!!,
            bk_app_secret = appSecret!!,
            bk_username = userId
        )
        val userCenterResponse = callUserCenter(RETRIEVE_DEPARTMENT, parentSearch)
        return getParentDept(userCenterResponse)
    }

    override fun getDeptByName(deptName: String, userId: String): DeptInfoVo? {
        val search = SearchUserAndDeptEntity(
            bk_app_code = appCode!!,
            bk_app_secret = appSecret!!,
            bk_username = userId,
            fields = null,
            lookupField = NAME,
            exactLookups = deptName,
            fuzzyLookups = null,
            accessToken = null
        )
        return listDeptInfos(search)
    }

    override fun getUserDeptInfo(userId: String): Set<String> {
        if (userId.endsWith("@tai")) {
            return emptySet()
        }

        userDeptCache.getIfPresent(userId)?.let {
            return it
        }
        val primaryDeptIds = userManageService.getUserDepartmentPath(userId)
        val resultDeptIds = primaryDeptIds
            .takeIf { it.isNotEmpty() }
            ?.toSet()
            ?: run {
                val deptFamilyInfo = getUserDeptFamily(userId)
                getUserDeptTreeIds(deptFamilyInfo)
            }
        userDeptCache.put(userId, resultDeptIds)
        return resultDeptIds
    }

    override fun getUserInfo(userId: String, name: String): UserAndDeptInfoVo? {
        return userInfoCache.getIfPresent(name) ?: getUserAndPutInCache(name)
    }

    override fun getUserInfo(userId: String): UserAndDeptInfoVo? {
        return userInfoCache.getIfPresent(userId) ?: getUserAndPutInCache(userId)
    }

    override fun getMemberInfo(
        memberId: String,
        memberType: ManagerScopesEnum
    ): UserAndDeptInfoVo {
        return when (memberType) {
            ManagerScopesEnum.USER -> {
                getUserInfo(memberId)
            }

            else -> {
                listMemberInfos(
                    memberIds = listOf(memberId),
                    memberType = memberType
                ).firstOrNull()
            }
        } ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.USER_NOT_EXIST,
            params = arrayOf(memberId),
            defaultMessage = "member $memberId not exist"
        )
    }

    @Suppress("NestedBlockDepth")
    override fun listMemberInfos(
        memberIds: List<String>,
        memberType: ManagerScopesEnum
    ): List<UserAndDeptInfoVo> {
        val cacheResult = memberInfoCache.getAllPresent(memberIds)
        val membersNotInCache = memberIds.filterNot { cacheResult.containsKey(it) }

        if (membersNotInCache.isNotEmpty()) {
            val memberInfos = fetchMemberInfos(membersNotInCache, memberType)
            membersNotInCache.forEach { memberId ->
                val memberInfo = when (memberType) {
                    ManagerScopesEnum.USER -> memberInfos.firstOrNull { it.name == memberId }
                    else -> memberInfos.firstOrNull { it.id == memberId.toInt() }
                }

                when {
                    memberType == ManagerScopesEnum.USER && memberInfo == null ->
                        departedMembersCache.put(memberId, true)

                    memberInfo != null ->
                        memberInfoCache.put(memberId, memberInfo)
                }
            }
        }

        return memberInfoCache.getAllPresent(memberIds).values.toList()
    }

    override fun listDepartedMembers(memberIds: List<String>): List<String> {
        val activeMembers = listMemberInfos(
            memberIds = memberIds,
            memberType = ManagerScopesEnum.USER
        ).map { it.name }
        return memberIds.subtract(activeMembers.toSet()).toList().also {
            logger.info("list departed members : $it")
        }
    }

    override fun isUserDeparted(userId: String): Boolean {
        return if (departedMembersCache.getIfPresent(userId) == true) {
            true
        } else {
            val userInfo = getUserInfo(userId)
            userInfo == null || userInfo.departed == true
        }.also {
            if (it) {
                departedMembersCache.put(userId, true)
                logger.info("user departed :$userId")
            }
        }
    }

    override fun getUserInfoFromExternal(userId: String): UserAndDeptInfoVo? {
        return try {
            val url = getAuthRequestUrl(String.format(USER_INFO, userId))
            val searchEntity = SearchUserAndDeptEntity(
                bk_app_code = appCode!!,
                bk_app_secret = appSecret!!,
                bk_username = userId,
                fields = USER_LABEL,
                lookupField = USERNAME
            )
            val request = Request.Builder().url(url)
                .headers(searchEntity.toMap().toHeaders())
                .get()
                .build()
            OkhttpUtils.doHttp(request).use {
                if (!it.isSuccessful) {
                    logger.warn("get user info fail {}|{}|{}", url, searchEntity, it)
                    throw OperationException(
                        I18nUtil.getCodeLanMessage(
                            messageCode = AuthMessageCode.USER_NOT_EXIST
                        )
                    )
                }
                val responseStr = it.body!!.string()
                logger.info("get user info response {}", responseStr)
                val responseDTO = JsonUtil.to(responseStr, object : TypeReference<ResponseDTO<BkUserInfo>>() {})
                if (responseDTO.code != 0L || responseDTO.result == false) {
                    logger.warn("get user info fail {}|{}|{}", url, searchEntity, it)
                    throw OperationException(
                        I18nUtil.getCodeLanMessage(
                            messageCode = AuthMessageCode.USER_NOT_EXIST
                        )
                    )
                }
                logger.info("get user info：${objectMapper.writeValueAsString(responseDTO.data)}")
                responseDTO.data.toUserAndDeptInfoVo()
            }
        } catch (ex: Exception) {
            logger.warn("User does not exist $userId |$ex")
            null
        }
    }

    private fun fetchMemberInfos(
        memberIds: List<String>,
        memberType: ManagerScopesEnum
    ): List<UserAndDeptInfoVo> {
        val memberInfos = when (memberType) {
            ManagerScopesEnum.USER -> {
                val userSearch = SearchUserAndDeptEntity(
                    bk_app_code = appCode!!,
                    bk_app_secret = appSecret!!,
                    bk_username = "admin",
                    fields = USER_NAME_AND_DISPLAY_NAME_LABEL,
                    lookupField = USERNAME,
                    exactLookups = memberIds.joinToString(",")
                )
                listUserInfos(userSearch).results.map { it.toUserAndDeptInfoVo() }
            }

            ManagerScopesEnum.DEPARTMENT -> {
                val deptSearch = SearchUserAndDeptEntity(
                    bk_app_code = appCode!!,
                    bk_app_secret = appSecret!!,
                    bk_username = "admin",
                    fields = DEPT_LABEL,
                    lookupField = ID,
                    exactLookups = memberIds.joinToString(",")
                )
                listDeptInfos(deptSearch).results.map { it.toUserAndDeptInfoVo() }
            }

            else -> emptyList()
        }
        return memberInfos
    }

    private fun BkUserInfo.toUserAndDeptInfoVo(): UserAndDeptInfoVo {
        val department = this.departments?.firstOrNull()
        return UserAndDeptInfoVo(
            id = this.id,
            name = this.userName,
            displayName = this.displayName,
            type = ManagerScopesEnum.USER,
            deptInfo = if (department == null) {
                emptyList()
            } else {
                department.fullName?.split("/")?.map {
                    BkUserDeptInfo(
                        id = null,
                        name = it,
                        fullName = it
                    )
                } ?: emptyList()
            },
            extras = this.extras
        )
    }

    private fun DeptInfo.toUserAndDeptInfoVo(): UserAndDeptInfoVo {
        return UserAndDeptInfoVo(
            id = this.id,
            displayName = this.name,
            name = this.name,
            type = ManagerScopesEnum.DEPARTMENT,
            hasChild = this.hasChildren
        )
    }

    private fun getUserAndPutInCache(userId: String): UserAndDeptInfoVo? {
        return userManageService.getUserInfo(userId)?.let {
            UserAndDeptInfoVo(
                id = 0,
                name = it.userId,
                displayName = it.userName,
                type = ManagerScopesEnum.USER,
                deptInfo = it.departments,
                departed = it.departed
            )
        } ?: getUserInfoFromExternal(userId).also {
            if (it != null) userInfoCache.put(userId, it)
        }
    }

    private fun getUserDeptFamily(userId: String): String {
        val deptSearch = SearchProfileDeptEntity(
            id = userId,
            with_family = true,
            bk_app_code = appCode!!,
            bk_app_secret = appSecret!!,
            bk_username = userId
        )
        return callUserCenter(LIST_PROFILE_DEPARTMENTS, deptSearch)
    }

    override fun getUserDeptDetails(userId: String): BkDeptDetailsVo? {
        val deptSearch = SearchProfileDeptEntity(
            id = userId,
            with_family = true,
            bk_app_code = appCode!!,
            bk_app_secret = appSecret!!,
            bk_username = userId
        )
        val deptInfoStr = callUserCenter(LIST_PROFILE_DEPARTMENTS, deptSearch)
        return objectMapper.readValue<List<BkDeptDetailsVo>>(deptInfoStr).firstOrNull()
    }

    private fun getAndRefreshDeptUser(deptId: Int, accessToken: String?): List<String> {
        val search = SearchDeptUserEntity(
            id = deptId,
            recursive = true,
            bk_app_code = appCode!!,
            bk_app_secret = appSecret!!,
            bk_username = "",
            accessToken = accessToken
        )
        val responseStr = callUserCenter(LIST_DEPARTMENT_PROFILES, search)
        return findUserName(responseStr)
    }

    override fun listDeptInfos(searchDeptEnity: SearchUserAndDeptEntity): DeptInfoVo {
        val responseDTO = callUserCenter(LIST_DEPARTMENTS, searchDeptEnity)
        return objectMapper.readValue(responseDTO)
    }

    override fun listUserInfos(searchUserEntity: SearchUserAndDeptEntity): BkUserInfoVo {
        val responseDTO = callUserCenter(USER_INFO_LIST, searchUserEntity)
        return objectMapper.readValue(responseDTO)
    }

    private fun callUserCenter(url: String, searchEntity: EsbBaseReq): String {
        val url = getAuthRequestUrl(url)
        val content = objectMapper.writeValueAsString(searchEntity)
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = RequestBody.create(mediaType, content)
        val request = Request.Builder().url(url)
            .headers(searchEntity.toMap().toHeaders())
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                // 请求错误
                logger.warn(
                    "call user center fail: url = $url | searchEntity = $searchEntity" +
                        " | response = ($it)"
                )
                throw OperationException(
                    I18nUtil.getCodeLanMessage(
                        messageCode = AuthMessageCode.USER_NOT_EXIST
                    )
                )
            }
            val responseStr = it.body!!.string()
            logger.info("callUserCenter : response = $responseStr")
            val responseDTO = JsonUtil.to(responseStr, ResponseDTO::class.java)
            if (responseDTO.code != 0L || responseDTO.result == false) {
                // 请求错误
                logger.warn(
                    "call user center fail: url = $url | searchEntity = $searchEntity" +
                        " | response = ($it)"
                )
                throw OperationException(
                    I18nUtil.getCodeLanMessage(
                        messageCode = AuthMessageCode.USER_NOT_EXIST
                    )
                )
            }
            logger.info("user center response：${objectMapper.writeValueAsString(responseDTO.data)}")
            return objectMapper.writeValueAsString(responseDTO.data)
        }
    }

    fun findUserName(str: String): List<String> {
        val dataMap = JsonUtil.to(str, Map::class.java)
        val userInfoList = JsonUtil.to(JsonUtil.toJson(dataMap[HTTP_RESULT]!!), List::class.java)
        val users = mutableListOf<String>()
        userInfoList.forEachIndexed foreach@{ userIndex, user ->
            if (userIndex == MAX_USER_OF_DEPARTMENT_RETURNED) return@foreach
            val userInfo = JsonUtil.toJson(user!!)
            val userInfoMap = JsonUtil.to(userInfo, Map::class.java)
            val userName = userInfoMap["username"].toString()
            users.add(userName)
        }
        return users
    }

    private fun getParentDept(responseData: String): Int {
        val dataMap = JsonUtil.to(responseData, Map::class.java)
        return dataMap["parent"]?.toString()?.toInt() ?: 0
    }

    private fun getUserLastDeptId(responseData: String): Int {
        val deptInfo = JsonUtil.to(responseData, List::class.java)
        val any = deptInfo[0] as Any
        if (any is Map<*, *>) {
            return any["id"].toString().toInt()
        }
        return 0
    }

    fun getUserDeptTreeIds(responseData: String): Set<String> {
        val deptInfo = JsonUtil.to(responseData, object : TypeReference<List<UserDeptTreeInfo>>() {})
        val deptTreeId = mutableSetOf<String>()
        val deptTree = deptInfo[0]
        deptTreeId.add(deptTree.id)
        val family = deptTree.family
        family.forEach {
            deptTreeId.add(it.id.toString())
        }
        return deptTreeId
    }

    /**
     * 生成请求url
     */
    private fun getAuthRequestUrl(uri: String): String {
        return if (bkUserHost?.endsWith("/")!!) {
            bkUserHost + uri
        } else {
            "$bkUserHost/$uri"
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthDeptServiceImpl::class.java)
        const val LIST_DEPARTMENTS = "api/c/compapi/v2/usermanage/list_departments/"
        const val LIST_DEPARTMENT_PROFILES = "api/c/compapi/v2/usermanage/list_department_profiles/"
        const val USER_INFO_LIST = "api/c/compapi/v2/usermanage/list_users/"
        const val USER_INFO = "api/c/compapi/v2/usermanage/retrieve_user?id=%s"
        const val RETRIEVE_DEPARTMENT = "api/c/compapi/v2/usermanage/retrieve_department/"
        const val LIST_PROFILE_DEPARTMENTS = "api/c/compapi/v2/usermanage/list_profile_departments/"
        const val MAX_USER_OF_DEPARTMENT_RETURNED = 500
    }
}
