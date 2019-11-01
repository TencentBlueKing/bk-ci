package com.tencent.devops.store.pojo.image.enums

enum class LabelTypeEnum(val status: Int) {
    ATOM(0), // 插件
    TEMPLATE(1), // 模板
    IMAGE(2); // 镜像

    companion object {

        fun getLabelType(name: String): LabelTypeEnum? {
            LabelTypeEnum.values().forEach { enumObj ->
                if (enumObj.name == name) {
                    return enumObj
                }
            }
            return null
        }

        fun getLabelType(type: Int): String {
            return when (type) {
                0 -> ATOM.name
                1 -> TEMPLATE.name
                2 -> IMAGE.name
                else -> ATOM.name
            }
        }
    }
}
