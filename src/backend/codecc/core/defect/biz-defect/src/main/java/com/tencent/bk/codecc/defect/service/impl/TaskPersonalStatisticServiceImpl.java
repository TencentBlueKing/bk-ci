package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongorepository.TaskPersonalStatisticRepository;
import com.tencent.bk.codecc.defect.model.TaskPersonalStatisticEntity;
import com.tencent.bk.codecc.defect.service.TaskPersonalStatisticService;
import com.tencent.bk.codecc.defect.vo.TaskPersonalStatisticRefreshReq;
import com.tencent.bk.codecc.defect.vo.TaskPersonalStatisticwVO;
import com.tencent.devops.common.web.mq.ConstantsKt;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TaskPersonalStatisticServiceImpl implements TaskPersonalStatisticService {

    @Autowired
    private TaskPersonalStatisticRepository taskPersonalStatisticRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public TaskPersonalStatisticwVO getPersonalStatistic(Long taskId, String username) {
        TaskPersonalStatisticEntity entity = taskPersonalStatisticRepository.findFirstByTaskIdAndUsername(taskId, username);

        if (entity == null) {
            refresh(taskId, "from user resources");
            return TaskPersonalStatisticwVO.builder()
                .taskId(taskId)
                .username(username)
                .defectCount(0)
                .securityCount(0)
                .standardCount(0)
                .riskCount(0)
                .dupFileCount(0)
                .build();
        }

        return TaskPersonalStatisticwVO.builder()
            .taskId(taskId)
            .username(username)
            .defectCount(entity.getDefectCount())
            .securityCount(entity.getSecurityCount())
            .standardCount(entity.getStandardCount())
            .riskCount(entity.getRiskCount())
            .dupFileCount(entity.getDupFileCount())
            .build();

    }

    @Override
    public List<TaskPersonalStatisticwVO> getPersonalStatistic(Long taskId) {
        List<TaskPersonalStatisticEntity> statisticEntities = taskPersonalStatisticRepository.findByTaskId(taskId);

        if (CollectionUtils.isEmpty(statisticEntities)) {
            return Collections.emptyList();
        }

        return statisticEntities.stream().map(entity -> {
            TaskPersonalStatisticwVO taskPersonalStatisticwVO = new TaskPersonalStatisticwVO();
            BeanUtils.copyProperties(entity, taskPersonalStatisticwVO);
            return taskPersonalStatisticwVO;
        }).collect(Collectors.toList());
    }

    @Override
    public void refresh(Long taskId, String extraInfo) {
        TaskPersonalStatisticRefreshReq request = new TaskPersonalStatisticRefreshReq(taskId, extraInfo);
        rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_TASK_PERSONAL, ConstantsKt.ROUTE_TASK_PERSONAL, request);
    }
}
