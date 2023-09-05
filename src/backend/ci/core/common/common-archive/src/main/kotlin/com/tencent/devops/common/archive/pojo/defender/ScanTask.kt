package com.tencent.devops.common.archive.pojo.defender

data class ScanTask(
    val name: String?,
    val taskId: String?,
    val projectId: String?,
    val createdBy: String?,
    val lastModifiedDateTime: String?,
    val triggerDateTime: String?,
    val startDateTime: String?,
    val finishedDateTime: String?,
    val triggerType: String?,
    val status: String?,
    val scanPlan: String?,
    val rule: Rule?,
    val total: Int?,
    val scanning: Int?,
    val failed: Int?,
    val scanned: Int?,
    val passed: Int?,
    val scanner: String?,
    val scannerType: String?,
    val scannerVersion: String?,
    val force: Boolean?,
    val metadata: ArrayList<Metadata>?
) {
    data class Rules(
        val field: String?,
        val value: String?,
        val operation: String?
    )

    data class Rule(
        val rules: ArrayList<Rules>?,
        val relation: String?
    )

    data class Metadata(
        val key: String?,
        val value: String?
    )
}
