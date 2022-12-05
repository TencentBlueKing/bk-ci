package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerHisRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.CheckerDetailHisEntity;
import com.tencent.bk.codecc.defect.service.ICheckerIntegratedBizService;
import com.tencent.devops.common.constant.ComConstants.ToolIntegratedStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CheckerIntegratedBizServiceImpl implements ICheckerIntegratedBizService {

    @Autowired
    private CheckerRepository checkerRepository;

    @Autowired
    private CheckerHisRepository checkerHisRepository;

    @Override
    public List<String> updateToStatus(String userName,
                                       String buildId,
                                       String toolName,
                                       ToolIntegratedStatus toStatus) {
        List<CheckerDetailEntity> fromCheckerList = init(toolName, toStatus);

        backup(toolName, fromCheckerList, buildId);

        List<CheckerDetailEntity> newCheckerList = doUpdate(userName, toolName, toStatus, fromCheckerList);

        return newCheckerList.stream().map(CheckerDetailEntity::getCheckerKey).collect(Collectors.toList());
    }

    private List<CheckerDetailEntity> doUpdate(String userName, String toolName,
                                               ToolIntegratedStatus toStatus,
                                               List<CheckerDetailEntity> fromCheckerList) {
        if (CollectionUtils.isEmpty(fromCheckerList)) {
            return new ArrayList<>();
        }

        log.info("batch update checker: {}, {}", toolName, toStatus);

        List<CheckerDetailEntity> newCheckerList = fromCheckerList.stream().map(it -> {
            it.setCheckerVersion(toStatus.value());
            it.setUpdatedBy(userName);
            it.setUpdatedDate(System.currentTimeMillis());
            return it;
        }).collect(Collectors.toList());
        checkerRepository.saveAll(newCheckerList);

        return newCheckerList;
    }

    private List<CheckerDetailEntity> init(String toolName, ToolIntegratedStatus toStatus) {
        if (toStatus == ToolIntegratedStatus.T) {
            log.info("update checker status do nothing: {}, {}", toolName, toStatus);
            return new ArrayList<>();
        }

        ToolIntegratedStatus fromStatus = toStatus == ToolIntegratedStatus.P
            ? ToolIntegratedStatus.G : ToolIntegratedStatus.T;

        log.info("start to copy checker: {}, {}, {}", toolName, fromStatus, toStatus);

        List<CheckerDetailEntity> fromCheckerList =
            checkerRepository.findByToolNameAndCheckerVersion(toolName, fromStatus.value());

        if (CollectionUtils.isEmpty(fromCheckerList)) {
            log.info("no changed checker, do nothing: {}, {}", toolName, toStatus);
        }

        return fromCheckerList;
    }

    private void backup(String toolName, List<CheckerDetailEntity> fromCheckerList, String buildId) {
        if (CollectionUtils.isEmpty(fromCheckerList)) {
            return;
        }

        List<CheckerDetailHisEntity> hisCheckerHisEntities = checkerHisRepository.findByToolName(toolName);
        if (CollectionUtils.isNotEmpty(hisCheckerHisEntities)) {
            if (hisCheckerHisEntities.get(0).getBuildId().equals(buildId)) {
                log.info("is the same back up build id, do nothing: {}", toolName);
                return;
            }
        }

        log.info("start to back up checker in from checker key set: {}, {}", toolName, buildId);
        List<CheckerDetailHisEntity> bakCheckerList = fromCheckerList.stream().map(it -> {
            CheckerDetailHisEntity checkerDetailHisEntity = new CheckerDetailHisEntity();
            BeanUtils.copyProperties(it, checkerDetailHisEntity);
            checkerDetailHisEntity.setBuildId(buildId);
            return checkerDetailHisEntity;
        }).collect(Collectors.toList());
        checkerHisRepository.deleteByToolName(toolName);
        checkerHisRepository.saveAll(bakCheckerList);
    }

    @Override
    public String revertStatus(String userName, String toolName, ToolIntegratedStatus status) {
        if (status == ToolIntegratedStatus.T) {
            return "do nothing";
        }

        List<String> bakCheckerIdSet = checkerHisRepository.findByToolName(toolName)
            .stream()
            .map(CheckerDetailHisEntity::getCheckerKey)
            .collect(Collectors.toList());

        log.info("change checker in : {}, {}", toolName, bakCheckerIdSet);

        ToolIntegratedStatus targetStatus = status == ToolIntegratedStatus.G
            ? ToolIntegratedStatus.T : ToolIntegratedStatus.G;
        List<CheckerDetailEntity> originCheckerList =
            checkerRepository.findByToolNameAndCheckerVersionAndCheckerKeyIn(toolName, status.value(), bakCheckerIdSet);
        originCheckerList.forEach(it -> {
            it.setCheckerVersion(targetStatus.value());
        });

        checkerRepository.saveAll(originCheckerList);

        return String.format("batch revert checker successfully: %s, %s", toolName, status);
    }
}
