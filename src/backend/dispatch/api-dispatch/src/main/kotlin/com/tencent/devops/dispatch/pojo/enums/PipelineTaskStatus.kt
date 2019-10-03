package com.tencent.devops.dispatch.pojo.enums

import javax.ws.rs.NotFoundException

enum class PipelineTaskStatus(val status: Int) {
    QUEUE(1),
    RUNNING(2),
    DONE(3),
    FAILURE(4);

    companion object {
        fun toStatus(status: Int): PipelineTaskStatus {
            PipelineTaskStatus.values().forEach {
                if (it.status == status)
                    return it
            }
            throw NotFoundException("Can't find the pipeline task status($status)")
        }
    }
}