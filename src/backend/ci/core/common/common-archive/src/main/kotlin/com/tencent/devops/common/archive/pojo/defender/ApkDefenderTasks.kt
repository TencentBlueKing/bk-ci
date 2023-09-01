package com.tencent.devops.common.archive.pojo.defender

data class ApkDefenderTasks(
    val tasks: List<Task>
) {
    data class Task(
        val id: String,
        val users: List<String>
    )
}

