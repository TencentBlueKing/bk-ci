package com.tencent.devops.image.pojo

data class GetImageInfoReq(
    var repoList: List<String>,
    var includeTags: Boolean?
)