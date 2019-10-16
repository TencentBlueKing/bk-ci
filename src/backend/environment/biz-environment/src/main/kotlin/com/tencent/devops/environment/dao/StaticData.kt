package com.tencent.devops.environment.dao

import com.tencent.devops.environment.pojo.BcsImageInfo
import com.tencent.devops.environment.pojo.BcsVmModel
import com.tencent.devops.environment.pojo.DevCloudModel

object StaticData {
    fun getBcsVmModelList() = listOf(BcsVmModel("system_base", "基础配置（8核心16G）", "8", "16384"))

    fun getBcsImageList() = listOf(BcsImageInfo("tlinux_2.2", "TLinux2.2", "sh.artifactory.oa.com:8443/devops/prod/tlinux2.2:v1"))

//    fun getDevCloudModelList() = listOf(DevCloudModel("system_base", "普通版（8核心16G）", 8, "16384M", "100G"),
//            DevCloudModel("system_pro", "高配版（32核心64G）", 32, "65535M", "500G"))

    fun getDevCloudModelList() = listOf(DevCloudModel("system_base", "8核16G（普通版）", 8, "16384M", "100G", listOf(
            "2.5GHz 64核 Intel Xeon Skylake 6133处理器",
            "32GB*12 DDR3 内存",
            "100GB 固态硬盘"),
            "预计交付周期：5分钟"
    ),
            DevCloudModel("system_pro", "32核64G（高配版）", 32, "65535M", "500G", listOf(
                    "2.5GHz 64核 Intel Xeon Skylake 6133处理器",
                    "32GB*12 DDR3 内存",
                    "500GB 固态硬盘"),
                    "预计交付周期：10分钟"
    ))
}