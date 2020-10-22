package com.tencent.bk.codecc.task.service.impl;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.task.dao.mongodbrepository.GongfengPublicProjRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.TaskDao;
import com.tencent.bk.codecc.task.model.GongfengPublicProjEntity;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.service.GongfengTaskService;
import com.tencent.bk.codecc.task.vo.TaskCodeLibraryVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.web.mq.TencentConstantsKt;
import jersey.repackaged.com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.tencent.devops.common.web.mq.TencentConstantsKt.EXCHANGE_SCORING_OPENSOURCE;
import static com.tencent.devops.common.web.mq.TencentConstantsKt.ROUTE_SCORING_OPENSOURCE;

@Service
@Primary
@Slf4j
public class GongfengTaskServiceImpl extends TaskServiceImpl implements GongfengTaskService {
    @Autowired
    private GongfengPublicProjRepository gongfengPublicProjRepository;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public List<Long> getBkPluginTaskIds() {
        Map<String, Object> params = Maps.newHashMap();
        params.put("status", ComConstants.Status.ENABLE.value());
        params.put("project_id", "CUSTOMPROJ_TEG_CUSTOMIZED");

        Map<String, Object> nParams = Maps.newHashMap();
        nParams.put("gongfeng_project_id", null);
        List<TaskInfoEntity> openSourceTaskList = taskDao.queryTaskInfoByCustomParam(params, nParams);

        log.info("bk plugin tasks {}", openSourceTaskList.size());

        Map<Integer, List<TaskInfoEntity>> proMap = openSourceTaskList.stream()
            .collect(Collectors.groupingBy(TaskInfoEntity::getGongfengProjectId));
        List<GongfengPublicProjEntity> projEntityList = gongfengPublicProjRepository.findByIdIn(proMap.keySet());
        List<Long> taskIds = Lists.newArrayList();
        projEntityList.stream()
            .filter(gongfengPublicProjEntity -> StringUtils.isNotBlank(gongfengPublicProjEntity.getHttpUrlToRepo())
                && gongfengPublicProjEntity.getHttpUrlToRepo().contains("/bkdevops-plugins/"))
            .forEach(gongfengPublicProjEntity -> taskIds.add(proMap.get(gongfengPublicProjEntity.getId())
                .get(0).getTaskId()));

        log.info("bk plugin gongfeng tasks {}", taskIds.size());
        return taskIds;
    }

    @Override
    public Boolean triggerBkPluginScoring() {
        rabbitTemplate.convertAndSend(EXCHANGE_SCORING_OPENSOURCE, ROUTE_SCORING_OPENSOURCE, "");
        return Boolean.TRUE;
    }

    @Override
    public void postGetCodeLibrary(TaskInfoEntity taskEntity, TaskCodeLibraryVO taskCodeLibrary) {
        if (Objects.nonNull(taskEntity.getGongfengProjectId()) && taskEntity.getCreateFrom().equals(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value()))
        {
            GongfengPublicProjEntity gongfengPublicProjEntity = gongfengPublicProjRepository.findById(taskEntity.getGongfengProjectId());
            if (Objects.nonNull(gongfengPublicProjEntity))
            {
                taskCodeLibrary.setRepoUrl(Collections.singletonList(gongfengPublicProjEntity.getWebUrl()));
                taskCodeLibrary.setBranch(Collections.singletonList(gongfengPublicProjEntity.getDefaultBranch()));
            }
        } else {
            super.postGetCodeLibrary(taskEntity, taskCodeLibrary);
        }
    }
}
