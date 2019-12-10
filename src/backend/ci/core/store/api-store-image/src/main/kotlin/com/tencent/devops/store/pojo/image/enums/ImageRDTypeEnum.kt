package com.tencent.devops.store.pojo.image.enums

enum class ImageRDTypeEnum(val type: Int) {
    SELF_DEVELOPED(0), // 自研
    THIRD_PARTY(1); // 第三方

    override fun toString() = type.toString()

    companion object {
        fun getImageRDTypeByName(name: String): ImageRDTypeEnum {
            values().forEach {
                if (it.name.toLowerCase() == name.toLowerCase()) {
                    return it
                }
            }
            // 默认第三方
            return THIRD_PARTY
        }

        fun getImageRDType(type: Int): ImageRDTypeEnum {
            return when (type) {
                0 -> SELF_DEVELOPED
                1 -> THIRD_PARTY
                else -> THIRD_PARTY
            }
        }

        fun getImageRDTypeStr(type: Int?): String {
            return when (type) {
                0 -> SELF_DEVELOPED.name
                1 -> THIRD_PARTY.name
                //默认第三方
                else -> THIRD_PARTY.name
            }
        }
    }
}