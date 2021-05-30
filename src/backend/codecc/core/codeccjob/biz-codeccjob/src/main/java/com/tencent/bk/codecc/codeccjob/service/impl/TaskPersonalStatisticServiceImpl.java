package com.tencent.bk.codecc.codeccjob.service.impl;

import com.tencent.bk.codecc.codeccjob.dao.mongorepository.DUPCDefectRepository;
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.DefectRepository;
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.TaskPersonalStatisticRepository;
import com.tencent.bk.codecc.codeccjob.dao.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.codeccjob.dao.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.codeccjob.dao.mongotemplate.TaskPersonalStatisticDao;
import com.tencent.bk.codecc.codeccjob.service.TaskPersonalStatisticService;
import com.tencent.bk.codecc.defect.model.DUPCDefectEntity;
import com.tencent.bk.codecc.defect.model.DefectEntity;
import com.tencent.bk.codecc.defect.model.TaskPersonalStatisticEntity;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.service.utils.PageableUtils;
import com.tencent.devops.common.service.utils.ToolParamUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TaskPersonalStatisticServiceImpl implements TaskPersonalStatisticService {

    @Autowired
    private ToolMetaCacheService toolMetaCacheService;

    @Autowired
    private DefectRepository defectRepository;

    @Autowired
    private DUPCDefectRepository dupcDefectRepository;

    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;

    @Autowired
    private CCNDefectDao ccnDefectDao;

    @Autowired
    private TaskPersonalStatisticRepository taskPersonalStatisticRepository;

    @Autowired
    private TaskPersonalStatisticDao taskPersonalStatisticDao;

    @Autowired
    private Client client;

    private final int DEFECT_MAX_PAGE = 200;
    private final int DEFECT_PAGE_SIZE = 10000;

    @Override
    public void refresh(Long taskId, String extraInfo) {
        Map<String, TaskPersonalStatisticEntity> taskPersonalStatisticMap = new HashMap<>();

        TaskDetailVO taskInfo = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId).getData();
        List<String> taskToolNameList = taskInfo.getToolConfigInfoList().stream().map(ToolConfigInfoVO::getToolName).collect(Collectors.toList());

        log.info("start to get overview defect count for task: {}, {}, {}", taskId, extraInfo, taskInfo);
        List<String> defectTools = toolMetaCacheService.getToolDetailByDimension(ComConstants.ToolType.DEFECT.name());
        defectTools.retainAll(taskToolNameList);

        if (CollectionUtils.isNotEmpty(defectTools)) {
            for (int curPage =1;curPage <= DEFECT_MAX_PAGE; curPage++) {
                Pageable pageable = PageableUtils.getPageable(curPage, DEFECT_PAGE_SIZE);
                List<DefectEntity> defectList = defectRepository.findAuthorListByTaskIdAndToolNameInAndStatus(
                    taskId, defectTools, ComConstants.DefectStatus.NEW.value(), pageable);

                if (CollectionUtils.isEmpty(defectList)) {
                    log.info("get overview defect page count for task is : {}, {}", taskId, curPage);
                    break;
                }

                defectList.forEach(defectEntity -> {
                    if (CollectionUtils.isEmpty(defectEntity.getAuthorList())) {
                        return;
                    }

                    defectEntity.getAuthorList().stream().map(ToolParamUtils::trimUserName).forEach(author -> {
                        TaskPersonalStatisticEntity entity = taskPersonalStatisticMap.get(author);
                        if (entity == null) {
                            entity = new TaskPersonalStatisticEntity();
                            entity.setTaskId(taskId);
                            entity.setUsername(author);
                            taskPersonalStatisticMap.put(author, entity);
                        }
                        entity.setDefectCount(entity.getDefectCount() + 1);
                    });
                });
            }
        }

        log.info("start to get overview security count for task: {}", taskId);
        List<String> securityTools = toolMetaCacheService.getToolDetailByDimension(ComConstants.ToolType.SECURITY.name());
        securityTools.retainAll(taskToolNameList);

        if (CollectionUtils.isNotEmpty(securityTools)) {
            lintDefectV2Dao.findStatisticGroupByAuthor(taskId, securityTools).forEach(defectEntity -> {
                String author = ToolParamUtils.trimUserName(defectEntity.getAuthorName());
                TaskPersonalStatisticEntity entity = taskPersonalStatisticMap.get(author);
                if (entity == null) {
                    entity = new TaskPersonalStatisticEntity();
                    entity.setTaskId(taskId);
                    entity.setUsername(author);
                    taskPersonalStatisticMap.put(author, entity);
                }
                entity.setSecurityCount(entity.getSecurityCount() + defectEntity.getDefectCount());
            });
        }

        log.info("start to get overview standard count for task: {}", taskId);
        List<String> standardTools = toolMetaCacheService.getToolDetailByDimension(ComConstants.ToolType.STANDARD.name());
        standardTools.retainAll(taskToolNameList);

        if (CollectionUtils.isNotEmpty(standardTools)) {
            lintDefectV2Dao.findStatisticGroupByAuthor(taskId, standardTools).forEach(defectEntity -> {
                String author = ToolParamUtils.trimUserName(defectEntity.getAuthorName());
                TaskPersonalStatisticEntity entity = taskPersonalStatisticMap.get(author);
                if (entity == null) {
                    entity = new TaskPersonalStatisticEntity();
                    entity.setTaskId(taskId);
                    entity.setUsername(author);
                    taskPersonalStatisticMap.put(author, entity);
                }
                entity.setStandardCount(entity.getStandardCount() + defectEntity.getDefectCount());
            });
        }


        log.info("start to get overview risk count for task: {}", taskId);
        if (taskToolNameList.contains(ComConstants.Tool.CCN.name())) {
            ccnDefectDao.findStatisticGroupByAuthor(taskId, ComConstants.DefectStatus.NEW.value()).forEach(defectEntity -> {
                String author = ToolParamUtils.trimUserName(defectEntity.getAuthorName());
                TaskPersonalStatisticEntity entity = taskPersonalStatisticMap.get(author);
                if (entity == null) {
                    entity = new TaskPersonalStatisticEntity();
                    entity.setTaskId(taskId);
                    entity.setUsername(author);
                    taskPersonalStatisticMap.put(author, entity);
                }
                entity.setRiskCount(entity.getRiskCount() + defectEntity.getDefectCount());
            });
        }

        log.info("start to get overview dup file count for task: {}", taskId);
        if (taskToolNameList.contains(ComConstants.Tool.DUPC.name())) {
            for (int curPage =1; curPage <= DEFECT_MAX_PAGE; curPage++) {
                Pageable pageable = PageableUtils.getPageable(curPage, DEFECT_MAX_PAGE);
                List<DUPCDefectEntity> dupcDefectList = dupcDefectRepository.findAuthorListByTaskIdAndAuthor(taskId, ComConstants.DefectStatus.NEW.value(), pageable);

                if (CollectionUtils.isEmpty(dupcDefectList)) {
                    log.info("get overview dupc defect page count for task is : {}, {}", taskId, curPage);
                    break;
                }

                dupcDefectList.forEach(defectEntity -> {
                    String authorListString = defectEntity.getAuthorList();
                    if (StringUtils.isNotBlank(authorListString)) {
                        String[] authorList = authorListString.split(";");
                        for (String rawAuthor : authorList) {
                            String author = ToolParamUtils.trimUserName(rawAuthor);
                            TaskPersonalStatisticEntity entity = taskPersonalStatisticMap.get(author);
                            if (entity == null) {
                                entity = new TaskPersonalStatisticEntity();
                                entity.setTaskId(taskId);
                                entity.setUsername(author);
                                taskPersonalStatisticMap.put(author, entity);
                            }
                            entity.setDupFileCount(entity.getDupFileCount() + 1);
                        }
                    }
                });
            }
        }


        log.info("start to delete old overview data and save new for task: {}", taskId);
        taskPersonalStatisticRepository.deleteByTaskId(taskId);
        taskPersonalStatisticDao.batchSave(taskPersonalStatisticMap.values());
    }
}
