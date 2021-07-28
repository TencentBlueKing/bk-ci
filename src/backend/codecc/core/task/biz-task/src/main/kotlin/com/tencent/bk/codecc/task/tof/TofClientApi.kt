package com.tencent.bk.codecc.task.tof

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.codecc.task.pojo.Response
import com.tencent.bk.codecc.task.pojo.TofDeptInfo
import com.tencent.bk.codecc.task.pojo.TofDeptStaffInfo
import com.tencent.bk.codecc.task.pojo.TofOrganizationInfo
import com.tencent.bk.codecc.task.pojo.TofStaffInfo
import com.tencent.devops.common.util.OkhttpUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class TofClientApi @Autowired constructor(
    private val objectMapper: ObjectMapper
) {

    @Value("\${tof.codecc.appcode:#{null}}")
    private val appCode: String? = null

    @Value("\${tof.codecc.appsecret:#{null}}")
    private val appSecret: String? = null

    @Value("\${tof.rootpath:#{null}}")
    private val rootPath: String? = null

    private val operator: String = "admin"

    /**
     * 根据名字获取员工信息
     */
    fun getStaffInfoByUserName(userName: String): Response<TofStaffInfo> {
        val url =
                "$rootPath/component/compapi/tof/get_staff_info_by_login_name"
        val requestBody = mapOf(
                "app_code" to appCode,
                "app_secret" to appSecret,
                "operator" to operator,
                "login_name" to userName
        )
        val result = OkhttpUtils.doHttpPost(url, objectMapper.writeValueAsString(requestBody))
        return objectMapper.readValue(result, object : TypeReference<Response<TofStaffInfo>>() {})
    }

    /**
     * 根据员工id获取员工信息
     */
    fun getStaffInfoByStaffId(staffId: Int): Response<TofStaffInfo> {
        val url =
                "$rootPath/component/compapi/tof/get_staff_info_by_login_name"
        val requestBody = mapOf(
                "app_code" to appCode,
                "app_secret" to appSecret,
                "operator" to operator,
                "staff_id" to staffId
        )
        val result = OkhttpUtils.doHttpPost(url, objectMapper.writeValueAsString(requestBody))
        return objectMapper.readValue(result, object : TypeReference<Response<TofStaffInfo>>() {})
    }

    /**
     * 根据groupId获取所有组织信息
     */
    fun getOrganizationInfoByGroupId(groupId: Int): TofOrganizationInfo? {
        val url = "$rootPath/component/compapi/tof/get_parent_dept_infos"
        val requestBody = mapOf(
                "app_code" to appCode,
                "app_secret" to appSecret,
                "operator" to operator,
                "dept_id" to groupId,
                "level" to 10
        )
        val result = OkhttpUtils.doHttpPost(url, objectMapper.writeValueAsString(requestBody))
        val organizationResult: Response<List<TofDeptInfo>> =
                objectMapper.readValue(result, object : TypeReference<Response<List<TofDeptInfo>>>() {})
        val organizationList = organizationResult.data
        if (organizationList.isNullOrEmpty()) {
            return null
        }
        val organizationInfo = TofOrganizationInfo()
        organizationList.forEach {
            when (it.Level) {
                "1" -> {
                    organizationInfo.bgId = it.ID
                    organizationInfo.bgName = it.Name
                }
                "2" -> {
                    organizationInfo.deptId = it.ID
                    organizationInfo.deptName = it.Name
                }
                "3" -> {
                    organizationInfo.centerId = it.ID
                    organizationInfo.centerName = it.Name
                }
                else -> return@forEach
            }
        }
        return organizationInfo
    }

    /**
     * 根据部门员工信息
     */
    fun getDeptStaffInfos(deptId: String): Response<List<TofDeptStaffInfo>> {
        val url =
                "$rootPath/component/compapi/tof/get_dept_staffs_with_level"
        val requestBody = mapOf(
                "app_code" to appCode,
                "app_secret" to appSecret,
                "operator" to operator,
                "dept_id" to deptId,
                "level" to 10
        )
        val result = OkhttpUtils.doHttpPost(url, objectMapper.writeValueAsString(requestBody))
        return objectMapper.readValue(result, object : TypeReference<Response<List<TofDeptStaffInfo>>>() {})
    }

    /**
     * 获取子部门信息
     */
    fun getChildDeptInfos(parentDeptId: String): Response<List<TofDeptInfo>> {
        val url =
                "$rootPath/component/compapi/tof/get_child_dept_infos"
        val requestBody = mapOf(
                "app_code" to appCode,
                "app_secret" to appSecret,
                "operator" to operator,
                "parent_dept_id" to parentDeptId,
                "level" to 1
        )
        val result = OkhttpUtils.doHttpPost(url, objectMapper.writeValueAsString(requestBody))
        return objectMapper.readValue(result, object : TypeReference<Response<List<TofDeptInfo>>>() {})
    }

    /**
     * 按RTX获取组织架构信息
     */
    fun getTofOrgInfoByUserName(userName: String): TofOrganizationInfo? {
        val tofStaffInfo = getStaffInfoByUserName(userName).data
        val groupId = tofStaffInfo?.GroupId ?: return null
        return getOrganizationInfoByGroupId(groupId)
    }
}