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

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CLOCDefectDao;
import com.tencent.bk.codecc.defect.model.CLOCDefectEntity;
import com.tencent.bk.codecc.defect.service.AbstractUploadDefectBizService;
import com.tencent.bk.codecc.defect.service.CLOCUploadStatisticService;
import com.tencent.bk.codecc.defect.vo.CLOCInfoVO;
import com.tencent.bk.codecc.defect.vo.CLOCLanguageVO;
import com.tencent.bk.codecc.defect.vo.UploadCLOCStatisticVO;
import com.tencent.bk.codecc.defect.vo.UploadDefectVO;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CLOC工具告警上报服务
 *
 * @version V1.0
 * @date 2019/9/29
 */
@Slf4j
@Service("CLOCUploadDefectBizService")
public class CLOCUploadDefectBizServiceImpl extends AbstractUploadDefectBizService
{

    private static Logger logger = LoggerFactory.getLogger(CLOCUploadDefectBizServiceImpl.class);

    @Autowired
    private CLOCDefectDao clocDefectDao;

    @Autowired
    private CLOCUploadStatisticService clocUploadStatisticService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public CodeCCResult processBiz(UploadDefectVO uploadDefectVO)
    {
        if(StringUtils.isNotBlank(uploadDefectVO.getDefectsCompress()))
        {
            String defectListJson = decompressDefects(uploadDefectVO.getDefectsCompress());
            List<CLOCInfoVO> clocInfoVOList = JsonUtil.INSTANCE.to(defectListJson, new TypeReference<List<CLOCInfoVO>>()
            {
            });
            if(CollectionUtils.isNotEmpty(clocInfoVOList))
            {
                List<CLOCDefectEntity> clocDefectEntityList = clocInfoVOList.stream().map(clocInfoVO -> {
                    CLOCDefectEntity clocDefectEntity = new CLOCDefectEntity();
                    clocDefectEntity.setTaskId(uploadDefectVO.getTaskId());
                    clocDefectEntity.setStreamName(uploadDefectVO.getStreamName());
                    clocDefectEntity.setFileName(clocInfoVO.getFilePath());
                    clocDefectEntity.setToolName(uploadDefectVO.getToolName());
                    clocDefectEntity.setBlank(clocInfoVO.getBlank());
                    clocDefectEntity.setCode(clocInfoVO.getCode());
                    clocDefectEntity.setComment(clocInfoVO.getComment());
                    clocDefectEntity.setLanguage(clocInfoVO.getLanguage());
                    return clocDefectEntity;
                }).collect(Collectors.toList());

                clocDefectDao.batchUpsertClocInfo(clocDefectEntityList);

                //上报统计信息
                UploadCLOCStatisticVO uploadCLOCStatisticVO = new UploadCLOCStatisticVO();
                uploadCLOCStatisticVO.setTaskId(uploadDefectVO.getTaskId());
                uploadCLOCStatisticVO.setStreamName(uploadDefectVO.getStreamName());
                Map<String, List<CLOCInfoVO>> clocLanguageMap = clocInfoVOList.stream().collect(Collectors.groupingBy(CLOCInfoVO::getLanguage));
                List<CLOCLanguageVO> languageVOList = clocLanguageMap.entrySet().stream().map(stringListEntry -> {
                    CLOCLanguageVO clocLanguageVO = new CLOCLanguageVO();
                    clocLanguageVO.setLanguage(stringListEntry.getKey());
                    List<CLOCInfoVO> clocInfoVOS = stringListEntry.getValue();
                    clocLanguageVO.setCodeSum(clocInfoVOS.stream().map(CLOCInfoVO::getCode).reduce((o1, o2) -> o1 + o2).orElse(0L));
                    clocLanguageVO.setBlankSum(clocInfoVOS.stream().map(CLOCInfoVO::getBlank).reduce((o1, o2) -> o1 + o2).orElse(0L));
                    clocLanguageVO.setCommentSum(clocInfoVOS.stream().map(CLOCInfoVO::getComment).reduce((o1, o2) -> o1 + o2).orElse(0L));
                    return clocLanguageVO;
                }).collect(Collectors.toList());
                uploadCLOCStatisticVO.setLanguageCodeList(languageVOList);
                uploadCLOCStatisticVO.setLanguages(clocInfoVOList.stream().map(CLOCInfoVO::getLanguage).distinct().collect(Collectors.toList()));
                clocUploadStatisticService.uploadStatistic(uploadCLOCStatisticVO);
            }
            else
            {
                UploadCLOCStatisticVO uploadCLOCStatisticVO = new UploadCLOCStatisticVO();
                uploadCLOCStatisticVO.setTaskId(uploadDefectVO.getTaskId());
                uploadCLOCStatisticVO.setStreamName(uploadDefectVO.getStreamName());
                uploadCLOCStatisticVO.setLanguageCodeList(new ArrayList<>());
                uploadCLOCStatisticVO.setLanguages(new ArrayList<>());
                clocUploadStatisticService.uploadStatistic(uploadCLOCStatisticVO);
            }
        }

        /*if (!writeResult.isUpdateOfExisting()) {
            Map<String, Object> clocDefectEntityMap = JsonUtil.INSTANCE.toMap(clocDefectEntity);
            clocDefectEntityMap.put("washTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            List<Map<String, Object>> clocDefectEntityMapList = Collections.singletonList(clocDefectEntityMap);
            rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_KAFKA_DATA_PLATFORM, ConstantsKt.ROUTE_KAFKA_DATA_CLOC_DEFECT, JsonUtil.INSTANCE.toJson(clocDefectEntityMapList));
            log.info("send cloc defect msg to kafka! task id:{}", taskId);
        }*/

        return new CodeCCResult(CommonMessageCode.SUCCESS, "upload defect ok");

    }
}
