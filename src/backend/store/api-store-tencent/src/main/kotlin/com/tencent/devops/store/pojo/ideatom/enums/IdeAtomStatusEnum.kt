package com.tencent.devops.store.pojo.ideatom.enums

enum class IdeAtomStatusEnum(val status: Int) {
    INIT(0), // 初始化
    AUDITING(1), // 审核中
    AUDIT_REJECT(2), // 审核驳回
    RELEASED(3), // 已发布
    GROUNDING_SUSPENSION(4), // 上架中止
    UNDERCARRIAGED(5); // 已下架

    companion object {

        fun getIdeAtomStatus(name: String): IdeAtomStatusEnum? {
            values().forEach { enumObj ->
                if (enumObj.name == name) {

                    return enumObj
                }
            }
            return null
        }

        fun getIdeAtomStatusObj(status: Int): IdeAtomStatusEnum? {
            values().forEach { enumObj ->
                if (enumObj.status == status) {
                    return enumObj
                }
            }
            return null
        }

        fun getIdeAtomStatus(status: Int): String {
            return when (status) {
                0 -> INIT.name
                1 -> AUDITING.name
                2 -> AUDIT_REJECT.name
                3 -> RELEASED.name
                4 -> GROUNDING_SUSPENSION.name
                5 -> UNDERCARRIAGED.name
                else -> INIT.name
            }
        }
    }
}
