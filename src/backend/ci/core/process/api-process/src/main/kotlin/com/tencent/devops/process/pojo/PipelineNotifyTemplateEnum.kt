/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.pojo

/**
 * 流水线通知模板编码
 */
enum class PipelineNotifyTemplateEnum(val templateCode: String) {
    /**
     * 流水线设置-启动的通知模板代码
     */
    PIPELINE_STARTUP_NOTIFY_TEMPLATE("PIPELINE_STARTUP_NOTIFY_TEMPLATE"),

    /**
     * 流水线设置-启动的详情通知模板代码
     */
    PIPELINE_STARTUP_NOTIFY_TEMPLATE_DETAIL("PIPELINE_STARTUP_NOTIFY_TEMPLATE_DETAIL"),

    /**
     * 流水线设置-执行成功的通知模板代码
     */
    PIPELINE_SHUTDOWN_SUCCESS_NOTIFY_TEMPLATE("PIPELINE_SHUTDOWN_SUCCESS_NOTIFY_TEMPLATE"),

    /**
     * 流水线设置-执行成功的详情通知模板代码
     */
    PIPELINE_SHUTDOWN_SUCCESS_NOTIFY_TEMPLATE_DETAIL("PIPELINE_SHUTDOWN_SUCCESS_NOTIFY_TEMPLATE_DETAIL"),

    /**
     * 流水线设置-执行失败的通知模板代码
     */
    PIPELINE_SHUTDOWN_FAILURE_NOTIFY_TEMPLATE("PIPELINE_SHUTDOWN_FAILURE_NOTIFY_TEMPLATE"),

    /**
     * 流水线设置-执行失败的详情通知模板代码
     */
    PIPELINE_SHUTDOWN_FAILURE_NOTIFY_TEMPLATE_DETAIL("PIPELINE_SHUTDOWN_FAILURE_NOTIFY_TEMPLATE_DETAIL"),

    /**
     * 流水线设置-执行取消的通知模板代码
     */
    PIPELINE_SHUTDOWN_CANCEL_NOTIFY_TEMPLATE("PIPELINE_SHUTDOWN_CANCEL_NOTIFY_TEMPLATE"),

    /**
     * 流水线设置-执行取消的详情通知模板代码
     */
    PIPELINE_SHUTDOWN_CANCEL_NOTIFY_TEMPLATE_DETAIL("PIPELINE_SHUTDOWN_CANCEL_NOTIFY_TEMPLATE_DETAIL"),

    /**
     * 流水线设置-触发审核的通知触发人模板代码
     */
    PIPELINE_TRIGGER_REVIEW_NOTIFY_TEMPLATE("PIPELINE_TRIGGER_REVIEW_NOTIFY_TEMPLATE"),

    /**
     * 流水线设置-人工审核插件的通知模板代码
     */
    PIPELINE_MANUAL_REVIEW_ATOM_NOTIFY_TEMPLATE("MANUAL_REVIEW_ATOM_NOTIFY_TEMPLATE"),

    /**
     * 流水线设置-stage阶段审核的通知触发人模板代码
     */
    PIPELINE_MANUAL_REVIEW_STAGE_NOTIFY_TO_TRIGGER_TEMPLATE("MANUAL_REVIEW_STAGE_NOTIFY_TO_TRIGGER_TEMPLATE"),

    /**
     * 流水线设置-stage阶段审核驳回时通知触发人模板代码
     */
    PIPELINE_MANUAL_REVIEW_STAGE_REJECT_TO_TRIGGER_TEMPLATE("MANUAL_REVIEW_STAGE_REJECT_TO_TRIGGER_TEMPLATE"),

    /**
     * 流水线设置-stage阶段审核的通知模板代码
     */
    PIPELINE_MANUAL_REVIEW_STAGE_NOTIFY_TEMPLATE("MANUAL_REVIEW_STAGE_NOTIFY_TEMPLATE"),

    /**
     * 使用模板批量更新流水线的通知模板代码
     */
    PIPELINE_UPDATE_TEMPLATE_INSTANCE_NOTIFY_TEMPLATE("UPDATE_TEMPLATE_INSTANCE_NOTIFY_TEMPLATE"),

    /**
     * 流水线webhook注册失败的通知模板代码
     */
    PIPELINE_WEBHOOK_REGISTER_FAILURE_NOTIFY_TEMPLATE("PIPELINE_WEBHOOK_REGISTER_FAILURE_NOTIFY_TEMPLATE"),

    /**
     * 流水线CallBack接口熔断通知模板代码
     *
     */
    PIPELINE_CALLBACK_DISABLE_NOTIFY_TEMPLATE("PIPELINE_CALLBACK_DISABLE_NOTIFY_TEMPLATE"),

    /**
     * 未知模板代码
     */
    UNKNOWN("NULL");

    companion object {

        fun parse(name: String?): PipelineNotifyTemplateEnum {
            return try {
                if (name == null) UNKNOWN else valueOf(name)
            } catch (ignored: Exception) {
                UNKNOWN
            }
        }
    }
}
