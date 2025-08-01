package com.tencent.devops.common.event.enums

enum class PipelineBuildStatusBroadCastEventType {
    BUILD_QUEUE,        /*构建排队，包含并发超限时排队、并发组排队。*/
    BUILD_START,        /*构建开始，不包含并发超限时排队、并发组排队。*/
    BUILD_END,          /*构建结束*/
    BUILD_QUALITY,      /*构建质量红线*/
    BUILD_STAGE_START,  /*stage开始*/
    BUILD_STAGE_PAUSE,  /*stage暂停、stage审核*/
    BUILD_STAGE_END,    /*stage结束*/
    BUILD_JOB_QUEUE,    /*job排队，包含互斥组排队、构建机复用互斥排队、最大job并发排队。*/
    BUILD_JOB_START,    /*job开始，不包含BUILD_JOB_QUEUE。如果job SKIP或没有可执行的插件，就不会有该事件。*/
    BUILD_JOB_END,      /*job结束，job SKIP或没有可执行的插件时会有该事件。*/
    BUILD_AGENT_START,  /*构建机启动，包含第三方构建机*/
    BUILD_AGENT_END,    /*构建机结束，包含第三方构建机*/
    BUILD_TASK_START,   /*插件开始*/
    BUILD_TASK_END,     /*插件结束*/
    BUILD_TASK_PAUSE;   /*插件前置暂停、人工审核插件审核*/
}
