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
 
package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongorepository.FileDefectGatherRepository;
import com.tencent.bk.codecc.defect.model.FileDefectGatherEntity;
import com.tencent.bk.codecc.defect.service.FileDefectGatherService;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.vo.FileDefectGatherVO;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件告警收敛业务实现类
 * 
 * @date 2020/5/26
 * @version V1.0
 */
@Service
@Slf4j
public class FileDefectGatherServiceImpl implements FileDefectGatherService
{
    @Autowired
    private FileDefectGatherRepository fileDefectGatherRepository;

    @Override
    public FileDefectGatherVO getFileDefectGather(long taskId, String toolName, String dimension)
    {
        List<String> toolNameSet = ParamUtils.getToolsByDimension(toolName, dimension, taskId);

        if (CollectionUtils.isEmpty(toolNameSet)) {
            return null;
        }

        List<FileDefectGatherEntity> gatherFileList = fileDefectGatherRepository.findByTaskIdAndToolNameInAndStatus(taskId, toolNameSet, ComConstants.DefectStatus.NEW.value());

        if (CollectionUtils.isNotEmpty(gatherFileList))
        {
            FileDefectGatherVO fileDefectGatherVO = new FileDefectGatherVO();
            fileDefectGatherVO.setDefectCount(0);
            List<FileDefectGatherVO.GatherFile> gatherFileVOList = gatherFileList.stream().map(gatherFileEntity ->
            {
                FileDefectGatherVO.GatherFile gatherFile = new FileDefectGatherVO.GatherFile();
                BeanUtils.copyProperties(gatherFileEntity, gatherFile);
                fileDefectGatherVO.setDefectCount(fileDefectGatherVO.getDefectCount() + gatherFile.getTotal());
                return gatherFile;
            }).collect(Collectors.toList());
            fileDefectGatherVO.setGatherFileList(gatherFileVOList);
            fileDefectGatherVO.setFileCount(gatherFileVOList.size());
            log.info("getFileDefectGather success, fileCount:{}, defectCount:{}", fileDefectGatherVO.getFileCount(), fileDefectGatherVO.getDefectCount());
            return fileDefectGatherVO;
        }
        log.info("getFileDefectGather success: Empty.");
        return null;
    }
}
