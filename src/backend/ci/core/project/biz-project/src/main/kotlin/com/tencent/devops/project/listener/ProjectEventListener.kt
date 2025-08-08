/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.project.listener

import com.tencent.devops.common.event.listener.EventListener
import com.tencent.devops.project.pojo.mq.ProjectBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectCreateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateLogoBroadCastEvent

/**
 * 项目事件监听器
 */
interface ProjectEventListener : EventListener<ProjectBroadCastEvent> {

    /**
     * 默认实现了Listener的消息处理方法做转换处理
     * @param event ProjectBroadCastEvent抽象类的处理，如有扩展请到子类操作
     */
    override fun execute(event: ProjectBroadCastEvent) {
        when (event) {
            is ProjectUpdateBroadCastEvent -> {
                onReceiveProjectUpdate(event)
            }
            is ProjectUpdateLogoBroadCastEvent -> {
                onReceiveProjectUpdateLogo(event)
            }
        }
    }

    /**
     *  处理创建项目事件
     *  @param event ProjectCreateBroadCastEvent
     */
    fun onReceiveProjectCreate(event: ProjectCreateBroadCastEvent)

    /**
     *  处理更新项目事件
     *  @param event ProjectUpdateBroadCastEvent
     */
    fun onReceiveProjectUpdate(event: ProjectUpdateBroadCastEvent)

    /**
     *  处理更新Logo项目事件
     *  @param event ProjectUpdateLogoBroadCastEvent
     */
    fun onReceiveProjectUpdateLogo(event: ProjectUpdateLogoBroadCastEvent)
}
