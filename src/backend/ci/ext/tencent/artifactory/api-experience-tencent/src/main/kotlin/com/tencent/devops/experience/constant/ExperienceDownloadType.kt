package com.tencent.devops.experience.constant

import com.tencent.devops.experience.constant.ExperienceDownloadType.values


enum class ExperienceDownloadType(
    private val id: Int
) {
    UNKNOWN(0),

    SERVER(1),

    P2P(2),

    ;

    companion object {
        fun of(id: Int): ExperienceDownloadType {
            for (v in values()) {
                if (id == v.id) {
                    return v
                }
            }

            return UNKNOWN
        }
    }
}