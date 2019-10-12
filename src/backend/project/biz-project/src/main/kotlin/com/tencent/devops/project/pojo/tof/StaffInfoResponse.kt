package com.tencent.devops.project.pojo.tof

/**
 * {
"LoginName": "rdeng",
"WeiXinUserName": "dengxian_zhi",
"WorkQQNumber": " ",
"DepartmentName": "技术运营部",
"Enabled": "true",
"FullName": "rdeng(邓贤智)",
"ChineseName": "邓贤智",
"GroupName": "DevOps平台组",
"DepartmentId": "25923",
"StatusName": "在职",
"EnglishName": "rdeng",
"GroupId": "26080",
"StatusId": "1",
"QQ": "280846108",
"TypeId": "2",
"PostName": "DevOps平台组员工",
"WorkDeptId": "25923",
"OfficialId": "8",
"ID": "104315",
"TypeName": "正式",
"WorkDeptName": "技术运营部",
"Gender": "男",
"ExProperties": null,
"Birthday": "1990-01-18T00:00:00",
"OfficialName": "普通员工",
"PostId": "77105",
"MobilePhoneNumber": "15196637247",
"RTX": "rdeng",
"BranchPhoneNumber": "68429"
}
 */
data class StaffInfoResponse(
    val LoginName: String,
    val DepartmentName: String,
    val FullName: String,
    val ChineseName: String,
    val GroupId: String,
    val GroupName: String
)