package com.tencent.devops.common.pipeline.type.docker

enum class ImageType(val type: String) {
    BKDEVOPS("bkdevops"), // 蓝盾镜像
    BKSTORE("bkstore"), // 蓝盾镜像市场镜像，存储于artifactory的项目{projectCode}路径下
    THIRD("third"); // 第三方镜像

    companion object {
        fun getType(type: String?): ImageType {
            if (type == null) {
                return BKDEVOPS
            }
            values().forEach {
                if (it.type.toLowerCase() == type.toLowerCase()) {
                    return it
                }
            }
            return BKDEVOPS
        }
    }
}