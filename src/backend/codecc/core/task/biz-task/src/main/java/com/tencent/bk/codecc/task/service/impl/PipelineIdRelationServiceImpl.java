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

package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.task.dao.mongorepository.PipelineIdRelationshipRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.PipelineIdRelationDao;
import com.tencent.bk.codecc.task.model.PipelineIdRelationshipEntity;
import com.tencent.bk.codecc.task.service.PipelineIdRelationService;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 流水线维度构建服务层代码
 *
 * @version V1.0
 * @date 2020/10/27
 */
@Service
public class PipelineIdRelationServiceImpl implements PipelineIdRelationService {

    @Autowired
    private PipelineIdRelationshipRepository pipelineIdRelationshipRepository;

    @Autowired
    private PipelineIdRelationDao pipelineIdRelationDao;


    private static final Logger logger = LoggerFactory.getLogger(PipelineIdRelationServiceImpl.class);


    @Override
    public void updateSuccessRecord(String pipelineId, LocalDate triggerDate) {
        pipelineIdRelationDao.updatePipelineIdRelationStatus(pipelineId, triggerDate, ComConstants.ScanStatus.SUCCESS.getCode());
    }


    @Override
    public void updateFailOrProcessRecord(PipelineIdRelationshipEntity pipelineIdRelationshipEntity){
        PipelineIdRelationshipEntity previousPipelineEntity = pipelineIdRelationshipRepository.findFirstByPipelineIdAndTriggerDate(
                pipelineIdRelationshipEntity.getPipelineId(), pipelineIdRelationshipEntity.getTriggerDate());
        logger.info("start to update fail or process record, pipeline id: {}", pipelineIdRelationshipEntity.getPipelineId());
        //查询该流水线下当日的构建记录，如果没有记录则新增，如果有记录则需要判断是否是成功状态，如果不是则需要更新状态
        //该表原则是一个流水线，一个日期下，如果有为成功状态的，则不再更新，如果无成功状态的话，则需要再下发
        if(null == previousPipelineEntity || StringUtils.isBlank(previousPipelineEntity.getPipelineId())){
            logger.info("new pipeline id relation record, need to insert");
            pipelineIdRelationshipRepository.save(pipelineIdRelationshipEntity);
        } else {
            if(!ComConstants.ScanStatus.SUCCESS.getCode().equals(previousPipelineEntity.getStatus())){
                logger.info("previous record with non-success status, need to update");
                previousPipelineEntity.setStatus(pipelineIdRelationshipEntity.getStatus());
                pipelineIdRelationshipRepository.save(previousPipelineEntity);
            } else {
                logger.info("previous record with success status, no need to operate");
            }
        }
    }

    @Override
    public List<PipelineIdRelationshipEntity> findAllFailOrProcessRecord() {
        return pipelineIdRelationshipRepository.findAllByTriggerDateAndStatusIsNot(LocalDate.now(),
                ComConstants.ScanStatus.SUCCESS.getCode());
    }

    @Override
    public void deleteExpiredRecord(){
        pipelineIdRelationDao.deleteRecordsBeforeDate(LocalDate.now().minusDays(2));
    }
}
