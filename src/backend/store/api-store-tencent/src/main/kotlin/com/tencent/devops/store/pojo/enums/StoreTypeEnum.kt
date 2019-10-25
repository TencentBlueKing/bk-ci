package com.tencent.devops.store.pojo.enums

enum class StoreTypeEnum(val type: Int) {
    ATOM(0), // 插件
    TEMPLATE(1), // 模板
    IDE_ATOM(3); // IDE插件

    companion object {
        fun getStoreType(type: Int): String {
            return when (type) {
                0 -> StoreTypeEnum.ATOM.name
                1 -> StoreTypeEnum.TEMPLATE.name
                3 -> StoreTypeEnum.IDE_ATOM.name
                else -> StoreTypeEnum.ATOM.name
            }
        }

        fun getStoreTypeObj(type: Int): StoreTypeEnum? {
            return when (type) {
                0 -> ATOM
                1 -> TEMPLATE
                3 -> IDE_ATOM
                else -> null
            }
        }
    }
}