package com.tencent.devops.scm.pojo.p4

import com.perforce.p4java.admin.ITriggerEntry

data class TriggerInfo(
    /**
     * 触发器名称
     * */
    val name: String,
    /**
     * 触发器类型
     * */
    val type: ITriggerEntry.TriggerType,
    /**
     * 监听的仓库文件路径
     * //depot/...
     * */
    val path: String,
    /**
     * 触发器执行脚本，由两部分组成，一部分为脚本文件，一部分为执行参数
     * 如："%/bin/trigger.sh% %change%"
     * 脚步文件可以存放在服务器上，也可以存放在仓库中
     * 仓库文件：%//file-path%
     * 服务器文件：/file-path
     * */
    val command: String,
    val order: Int = 0
) {
    override fun toString(): String {
        return "$name $type $path $command"
    }
}
