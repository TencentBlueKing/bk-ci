package com.tencent.devops.experience.constant

enum class ExperienceAppStatusEnum(
    val id: Int
) {
    UNKNOWN(0),

    UPGRADE(1),

    OPEN(2),

    DOWNLOAD(3),

    INSTALL(4),

    EXPIRE(5),

    ;
}
