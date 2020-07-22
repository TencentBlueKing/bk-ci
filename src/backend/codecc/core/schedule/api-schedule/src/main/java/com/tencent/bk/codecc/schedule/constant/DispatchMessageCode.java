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

package com.tencent.bk.codecc.schedule.constant;

/**
 * 任务模块的业务结果码
 * 返回码制定规则：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表平台（如23代表CodeCC平台）
 * 3、第3位和第4位数字代表子服务模块（00：common-公共模块 01：task-任务模块 02：rule-规则模块 03:defect-告警模块 04:coverity-Coverity模块 05:schedule-调度模块）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如0001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 *
 * @version V1.0
 * @date 2019/4/23
 */
public interface DispatchMessageCode
{
    /**
     * 上传文件异常，请查询日志
     */
    String UPLOAD_FILE_ERR = "2305001";

    /**
     * 下载文件异常，请查询日志
     */
    String DOWNLOAD_FILE_ERR = "2305002";

    /**
     * "${fileName} 不存在"
     */
    String FILE_NOT_EXISTS = "2305003";

    /**
     * "${fileName} 不是一个文件"
     */
    String NOT_A_FILE = "2305004";

    /**
     * 获取最空闲的机器失败
     */
    String GET_MOST_IDLE_HOST_FAIL = "2305005";
}
