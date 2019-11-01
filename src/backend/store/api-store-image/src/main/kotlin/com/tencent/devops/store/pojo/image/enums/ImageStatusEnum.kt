package com.tencent.devops.store.pojo.image.enums

enum class ImageStatusEnum(val status: Int) {
    INIT(0), // 初始化
    COMMITTING(1), // 提交中
    CHECKING(2), // 验证中
    CHECK_FAIL(3), // 验证失败
    TESTING(4), // 测试中
    AUDITING(5), // 审核中
    AUDIT_REJECT(6), // 审核驳回
    RELEASED(7), // 已发布
    GROUNDING_SUSPENSION(8), // 上架中止
    UNDERCARRIAGING(9), // 下架中
    UNDERCARRIAGED(10); // 已下架

    companion object {

        fun getImageStatus(name: String): ImageStatusEnum? {
            values().forEach { enumObj ->
                if (enumObj.name == name) {
                    return enumObj
                }
            }
            return null
        }

        fun getImageStatus(status: Int): String {
            return when (status) {
                0 -> INIT.name
                1 -> COMMITTING.name
                2 -> CHECKING.name
                3 -> CHECK_FAIL.name
                4 -> TESTING.name
                5 -> AUDITING.name
                6 -> AUDIT_REJECT.name
                7 -> RELEASED.name
                8 -> GROUNDING_SUSPENSION.name
                9 -> UNDERCARRIAGING.name
                10 -> UNDERCARRIAGED.name
                else -> INIT.name
            }
        }

        /**
         * 获取处于非终止态的所有状态
         */
        fun getInprocessStatusSet(): Set<Int> {
            return setOf(
                INIT.status,
                COMMITTING.status,
                CHECKING.status,
                TESTING.status,
                AUDITING.status,
                UNDERCARRIAGING.status
            )
        }

        /**
         * 获取处于终止态的所有状态
         */
        fun getFinishedStatusSet(): Set<Int> {
            return setOf(
                CHECK_FAIL.status,
                AUDIT_REJECT.status,
                RELEASED.status,
                GROUNDING_SUSPENSION.status,
                UNDERCARRIAGED.status
            )
        }
    }
}
