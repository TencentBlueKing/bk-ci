package com.tencent.devops.remotedev.pojo.record

/**
 * 工作空间录屏密钥类型
 */
enum class WorkspaceRecordTicketType {
    /**
     * 录屏类型
     */
    RECORD,
    
    /**
     * 缩略图类型
     */
    THUMBNAIL;

    companion object {
        fun parse(type: String?): WorkspaceRecordTicketType {
            if (type.isNullOrBlank()) return RECORD
            return try {
                valueOf(type)
            } catch (ignore: Throwable) {
                RECORD
            }
        }
    }
}
