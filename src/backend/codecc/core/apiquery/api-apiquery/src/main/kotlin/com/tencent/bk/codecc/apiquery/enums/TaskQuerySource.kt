package com.tencent.bk.codecc.apiquery.enums

enum class TaskQuerySource(
    val beanNamePrefix: String
) {

    /**
     * teg运管查询啄木鸟
     */
    TEG_SECURITY("TEGSecurity"),

    /**
     * pcg研发查询个人构建
     */
    PCG_DEVELOPMENT("PCGDev"),

    /**
     * teg技术图谱
     */
    TEG_TECHMAP("TegTechMap"),

    /**
     * cdg_fit线
     */
    CDG_FIT("CDGFit"),

    /**
     * teg开源协同
     */
    TEG_CODE_STYLE("TEGCodeStyle")
}