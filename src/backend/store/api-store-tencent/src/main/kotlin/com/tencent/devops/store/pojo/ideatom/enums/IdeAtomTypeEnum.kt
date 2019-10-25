package com.tencent.devops.store.pojo.ideatom.enums

enum class IdeAtomTypeEnum(val type: Int) {
    SELF_DEVELOPED(0), // 自研
    THIRD_PARTY(1); // 第三方

    override fun toString() = type.toString()

    companion object {
        fun getAtomType(type: Int): String {
            return when (type) {
                0 -> SELF_DEVELOPED.name
                1 -> THIRD_PARTY.name
                else -> THIRD_PARTY.name
            }
        }

        fun getAtomTypeObj(type: Int): IdeAtomTypeEnum? {
            values().forEach { enumObj ->
                if (enumObj.type == type) {
                    return enumObj
                }
            }
            return null
        }
    }
}