package com.tencent.devops.store.pojo.image.enums

enum class CategoryTypeEnum(val status: Int) {
    ATOM(0), // 插件
    TEMPLATE(1), // 模板
    IMAGE(2); // 镜像

    companion object {

        fun getCategoryType(name: String): CategoryTypeEnum? {
            CategoryTypeEnum.values().forEach { enumObj ->
                if (enumObj.name == name) {
                    return enumObj
                }
            }
            return null
        }

        fun getCategoryType(type: Int): String {
            return when (type) {
                0 -> ATOM.name
                1 -> TEMPLATE.name
                2 -> IMAGE.name
                else -> ATOM.name
            }
        }
    }
}
