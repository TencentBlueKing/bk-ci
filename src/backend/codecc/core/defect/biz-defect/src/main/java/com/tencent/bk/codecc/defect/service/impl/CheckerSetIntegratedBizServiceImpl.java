package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetHisRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetProjectRelationshipRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetRepository;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetHisEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetProjectRelationshipEntity;
import com.tencent.bk.codecc.defect.service.ICheckerSetIntegratedBizService;
import com.tencent.bk.codecc.defect.service.IV3CheckerSetBizService;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.ToolIntegratedStatus;
import com.tencent.devops.common.util.GsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CheckerSetIntegratedBizServiceImpl implements ICheckerSetIntegratedBizService {

    @Autowired
    private CheckerSetRepository checkerSetRepository;

    @Autowired
    private CheckerSetHisRepository checkerSetHisRepository;

    @Autowired
    private CheckerSetProjectRelationshipRepository projectRelationshipRepository;

    @Autowired
    private IV3CheckerSetBizService v3CheckerSetBizService;

    @Override
    public String updateToStatus(String toolName,
                                 String buildId,
                                 ToolIntegratedStatus toStatus,
                                 String user,
                                 Set<String> checkerSetIds,
                                 Set<String> changeCheckerIds) {
        List<CheckerSetEntity> changedFromCheckerSetList = init(toolName, toStatus, checkerSetIds, changeCheckerIds);
        List<String> changedFromCheckerSetIdList =
                changedFromCheckerSetList.stream().map(CheckerSetEntity::getCheckerSetId).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(changedFromCheckerSetList)) {
            return "no change checker set for update, do nothing...";
        }

        List<CheckerSetEntity> newCheckerSetList = new ArrayList<>();
        if (toStatus == ToolIntegratedStatus.G) {
            List<CheckerSetEntity> toCheckerSetList =
                    checkerSetRepository.findByCheckerSetIdInAndVersion(changedFromCheckerSetIdList, toStatus.value());

            backup(toolName, toStatus, buildId, toCheckerSetList);

            checkerSetRepository.deleteAll(toCheckerSetList);

            newCheckerSetList = getNewCheckerSetForGray(changedFromCheckerSetList, user);

            checkerSetRepository.saveAll(newCheckerSetList);

            // 把旧规则集toCheckerSetList换成新规则newCheckerSetList，需要设置强制全量及告警状态
            Map<String, CheckerSetEntity> oldCheckerSetMap = toCheckerSetList.stream()
                    .collect(Collectors.toMap(CheckerSetEntity::getCheckerSetId, Function.identity(), (k, v) -> k));
            updateTaskAfterChangeCheckerSet(newCheckerSetList, oldCheckerSetMap, user);
        } else if (toStatus == ToolIntegratedStatus.P) {
            Map<String, CheckerSetEntity> checkerSetMap = getMaxVersionMap(changedFromCheckerSetIdList);

            backup(toolName, toStatus, buildId, checkerSetMap.values());

            newCheckerSetList = getNewCheckerSetForProd(changedFromCheckerSetList, checkerSetMap, user);

            checkerSetRepository.saveAll(newCheckerSetList);

            // 把旧规则集checkerSetMap换成新规则newCheckerSetList，需要设置强制全量及告警状态
            updateTaskAfterChangeCheckerSet(newCheckerSetList, checkerSetMap, user);
        }

        return String.format("batch update checker set successfully: %s, %s, %s", toolName, toStatus, newCheckerSetList);
    }

    private void updateTaskAfterChangeCheckerSet(List<CheckerSetEntity> newCheckerSetList, Map<String,
            CheckerSetEntity> oldCheckerSetMap, String user) {

        log.info("====================================newCheckerSetList======================================\n{}",
                GsonUtils.toJson(newCheckerSetList));
        log.info("====================================oldCheckerSetMap======================================\n{}",
                GsonUtils.toJson(oldCheckerSetMap));

        newCheckerSetList.forEach(newCheckerSet -> {
            String checkerSetId = newCheckerSet.getCheckerSetId();
            CheckerSetEntity oldCheckerSet = oldCheckerSetMap.get(checkerSetId);
            if (oldCheckerSet != null) {
                // 查询已关联此规则集，且选择了latest版本自动更新的项目数据
                List<CheckerSetProjectRelationshipEntity> projectRelationships = projectRelationshipRepository
                        .findByCheckerSetIdAndUselatestVersion(oldCheckerSet.getCheckerSetId(), true);
                v3CheckerSetBizService.updateTaskAfterChangeCheckerSet(newCheckerSet, oldCheckerSet,
                        projectRelationships, user);
            }
        });
    }

    private List<CheckerSetEntity> getNewCheckerSetForGray(List<CheckerSetEntity> changedFromCheckerSetList,
                                                           String user) {
        log.info("get gray data and change to gray status");

        return changedFromCheckerSetList.stream().map(testCheckerSet -> {
            testCheckerSet.setVersion(ToolIntegratedStatus.G.value());
            testCheckerSet.setEntityId(null);
            testCheckerSet.setLastUpdateTime(System.currentTimeMillis());
            testCheckerSet.setUpdatedBy(user);
            return testCheckerSet;
        }).collect(Collectors.toList());
    }

    private List<CheckerSetEntity> getNewCheckerSetForProd(List<CheckerSetEntity> changedFromCheckerSetList,
                                                           Map<String, CheckerSetEntity> checkerSetMap,
                                                           String user) {
        log.info("get gray data and change to prod status");

        return changedFromCheckerSetList.stream().map(it -> {
            CheckerSetEntity maxVersionCheckerSet = checkerSetMap.get(it.getCheckerSetId());
            if (maxVersionCheckerSet == null) {
                it.setVersion(1);
            } else {
                it.setEntityId(null);
                it.setVersion(maxVersionCheckerSet.getVersion() + 1);
            }
            it.setUpdatedBy(user);
            it.setLastUpdateTime(System.currentTimeMillis());
            it.setToolName(null);
            return it;
        }).collect(Collectors.toList());
    }


    private void backup(String toolName,
                        ToolIntegratedStatus toStatus,
                        String buildId,
                        Collection<CheckerSetEntity> toCheckerSetList) {
        List<CheckerSetHisEntity> hisCheckerSetEntities =
                checkerSetHisRepository.findByToolNameInAndVersion(toolName, toStatus.value());

        if (CollectionUtils.isNotEmpty(hisCheckerSetEntities)) {
            if (hisCheckerSetEntities.get(0).getBuildId().equals(buildId)) {
                log.info("is the same back up build id, do nothing: {}", toolName);
                return;
            }
        }

        log.info("back up checker set: {}, {}, {}, {}", toolName, toStatus, buildId, toCheckerSetList);

        List<CheckerSetHisEntity> bakCheckerSetList = toCheckerSetList.stream().map(it -> {
            CheckerSetHisEntity checkerSetHisEntity = new CheckerSetHisEntity();
            BeanUtils.copyProperties(it, checkerSetHisEntity);
            checkerSetHisEntity.setToolName(toolName);
            checkerSetHisEntity.setBuildId(buildId);

            return checkerSetHisEntity;
        }).collect(Collectors.toList());

        if (toStatus == ToolIntegratedStatus.G) {
            checkerSetHisRepository.deleteByToolNameAndVersion(toolName, toStatus.value());
        } else {
            checkerSetHisRepository.deleteByToolNameAndVersionNot(toolName, ToolIntegratedStatus.G.value());
        }
        checkerSetHisRepository.saveAll(bakCheckerSetList);
    }

    private List<CheckerSetEntity> init(String toolName, ToolIntegratedStatus toStatus,
                                        Set<String> checkerSetIds, Set<String> changeCheckerIds) {
        if (toStatus == ToolIntegratedStatus.T) {
            log.info("do nothing for checker set status init: {}", toolName);
            return new ArrayList<>();
        }
//
//        if (CollectionUtils.isEmpty(changeCheckerIds)) {
//            log.info("no changed checker, do nothing for init...: {}", toolName);
//            return new ArrayList<>();
//        }

        ToolIntegratedStatus fromStatus = toStatus == ToolIntegratedStatus.G
                ? ToolIntegratedStatus.T : ToolIntegratedStatus.G;

        log.info("get from data and change to status");
        List<CheckerSetEntity> fromCheckerSetList =
                checkerSetRepository.findByCheckerSetIdInAndVersion(checkerSetIds, fromStatus.value());
        return fromCheckerSetList;
        //        List<CheckerSetEntity> changedFromCheckerSetList = new ArrayList<>();
        //        fromCheckerSetList.forEach(it -> {
        //            for (CheckerPropsEntity checkerPropsEntity : it.getCheckerProps()) {
        //                if (changeCheckerIds.contains(checkerPropsEntity.getCheckerKey())) {
        //                    changedFromCheckerSetList.add(it);
        //                    break;
        //                }
        //            }
        //        });
        //
        //        return changedFromCheckerSetList;
    }

    @Override
    public String revertStatus(String toolName, ToolIntegratedStatus status, String user, Set<String> checkerSetIds) {
        if (status == ToolIntegratedStatus.T) {
            return "do nothing";
        }

        log.info("get backup checker set: {}, {}", checkerSetIds, status);
        List<CheckerSetEntity> bakCheckerSetList =
                checkerSetHisRepository.findByToolNameInAndVersion(toolName, status.value()).stream().map(it -> {
                    CheckerSetEntity checkerSetEntity = new CheckerSetEntity();
                    BeanUtils.copyProperties(it, checkerSetEntity);
                    return checkerSetEntity;
                }).collect(Collectors.toList());

        if (status == ToolIntegratedStatus.G) {
            List<CheckerSetEntity> bakGrayCheckerSetList = bakCheckerSetList.stream().map(it -> {
                CheckerSetEntity checkerSetEntity = new CheckerSetEntity();
                BeanUtils.copyProperties(it, checkerSetEntity);
                checkerSetEntity.setLastUpdateTime(System.currentTimeMillis());
                checkerSetEntity.setUpdatedBy(user);
                return checkerSetEntity;
            }).collect(Collectors.toList());

            List<CheckerSetEntity> oldCheckerSetList =
                    checkerSetRepository.findByCheckerSetIdInAndVersion(checkerSetIds, status.value());
            Map<String, CheckerSetEntity> oldCheckerSetMap = oldCheckerSetList.stream()
                    .collect(Collectors.toMap(CheckerSetEntity::getCheckerSetId, Function.identity(), (k, v) -> k));
            bakGrayCheckerSetList.forEach(it -> {
                CheckerSetEntity oldCheckerSet = oldCheckerSetMap.get(it.getCheckerSetId());
                if (oldCheckerSet != null) {
                    it.setEntityId(oldCheckerSet.getEntityId());
                }
            });
            checkerSetRepository.saveAll(bakGrayCheckerSetList);

            // 把旧规则集换成新规则，需要设置强制全量及告警状态
            updateTaskAfterChangeCheckerSet(bakGrayCheckerSetList, oldCheckerSetMap, user);
        } else if (status == ToolIntegratedStatus.P) {
            // use bak checker to cover the prod
            List<CheckerSetEntity> newProdCheckerSetList = new ArrayList<>();
            List<String> bakCheckerSetIdList =
                    bakCheckerSetList.stream().map(CheckerSetEntity::getCheckerSetId).collect(Collectors.toList());
            Map<String, CheckerSetEntity> maxVersionMap = getMaxVersionMap(bakCheckerSetIdList);
            bakCheckerSetList.stream().forEach(checkerSetEntity -> {
                CheckerSetEntity latestCheckerSet = maxVersionMap.get(checkerSetEntity.getCheckerSetId());

                if (latestCheckerSet != null) {
                    CheckerSetEntity newCheckerSetEntity = new CheckerSetEntity();
                    BeanUtils.copyProperties(checkerSetEntity, newCheckerSetEntity);
                    newCheckerSetEntity.setVersion(latestCheckerSet.getVersion());
                    newCheckerSetEntity.setEntityId(latestCheckerSet.getEntityId());
                    newCheckerSetEntity.setLastUpdateTime(System.currentTimeMillis());
                    newCheckerSetEntity.setUpdatedBy(user);
                    newProdCheckerSetList.add(newCheckerSetEntity);
                }
            });

            checkerSetRepository.saveAll(newProdCheckerSetList);

            // 把旧规则集换成新规则，需要设置强制全量及告警状态
            updateTaskAfterChangeCheckerSet(newProdCheckerSetList, maxVersionMap, user);
        }

        return String.format("batch revert checker set successfully: %s, %s, %s", toolName, status, checkerSetIds);
    }

    private Map<String, CheckerSetEntity> getMaxVersionMap(List<String> checkerSetIdList) {
        Map<String, CheckerSetEntity> checkerSetMap = new HashMap<>(8);
        // get the max checker set version
        checkerSetRepository.findByCheckerSetIdIn(checkerSetIdList).forEach(it -> {
            if (it.getVersion() > 0) {
                CheckerSetEntity checkerSetEntity = checkerSetMap.get(it.getCheckerSetId());
                if (checkerSetEntity == null || checkerSetEntity.getVersion() < it.getVersion()) {
                    checkerSetMap.put(it.getCheckerSetId(), it);
                }
            }
        });

        return checkerSetMap;
    }
}
