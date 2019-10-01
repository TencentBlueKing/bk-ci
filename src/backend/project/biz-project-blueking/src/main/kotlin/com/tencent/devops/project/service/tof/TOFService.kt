package com.tencent.devops.project.service.tof

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.project.pojo.DeptInfo
import com.tencent.devops.project.pojo.OrganizationInfo
import com.tencent.devops.project.pojo.enums.OrganizationType
import com.tencent.devops.project.pojo.tof.APIModule
import com.tencent.devops.project.pojo.tof.CCAppNameApplicationID
import com.tencent.devops.project.pojo.tof.CCAppNameRequest
import com.tencent.devops.project.pojo.tof.CCAppNameResponse
import com.tencent.devops.project.pojo.tof.ChildDeptRequest
import com.tencent.devops.project.pojo.tof.ChildDeptResponse
import com.tencent.devops.project.pojo.tof.DeptInfoRequest
import com.tencent.devops.project.pojo.tof.DeptInfoResponse
import com.tencent.devops.project.pojo.tof.ParentDeptInfoRequest
import com.tencent.devops.project.pojo.tof.Response
import com.tencent.devops.project.pojo.tof.StaffInfoRequest
import com.tencent.devops.project.pojo.tof.StaffInfoResponse
import com.tencent.devops.project.pojo.user.UserDeptDetail
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * API
 * http://open.oa.com/esb/docs/ieod/system/tof/
 */
@Service
class TOFService @Autowired constructor(private val objectMapper: ObjectMapper) {

    @Value("\${tof.host:#{null}}")
    private val tofHost: String? = null

    @Value("\${tof.appCode:#{null}}")
    private val tofAppCode: String? = null

    @Value("\${tof.appSecret:#{null}}")
    private val tofAppSecret: String? = null

    init {
        logger.info("Get the tof host($tofHost), code($tofAppCode) and secret($tofAppSecret)")
    }

    fun getUserDeptDetail(operator: String?, userId: String, bk_ticket: String): UserDeptDetail {
        validate()
        val staffInfo = getStaffInfo(operator, userId, bk_ticket)
        // 通过用户组查询父部门信息　(由于tof系统接口查询结构是从当前机构往上推查询，如果创建者机构层级大于4就查不完整1到3级的机构，所以查询级数设置为10)
        val deptInfos = getParentDeptInfo(staffInfo.GroupId, 10) // 一共三级，从事业群->部门->中心
        var bgName = ""
        var bgId = "0"
        var deptName = ""
        var deptId = "0"
        var centerName = ""
        var centerId = "0"
        val groupId = staffInfo.GroupId
        val groupName = staffInfo.GroupName
        for (deptInfo in deptInfos) {
            val level = deptInfo.level
            val name = deptInfo.name
            when (level) {
                "1" -> {
                    bgName = name
                    bgId = deptInfo.id
                }
                "2" -> {
                    deptName = name
                    deptId = deptInfo.id
                }
                "3" -> {
                    centerName = name
                    centerId = deptInfo.id
                }
            }
        }
        return UserDeptDetail(
            bgName,
            bgId,
            deptName,
            deptId,
            centerName,
            centerId,
            groupId,
            groupName
        )
    }

    fun getUserDeptDetail(userId: String): UserDeptDetail {
        return getUserDeptDetail(userId, "")
    }

    fun getUserDeptDetail(userId: String, bk_ticket: String): UserDeptDetail {
        return getUserDeptDetail(null, userId, bk_ticket)
    }

    fun getOrganizationInfo(
        userId: String,
        type: OrganizationType,
        id: Int
    ): List<OrganizationInfo> {
        validate()
        return getChildDeptInfos(userId, type, id).map {
            OrganizationInfo(it.ID, it.Name)
        }
    }

    fun getDeptInfo(userId: String, id: Int): DeptInfo {
        try {
            val path = "get_dept_info"
            val responseContent = request(
                path, DeptInfoRequest(
                tofAppCode!!,
                tofAppSecret!!,
                id.toString()
            ), "获取部门信息失败"
            )
            val response: Response<DeptInfoResponse> =
                objectMapper.readValue(responseContent)
            if (response.data == null) {
                logger.warn("Fail to get the dept info of id $id with response $responseContent")
                throw OperationException("获取部门信息失败")
            }
            val deptInfoResp = response.data
            return DeptInfo(deptInfoResp.TypeId, deptInfoResp.LeaderId, deptInfoResp.Name, deptInfoResp.Level, deptInfoResp.Enabled, deptInfoResp.ParentId, deptInfoResp.ID)
        } catch (t: Throwable) {
            logger.warn("Fail to get the organization info of id $id", t)
            throw OperationException("获取部门信息失败")
        }
    }

