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

package com.tencent.bk.codecc.task.service;

import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.bk.codecc.task.vo.FilterPathOutVO;
import com.tencent.bk.codecc.task.vo.TreeNodeTaskVO;
import com.tencent.bk.codecc.task.vo.path.CodeYmlFilterPathVO;

import java.util.List;

/**
 * 路径忽略服务类
 *
 * @version V1.0
 * @date 2019/4/26
 */
public interface PathFilterService
{
    /**
     * 将共通化过滤路径库添加到默认过滤路径中
     *
     * @param taskInfoEntity
     */
    void addDefaultFilterPaths(TaskInfoEntity taskInfoEntity);


    /**
     * 添加路径屏蔽文件
     *
     * @param filterPathInput
     * @param userName
     * @return
     */
    Boolean addFilterPaths(FilterPathInputVO filterPathInput, String userName);


    /**
     * 删除路径屏蔽
     *
     * @param path
     * @param pathType
     * @param userName
     * @return
     */
    Boolean deleteFilterPath(String path, String pathType, Long taskId, String userName);


    /**
     * 获取屏蔽路径信息
     *
     * @param taskId
     * @return
     */
    FilterPathOutVO getFilterPath(Long taskId);


    /**
     * 路径屏蔽树
     *
     * @param taskId
     * @return
     */
    TreeNodeTaskVO filterPathTree(Long taskId);


    Boolean codeYmlFilterPath(Long taskId, String userName, CodeYmlFilterPathVO codeYmlFilterPathVO);

    CodeYmlFilterPathVO listCodeYmlFilterPath(Long taskId);
}
