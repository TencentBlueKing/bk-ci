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

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_EXTERNAL_JOB;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.quartz.pojo.JobExternalDto;
import com.tencent.bk.codecc.quartz.pojo.OperationType;
import com.tencent.bk.codecc.task.dao.mongorepository.CustomProjRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.GongfengActiveProjRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.GongfengPublicProjRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.GongfengStatProjRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.GongfengStatProjDao;
import com.tencent.bk.codecc.task.dao.mongotemplate.TaskDao;
import com.tencent.bk.codecc.task.model.CustomProjEntity;
import com.tencent.bk.codecc.task.model.ForkProjEntity;
import com.tencent.bk.codecc.task.model.GongfengActiveProjEntity;
import com.tencent.bk.codecc.task.model.GongfengPublicProjEntity;
import com.tencent.bk.codecc.task.model.GongfengStatProjEntity;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.service.GongfengPublicProjService;
import com.tencent.bk.codecc.task.vo.CustomProjVO;
import com.tencent.bk.codecc.task.vo.GongfengPublicProjVO;
import com.tencent.bk.codecc.task.vo.gongfeng.ForkProjVO;
import com.tencent.bk.codecc.task.vo.gongfeng.ProjectStatVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.service.utils.PageableUtils;
import com.tencent.devops.common.util.OkhttpUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import kotlin.TuplesKt;
import kotlin.collections.MapsKt;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * 工蜂公共项目服务层代码
 *
 * @version V1.0
 * @date 2019/9/26
 */
@Service
public class GongfengPublicProjServiceImpl implements GongfengPublicProjService {

    private static Logger logger = LoggerFactory.getLogger(GongfengPublicProjServiceImpl.class);

    @Autowired
    private GongfengPublicProjRepository gongfengPublicProjRepository;

    @Autowired
    private GongfengActiveProjRepository gongfengActiveProjRepository;

    @Autowired
    private GongfengStatProjRepository gongfengStatProjRepository;

    @Autowired
    private GongfengStatProjDao gongfengStatProjDao;

