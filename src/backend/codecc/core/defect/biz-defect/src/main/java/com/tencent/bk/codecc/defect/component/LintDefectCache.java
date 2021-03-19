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
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * lint类工具规则严重级别缓存
 *
 * @version V1.0
 * @date 2019/7/2
 */
@Component
public class LintDefectCache
{
    @Autowired
    private CheckerService multitoolCheckerService;

    private LoadingCache<String, Map<String, Integer>> cache = CacheBuilder.newBuilder()
            .maximumSize(20000)
            .build(new CacheLoader<String, Map<String, Integer>>()
            {
                @Override
                public Map<String, Integer> load(String key)
                {
                    return getSeverityMapByTool(key);
                }
            });


    /**
     * 获取严重等级映射
     *
     * @param toolName
     * @return
     */
    private Map<String, Integer> getSeverityMapByTool(String toolName)
    {
        Map<String, CheckerDetailVO> checkerDetailVOMap = multitoolCheckerService.queryAllChecker(toolName);
        return checkerDetailVOMap.entrySet().stream().
                collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getSeverity(), (k, v) -> v));
    }

    /**
     * 在缓存中获取严重等级
     *
     * @param toolName
     * @param checkerName
     * @return
     */
    public int getDefectLevel(String toolName, String checkerName)
    {
        int level = 0;
        Map<String, Integer> checkerMap = cache.getUnchecked(toolName);
        if (MapUtils.isNotEmpty(checkerMap))
        {
            if (checkerMap.containsKey(checkerName))
            {
                level = checkerMap.get(checkerName);
            }
        }
        return level;
    }

}
