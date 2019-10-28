package com.tencent.devops.store.service.common

import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.ServiceUserResource
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.pojo.user.UserDeptDetail
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * store用户通用业务逻辑类
 * author: carlyin
 * since: 2019-03-26
 */
@Service
class StoreUserService @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeVisibleDeptService: StoreVisibleDeptService,
    private val storeMemberDao: StoreMemberDao,
    private val client: Client
) {
    private val logger = LoggerFactory.getLogger(StoreUserService::class.java)

    /**
     * 获取用户机构ID信息
     */
    fun getUserDeptList(userId: String): List<Int> {
        val userInfo = client.get(ServiceUserResource::class).getDetailFromCache(userId).data
        logger.info("the userInfo is:$userInfo")
        return if (userInfo == null) {
            listOf(0, 0, 0, 0)
        } else {
            listOf(userInfo.bg_id.toInt(), userInfo.dept_id.toInt(), userInfo.center_id.toInt(), userInfo.group_id.toInt())
        }
    }

    /**
     * 判断用户是否能安装store组件
     */
    fun isCanInstallStoreComponent(defaultFlag: Boolean, userId: String, storeCode: String, storeType: StoreTypeEnum): Boolean {
        return if (defaultFlag || storeMemberDao.isStoreMember(dslContext, userId, storeCode, storeType.type.toByte())) {
            true
        } else {
            // 获取用户组织架构
            val userDeptList = getUserDeptList(userId)
            val storeDept = storeVisibleDeptService.batchGetVisibleDept(listOf(storeCode), storeType).data?.get(storeCode)
            storeDept != null && (storeDept.contains(0) || storeDept.intersect(userDeptList).count() > 0)
        }
    }

    fun getUserFullDeptName(userId: String): Result<String?> {
        val userDeptInfo: UserDeptDetail?
        try {
            // 获取用户的机构信息
            userDeptInfo = client.get(ServiceUserResource::class).getDetailFromCache(userId).data
        } catch (e: Exception) {
            logger.info("getUserDeptDetailFromCache error  is :$e")
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
        }
        logger.info("the userDeptInfo is:$userDeptInfo")
        return if (null != userDeptInfo) {
            val commenterDept = StringBuilder(userDeptInfo.bg_name) // 组装评论者的机构信息
            if (userDeptInfo.dept_name.isNotEmpty()) commenterDept.append("/").append(userDeptInfo.dept_name)
            if (userDeptInfo.center_name.isNotEmpty()) commenterDept.append("/").append(userDeptInfo.center_name)
            if (userDeptInfo.group_name.isNotEmpty()) commenterDept.append("/").append(userDeptInfo.group_name)
            Result(commenterDept.toString())
        } else {
            MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
        }
    }
}
