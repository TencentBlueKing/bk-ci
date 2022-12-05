/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.devops.common.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rpc调度工具
 *
 * @version V1.0
 * @date 2019/11/7
 */
public class RpcClient<T>
{
    private static Logger logger = LoggerFactory.getLogger(RpcClient.class);
    /**
     * 一般的请求超时,设置超时时间12秒钟,根据业务调整
     */
    private final int GENERAL_TIMEOUT = 12 * 1000;

    /**
     * RPC请求通用处理
     * @param serverURL
     * @param methodName
     * @param params
     * @return
     */
    public T doRequest(String serverURL, String methodName, Object[] params) {
        //@TODO
        T response = null;

        return response;
    }
}
