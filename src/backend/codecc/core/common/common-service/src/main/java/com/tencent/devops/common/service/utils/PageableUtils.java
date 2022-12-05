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

package com.tencent.devops.common.service.utils;

import org.apache.commons.lang.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 分页辅助类
 *
 * @version V1.0
 * @date 2020/3/30
 */

public class PageableUtils
{
    /**
     * 生成分页请求对象
     *
     * @param pageNum          页码
     * @param pageSize         每页数量
     * @param sortField        排序字段
     * @param sortType         排序类型
     * @param defaultSortField 默认排序字段
     * @return PR
     */
    public static Pageable getPageable(Integer pageNum, Integer pageSize, String sortField, Sort.Direction sortType,
            String defaultSortField)
    {
        Sort pageSort;
        if (StringUtils.isEmpty(sortField) || null == sortType)
        {
            pageSort = Sort.by(Sort.Direction.ASC, defaultSortField);
        }
        else
        {
            pageSort = Sort.by(sortType, sortField);
        }

        return PageRequest.of(pageNum == null || pageNum - 1 < 0 ? 0 : pageNum - 1,
                pageSize == null || pageSize <= 0 ? 10 : pageSize, pageSort);
    }

    /**
     * 生成分页请求对象
     *
     * @param pageNum          页码
     * @param pageSize         每页数量
     * @return PR
     */
    public static Pageable getPageable(Integer pageNum, Integer pageSize)
    {

        return PageRequest.of(pageNum == null || pageNum - 1 < 0 ? 0 : pageNum - 1,
            pageSize == null || pageSize <= 0 ? 10 : pageSize);
    }
}
