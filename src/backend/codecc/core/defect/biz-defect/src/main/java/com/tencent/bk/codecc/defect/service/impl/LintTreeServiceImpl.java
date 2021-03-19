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

package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongotemplate.LintDefectDao;
import com.tencent.bk.codecc.defect.model.LintFileEntity;
import com.tencent.bk.codecc.defect.service.AbstractTreeService;
import com.tencent.devops.common.util.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Lint告警文件树
 *
 * @version V1.0
 * @date 2019/5/14
 */
@Service("LINTTreeBizService")
public class LintTreeServiceImpl extends AbstractTreeService
{
    @Autowired
    private LintDefectDao lintDefectDao;

    @Override
    public Set<String> getDefectPaths(Long taskId, String toolName)
    {
        List<LintFileEntity> lintFiles = lintDefectDao.findFileInfoList(taskId, toolName);
        Set<String> defectPaths = new TreeSet<>();
        lintFiles.forEach(fileInfo ->
        {
            // 获取所有警告文件的绝对路径
            String relativePath = PathUtils.getRelativePath(fileInfo.getUrl(), fileInfo.getRelPath());
            if (StringUtils.isNotBlank(relativePath))
            {
                defectPaths.add(relativePath);
            }
            else
            {
                defectPaths.add(fileInfo.getFilePath());
            }

        });

        return defectPaths;
    }


}
