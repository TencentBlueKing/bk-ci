package com.tencent.devops.store.pojo.image.enums

/**
 * 镜像可运行的机器环境
 */
enum class ImageAgentTypeEnum(val type: Int) {
    DOCKER(0), // Docker on Devnet 物理机
    IDC(1), // Docker on IDC CVM
    PUBLIC_DEVCLOUD(2); // Docker on DevCloud

    companion object {

        fun getImageAgentType(name: String): ImageAgentTypeEnum? {
            values().forEach { enumObj ->
                if (enumObj.name.toLowerCase() == name.toLowerCase()) {
                    return enumObj
                }
            }
            return null
        }

        fun getImageAgentType(type: Int): String {
            return when (type) {
                0 -> DOCKER.name
                1 -> IDC.name
                2 -> PUBLIC_DEVCLOUD.name
                else -> DOCKER.name
            }
        }

        fun getAllAgentTypes(): MutableList<ImageAgentTypeEnum> {
            return mutableListOf(
                DOCKER,
                IDC,
                PUBLIC_DEVCLOUD
            )
        }
    }
}
