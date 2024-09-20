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

package com.tencent.devops.environment.constant

object Constants {

    /**
     * 请求ESB-CMDB接口的请求参数
     */
    const val COLUMN_SVR_BAK_OPERATOR = "SvrBakOperator"
    const val COLUMN_SVR_OPERATOR = "SvrOperator"
    const val COLUMN_SVR_IP = "SvrIp"
    const val COLUMN_SVR_NAME = "SvrName"
    const val COLUMN_SFW_NAME = "SfwName"
    const val COLUMN_SEVER_LAN_IP = "serverLanIP"
    const val COLUMN_DEPT_ID = "DeptId"
    const val COLUMN_SERVER_ID = "serverId"

    /**
     * 请求CC接口的请求参数
     */
    const val FIELD_BK_HOST_ID = "bk_host_id"
    const val FIELD_BK_CLOUD_ID = "bk_cloud_id"
    const val FIELD_BK_HOST_INNERIP = "bk_host_innerip"
    const val FIELD_OPERATOR = "operator"
    const val FIELD_BAK_OPERATOR = "bk_bak_operator"
    const val FIELD_BK_OS_TYPE = "bk_os_type"
    const val FIELD_BK_SVR_ID = "svr_id"

    /**
     * CC接口返回值中 操作系统类型对应CODE
     */
    const val OS_TYPE_CC_CODE_LINUX = "1"
    const val OS_TYPE_CC_CODE_WINDOWS = "2"
    const val OS_TYPE_CC_CODE_AIX = "3"
    const val OS_TYPE_CC_CODE_UNIX = "4"
    const val OS_TYPE_CC_CODE_SOLARIS = "5"
    const val OS_TYPE_CC_CODE_FREEBSD = "7"
}
