package com.tencent.bk.codecc.task.pojo

import com.tencent.devops.common.pojo.GongfengStatProjVO

data class GongfengStatPageModel(
    val pageNum: Int?,
    val bgId: String,
    val statsProjList: List<GongfengStatProjVO>
)