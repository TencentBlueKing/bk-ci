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

package com.tencent.bk.codecc.defect.component;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 风险系数缓存
 *
 * @version V1.0
 * @date 2019/7/30
 */
@Component
public class RiskConfigCache
{
    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    private LoadingCache<String, Map<String, String>> cache = CacheBuilder.newBuilder()
            .maximumSize(5)
            .build(new CacheLoader<String, Map<String, String>>()
            {
                @Override
                public Map<String, String> load(String key)
                {
                    return getRiskConfigMapToCache(key);
                }
            });

    /**
     * 将风险系数信息获取至缓存
     *
     * @param key
     * @return
     */
    private Map<String, String> getRiskConfigMapToCache(String key)
    {
        return thirdPartySystemCaller.getRiskFactorConfig(key);
    }

    /**
     * 获取风险系数信息方法
     *
     * @param toolName
     * @return
     */
    public Map<String, String> getRiskConfig(String toolName)
    {
        return cache.getUnchecked(toolName);
    }


}
