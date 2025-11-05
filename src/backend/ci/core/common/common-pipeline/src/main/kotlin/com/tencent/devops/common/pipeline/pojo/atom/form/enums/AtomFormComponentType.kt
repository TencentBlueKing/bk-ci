package com.tencent.devops.common.pipeline.pojo.atom.form.enums

import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

enum class AtomFormComponentType(
    @JsonValue
    val value: String
) {
    @Schema(title = "单行文本框")
    VUE_INPUT(value = "vuex-input"),
    @Schema(title = "多行文本框")
    VUE_TEXTAREA(value = "vuex-textarea"),
    @Schema(title = "代码编辑框")
    ATOM_ACE_EDITOR(value = "atom-ace-editor"),
    @Schema(title = "下拉框", description = "只能选择，不能输入")
    SELECTOR(value = "selector"),
    @Schema(title = "可输入下拉框", description = "输入的值可以是下列表中没有的值（包括全量），选中后流程里得到的id")
    SELECT_INPUT(value = "select-input"),
    @Schema(title = "可输入下拉框", description = "输入的值只能是选项，选中后流程里得到的name")
    DEVOPS_SELECT(value = "devops-select"),
    @Schema(title = "复选框列表")
    ATOM_CHECKBOX_LIST(value = "atom-checkbox-list"),
    @Schema(title = "复选框（布尔）")
    ATOM_CHECKBOX(value = "atom-checkbox"),
    @Schema(title = "单选")
    ENUM_INPUT(value = "enum-input"),
    @Schema(title = "时间选择器")
    CRON_TIMER(value = "cron-timer"),
    @Schema(title = "日期选择器")
    TIME_PICKER(value = "time-picker"),
    @Schema(title = "人名选择器", description = "只能添加当前项目成员")
    STAFF_INPUT(value = "staff-input"),
    @Schema(title = "人名选择器", description = "可以添加公司内任意成员")
    COMPANY_STAFF_INPUT(value = "company-staff-input"),
    @Schema(title = "提示信息", description = "支持动态获取用户输入的参数，支持超链接")
    TIPS(value = "tips"),
    @Schema(title = "不定参数列表", description = "参数列表支持从接口获取")
    PARAMETER(value = "parameter"),
    @Schema(title = "不定参数列表", description = "支持从接口获取，支持多行多列，支持动态增删")
    DYNAMIC_PARAMETER(value = "dynamic-parameter"),
    @Schema(title = "动态参数(简易版)", description = "支持多行多列，支持动态增删")
    DYNAMIC_PARAMETER_SIMPLE(value = "dynamic-parameter-simple"),
    @Schema(title = "分组组件", description = "分组组件")
    GROUP(value = "group"),
    @Schema(title = "分组组件子集", description = "分组组件的下级组件")
    GROUP_ITEM(value = "groupItem"),
    @Schema(title = "分组输入框", description = "分组组件的下级组件")
    COMPOSITE_INPUT(value = "composite-input");
}