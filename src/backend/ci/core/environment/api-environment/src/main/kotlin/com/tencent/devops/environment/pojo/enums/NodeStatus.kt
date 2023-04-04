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

package com.tencent.devops.environment.pojo.enums

@Suppress("UNUSED")
enum class NodeStatus(val statusName: String) {
    NORMAL("正常"),
    ABNORMAL("异常"),
    DELETED("已删除"),
    LOST("失联"),
    CREATING("正在创建中"),
    RUNNING("安装Agent"),
    STARTING("正在开机中"),
    STOPPING("正在关机中"),
    STOPPED("已关机"),
    RESTARTING("正在重启中"),
    DELETING("正在销毁中"),
    BUILDING_IMAGE("正在制作镜像中"),
    BUILD_IMAGE_SUCCESS("制作镜像成功"),
    BUILD_IMAGE_FAILED("制作镜像失败"),
    UNKNOWN("未知");

    companion object {
        fun getStatusName(status: String): String {
            values().forEach {
                if (it.name == status) {
                    return it.statusName
                }
            }
            return UNKNOWN.statusName
//            return when (status) {
//                NORMAL.name -> NORMAL.statusName
//                ABNORMAL.name -> ABNORMAL.statusName
//                DELETED.name -> DELETED.statusName
//                LOST.name -> LOST.statusName
//                CREATING.name -> CREATING.statusName
//                RUNNING.name -> RUNNING.statusName
//                STARTING.name -> STARTING.statusName
//                STOPPING.name -> STOPPING.statusName
//                STOPPED.name -> STOPPED.statusName
//                RESTARTING.name -> RESTARTING.statusName
//                DELETING.name -> DELETING.statusName
//                BUILDING_IMAGE.name -> BUILDING_IMAGE.statusName
//                BUILD_IMAGE_SUCCESS.name -> BUILD_IMAGE_SUCCESS.statusName
//                BUILD_IMAGE_FAILED.name -> BUILD_IMAGE_FAILED.statusName
//                else -> UNKNOWN.statusName
//            }
        }

        fun parseByName(name: String): NodeStatus {
            values().forEach {
                if (it.name == name) {
                    return it
                }
            }
            return UNKNOWN
//            return when (name) {
//                NORMAL.name -> NORMAL
//                ABNORMAL.name -> ABNORMAL
//                DELETED.name -> DELETED
//                LOST.name -> LOST
//                CREATING.name -> CREATING
//                RUNNING.name -> RUNNING
//                STARTING.name -> STARTING
//                STOPPING.name -> STOPPING
//                STOPPED.name -> STOPPED
//                RESTARTING.name -> RESTARTING
//                DELETING.name -> DELETING
//                BUILDING_IMAGE.name -> BUILDING_IMAGE
//                BUILD_IMAGE_SUCCESS.name -> BUILD_IMAGE_SUCCESS
//                BUILD_IMAGE_FAILED.name -> BUILD_IMAGE_FAILED
//                else -> UNKNOWN
//            }
        }

        fun parseByStatusName(statusName: String): NodeStatus {
            values().forEach {
                if (it.statusName == statusName) {
                    return it
                }
            }
            return UNKNOWN
//            return when (statusName) {
//                NORMAL.statusName -> NORMAL
//                ABNORMAL.statusName -> ABNORMAL
//                DELETED.statusName -> DELETED
//                LOST.statusName -> LOST
//                CREATING.statusName -> CREATING
//                RUNNING.statusName -> RUNNING
//                STARTING.statusName -> STARTING
//                STOPPING.statusName -> STOPPING
//                STOPPED.statusName -> STOPPED
//                RESTARTING.statusName -> RESTARTING
//                DELETING.statusName -> DELETING
//                BUILDING_IMAGE.statusName -> BUILDING_IMAGE
//                BUILD_IMAGE_SUCCESS.statusName -> BUILD_IMAGE_SUCCESS
//                BUILD_IMAGE_FAILED.statusName -> BUILD_IMAGE_FAILED
//                else -> UNKNOWN
//            }
        }

       /* fun i18n(status: String): String {
            return when (status) {
                NORMAL.name -> MessageCodeUtil.getCodeLanMessage(BK_NORMAL)
                ABNORMAL.name -> MessageCodeUtil.getCodeLanMessage(BK_ABNORMAL)
                DELETED.name -> MessageCodeUtil.getCodeLanMessage(BK_DELETED)
                LOST.name -> MessageCodeUtil.getCodeLanMessage(BK_LOST)
                CREATING.name -> MessageCodeUtil.getCodeLanMessage(BK_CREATING)
                RUNNING.name -> MessageCodeUtil.getCodeLanMessage(BK_RUNNING)
                STARTING.name -> MessageCodeUtil.getCodeLanMessage(BK_STARTING)
                STOPPING.name -> MessageCodeUtil.getCodeLanMessage(BK_STOPPING)
                STOPPED.name -> MessageCodeUtil.getCodeLanMessage(BK_STOPPED)
                RESTARTING.name -> MessageCodeUtil.getCodeLanMessage(BK_RESTARTING)
                DELETING.name -> MessageCodeUtil.getCodeLanMessage(BK_DELETING)
                BUILDING_IMAGE.name -> MessageCodeUtil.getCodeLanMessage(BK_BUILDING_IMAGE)
                BUILD_IMAGE_SUCCESS.name -> MessageCodeUtil.getCodeLanMessage(BK_BUILD_IMAGE_SUCCESS)
                BUILD_IMAGE_FAILED.name -> MessageCodeUtil.getCodeLanMessage(BK_BUILD_IMAGE_FAILED)
                else -> MessageCodeUtil.getCodeLanMessage(BK_UNKNOWN)
            }
        }*/
    }
}
