package com.tencent.devops.notify.blueking.service

interface OrgService {
    fun parseStaff(staffs: Set<String>): Set<String>
}