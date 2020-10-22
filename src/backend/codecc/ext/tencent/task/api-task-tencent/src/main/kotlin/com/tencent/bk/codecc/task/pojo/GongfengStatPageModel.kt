package com.tencent.bk.codecc.task.pojo

data class GongfengStatPageModel(
    val pageNum: Int?,
    val bgId : String,
    val statsProjList : List<GongfengStatProjVO>
)