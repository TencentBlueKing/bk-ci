package com.tencent.devops.common.gcloud.api.pojo

enum class ActionParam {
    UploadApp,
    UploadRes,
    GetUploadTask,
    NewApp,
    NewRes,
    PrePublish,
    UpdateVersion,
    DeleteVersion,
    DeleteRes,
    UploadDynamicRes,
    UpdateRes,
    QueryVersion,

    // new version
    NewUploadTask,
    UploadUpdateFile,
    GetUploadTaskStat,
    ;
}