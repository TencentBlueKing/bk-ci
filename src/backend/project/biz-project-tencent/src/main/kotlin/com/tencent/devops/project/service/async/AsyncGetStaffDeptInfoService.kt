package com.tencent.devops.project.service.async

import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.tof.TOFService
import org.slf4j.LoggerFactory
/**
 * 异步获取人员机构信息
 * author: carlyin
 * since: 2018-12-09
 */
class AsyncGetStaffDeptInfoService(private val userId: String, private val bk_ticket: String) {
    private val logger = LoggerFactory.getLogger(AsyncGetStaffDeptInfoService::class.java)
    val content: UserDeptDetail?
        get() {
            val tofService = SpringContextUtil.getBean(TOFService::class.java)
            try {
                return tofService.getUserDeptDetail(userId, bk_ticket) // 远程调用tof获取人员机构信息
            } catch (e: Exception) {
                logger.error("async staff deptInfo fail,the error is :{}", e)
            }
            return null
        }
}