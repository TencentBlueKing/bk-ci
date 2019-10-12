package com.tencent.devops.project.pojo.tof

/**
 * http://open.oa.com/esb/docs/ieod/system/tof/get_child_dept_infos/
 */
class DeptInfoResponse(
    val TypeId: String,
    val LeaderId: String,
    val Name: String,
    val Level: String,
    val Enabled: String,
    val SecretaryId: String,
    val TypeName: String,
    val VicePresidentId: String,
    val ParentId: String,
    val ExProperties: String,
    val ExchangeGroupName: String,
    val ID: String
)
