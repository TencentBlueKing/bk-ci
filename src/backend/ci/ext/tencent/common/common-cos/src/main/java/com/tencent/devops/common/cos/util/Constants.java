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

package com.tencent.devops.common.cos.util;

/**
 * Created by schellingma on 2017/04/21.
 * Powered By Tencent
 */
public class Constants {
    public static final int MAX_RETRY_UPLOAD_COUNT = 3;
    public static final String EXCHANGE_FILE = "exchange_file";
    public static final String QUEUE_FILE = "queue_file";
    public static final String ROUTE_FILE = "file";//    /**


    public static final String DEFAULT_CONTENT_TYPE = "text/plain";

//     * 第1次重试上传的时间延迟，秒
//     */
//    public static final int SECONDS_RETRY_DELAY_1ST = 10 * 60;
//    /**
//     * 第2次重试上传的时间延迟，秒
//     */
//    public static final int SECONDS_RETRY_DELAY_2ND = 1 * 60 * 60;
//    /**
//     * 第3次重试上传的时间延迟，秒
//     */
//    public static final int SECONDS_RETRY_DELAY_3RD = 12 * 60 * 60;


    /**
     * 第1次重试上传的时间延迟，秒
     */
    public static final int SECONDS_RETRY_DELAY_1ST = 10;
    /**
     * 第2次重试上传的时间延迟，秒
     */
    public static final int SECONDS_RETRY_DELAY_2ND = 1 * 60;
    /**
     * 第3次重试上传的时间延迟，秒
     */
    public static final int SECONDS_RETRY_DELAY_3RD = 1 * 60;


    public static final int RETRY_UPLOAD_TRUNK_SIZE = 4 * 1024 * 1024;
}
