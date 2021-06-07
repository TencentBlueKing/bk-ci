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

package com.tencent.bk.codecc.quartz.job;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext;
import com.tencent.bk.codecc.task.pojo.GongfengStatPageModel;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.pojo.GongfengStatProjVO;
import com.tencent.devops.common.util.OkhttpUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_GONGFENG_STAT_SYNC;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_GONGFENG_STAT_SYNC;

/**
 * 同步工蜂统计信息定时任务
 *
 * @date 2020/4/1
 * @version V1.0
 */
public class SyncGongfengStatProjTask implements IScheduleTask
{
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static Logger logger = LoggerFactory.getLogger(SyncGongfengStatProjTask.class);

    @Override
    public void executeTask(@NotNull QuartzJobContext quartzJobContext) {
        Map<String, Object> jobCustomParam = quartzJobContext.getJobCustomParam();
        if(null == jobCustomParam)
        {
            logger.info("job custom param is null");
            return;
        }

        String gitCodePath = (String) jobCustomParam.get("gitCodePath");
        String gitPrivateToken = (String) jobCustomParam.get("gitPrivateToken");
        Integer startPage = (Integer) jobCustomParam.get("startPage");
        Integer endPage = (Integer) jobCustomParam.get("endPage");
        String bgIdStr = (String) jobCustomParam.get("bgId");

        if(StringUtils.isNotBlank(bgIdStr))
        {
            String[] bgIdArray = bgIdStr.split(ComConstants.SEMICOLON);
            for(String bgId : bgIdArray)
            {
                Integer page = startPage;
                Integer dataSize = 100;
                while (dataSize >= 100 && page <= endPage + 1) {
                    String url = String.format("%s/api/stat/v1/org/%s/projects/statistics?page=%d&per_page=100&sort=asc", gitCodePath, bgId, page);
                    //从工蜂拉取信息，并按分页下发
                    String result = OkhttpUtils.INSTANCE.doGet(url, new HashMap<String, String>(){{put("PRIVATE-TOKEN", gitPrivateToken);}});
                    if(StringUtils.isBlank(result))
                    {
                        logger.info("null returned from api");
                        return;
                    }
                    List<GongfengStatProjVO> gongfengModelList;
                    try {
                        gongfengModelList = objectMapper.readValue(result, new TypeReference<List<GongfengStatProjVO>>() {});
                    } catch (IOException e) {
                        logger.error("deserialize gongfeng model list fail! bg id: {}", bgId, e);
                        return;
                    }

                    if(CollectionUtils.isEmpty(gongfengModelList))
                    {
                        logger.error("empty gongfeng model list, bg id: {}", bgId);
                        return;
                    }
                    dataSize = gongfengModelList.size();
                    logger.info("size of json array is: {}");

                    for(GongfengStatProjVO gongfengStatProjVO : gongfengModelList)
                    {
                        gongfengStatProjVO.setBgId(Integer.valueOf(bgId));
                    }
                    GongfengStatPageModel gongfengStatPageModel = new GongfengStatPageModel(page, bgId, gongfengModelList);
                    rabbitTemplate.convertAndSend(EXCHANGE_GONGFENG_STAT_SYNC, ROUTE_GONGFENG_STAT_SYNC, gongfengStatPageModel);
                    //每一次线程休息2秒，确保负载正常
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        logger.error("thread sleep fail! bg id: {}", bgId);
                        return;
                    }
                    page++;
                }
            }
        }


    }
}
