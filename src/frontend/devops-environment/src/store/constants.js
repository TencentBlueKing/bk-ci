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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

export const nodeTypeMap = {
    'CC': 'CC',
    'CMDB': 'CMDB',
    'BCSVM': 'BCS虚拟机',
    'DEVCLOUD': '腾讯自研云（云devnet资源）',
    'THIRDPARTY': '第三方构建机',
    'TSTACK': 'TStack虚拟机',
    'OTHER': '其他',
    'UNKNOWN': '未知'
}

export const nodeStatusMap = {
    'NORMAL': '正常',
    'ABNORMAL': '异常',
    'DELETED': '已删除',
    'LOST': '失联',
    'CREATING': '正在创建中',
    'RUNNING': '安装Agent',
    'STARTING': '正在开机中',
    'STOPPING': '正在关机中',
    'STOPPED': '已关机',
    'RESTARTING': '正在重启中',
    'DELETING': '正在销毁中',
    'BUILDING_IMAGE': '正在制作镜像中',
    'BUILD_IMAGE_SUCCESS': '制作镜像成功',
    'BUILD_IMAGE_FAILED': '制作镜像失败',
    'UNKNOWN': '未知'
}
