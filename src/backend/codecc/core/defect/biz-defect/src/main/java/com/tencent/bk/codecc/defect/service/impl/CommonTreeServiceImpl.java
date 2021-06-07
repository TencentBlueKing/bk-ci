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

import com.tencent.bk.codecc.defect.dao.mongorepository.DefectRepository;
import com.tencent.bk.codecc.defect.model.DefectEntity;
import com.tencent.bk.codecc.defect.service.AbstractTreeService;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 告警文件树
 *
 * @version V1.0
 * @date 2019/10/28
 */
@Service("CommonTreeBizService")
public class CommonTreeServiceImpl extends AbstractTreeService
{
    @Autowired
    private DefectRepository defectRepository;

    @Override
    public Set<String> getDefectPaths(Long taskId, String toolName)
    {
        List<DefectEntity> defectEntityList = defectRepository.findByTaskIdAndToolNameAndStatus(taskId, toolName, ComConstants.DefectStatus.NEW.value());

        Set<String> defectPaths = new TreeSet<>();
        if (CollectionUtils.isNotEmpty(defectEntityList))
        {
            Map<String, String> codeRepoUrlMap = getRelatePathMap(taskId);
            defectEntityList.forEach(defectEntity ->
            {
                String filePathname = trimWinPathPrefix(defectEntity.getFilePathname());
                String codeFileUrl = codeRepoUrlMap.get(filePathname.toLowerCase());

                if (StringUtils.isEmpty(codeFileUrl))
                {
                    codeFileUrl = filePathname;
                }
                defectPaths.add(codeFileUrl);
            });
        }
        return defectPaths;
    }

    protected String trimWinPathPrefix(String filePath)
    {
        return filePath;
    }
}
