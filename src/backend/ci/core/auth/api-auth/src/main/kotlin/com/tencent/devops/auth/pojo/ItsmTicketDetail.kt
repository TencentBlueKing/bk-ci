package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "Itsm工单详情")
data class ItsmTicketDetail(
    @get:Schema(title = "id")
    val id: String,
    @get:Schema(title = "单号")
    val sn: String,
    @get:Schema(title = "标题")
    val title: String,
    @get:Schema(title = "提单时间")
    val created_at: String,
    @get:Schema(title = "更新时间")
    val updated_at: Boolean,
    @get:Schema(title = "结束时间")
    val end_at: String,
    @get:Schema(title = "状态标识，默认情况下有draft草稿、new新、running处理中、suspend挂起、finished已完成、termination终止、revoked已撤销")
    val status: String,
    @get:Schema(title = "状态展示名")
    val status_display: String,
    @get:Schema(title = "流程id")
    val workflow_id: String,
    @get:Schema(title = "服务id")
    val service_id: String,
    @get:Schema(title = "门户id")
    val portal_id: String,
    @get:Schema(title = "当前处理人列表")
    val current_processors: List<CurrentProcessor>,
    @get:Schema(title = "当前步骤列表")
    val current_steps: List<CurrentStep>,
    @get:Schema(title = "工单前端访问地址")
    val frontend_url: String,
    @get:Schema(title = "工单表单实例化数据")
    val form_data: Map<String, Any>,
    @get:Schema(title = "审批结果")
    val approve_result: Boolean,
    @get:Schema(title = "回调结果")
    val callback_result: CallbackResult
) {
    @Schema(title = "当前处理人")
    data class CurrentProcessor(
        @get:Schema(title = "类型处理人标识列表字符串")
        val processor: String,
        @get:Schema(title = "处理人类型，user 用户、group 用户组、organization 组织")
        val processor_type: String
    )

    @Schema(title = "当前步骤")
    data class CurrentStep(
        @get:Schema(title = "步骤名称")
        val name: String
    )

    @Schema(title = "回调结果")
    data class CallbackResult(
        @get:Schema(title = "回调接口最外层的result信息")
        val result: Boolean,
        @get:Schema(title = "回调报错信息或者回调接口最外层的message信息")
        val message: String
    )
}