package com.tencent.devops.notify.service

interface OrgService {
    fun parseStaff(staffs: Set<String>): Set<String>
}