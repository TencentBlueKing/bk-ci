package com.tencent.devops.store.pojo

data class EditInfoDTO(
    val baseInfo: UpdateExtBaseInfo?,
    val mediaInfo: List<UpdateMediaInfo>?,
    val settingInfo: UpdateSettingInfo?
)