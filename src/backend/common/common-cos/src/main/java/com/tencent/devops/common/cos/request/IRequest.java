/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.common.cos.request;

import com.tencent.devops.common.cos.model.enums.HttpMethodEnum;
import com.tencent.devops.common.cos.model.enums.SignTypeEnum;
import okhttp3.RequestBody;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

public interface IRequest {
    /**
     * 返回请求的方法
     * @return 默认方法：GET，无数据提交
     */
    default Pair<HttpMethodEnum, RequestBody> getMethod() {
        return Pair.of(HttpMethodEnum.GET, null);
    }

    /**
     * 返回请求的查询参数
     * @return 默认方法：空
     */
    default Map<String, String> getQueryParams() {
        return new HashMap<>();
    }

    /**
     * 返回请求的头参数
     * @return 默认方法：空
     */
    default Map<String, String> getHeaderParams() {
        return new HashMap<>();
    }

    /**
     * 返回请求的路径
     * @return 默认方法：/（表示根目录）
     */
    default String getPath() {
        return "/";
    }

    /**
     * 返回请求是否需要签名
     * @return 默认方法：需要签名
     */
    default boolean isNeedSign() {
        return true;
    }

    /**
     * 返回请求签名的方式
     * @return 默认方法：签名在头部
     */
    default SignTypeEnum getSignType() {
        return SignTypeEnum.HEADER;
    }

    /**
     * 返回请求签名的有效期秒数
     * @return 默认方法：24小时
     */
    default long getSignExpireSeconds() {
        return 24 * 60 * 60;
    }

}
