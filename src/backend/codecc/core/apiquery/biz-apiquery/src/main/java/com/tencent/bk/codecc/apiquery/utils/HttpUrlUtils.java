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

package com.tencent.bk.codecc.apiquery.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 获取httpURL工具类
 *
 * @version V1.0
 * @date 2020/12/10
 */
@Component
public class HttpUrlUtils {

    @Value("${bkci.public.url:#{null}}")
    private static String codeccServerApi;

    public static String getCodeccServerApi() {
        return codeccServerApi;
    }

    // 注意set方法没有static修饰
    public void setCodeccServerApi(String codeccServerApi) {
        HttpUrlUtils.codeccServerApi = codeccServerApi;
    }
}
