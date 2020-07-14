package com.tencent.devops.sign.api.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.io.File

data class MobileProvisionInfo(
    val mobileProvisionFile: File,
    val plistFile: File,
    val entitlementFile: File,
    val bundleId: String
)