    /**
     * 通过 app ID 获取 app Name
     */
    fun getCCAppName(ccAppId: Long): String {
        try {
            val path = "get_query_info"
            val responseContent = request(
                path,
                CCAppNameRequest(
                    tofAppCode!!,
                    tofAppSecret!!,
                    CCAppNameApplicationID(ccAppId)
                ), "获取CC APP Name失败", APIModule.cc
            )
            val response: Response<List<CCAppNameResponse>> = objectMapper.readValue(responseContent)
            if (response.data == null || response.data.isEmpty()) {
                logger.warn("Fail to get cc app name of $ccAppId with response $responseContent")
                throw OperationException("后去CC APP名称失败")
            }
            return response.data[0].DisplayName
        } catch (t: Throwable) {
            logger.warn("Fail to get cc app name of $ccAppId", t)
            throw OperationException("后去CC APP名称失败")
        }
    }

    private fun getChildDeptInfos(userId: String, type: OrganizationType, id: Int): List<ChildDeptResponse> {
        try {
            val path = "get_child_dept_infos"
            val responseContent = request(
                path, ChildDeptRequest(
                    tofAppCode!!,
                    tofAppSecret!!,
                    getParentDeptIdByOrganizationType(type, id),
                    1
                ), "获取子部门信息失败"
            )
            val response: Response<List<ChildDeptResponse>> =
                objectMapper.readValue(responseContent)
            if (response.data == null) {
                logger.warn("Fail o get the child dept info of type $type and id $id with response $responseContent")
                throw OperationException("获取子部门信息失败")
            }
            return response.data
        } catch (t: Throwable) {
            logger.warn("Fail to get the organization info of type $type and id $id", t)
            throw OperationException("获取子部门信息失败")
        }
    }

    private fun getParentDeptIdByOrganizationType(type: OrganizationType, id: Int): Int {
        return when (type) {
            OrganizationType.bg -> 0
            else -> id
        }
    }

    fun getStaffInfo(operator: String?, userId: String, bk_ticket: String): StaffInfoResponse {
        try {
            val path = "get_staff_info"
            val responseContent = request(
                path, StaffInfoRequest(
                    tofAppCode!!,
                    tofAppSecret!!, operator, userId, bk_ticket
                ), "获取用户信息失败"
            )
            val response: Response<StaffInfoResponse> = objectMapper.readValue(responseContent)
            if (response.data == null) {
                logger.warn("Fail to get the staff info of user $userId with bk_ticket $bk_ticket and response $responseContent")
                throw OperationException("获取用户信息失败")
            }
            return response.data
        } catch (t: Throwable) {
            logger.warn("Fail to get the staff info of userId $userId with ticket $bk_ticket", t)
            throw OperationException("获取用户信息失败")
        }
    }

    fun getStaffInfo(userId: String, bk_ticket: String): StaffInfoResponse {
        return getStaffInfo(null, userId, bk_ticket)
    }

    fun getStaffInfo(userId: String): StaffInfoResponse {
        return getStaffInfo(null, userId, "")
    }

    fun getParentDeptInfo(groupId: String, level: Int): List<DeptInfo> {
        try {
            val path = "get_parent_dept_infos"
            val responseContent = request(
                path,
                ParentDeptInfoRequest(tofAppCode!!, tofAppSecret!!, groupId, level), "获取公司组织架构信息失败"
            )
            val response: Response<List<DeptInfo>> = objectMapper.readValue(responseContent)
            if (response.data == null) {
                logger.warn("Fail to get the parent dept info of group $groupId and level $level with response $responseContent")
                throw OperationException("获取公司组织架构信息失败")
            }
            return response.data
        } catch (t: Throwable) {
            logger.warn("Fail to get the parent dept info of group $groupId and level $level", t)
            throw OperationException("获取父部门信息失败")
        }
    }

    private fun request(path: String, body: Any, errorMessage: String, apiModule: APIModule = APIModule.tof): String {
        val url = "http://$tofHost/component/compapi/${apiModule.name}/$path"
        val requestContent = objectMapper.writeValueAsString(body)
        logger.info("Start to request $url with body $requestContent")
        val requestBody = Request.Builder()
            .url(url)
            .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestContent))
            .build()
        val response = request(requestBody, errorMessage)
        logger.info("Get the response $response of request $url")
        return response
    }

    private fun request(request: Request, errorMessage: String): String {
//        val httpClient = HttpUtil.getHttpClient()
//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("Fail to request $request with code ${response.code()}, message ${response.message()} and body $responseContent")
                throw RuntimeException(errorMessage)
            }
            return responseContent
        }
    }

    private fun validate() {
        if (tofHost.isNullOrBlank()) {
            throw RuntimeException("TOF HOST is empty")
        }
        if (tofAppCode.isNullOrBlank()) {
            throw RuntimeException("TOF app code is empty")
        }
        if (tofAppSecret.isNullOrBlank()) {
            throw RuntimeException("TOF app secret is empty")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TOFService::class.java)
    }
}