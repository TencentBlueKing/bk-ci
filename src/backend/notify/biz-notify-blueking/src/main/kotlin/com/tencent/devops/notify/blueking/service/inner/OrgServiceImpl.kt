package com.tencent.devops.notify.blueking.service.inner

import com.tencent.devops.notify.service.OrgService
import org.springframework.stereotype.Service

@Service
class OrgServiceImpl : OrgService {

    override fun parseStaff(staffs: Set<String>): Set<String> {
        val result = HashSet<String>()
        val staffIds = HashSet<Int>()
        if (!staffs.isEmpty()) {
            staffs.forEach { staff ->
                try {
                    val staffId = Integer.parseInt(staff, 10)
                    if (staffId > 10) {
                        staffIds.add(staffId)
                    }
                } catch (ignore: NumberFormatException) {
                    result.add(staff)
                }
            }
        }
        if (!staffIds.isEmpty()) {
            // TODO: 转换数字用户ID为rtx名
        }
        return result
    }
}