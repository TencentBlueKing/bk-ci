/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.web.aop

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_TASK_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.ComConstants.*
import com.tencent.devops.common.web.aop.annotation.OperationHistory
import com.tencent.devops.common.web.aop.model.OperationHistoryDTO
import com.tencent.devops.common.web.mq.EXCHANGE_OPERATION_HISTORY
import com.tencent.devops.common.web.mq.ROUTE_OPERATION_HISTORY
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * 上报分析记录aop方法
 *
 * @date 2019/6/14
 * @version V1.0
 */
@Aspect
class OperationHistoryAop @Autowired constructor(
        val rabbitTemplate: RabbitTemplate
) {

    @Pointcut("@annotation(com.tencent.devops.common.web.aop.annotation.OperationHistory)")
    fun operationHistory() {
    }

    @AfterReturning("operationHistory()&&@annotation(annotation)")
    fun reportOperationHistory(joinPoint: JoinPoint,
                               annotation: OperationHistory) {
        //获取功能id
        val funcId = annotation.funcId
        //获取操作类型
        val operType = getOperType(joinPoint, funcId, annotation.operType)
        //获取特定工具
        var toolName: String? = null
        if (funcId == FUNC_DEFECT_MANAGE || funcId == FUNC_CHECKER_CONFIG) {
            val objects = joinPoint.args
            when (operType) {
                AUTHOR_TRANSFER -> {
                    val jsonObject = JSONObject.fromObject(objects[0])
                }
                OPEN_CHECKER, CLOSE_CHECKER -> {
                    val jsonObject = JSONObject.fromObject(objects[2])
                    toolName = jsonObject.getString("toolName")
                }
            }
        }

        //获取任务id
        val request = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
        val taskId = request.getHeader(AUTH_HEADER_DEVOPS_TASK_ID)?.toLong() ?: 0L
        // 获取流水线id
        val pipelineId = request.getParameter("pipelineId") ?: ""
        //获取操作用户
        val userName = request.getHeader(AUTH_HEADER_DEVOPS_USER_ID) ?: request.getParameter("userName")
        //获取操作消息
        val paramArray = getParamArray(joinPoint, funcId, userName)
        //获取当前时间
        val currentTime = System.currentTimeMillis()

        val operationHistoryDTO = OperationHistoryDTO(
                taskId = taskId,
                pipelineId = pipelineId,
                funcId = funcId,
                operType = operType,
                operTypeName = null,
                time = currentTime,
                paramArray = paramArray,
                operMsg = null,
                toolName = toolName,
                operator = userName
        )
        //发送消息，异步处理
        rabbitTemplate.convertAndSend(EXCHANGE_OPERATION_HISTORY,
                ROUTE_OPERATION_HISTORY, operationHistoryDTO)
    }


    /**
     * 获取操作类型
     */
    private fun getOperType(joinPoint: JoinPoint, funcId: String,
                            operType: String): String {
        val objects = joinPoint.args
        return with(funcId)
        {
            when (this) {
                //停用启用任务
                FUNC_TOOL_SWITCH -> {
                    val manageType = objects[1] as String
                    if (ComConstants.CommonJudge.COMMON_Y.value() == manageType) {
                        ENABLE_ACTION
                    } else {
                        DISABLE_ACTION
                    }
                }
                // 打开关闭规则配置
                FUNC_CHECKER_CONFIG -> {
                    val checker = JSONObject.fromObject(objects[2])
                    if ((checker.get("openedCheckers") as JSONArray).size > 0) {
                        OPEN_CHECKER
                    } else {
                        CLOSE_CHECKER
                    }
                }
                else -> {
                    operType
                }
            }
        }
    }


    /**
     * 获取操作记录消息
     */
    private fun getParamArray(joinPoint: JoinPoint, funcId: String,
                              user: String): Array<String> {

        val objects = joinPoint.args
        return with(funcId)
        {
            when (this) {
                //注册工具功能
                FUNC_REGISTER_TOOL -> {
                    val jsonObject = JSONObject.fromObject(objects[0])
                    val toolArray = jsonObject.getJSONArray("tools")
                    val toolName = toolArray.map { tool ->
                        (tool as JSONObject).getString("toolName")
                    }.reduce { acc, s -> "$acc,$s" }
                    arrayOf(user, toolName)
                }
                //修改任务信息功能
                FUNC_TASK_INFO -> {
                    arrayOf(user)
                }
                //停用启用任务
                FUNC_TASK_SWITCH -> {
                    arrayOf(user)
                }
                //停用启用工具
                FUNC_TOOL_SWITCH -> {
                    arrayOf(user)
                }
                //触发立即分析
                FUNC_TRIGGER_ANALYSIS -> {
                    arrayOf(user)
                }
                //定时扫描分析
                FUNC_SCAN_SCHEDULE -> {
                    arrayOf(user)
                }
                //操作屏蔽路径
                FUNC_FILTER_PATH -> {
                    arrayOf(user)
                }
                //作者批量转换
                FUNC_DEFECT_MANAGE -> {
                    val jsonObject = JSONObject.fromObject(objects[1])
                    val toolName = jsonObject.getString("toolName")
                    val sourceAuthor = jsonObject.getJSONArray("sourceAuthor")
                    val targetAuthor = jsonObject.getJSONArray("targetAuthor")
                    arrayOf(user, toolName, sourceAuthor.join(","), targetAuthor.join(","))
                }
                //任务代码库更新
                FUNC_CODE_REPOSITORY -> {
                    arrayOf(user)
                }
                else -> {
                    arrayOf(user)
                }
            }
        }
    }


}