package com.tencent.devops.common.auth.api.pojo.external

enum class PipelineAuthAction(val actionName: String,
                              val alias: String) {

    DELETE("delete", "删除"),
    DOWNLOAD("download", "下载构件"),
    EDIT("edit", "编辑"),
    EXECUTE("execute", "执行"),
    LIST("list", "列表"),
    SHARE("share", "分享构件"),
    VIEW("view", "查看");
}