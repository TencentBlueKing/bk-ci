package com.tencent.devops.common.pipeline.pojo.coverity

/**
 * deng
 * 26/01/2018
 */
enum class ProjectLanguage(val value: String) {
    C("c"),
    C_PLUS_PLUSH("c++"),
    C_CPP("cpp"),
    OBJECTIVE_C("objective-C"),
    OC("objective-C"),
    C_SHARP("c#"),
    JAVA("java"),
    PYTHON("python"),
    JAVASCRIPT("javascript"),
    JS("javascript"),
    PHP("php"),
    RUBY("ruby"),
    LUA("lua"),
    GOLANG("golang"),
    SWIFT("swift"),
    TYPESCRIPT("typescript"),
    KOTLIN("kotlin"),
    OTHERS("others");

    companion object {
        fun fromValue(value: String) =
                ProjectLanguage.values().associateBy(ProjectLanguage::value)[value]
                        ?: throw RuntimeException("The project language($value) is not exist")
    }
}