    @Autowired
    private CustomProjRepository customProjRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${codecc.classurl:#{null}}")
    private String publicClassUrl;

    @Value("${git.path:#{null}}")
    private String gitCodePath;

    @Value("${codecc.privatetoken:#{null}}")
    private String privateToken;


    @Override
    public List<GongfengPublicProjEntity> findAllProjects() {
        return gongfengPublicProjRepository.findAll();
    }

    @Override
    public GongfengPublicProjEntity findProjectById(Integer id) {
        return gongfengPublicProjRepository.findFirstById(id);
    }

    @Override
    public void saveProject(GongfengPublicProjEntity gongfengPublicProjEntity) {
        gongfengPublicProjRepository.save(gongfengPublicProjEntity);
    }

    @Override
    public void saveActiveProject(GongfengActiveProjEntity gongfengActiveProjEntity) {
        gongfengActiveProjRepository.save(gongfengActiveProjEntity);
    }

    @Override
    public String getGongfengUrl(Long taskId) {
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        if (null == taskInfoEntity) {
            return null;
        }
        GongfengPublicProjEntity gongfengPublicProjEntity = gongfengPublicProjRepository.findFirstById(taskInfoEntity.getGongfengProjectId());
        if (null == gongfengPublicProjEntity) {
            return null;
        }
        return gongfengPublicProjEntity.getHttpUrlToRepo();
    }

    @Override
    public List<GongfengActiveProjEntity> findAllActiveProjects()
    {
        return gongfengActiveProjRepository.findAll();
    }

    @Override
    public Boolean judgeActiveProjExists(Integer id)
    {
        return null != gongfengActiveProjRepository.findFirstById(id);
    }

    @Override
    public Boolean extendGongfengScanRange(Integer startPage, Integer endPage, Integer startHour, Integer startMinute) {
        JobExternalDto jobExternalDto = new JobExternalDto(
                null,
                String.format("%sCreateTaskScheduleTask.java", publicClassUrl),
                "CreateTaskScheduleTask",
                String.format("0 %d %d * * ?", startMinute, startHour),
                new HashMap<String, Object>(){{
                    put("gitCodePath", gitCodePath);
                    put("gitPrivateToken", privateToken);
                    put("startPage", startPage);
                    put("endPage", endPage);
                }},
                OperationType.ADD
        );
        rabbitTemplate.convertAndSend(EXCHANGE_EXTERNAL_JOB, "", jobExternalDto);
        return true;
    }

    @Override
    public List<GongfengPublicProjEntity> findProjectListByIds(Collection<Integer> idSet)
    {
        List<GongfengPublicProjEntity> gongfengPublicProjEntitys;
        if (CollectionUtils.isNotEmpty(idSet))
        {
            gongfengPublicProjEntitys = gongfengPublicProjRepository.findByIdIn(idSet);
        }
        else
        {
            gongfengPublicProjEntitys = Lists.newArrayList();
        }
        return gongfengPublicProjEntitys;
    }

    @Override
    public Map<Integer, GongfengPublicProjVO> queryGongfengProjectMapById(Collection<Integer> idSet)
    {
        Map<Integer, GongfengPublicProjVO> projectEntityMap;
        if (CollectionUtils.isEmpty(idSet))
        {
            projectEntityMap = Maps.newHashMap();
        }
        else
        {
            List<GongfengPublicProjEntity> projectListByIds = findProjectListByIds(idSet);
            projectEntityMap = projectListByIds.stream().map(entity ->
            {
                GongfengPublicProjVO gongfengPublicProjVO = new GongfengPublicProjVO();
                BeanUtils.copyProperties(entity, gongfengPublicProjVO, "forkedFromProject");
                ForkProjEntity forkedFromProject = entity.getForkedFromProject();
                int gongfengId = 0;
                ForkProjVO forkProjVO = new ForkProjVO();
                if (forkedFromProject != null)
                {
                    gongfengId = forkedFromProject.getId();
                }
                forkProjVO.setId(gongfengId);
                gongfengPublicProjVO.setForkedFromProject(forkProjVO);
                gongfengPublicProjVO.setHttpUrlToRepo(entity.getHttpUrlToRepo());
                return gongfengPublicProjVO;
            }).collect(Collectors.toMap(GongfengPublicProjVO::getId, Function.identity(), (k, v) -> v));
        }
        return projectEntityMap;
    }

    @Override
    public Boolean saveStatProject(Integer bgId)
    {
        if (bgId == null || bgId == 0)
        {
            logger.error("param bgId is null!");
            return false;
        }

        int page = 1;
        int pageSize = 3000;
        int dataSize;
        do
        {
            try
            {
                String url = this.gitCodePath + "/api/stat/v1/org/" + bgId + "/projects/statistics?page=" + page +
                        "&per_page=" + pageSize + "&sort=asc";

                String result =
                        OkhttpUtils.INSTANCE.doGet(url, MapsKt.mapOf(TuplesKt.to("PRIVATE-TOKEN", privateToken)));
                List<GongfengStatProjEntity> statProjectList =
                        objectMapper.readValue(result, new TypeReference<List<GongfengStatProjEntity>>()
                        {
                        });
                if (CollectionUtils.isEmpty(statProjectList))
                {
                    logger.error("get gong feng stat project info fail! bg id: {}", bgId);
                    return false;
                }

                statProjectList.forEach(entity -> entity.setBgId(bgId));

                dataSize = statProjectList.size();
                logger.info(">>>>>>>>>bg id " + bgId + " fetch page " + page + " size " + dataSize);
                long start = System.currentTimeMillis();
                gongfengStatProjDao.upsertGongfengStatProjList(statProjectList);
                logger.info("time >>> {}", System.currentTimeMillis() - start);
                // 翻页
                page++;
            }
            catch (Exception e)
            {
                logger.error("get gong feng stat project info fail! bg id: {}\n{}", bgId, e.getMessage());
                return false;
            }
        }
        while (dataSize >= pageSize);

        return true;
    }

    @Override
    public List<GongfengStatProjEntity> findStatProjectList(Integer bgId, Collection<Integer> idSet)
    {
        List<GongfengStatProjEntity> gongfengStatProjectEntitys;
        if (CollectionUtils.isNotEmpty(idSet))
        {
            gongfengStatProjectEntitys = gongfengStatProjRepository.findByBgIdIsAndIdIn(bgId, idSet);
        }
        else
        {
            gongfengStatProjectEntitys = Lists.newArrayList();
        }
        return gongfengStatProjectEntitys;
    }


    @Override
    public GongfengStatProjEntity findStatByProjectId(Integer projectId)
    {
        return gongfengStatProjRepository.findFirstById(projectId);
    }

    @Override
    public Map<Integer, ProjectStatVO> queryGongfengStatProjectById(Integer bgId, Collection<Integer> idSet)
    {
        Map<Integer, ProjectStatVO> projectEntityMap;
        if (CollectionUtils.isEmpty(idSet))
        {
            projectEntityMap = Maps.newHashMap();
        }
        else
        {
            List<GongfengStatProjEntity> projectListByIds = findStatProjectList(bgId, idSet);

            projectEntityMap = projectListByIds.stream().map(entity ->
            {
                ProjectStatVO gongfengStatProjVO = new ProjectStatVO();
                BeanUtils.copyProperties(entity, gongfengStatProjVO);
                return gongfengStatProjVO;
            }).collect(Collectors.toMap(ProjectStatVO::getId, Function.identity(), (k, v) -> v));
        }
        return projectEntityMap;
    }

    @Override
    public Page<CustomProjVO> queryCustomTaskByPageable(@NotNull QueryTaskListReqVO reqVO)
    {
        List<CustomProjVO> contentList = Lists.newArrayList();

        String sortType = reqVO.getSortType();
        Sort.Direction direction = Sort.Direction.valueOf(sortType);
        Pageable pageable = PageableUtils
                .getPageable(reqVO.getPageNum(), reqVO.getPageSize(), reqVO.getSortField(), direction, "task_id");

        org.springframework.data.domain.Page<CustomProjEntity> entityPage =
                customProjRepository.findByCustomProjSource(reqVO.getCustomTaskSource(), pageable);

        List<CustomProjEntity> entityList = entityPage.getContent();
        if (CollectionUtils.isNotEmpty(entityList))
        {
            contentList = entityList.stream().map(entity ->
            {
                CustomProjVO customProjVO = new CustomProjVO();
                BeanUtils.copyProperties(entity, customProjVO);
                return customProjVO;
            }).collect(Collectors.toList());
        }

        // 页码+1展示
        return new Page<>(entityPage.getTotalElements(), entityPage.getNumber() + 1, entityPage.getSize(),
                entityPage.getTotalPages(), contentList);
    }

    @Override
    public Boolean delete(Integer id){
        gongfengPublicProjRepository.deleteByIdIs(id);
        return true;
    }

    @Override
    public Map<Long, GongfengPublicProjVO> queryGongfengProjectMapByTaskId(List<Long> taskId) {
        List<TaskInfoEntity> taskEntityList = taskRepository.findByTaskIdIn(taskId);
        if (CollectionUtils.isEmpty(taskEntityList)) {
            logger.info("get gongfeng projrct by taskId fail");
            return Maps.newHashMap();
        }

        Set<Integer> gongfengId = taskEntityList.stream()
                .map(TaskInfoEntity::getGongfengProjectId)
                .collect(Collectors.toSet());

        Map<Integer, GongfengPublicProjVO> gongfengPublicProjVOMap = queryGongfengProjectMapById(gongfengId);
        logger.info("get gongfeng proj by taskId, task size: {} | proj size: {}", taskId.size(), gongfengId.size());
        Map<Long, GongfengPublicProjVO> gongfengMap = new HashMap<>();
        for (TaskInfoEntity taskInfoEntity : taskEntityList) {
            GongfengPublicProjVO proj = gongfengPublicProjVOMap.get(taskInfoEntity.getGongfengProjectId());
            if (proj != null) {
                gongfengMap.put(taskInfoEntity.getTaskId(), proj);
            }
        }

        return gongfengMap;
    }
}
