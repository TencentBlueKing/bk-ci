package com.tencent.bk.codecc.defect.service.impl;

import static com.tencent.devops.common.constant.ComConstants.ONCE_CHECKER_SET_KEY;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_TASK_CHECKER_CONFIG;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_IGNORE_CHECKER;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetProjectRelationshipRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetTaskRelationshipRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CheckerDetailDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CheckerSetDao;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerPropsEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetCatagoryEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetProjectRelationshipEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetTaskRelationshipEntity;
import com.tencent.bk.codecc.defect.service.IV3CheckerSetBizService;
import com.tencent.bk.codecc.defect.service.ToolBuildInfoService;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.vo.CheckerCommonCountVO;
import com.tencent.bk.codecc.defect.vo.CheckerCountListVO;
import com.tencent.bk.codecc.defect.vo.CheckerListQueryReq;
import com.tencent.bk.codecc.defect.vo.CheckerSetListQueryReq;
import com.tencent.bk.codecc.defect.vo.ConfigCheckersPkgReqVO;
import com.tencent.bk.codecc.defect.vo.OtherCheckerSetListQueryReq;
import com.tencent.bk.codecc.defect.vo.UpdateAllCheckerReq;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetCategory;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetSource;
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.bk.codecc.task.api.ServiceGrayToolProjectResource;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.api.ServiceToolRestResource;
import com.tencent.bk.codecc.task.vo.BatchRegisterVO;
import com.tencent.bk.codecc.task.vo.GrayToolProjectVO;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.checkerset.CheckerPropVO;
import com.tencent.devops.common.api.checkerset.CheckerSetCategoryVO;
import com.tencent.devops.common.api.checkerset.CheckerSetCodeLangVO;
import com.tencent.devops.common.api.checkerset.CheckerSetManagementReqVO;
import com.tencent.devops.common.api.checkerset.CheckerSetParamsVO;
import com.tencent.devops.common.api.checkerset.CheckerSetRelationshipVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVersionVO;
import com.tencent.devops.common.api.checkerset.CreateCheckerSetReqVO;
import com.tencent.devops.common.api.checkerset.V3UpdateCheckerSetReqExtVO;
import com.tencent.devops.common.api.checkerset.V3UpdateCheckerSetReqVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.CheckerConstants;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.ToolIntegratedStatus;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.service.utils.PageableUtils;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.util.List2StrUtil;
import com.tencent.devops.common.util.ListSortUtil;
import com.tencent.devops.common.util.ThreadPoolUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.CloneUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * V3规则集服务实现类
 *
 * @version V1.0
 * @date 2020/1/2
 */
@Slf4j
@Service
public class V3CheckerSetBizServiceImpl implements IV3CheckerSetBizService {
    /**
     * 规则集语言参数
     */
    private static final String KEY_LANG = "LANG";

    @Autowired
    private CheckerSetRepository checkerSetRepository;

    @Autowired
    private CheckerSetDao checkerSetDao;

    @Autowired
    private Client client;

    @Autowired
    private ToolBuildInfoService toolBuildInfoService;

    @Autowired
    private CheckerSetProjectRelationshipRepository checkerSetProjectRelationshipRepository;

    @Autowired
    private CheckerSetTaskRelationshipRepository checkerSetTaskRelationshipRepository;

    @Autowired
    private CheckerDetailDao checkerDetailDao;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private AuthExPermissionApi authExPermissionApi;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 创建规则集
     *
     * @param user
     * @param projectId
     * @param createCheckerSetReqVO
     * @return
     */
    @Override
    public void createCheckerSet(String user, String projectId, CreateCheckerSetReqVO createCheckerSetReqVO) {
        if (StringUtils.isEmpty(createCheckerSetReqVO.getCheckerSetId())
                || StringUtils.isEmpty(createCheckerSetReqVO.getCheckerSetName())) {
            String errMsgStr = "规则集ID、规则集名称";
            log.error("{}不能为空", errMsgStr);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{errMsgStr}, null);
        }

        // 校验规则集ID是否已存在
        checkIdDuplicate(createCheckerSetReqVO.getCheckerSetId());

        // 校验规则集名称在项目中是否已存在
        checkNameExistInProject(createCheckerSetReqVO.getCheckerSetName(), projectId);

        // 获取规则集基础信息
        long currentTime = System.currentTimeMillis();
        CheckerSetEntity checkerSetEntity = new CheckerSetEntity();
        BeanUtils.copyProperties(createCheckerSetReqVO, checkerSetEntity);
        checkerSetEntity.setCreateTime(currentTime);
        checkerSetEntity.setCreator(user);
        checkerSetEntity.setLastUpdateTime(currentTime);
        checkerSetEntity.setOfficial(CheckerConstants.CheckerSetOfficial.NOT_OFFICIAL.code());
        if (checkerSetEntity.getVersion() == null) {
            checkerSetEntity.setVersion(CheckerConstants.DEFAULT_VERSION);
        }
        checkerSetEntity.setEnable(CheckerConstants.CheckerSetEnable.ENABLE.code());
        checkerSetEntity.setProjectId(projectId);
        checkerSetEntity.setScope(CheckerConstants.CheckerSetScope.PRIVATE.code());

        checkerSetEntity.setDefaultCheckerSet(false);
        checkerSetEntity.setOfficial(CheckerConstants.CheckerSetOfficial.NOT_OFFICIAL.code());

        // 查询语言参数列表
        Result<List<BaseDataVO>> paramsResult =
                client.get(ServiceBaseDataResource.class).getParamsByType(ComConstants.KEY_CODE_LANG);
        if (paramsResult.isNotOk() || CollectionUtils.isEmpty(paramsResult.getData())) {
            log.error("param list is empty! param type: {}", ComConstants.KEY_CODE_LANG);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        List<BaseDataVO> codeLangParams = paramsResult.getData();
        checkerSetEntity.setCheckerSetLang(List2StrUtil.toString(getCodelangs(createCheckerSetReqVO.getCodeLang(),
                codeLangParams), ","));

        // 加入规则集类型中英文名称
        List<CheckerSetCatagoryEntity> catagoryEntities = getCatagoryEntities(createCheckerSetReqVO.getCatagories());
        checkerSetEntity.setCatagories(catagoryEntities);

        // 如果选择了基于某个规则集或者复制与某个规则集，则需要更新规则集中的规则
        if (StringUtils.isNotEmpty(createCheckerSetReqVO.getBaseCheckerSetId())) {
            CheckerSetEntity baseCheckerSet;
            if (createCheckerSetReqVO.getBaseCheckerSetVersion() == null
                    || createCheckerSetReqVO.getBaseCheckerSetVersion() == Integer.MAX_VALUE) {
                List<CheckerSetEntity> baseCheckerSets =
                        checkerSetRepository.findByCheckerSetId(createCheckerSetReqVO.getBaseCheckerSetId());
                baseCheckerSets.sort(((o1, o2) -> o2.getVersion().compareTo(o1.getVersion())));
                baseCheckerSet = baseCheckerSets.get(0);
            } else {
                baseCheckerSet = checkerSetRepository.findFirstByCheckerSetIdAndVersion(
                        createCheckerSetReqVO.getBaseCheckerSetId(),
                        createCheckerSetReqVO.getBaseCheckerSetVersion());
            }
            if (baseCheckerSet == null) {
                String errMsg = "找不到规则集，ID：" + createCheckerSetReqVO.getBaseCheckerSetId();
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
            checkerSetEntity.setCheckerProps(baseCheckerSet.getCheckerProps());
            checkerSetEntity.setInitCheckers(true);
        } else {
            checkerSetEntity.setInitCheckers(false);
        }

        // 入库
        checkerSetRepository.save(checkerSetEntity);

        // 保存规则集与项目的关系
        CheckerSetProjectRelationshipEntity relationshipEntity = new CheckerSetProjectRelationshipEntity();
        relationshipEntity.setCheckerSetId(checkerSetEntity.getCheckerSetId());
        relationshipEntity.setVersion(checkerSetEntity.getVersion());
        relationshipEntity.setProjectId(projectId);
        relationshipEntity.setCreatedBy(user);
        relationshipEntity.setCreatedDate(System.currentTimeMillis());
        relationshipEntity.setUselatestVersion(true);
        relationshipEntity.setDefaultCheckerSet(false);
        checkerSetProjectRelationshipRepository.save(relationshipEntity);
    }

    @Override
    public Boolean updateCheckersOfSetForAll(String user, UpdateAllCheckerReq updateAllCheckerReq) {
        CheckerListQueryReq checkerListQueryReq = updateAllCheckerReq.getCheckerListQueryReq();
        List<CheckerPropVO> checkerPropVOS;
        if (CollectionUtils.isEmpty(updateAllCheckerReq.getCheckerProps())) {
            checkerPropVOS = new ArrayList<>();
        } else {
            checkerPropVOS = updateAllCheckerReq.getCheckerProps();
        }
        List<CheckerDetailEntity> checkerDetailEntityList =
                checkerDetailDao.findByComplexCheckerCondition(checkerListQueryReq.getKeyWord(),
                        checkerListQueryReq.getCheckerLanguage(),
                        checkerListQueryReq.getCheckerCategory(),
                        checkerListQueryReq.getToolName(),
                        checkerListQueryReq.getTag(),
                        checkerListQueryReq.getSeverity(),
                        checkerListQueryReq.getEditable(),
                        checkerListQueryReq.getCheckerRecommend(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        ToolIntegratedStatus.P);
        if (CollectionUtils.isNotEmpty(checkerDetailEntityList)) {
            checkerDetailEntityList.forEach(checkerDetailEntity ->
            {
                if (checkerPropVOS.stream().noneMatch(checkerPropVO -> checkerPropVO.getCheckerKey().equals(checkerDetailEntity.getCheckerKey()) &&
                        checkerPropVO.getToolName().equals(checkerDetailEntity.getToolName()))) {
                    CheckerPropVO checkerPropVO = new CheckerPropVO();
                    checkerPropVO.setToolName(checkerDetailEntity.getToolName());
                    checkerPropVO.setCheckerKey(checkerDetailEntity.getCheckerKey());
                    checkerPropVO.setCheckerName(checkerDetailEntity.getCheckerName());
                    checkerPropVOS.add(checkerPropVO);
                }
            });
        }
        updateCheckersOfSet(checkerListQueryReq.getCheckerSetId(), user, checkerPropVOS, null);
        return true;

    }


    /**
     * 更新规则集中的规则
     *
     * @param checkerSetId
     * @param checkerProps
     * @param version
     * @return
     */
    @Override
    public void updateCheckersOfSet(String checkerSetId, String user,
                                    List<CheckerPropVO> checkerProps, Integer version) {
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetId(checkerSetId);
        if (CollectionUtils.isNotEmpty(checkerSetEntities)) {
            List<CheckerPropsEntity> checkerPropsEntities = Lists.newArrayList();
            if (CollectionUtils.isNotEmpty(checkerProps)) {
                for (CheckerPropVO checkerPropVO : checkerProps) {
                    CheckerPropsEntity checkerPropsEntity = new CheckerPropsEntity();
                    BeanUtils.copyProperties(checkerPropVO, checkerPropsEntity);
                    checkerPropsEntities.add(checkerPropsEntity);
                }
            }

            // 获取规则集的版本，T-测试，G-灰度，P-发布
            Integer checkerSetVersion = version != null ? version : getCheckerSetVersion(checkerProps);

            CheckerSetEntity checkerSetEntity = checkerSetEntities.get(0);

            // 还未初始化过的规则集需要设置初始化为true，并且不用生成新的规则集版本
            if (checkerSetEntities.size() == 1
                    && checkerSetEntity.getInitCheckers() != null && !checkerSetEntity.getInitCheckers()) {
                checkerSetEntity.setInitCheckers(true);
                if (checkerSetVersion != null) {
                    checkerSetEntity.setVersion(checkerSetVersion);
                }
            } else {
                /*
                 * 已经初始化的规则集：
                 * 1 新规则集是测试、灰度规则集，则
                 * 1.1 已经存在测试/灰度的规则集，则不增加新数据，直接找到旧的测试/灰度规则集，更新相应字段即可
                 * 1.2 不存在测试/灰度规则集，则找到最新的一个版本的规则集，在最新版本上更新相应字段，然后生成一条新的测试/灰度规则集
                 * 2 新规则集是普通规则集，生成一条新版本的规则集（version + 1）
                 */
                checkerSetEntity = checkerSetEntities.stream()
                        .max(Comparator.comparing(CheckerSetEntity::getVersion)).get();
                if (checkerSetVersion != null) {
                    CheckerSetEntity grayCheckerSet = checkerSetEntities.stream()
                            .filter(it -> checkerSetVersion.equals(it.getVersion())).findFirst().orElse(null);
                    if (grayCheckerSet != null) {
                        checkerSetEntity = grayCheckerSet;
                    } else {
                        checkerSetEntity.setEntityId(null);
                    }
                    checkerSetEntity.setVersion(checkerSetVersion);
                } else {
                    checkerSetEntity.setEntityId(null);
                    checkerSetEntity.setVersion(checkerSetEntity.getVersion() + 1);
                }
            }

            // 更新前，先保留规则集的老版本规则列表
            List<CheckerPropsEntity> oldCheckerProps = checkerSetEntity.getCheckerProps();
            checkerSetEntity.setCheckerProps(checkerPropsEntities);
            checkerSetEntity.setUpdatedBy(user);
            checkerSetEntity.setLastUpdateTime(System.currentTimeMillis());

            // 新规则集数据入库
            checkerSetRepository.save(checkerSetEntity);

            // 查询已关联此规则集，且选择了latest版本自动更新的项目数据
            List<CheckerSetProjectRelationshipEntity> projectRelationships =
                    checkerSetProjectRelationshipRepository.findByCheckerSetIdAndUselatestVersion(checkerSetId, true);
            if (CollectionUtils.isNotEmpty(projectRelationships)) {

                Integer newCheckerSetVersion = checkerSetEntity.getVersion();

                // 如果新规则集版本不是测试或灰度的规则集，则需要更新项目规则集版本
                if (newCheckerSetVersion != ToolIntegratedStatus.T.value()
                        && newCheckerSetVersion != ToolIntegratedStatus.G.value()) {
                    projectRelationships.forEach(it -> it.setVersion(newCheckerSetVersion));
                    checkerSetProjectRelationshipRepository.saveAll(projectRelationships);

                    handleNormalProject(checkerSetEntity, projectRelationships, user);
                } else {
                    // 如果是测试或灰度规则集，且项目是测试或灰度项目，则设置测试或灰度的项目为强制全量，且更新工具
                    CheckerSetEntity fromCheckerSet = new CheckerSetEntity();
                    fromCheckerSet.setCheckerProps(oldCheckerProps);
                    updateTaskAfterChangeCheckerSet(checkerSetEntity, fromCheckerSet, projectRelationships, user);
                }
            }
        }
    }

    /**
     * 刷新本次规则更新涉及的任务的信息。包括强制全量标志，工具，告警状态等
     *
     * @param checkerSetEntity
     * @param fromCheckerSet
     * @param projectRelationships
     * @param user
     */
    @Override
    public void updateTaskAfterChangeCheckerSet(CheckerSetEntity checkerSetEntity, CheckerSetEntity fromCheckerSet,
                                                List<CheckerSetProjectRelationshipEntity> projectRelationships,
                                                String user) {
        String checkerSetId = checkerSetEntity.getCheckerSetId();

        // 获取灰度项目清单
        Map<String, Integer> grayToolProjectMap = getGrayToolProjectMap(projectRelationships);

        Set<String> needRefreshProjects = projectRelationships.stream()
                .filter(it -> {
                    Integer version = checkerSetEntity.getVersion();
                    Integer grayStatus = grayToolProjectMap.get(it.getProjectId());
                    if (version < 0 && version.equals(grayStatus)) {
                        // 测试/灰度的规则集，只需要刷测试/灰度的项目
                        return true;
                    } else if (version > 0 && (grayStatus == null || grayStatus == 0)) {
                        // 正式的规则集，只需要刷正式的项目
                        return true;
                    }
                    return false;
                })
                .map(it -> it.getProjectId()).collect(Collectors.toSet());

        if (CollectionUtils.isNotEmpty(needRefreshProjects)) {
            List<CheckerSetTaskRelationshipEntity> taskRelationshipEntities =
                    checkerSetTaskRelationshipRepository.findByCheckerSetIdAndProjectIdIn(checkerSetId,
                            needRefreshProjects);
            List<CheckerSetEntity> fromCheckerSets = Lists.newArrayList(fromCheckerSet);
            List<CheckerSetEntity> toCheckerSets = Lists.newArrayList(checkerSetEntity);
            // 对各任务设置强制全量扫描标志，并修改告警状态
            ThreadPoolUtil.addRunnableTask(() -> {
                taskRelationshipEntities.forEach(it -> {
                    Long taskId = it.getTaskId();

                    setForceFullScanAndUpdateDefectAndToolStatus(taskId, fromCheckerSets, toCheckerSets);

                    Set<String> updatedToolSet = Sets.newHashSet(ComConstants.Tool.CLOC.name());
                    for (CheckerPropsEntity checkerPropsEntity : toCheckerSets.get(0).getCheckerProps()) {
                        if (StringUtils.isNotBlank(checkerPropsEntity.getToolName())) {
                            updatedToolSet.add(checkerPropsEntity.getToolName());
                        }
                    }

                    updateTools(user, taskId, updatedToolSet);
                });
            });
        }
    }

    /**
     * 处理本次规则更新涉及的普通项目
     *
     * @param checkerSetEntity
     * @param projectRelationships
     * @param user
     */
    private void handleNormalProject(CheckerSetEntity checkerSetEntity,
                                     List<CheckerSetProjectRelationshipEntity> projectRelationships, String user) {
        String checkerSetId = checkerSetEntity.getCheckerSetId();

        // 获取灰度项目清单
        Map<String, Integer> grayToolProjectMap = getGrayToolProjectMap(projectRelationships);

        // 找出普通项目之前关联的规则集版本映射
        Map<String, Integer> oldCheckerSetVersionMap = Maps.newHashMap();
        projectRelationships.forEach(it -> {
            Integer grayToolProjectStatus = grayToolProjectMap.get(it.getProjectId());
            if (grayToolProjectStatus == null || grayToolProjectStatus == 0) {
                oldCheckerSetVersionMap.put(it.getProjectId(), it.getVersion());
            }
        });
        if (MapUtils.isNotEmpty(oldCheckerSetVersionMap)) {
            // 刷新告警状态并设置强制全量扫描标志(只有使用了latest规则集的任务才需要刷新)
            Map<Long, Map<String, Integer>> currentTaskCheckerSetMap = Maps.newHashMap();
            List<CheckerSetTaskRelationshipEntity> taskRelationshipEntities =
                    checkerSetTaskRelationshipRepository.findByCheckerSetIdAndProjectIdIn(checkerSetId,
                            oldCheckerSetVersionMap.keySet());
            taskRelationshipEntities.stream()
                    .filter(it -> oldCheckerSetVersionMap.get(it.getProjectId()) != null)
                    .forEach(it -> {
                        currentTaskCheckerSetMap.computeIfAbsent(it.getTaskId(), k -> Maps.newHashMap());
                        currentTaskCheckerSetMap.get(it.getTaskId()).put(checkerSetId,
                                oldCheckerSetVersionMap.get(it.getProjectId()));
                    });

            Map<Long, Map<String, Integer>> updatedTaskCheckerSetMap = Maps.newHashMap();
            updatedTaskCheckerSetMap.putAll(currentTaskCheckerSetMap);

            for (Map.Entry<Long, Map<String, Integer>> entry : updatedTaskCheckerSetMap.entrySet()) {
                if (entry.getValue().get(checkerSetId) != null) {
                    entry.getValue().put(checkerSetId, checkerSetEntity.getVersion());
                }
            }

            // 对各任务设置强制全量扫描标志，并修改告警状态
            ThreadPoolUtil.addRunnableTask(() -> setForceFullScanAndUpdateDefectAndToolStatus(
                    currentTaskCheckerSetMap, updatedTaskCheckerSetMap, user));
        }
    }

    /**
     * 获取灰度项目清单
     *
     * @param relationships
     * @return
     */
    @NotNull
    private Map<String, Integer> getGrayToolProjectMap(List<CheckerSetProjectRelationshipEntity> relationships) {
        Set<String> projectSet = relationships.stream().map(it -> it.getProjectId()).collect(Collectors.toSet());
        Result<List<GrayToolProjectVO>> result = client.get(ServiceGrayToolProjectResource.class)
                .getGrayToolProjectByProjectIds(projectSet);
        if (result.isNotOk()) {
            log.error("getGrayToolProjectByProjectIds fail.");
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        Map<String, Integer> grayToolProjectVOMap = new HashMap<>();
        List<GrayToolProjectVO> grayToolProjectVOList = result.getData();
        if (CollectionUtils.isNotEmpty(grayToolProjectVOList)) {
            grayToolProjectVOList.forEach(it -> grayToolProjectVOMap.put(it.getProjectId(), it.getStatus()));
        }
        return grayToolProjectVOMap;
    }

    /**
     * 获取规则集的版本，T-测试，G-灰度，P-发布
     * 1.如果规则集包含 T-测试 版本的规则，那么优先设置规则集为测试规则集
     * 2.如果规则集包含 G-灰度 版本的规则，那么优先设置规则集为灰度规则集
     * 3.否则规则集为正式规则集
     *
     * @param checkerProps
     * @return
     */
    @Nullable
    private Integer getCheckerSetVersion(List<CheckerPropVO> checkerProps) {
        Integer checkerSetVersion = null;
        Map<String, List<CheckerPropVO>> toolCheckerMap =
                checkerProps.stream().collect(Collectors.groupingBy(CheckerPropVO::getToolName));
        List<CheckerDetailEntity> checkerList = checkerDetailDao.findByToolNameAndCheckers(toolCheckerMap);
        Map<Integer, List<CheckerDetailEntity>> checkerVersionMap =
                checkerList.stream().collect(Collectors.groupingBy(CheckerDetailEntity::getCheckerVersion));
        if (checkerVersionMap.containsKey(ToolIntegratedStatus.T.value())) {
            checkerSetVersion = ToolIntegratedStatus.T.value();
        } else if (checkerVersionMap.containsKey(ToolIntegratedStatus.G.value())) {
            checkerSetVersion = ToolIntegratedStatus.G.value();
        }
        return checkerSetVersion;
    }

    /**
     * 查询规则集列表
     *
     * @param projectId
     * @param queryCheckerSetReq
     * @return
     */
    @Override
    public Page<CheckerSetVO> getOtherCheckerSets(String projectId, OtherCheckerSetListQueryReq queryCheckerSetReq) {
        if (null == queryCheckerSetReq.getSortType()) {
            queryCheckerSetReq.setSortType(Sort.Direction.DESC);
        }

        if (StringUtils.isEmpty(queryCheckerSetReq.getSortField())) {
            queryCheckerSetReq.setSortField("task_usage");
        }
        int pageNum = Math.max(queryCheckerSetReq.getPageNum() - 1, 0);
        int pageSize = queryCheckerSetReq.getPageSize() <= 0 ? 10 : queryCheckerSetReq.getPageSize();
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(queryCheckerSetReq.getSortType(),
                queryCheckerSetReq.getSortField()));

        // 先查出项目已安装的规则集列表
        Set<String> projectCheckerSetIds = Sets.newHashSet();
        List<CheckerSetProjectRelationshipEntity> projectRelationshipEntities =
                checkerSetProjectRelationshipRepository.findByProjectId(projectId);
        Map<String, Boolean> defaultCheckerSetMap;
        if (CollectionUtils.isNotEmpty(projectRelationshipEntities)) {
            defaultCheckerSetMap = projectRelationshipEntities.stream().collect(Collector.of(HashMap::new, (k, v) ->
                            k.put(v.getCheckerSetId(), v.getDefaultCheckerSet()), (k, v) -> v,
                    Collector.Characteristics.IDENTITY_FINISH
            ));
            for (CheckerSetProjectRelationshipEntity checkerSetProjectRelationshipEntity :
                    projectRelationshipEntities) {
                projectCheckerSetIds.add(checkerSetProjectRelationshipEntity.getCheckerSetId());
            }
        } else {
            defaultCheckerSetMap = new HashMap<>();
        }

        Result<GrayToolProjectVO> grayResult = client.get(ServiceGrayToolProjectResource.class)
                .getGrayToolProjectInfoByProjrctId(projectId);

        if (grayResult.isNotOk() || grayResult.getData() == null) {
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
        }
        int toolIntegratedStatus = grayResult.getData().getStatus();

        List<CheckerSetEntity> checkerSetEntities =
                checkerSetDao.findMoreByCondition(queryCheckerSetReq.getQuickSearch(),
                        queryCheckerSetReq.getCheckerSetLanguage(), queryCheckerSetReq.getCheckerSetCategory(),
                        projectCheckerSetIds,
                        queryCheckerSetReq.getProjectInstalled(), toolIntegratedStatus, pageable);

        if (CollectionUtils.isEmpty(checkerSetEntities)) {
            return new PageImpl<>(Lists.newArrayList(), pageable, 0);
        }

        List<CheckerSetVO> result = checkerSetEntities.stream().map(checkerSetEntity ->
        {
            CheckerSetVO checkerSetVO = new CheckerSetVO();
            BeanUtils.copyProperties(checkerSetEntity, checkerSetVO, "checkerProps");
            checkerSetVO.setCodeLangList(List2StrUtil.fromString(checkerSetEntity.getCheckerSetLang(), ","));
            checkerSetVO.setToolList(Sets.newHashSet());
            if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())) {
                for (CheckerPropsEntity checkerPropsEntity : checkerSetEntity.getCheckerProps()) {
                    checkerSetVO.getToolList().add(checkerPropsEntity.getToolName());
                }
            }
            int checkerCount = checkerSetEntity.getCheckerProps() != null
                    ? checkerSetEntity.getCheckerProps().size() : 0;
            checkerSetVO.setCheckerCount(checkerCount);
            if (CheckerSetSource.DEFAULT.name().equals(checkerSetEntity.getCheckerSetSource())
                    || CheckerSetSource.RECOMMEND.name().equals(checkerSetEntity.getCheckerSetSource())
                    || projectCheckerSetIds.contains(checkerSetEntity.getCheckerSetId())) {
                checkerSetVO.setProjectInstalled(true);
            } else {
                checkerSetVO.setProjectInstalled(false);
            }
            //设置默认标签
            String checkerSetSource = checkerSetVO.getCheckerSetSource();
            checkerSetVO.setDefaultCheckerSet((CheckerSetSource.DEFAULT.name().equals(checkerSetSource)
                    && null == defaultCheckerSetMap.get(checkerSetVO.getCheckerSetId())
                    || (null != defaultCheckerSetMap.get(checkerSetVO.getCheckerSetId())
                    && defaultCheckerSetMap.get(checkerSetVO.getCheckerSetId()))));
            return checkerSetVO;
        }).collect(Collectors.toList());

        long total = pageNum * pageSize + result.size() + 1;

        //封装分页类
        return new PageImpl<>(result, pageable, total);
    }

    /**
     * 查询规则集列表
     *
     * @param queryCheckerSetReq
     * @return
     */
    @Override
    public List<CheckerSetVO> getCheckerSetsOfProject(CheckerSetListQueryReq queryCheckerSetReq) {
        log.info("start to get checker set of project: {}", queryCheckerSetReq.getProjectId());

        String projectId = queryCheckerSetReq.getProjectId();
        List<CheckerSetProjectRelationshipEntity> checkerSetRelationshipRepositoryList =
                checkerSetProjectRelationshipRepository.findByProjectId(projectId);

        log.info("find project relationship of project: {}", queryCheckerSetReq.getProjectId());

        Set<String> checkerSetIds;
        Map<String, Integer> checkerSetVersionMap = Maps.newHashMap();
        Map<String, Boolean> checkerSetDefaultMap = Maps.newHashMap();
        Set<String> latestVersionCheckerSets = Sets.newHashSet();
        if (CollectionUtils.isEmpty(checkerSetRelationshipRepositoryList)) {
            checkerSetIds = new HashSet<>();
        } else {
            checkerSetIds = checkerSetRelationshipRepositoryList.stream().
                    map(CheckerSetProjectRelationshipEntity::getCheckerSetId).
                    collect(Collectors.toSet());
            for (CheckerSetProjectRelationshipEntity projectRelationshipEntity : checkerSetRelationshipRepositoryList) {
                checkerSetVersionMap.put(projectRelationshipEntity.getCheckerSetId(),
                        projectRelationshipEntity.getVersion());
                checkerSetDefaultMap.put(projectRelationshipEntity.getCheckerSetId(),
                        projectRelationshipEntity.getDefaultCheckerSet());
                if (projectRelationshipEntity.getUselatestVersion() != null
                        && projectRelationshipEntity.getUselatestVersion()) {
                    latestVersionCheckerSets.add(projectRelationshipEntity.getCheckerSetId());
                }
            }
        }

        log.info("find checkerset by complex condition of project: {}", queryCheckerSetReq.getProjectId());

        Pageable pageable = null;
        if (queryCheckerSetReq.getPageNum() != null && queryCheckerSetReq.getPageSize() != null) {
            Optional<Sort.Direction> direction = Sort.Direction.fromOptionalString(queryCheckerSetReq.getSortType());
            pageable = PageableUtils.getPageable(queryCheckerSetReq.getPageNum(),
                    queryCheckerSetReq.getPageSize(),
                    queryCheckerSetReq.getSortField(),
                    direction.orElse(null),
                    "task_usage");
        }
        List<CheckerSetEntity> pCheckerSetEntityList =
                checkerSetDao.findByComplexCheckerSetCondition(queryCheckerSetReq.getKeyWord(),
                        checkerSetIds, queryCheckerSetReq.getCheckerSetLanguage(),
                        queryCheckerSetReq.getCheckerSetCategory(),
                        queryCheckerSetReq.getToolName(), queryCheckerSetReq.getCheckerSetSource(),
                        queryCheckerSetReq.getCreator(), true,
                        pageable, true);

        Result<GrayToolProjectVO> result = client.get(ServiceGrayToolProjectResource.class)
                .getGrayToolProjectInfoByProjrctId(projectId);

        if (result.isNotOk() || result.getData() == null) {
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
        }

        boolean isP = result.getData().getStatus() == ToolIntegratedStatus.P.value();
        int toolIntegratedStatus = result.getData().getStatus();
        List<CheckerSetEntity> checkerSetEntityList = new ArrayList<>();
        if (!isP) {
            checkerSetEntityList = checkerSetRepository.findByCheckerSetIdInAndVersion(
                    pCheckerSetEntityList.stream().map(CheckerSetEntity::getCheckerSetId).collect(Collectors.toList()),
                    toolIntegratedStatus);
        }

        if (CollectionUtils.isEmpty(pCheckerSetEntityList)) {
            return new ArrayList<>();
        }

        // 查询使用量
        log.info("find checkerset usage of project: {}", queryCheckerSetReq.getProjectId());
        List<CheckerSetTaskRelationshipEntity> checkerSetTaskRelationshipEntityList =
                checkerSetTaskRelationshipRepository.findByProjectId(projectId);
        Map<String, Long> checkerSetCountMap =
                checkerSetTaskRelationshipEntityList.stream().filter(checkerSetTaskRelationshipEntity ->
                        StringUtils.isNotBlank(checkerSetTaskRelationshipEntity.getCheckerSetId()))
                        .collect(Collectors.groupingBy(CheckerSetTaskRelationshipEntity::getCheckerSetId,
                                Collectors.counting()));

        //按任务使用量排序
        log.info("sort checkerset by usage or time of project: {}", queryCheckerSetReq.getProjectId());
        if (CheckerConstants.CheckerSetSortField.TASK_USAGE.name().equals(queryCheckerSetReq.getSortField())) {
            //要去除版本信息，设置到versionList字段里面
            Map<String, CheckerSetEntity> finalCheckerSetEntityMap = checkerSetEntityList.stream()
                    .collect(Collectors.toMap(CheckerSetEntity::getCheckerSetId, entry -> entry, (v1, v2) -> v2));
            return pCheckerSetEntityList.stream().filter(checkerSetEntity ->
                    judgeQualifiedCheckerSet(null, null, null,
                            queryCheckerSetReq.getCheckerSetSource(),
                            checkerSetEntity))
                    .collect(Collectors.groupingBy(CheckerSetEntity::getCheckerSetId)).entrySet().stream()
                    .map(entry -> getCheckerSetVO(entry,
                            checkerSetVersionMap,
                            checkerSetDefaultMap,
                            checkerSetCountMap,
                            latestVersionCheckerSets,
                            finalCheckerSetEntityMap,
                            isP,
                            toolIntegratedStatus))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingLong(o -> sortByOfficialProps(o) + (checkerSetCountMap
                            .containsKey(o.getCheckerSetId()) ? -checkerSetCountMap.get(o.getCheckerSetId()) : 0L)))
                    .collect(Collectors.toList());
        }
        //按创建时间倒序(默认)
        else {
            if (StringUtils.isEmpty(queryCheckerSetReq.getSortType())) {
                queryCheckerSetReq.setSortType(Sort.Direction.DESC.name());
            }
            Long coefficient = queryCheckerSetReq.getSortType().equals(Sort.Direction.ASC.name()) ? 1L : -1L;
            Map<String, CheckerSetEntity> finalCheckerSetEntityMap = checkerSetEntityList.stream()
                    .collect(Collectors.toMap(CheckerSetEntity::getCheckerSetId, entry -> entry, (v1, v2) -> v2));
            return pCheckerSetEntityList.stream().filter(checkerSetEntity ->
                    judgeQualifiedCheckerSet(null, null, null,
                            queryCheckerSetReq.getCheckerSetSource(),
                            checkerSetEntity))
                    .collect(Collectors.groupingBy(CheckerSetEntity::getCheckerSetId)).entrySet().stream()
                    .map(entry -> getCheckerSetVO(entry,
                            checkerSetVersionMap,
                            checkerSetDefaultMap,
                            checkerSetCountMap,
                            latestVersionCheckerSets,
                            finalCheckerSetEntityMap,
                            isP,
                            toolIntegratedStatus))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingLong(o -> sortByOfficialProps(o) + coefficient * o.getCreateTime()))
                    .collect(Collectors.toList());
        }
    }


    private Long sortByOfficialProps(CheckerSetVO checkerSetVO) {
        Long sortNum = (long) Integer.MAX_VALUE;
        if (null != checkerSetVO.getDefaultCheckerSet() && checkerSetVO.getDefaultCheckerSet()) {
            sortNum = sortNum + ((long) Integer.MAX_VALUE * -1000000);
        }
        if (CheckerSetSource.DEFAULT.name().equals(checkerSetVO.getCheckerSetSource())) {
            sortNum = sortNum + ((long) Integer.MAX_VALUE * -100000);
        }
        if (CheckerSetSource.RECOMMEND.name().equals(checkerSetVO.getCheckerSetSource())) {
            sortNum = sortNum + ((long) Integer.MAX_VALUE * -10000);
        }
        return sortNum;
    }

    @Override
    public Page<CheckerSetVO> getCheckerSetsOfProjectPage(CheckerSetListQueryReq queryCheckerSetReq) {
        if (null == queryCheckerSetReq.getSortType()) {
            queryCheckerSetReq.setSortType(Sort.Direction.DESC.name());
        }

        if (StringUtils.isEmpty(queryCheckerSetReq.getSortField())) {
            queryCheckerSetReq.setSortField("task_usage");
        }

        // 获取结果
        List<CheckerSetVO> result = getCheckerSetsOfProject(queryCheckerSetReq);

        log.info("finish to get checker set of project: {}", queryCheckerSetReq.getProjectId());

        //封装分页类
        int pageNum = Math.max(queryCheckerSetReq.getPageNum() - 1, 0);
        int pageSize = queryCheckerSetReq.getPageSize() <= 0 ? 10 : queryCheckerSetReq.getPageSize();
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(queryCheckerSetReq.getSortType(),
                queryCheckerSetReq.getSortField()));
        long total = pageNum * pageSize + result.size() + 1;
        return new PageImpl<>(result, pageable, total);
    }

    @Override
    public Map<String, List<CheckerSetVO>> getAvailableCheckerSetsOfProject(String projectId) {
        Map<String, List<CheckerSetVO>> resultCheckerSetMap = new LinkedHashMap<>();
        for (CheckerSetSource checkerSetSource : CheckerSetSource.values()) {
            resultCheckerSetMap.put(checkerSetSource.getName(), new ArrayList<>());
        }

        // 根据项目ID查询非旧插件规则集
        Result<GrayToolProjectVO> result = client.get(ServiceGrayToolProjectResource.class)
                .getGrayToolProjectInfoByProjrctId(projectId);

        if (result.isNotOk() || result.getData() == null) {
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
        }

        List<Boolean> legacyList = new ArrayList<>();
        legacyList.add(false);
        legacyList.add(true);
        int toolIntegratedStatus = result.getData().getStatus();
        List<CheckerSetEntity> filteredCheckerSetList = findAvailableCheckerSetsByProject(projectId, legacyList, toolIntegratedStatus);
        List<CheckerSetProjectRelationshipEntity> projectRelationshipEntities =
                checkerSetProjectRelationshipRepository.findByProjectId(projectId);
        Map<String, CheckerSetProjectRelationshipEntity> checkerSetRelationshipMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(projectRelationshipEntities)) {
            for (CheckerSetProjectRelationshipEntity projectRelationshipEntity : projectRelationshipEntities) {
                checkerSetRelationshipMap.put(projectRelationshipEntity.getCheckerSetId(), projectRelationshipEntity);
            }
        }
        if (CollectionUtils.isNotEmpty(filteredCheckerSetList)) {
            for (CheckerSetEntity checkerSetEntity : filteredCheckerSetList) {
                CheckerSetProjectRelationshipEntity projectRelationshipEntity =
                        checkerSetRelationshipMap.get(checkerSetEntity.getCheckerSetId());
                if ((projectRelationshipEntity != null && null != projectRelationshipEntity.getDefaultCheckerSet()
                        && projectRelationshipEntity.getDefaultCheckerSet()) || (CheckerSetSource.DEFAULT.name().
                        equals(checkerSetEntity.getCheckerSetSource()) && null == projectRelationshipEntity)) {
                    checkerSetEntity.setDefaultCheckerSet(true);
                } else {
                    checkerSetEntity.setDefaultCheckerSet(false);
                }
            }
        }

        if (CollectionUtils.isEmpty(filteredCheckerSetList)) {
            return resultCheckerSetMap;
        }

        //官方优选 官方推荐版本
        Map<String, Integer> officialMap = filteredCheckerSetList.stream().filter(checkerSetEntity ->
                Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name()).contains(checkerSetEntity.getCheckerSetSource())).
                collect(Collectors.groupingBy(CheckerSetEntity::getCheckerSetId)).entrySet().stream().
                collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().max(Comparator.comparingInt(CheckerSetEntity::getVersion)).orElse(new CheckerSetEntity()).getVersion()));


        //进行过滤，去掉规则为空、单语言的规则集
        filteredCheckerSetList = filteredCheckerSetList.stream().filter(checkerSetEntity ->
                CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())
                        && (StringUtils.isEmpty(checkerSetEntity.getCheckerSetLang()) || !checkerSetEntity.getCheckerSetLang().contains(ComConstants.STRING_SPLIT))).collect(Collectors.toList());
        // 查询语言参数列表
        Result<List<BaseDataVO>> paramsResult =
                client.get(ServiceBaseDataResource.class).getParamsByType(ComConstants.KEY_CODE_LANG);
        if (paramsResult.isNotOk() || CollectionUtils.isEmpty(paramsResult.getData())) {
            log.error("param list is empty! param type: {}", ComConstants.KEY_CODE_LANG);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        List<BaseDataVO> codeLangParams = paramsResult.getData();

        //按使用量排序
        List<CheckerSetTaskRelationshipEntity> checkerSetTaskRelationshipEntityList =
                checkerSetTaskRelationshipRepository.findByProjectId(projectId);
        Map<String, Long> checkerSetCountMap = checkerSetTaskRelationshipEntityList.stream().
                collect(Collectors.groupingBy(CheckerSetTaskRelationshipEntity::getCheckerSetId,
                        Collectors.counting()));
        filteredCheckerSetList.stream()
                .sorted(Comparator.comparingLong(o -> checkerSetCountMap.containsKey(o.getCheckerSetId())
                        ? -checkerSetCountMap.get(o.getCheckerSetId()) : 0L))
                .forEach(checkerSetEntity -> {
                    if (CheckerSetSource.DEFAULT.name().equals(checkerSetEntity.getCheckerSetSource()) ||
                            CheckerSetSource.RECOMMEND.name().equals(checkerSetEntity.getCheckerSetSource())) {
                        resultCheckerSetMap.compute(
                                CheckerSetSource.valueOf(checkerSetEntity.getCheckerSetSource()).getName(), (k, v) -> {
                                    if (null == v) {
                                        return new ArrayList<>();
                                    } else {
                                        if (!checkerSetEntity.getVersion().equals(
                                                officialMap.get(checkerSetEntity.getCheckerSetId()))) {
                                            return v;
                                        }
                                        v.add(handleCheckerSetForCateList(checkerSetEntity, codeLangParams));
                                        return v;
                                    }
                                }
                        );
                    } else {
                        resultCheckerSetMap.compute(CheckerSetSource.SELF_DEFINED.getName(), (k, v) -> {
                            if (null == v) {
                                return new ArrayList<>();
                            } else {
                                v.add(handleCheckerSetForCateList(checkerSetEntity, codeLangParams));
                                return v;
                            }
                        });
                    }
                });
        return resultCheckerSetMap;
    }


    private CheckerSetVO handleCheckerSetForCateList(CheckerSetEntity checkerSetEntity,
                                                     List<BaseDataVO> codeLangParams) {
        CheckerSetVO checkerSetVO = new CheckerSetVO();
        BeanUtils.copyProperties(checkerSetEntity, checkerSetVO);
        if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())) {
            checkerSetVO.setToolList(checkerSetEntity.getCheckerProps().stream().
                    map(CheckerPropsEntity::getToolName).collect(Collectors.toSet()));
            checkerSetVO.setCheckerCount(checkerSetEntity.getCheckerProps().size());
        }
        List<String> codeLangs = Lists.newArrayList();
        for (BaseDataVO codeLangParam : codeLangParams) {
            int paramCodeInt = Integer.valueOf(codeLangParam.getParamCode());
            if (null != checkerSetVO.getCodeLang() && (checkerSetVO.getCodeLang() & paramCodeInt) != 0) {
                // 蓝盾流水线使用的是语言别名的第一个值作为语言的ID来匹配的
                codeLangs.add(new JSONArray(codeLangParam.getParamExtend2()).getString(0));
            }
        }
        checkerSetVO.setCodeLangList(codeLangs);

        return checkerSetVO;
    }

    private List<String> getCodelangs(long codeLang, List<BaseDataVO> codeLangParams) {
        List<String> codeLangs = Lists.newArrayList();
        for (BaseDataVO codeLangParam : codeLangParams) {
            int paramCodeInt = Integer.valueOf(codeLangParam.getParamCode());
            if ((codeLang & paramCodeInt) != 0) {
                codeLangs.add(codeLangParam.getParamName());
            }
        }
        return codeLangs;
    }

    private CheckerSetVO getCheckerSetVO(Map.Entry<String, List<CheckerSetEntity>> entry,
                                         Map<String, Integer> versionMap, Map<String, Boolean> defaultMap,
                                         Map<String, Long> checkerSetCountMap, Set<String> latestVersionCheckerSets,
                                         Map<String, CheckerSetEntity> checkerSetEntityMap, boolean isP, int version) {
        List<CheckerSetEntity> checkerSetEntities = entry.getValue();

        CheckerSetVO checkerSetVO = new CheckerSetVO();
        checkerSetVO.setToolList(Sets.newHashSet());
        CheckerSetEntity selectedCheckerSet = checkerSetEntities.stream().
                filter(checkerSetEntity ->
                {
                    if (null != versionMap.get(checkerSetEntity.getCheckerSetId())) {
                        return (checkerSetEntity.getVersion().equals(versionMap.get(checkerSetEntity.getCheckerSetId()))
                                || (!isP && latestVersionCheckerSets.contains(checkerSetEntity.getCheckerSetId())));
                    } else {
                        return true;
                    }

                }).
                max(Comparator.comparingInt(CheckerSetEntity::getVersion)).
                orElse(null);

        if (selectedCheckerSet == null
                || (isP && (selectedCheckerSet.getVersion() == ToolIntegratedStatus.G.value()
                || selectedCheckerSet.getVersion() == ToolIntegratedStatus.T.value()))) {
            return null;
        }

        // 如果是 灰度/测试 项目，并且当前规则集有 灰度/测试 版本，并且当前规则集选的是 latest 版本，将规则集设置为 灰度/测试 版本
        if (!isP && checkerSetEntityMap.get(selectedCheckerSet.getCheckerSetId()) != null
                && latestVersionCheckerSets.contains(selectedCheckerSet.getCheckerSetId())) {
            selectedCheckerSet = checkerSetEntityMap.get(selectedCheckerSet.getCheckerSetId());
        }

        if (!isP && selectedCheckerSet.getVersion() < 0 && selectedCheckerSet.getVersion() != version) {
            return null;
        }

        BeanUtils.copyProperties(selectedCheckerSet, checkerSetVO);
        // 加入工具列表
        if (CollectionUtils.isNotEmpty(selectedCheckerSet.getCheckerProps())) {
            for (CheckerPropsEntity checkerPropsEntity : selectedCheckerSet.getCheckerProps()) {
                checkerSetVO.getToolList().add(checkerPropsEntity.getToolName());
            }
        }

        List<CheckerSetVersionVO> versionList = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(checkerSetEntities)) {
            // 加入latest
            CheckerSetVersionVO latestCheckerSetVersionVO = new CheckerSetVersionVO();
            latestCheckerSetVersionVO.setVersion(Integer.MAX_VALUE);
            latestCheckerSetVersionVO.setDisplayName("latest");
            versionList.add(latestCheckerSetVersionVO);

            for (CheckerSetEntity checkerSetEntity : checkerSetEntities) {
                if ((isP && checkerSetEntity.getVersion() < 0) || (checkerSetEntity.getVersion() < 0
                        && checkerSetEntity.getVersion() != version)) {
                    continue;
                }
                CheckerSetVersionVO checkerSetVersionVO = new CheckerSetVersionVO();
                checkerSetVersionVO.setVersion(checkerSetEntity.getVersion());
                if (checkerSetEntity.getVersion() == ToolIntegratedStatus.G.value()) {
                    checkerSetVersionVO.setDisplayName("灰度");
                } else if (checkerSetEntity.getVersion() == ToolIntegratedStatus.T.value()) {
                    checkerSetVersionVO.setDisplayName("测试");
                } else {
                    checkerSetVersionVO.setDisplayName("V" + checkerSetEntity.getVersion());
                }
                versionList.add(checkerSetVersionVO);
            }
        }
        versionList.sort(((o1, o2) -> o2.getVersion().compareTo(o1.getVersion())));
        checkerSetVO.setVersionList(versionList);
        checkerSetVO.setTaskUsage(checkerSetCountMap.get(checkerSetVO.getCheckerSetId()) == null ? 0 :
                checkerSetCountMap.get(checkerSetVO.getCheckerSetId()).intValue());

        // 加入语言显示名称
        checkerSetVO.setCodeLangList(List2StrUtil.fromString(selectedCheckerSet.getCheckerSetLang(), ","));

        // 如果选择了latest，或者是默认的规则集，则传入整数最大值对应版本列表中的latest
        if (latestVersionCheckerSets.contains(selectedCheckerSet.getCheckerSetId()) ||
                (Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name())
                        .contains(checkerSetVO.getCheckerSetSource())
                        && null == defaultMap.get(checkerSetVO.getCheckerSetId()))) {
            if (isP || checkerSetEntityMap.get(checkerSetVO.getCheckerSetId()) == null) {
                checkerSetVO.setVersion(Integer.MAX_VALUE);
            }
        }

        int checkerCount = selectedCheckerSet.getCheckerProps() != null
                ? selectedCheckerSet.getCheckerProps().size() : 0;
        checkerSetVO.setCheckerCount(checkerCount);
        if (null == checkerSetVO.getCreateTime()) {
            checkerSetVO.setCreateTime(0L);
        }
        //加入是否默认
        checkerSetVO.setDefaultCheckerSet((CheckerSetSource.DEFAULT.name().equals(checkerSetVO.getCheckerSetSource())
                && null == defaultMap.get(checkerSetVO.getCheckerSetId()))
                || (null != defaultMap.get(checkerSetVO.getCheckerSetId())
                && defaultMap.get(checkerSetVO.getCheckerSetId())));
        return checkerSetVO;
    }

    /**
     * 查询规则集列表
     * 对于服务创建的任务，有可能存在规则集迁移自动生成的多语言规则，此处查询逻辑如下：
     * 1、展示适合项目语言的新规则集
     * 2、展示适合项目语言的单语言的老规则集
     * 3、如果有多语言的老规则集，且已经被迁移脚本开启了，则也需要进行展示。用户关闭后则不再展示。
     * 4、多语言的老规则集只能关闭，不能再打开，需做下限制
     *
     * @param queryCheckerSetReq
     * @return
     */
    @Override
    public List<CheckerSetVO> getCheckerSetsOfTask(CheckerSetListQueryReq queryCheckerSetReq) {
        log.info("start to get checker set of project, task: {}, {}",
                queryCheckerSetReq.getProjectId(), queryCheckerSetReq.getTaskId());

        String projectId = queryCheckerSetReq.getProjectId();
        Long taskId = queryCheckerSetReq.getTaskId();
        List<CheckerSetProjectRelationshipEntity> taskRelationships =
                checkerSetProjectRelationshipRepository.findByProjectId(projectId);

        log.info("find relationship of project, task: {}, {}"
                , queryCheckerSetReq.getProjectId(), queryCheckerSetReq.getTaskId());

        if (CollectionUtils.isEmpty(taskRelationships)) {
            taskRelationships = new ArrayList<>();
        }
        Map<String, Boolean> defaultCheckerSetMap = taskRelationships.stream().collect(Collector.of(HashMap::new, (k,
                                                                                                                   v) ->
                        k.put(v.getCheckerSetId(), v.getDefaultCheckerSet()), (k, v) -> v,
                Collector.Characteristics.IDENTITY_FINISH
        ));

        //查出项目纬度的id集合
        log.info("start to get checker by taskId set of project, task: {}, {}"
                , queryCheckerSetReq.getProjectId(), queryCheckerSetReq.getTaskId());
        Set<String> projCheckerSetIds = taskRelationships.stream().
                map(CheckerSetProjectRelationshipEntity::getCheckerSetId).
                collect(Collectors.toSet());
        Map<String, Integer> checkerSetVersionMap = taskRelationships.stream().
                collect(HashMap::new, (m, v) -> m.put(v.getCheckerSetId(), v.getVersion()), HashMap::putAll);

        Result<GrayToolProjectVO> grayResult = client.get(ServiceGrayToolProjectResource.class)
                .getGrayToolProjectInfoByProjrctId(projectId);

        if (grayResult.isNotOk() || grayResult.getData() == null) {
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
        }

        // 获取当前项目版本（灰度/测试/发布）
        boolean isP = grayResult.getData().getStatus() == ToolIntegratedStatus.P.value();
        int toolIntegratedStatus = grayResult.getData().getStatus();
        List<CheckerSetEntity> grayCheckerSetEntityList = new ArrayList<>();
        if (!isP) {
            grayCheckerSetEntityList = checkerSetRepository.findByCheckerSetIdInAndVersion(
                    new ArrayList<>(projCheckerSetIds),
                    toolIntegratedStatus);
        }
        Map<String, CheckerSetEntity> grayCheckerSetEntityMap = grayCheckerSetEntityList.stream()
                .collect(Collectors.toMap(CheckerSetEntity::getCheckerSetId, entry -> entry, (v1, v2) -> v2));

        //查出任务维度的id集合
        List<CheckerSetTaskRelationshipEntity> checkerSetTaskRelationshipEntityList =
                checkerSetTaskRelationshipRepository.findByTaskId(taskId);
        Result<TaskDetailVO> taskDetailVOResult =
                client.get(ServiceTaskRestResource.class).getTaskInfoWithoutToolsByTaskId(taskId);
        Long codeLang;
        if (taskDetailVOResult.isNotOk() || taskDetailVOResult.getData() == null) {
            log.error("task info empty! task id: {}", taskId);
            codeLang = 0L;
        } else {
            codeLang = taskDetailVOResult.getData().getCodeLang();
        }
        Set<String> taskCheckerSetIds = checkerSetTaskRelationshipEntityList.stream().
                map(CheckerSetTaskRelationshipEntity::getCheckerSetId).
                collect(Collectors.toSet());

        log.info("find by complex condition checker set  of project, task: {}, {}"
                , queryCheckerSetReq.getProjectId(), queryCheckerSetReq.getTaskId());
        //查出项目纬度下的规则集
        List<CheckerSetEntity> checkerSetEntityList =
                checkerSetDao.findByComplexCheckerSetCondition(queryCheckerSetReq.getKeyWord(),
                        projCheckerSetIds, queryCheckerSetReq.getCheckerSetLanguage(),
                        queryCheckerSetReq.getCheckerSetCategory(),
                        queryCheckerSetReq.getToolName(), queryCheckerSetReq.getCheckerSetSource(),
                        queryCheckerSetReq.getCreator(), true,
                        null, true);

        log.info("find the official checker set map of project, task: {}, {}",
                queryCheckerSetReq.getProjectId(), queryCheckerSetReq.getTaskId());
        //官方优选 官方推荐版本
        Map<String, Integer> officialMap = checkerSetEntityList.stream().filter(checkerSetEntity ->
                !projCheckerSetIds.contains(checkerSetEntity.getCheckerSetId()) &&
                        Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name())
                                .contains(checkerSetEntity.getCheckerSetSource()))
                .collect(Collectors.groupingBy(CheckerSetEntity::getCheckerSetId)).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().max(Comparator.comparingInt(CheckerSetEntity::getVersion))
                                .orElse(new CheckerSetEntity()).getVersion()));

        //        // 任务是否是老插件
        //        boolean isLegacyTask = false;
        //        //如果是流水线创建的
        //        if (taskDetailVOResult.getData() != null && ComConstants.BsTaskCreateFrom.BS_PIPELINE.value().
        //                equals(taskDetailVOResult.getData().getCreateFrom()) && StringUtils.isEmpty
        //                (taskDetailVOResult.getData().getAtomCode()))
        //        {
        //            isLegacyTask = true;
        //        }

        Map<String, Boolean> useLatestVersionMap = taskRelationships.stream()
                .collect(Collectors.toMap(CheckerSetProjectRelationshipEntity::getCheckerSetId,
                        checkerSetProjectRelationshipEntity -> {
                            if (checkerSetProjectRelationshipEntity.getUselatestVersion() == null) {
                                return false;
                            }
                            return checkerSetProjectRelationshipEntity.getUselatestVersion();
                        }, (v1, v2) -> v2));
        log.info("scan the checkerSetEntityList of project, task: {}, {}",
                queryCheckerSetReq.getProjectId(), queryCheckerSetReq.getTaskId());
        List<CheckerSetVO> result = Lists.newArrayList();
        List<CheckerSetVO> taskCheckerSets = Lists.newArrayList();
        List<CheckerSetVO> otherCheckerSets = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(checkerSetEntityList)) {
            for (CheckerSetEntity checkerSetEntity : checkerSetEntityList) {
                if (!checkCheckerSetEntity(checkerSetEntity, officialMap, checkerSetVersionMap, taskCheckerSetIds)) {
                    continue;
                }

                // 如果是 灰度/测试 项目，并且规则集带有 灰度/测试 版本，并且选中了 latest 版本，规则集版本展示为 灰度/测试 版本
                if (!isP && useLatestVersionMap.getOrDefault(checkerSetEntity.getCheckerSetId(), false)
                        && grayCheckerSetEntityMap.get(checkerSetEntity.getCheckerSetId()) != null) {
                    checkerSetEntity = grayCheckerSetEntityMap.get(checkerSetEntity.getCheckerSetId());
                }

                CheckerSetVO checkerSetVO = new CheckerSetVO();
                BeanUtils.copyProperties(checkerSetEntity, checkerSetVO);
                checkerSetVO.setToolList(Sets.newHashSet());
                if (checkerSetEntity.getCodeLang() != null && (codeLang & checkerSetEntity.getCodeLang()) > 0L) {
                    if (taskCheckerSetIds.contains(checkerSetVO.getCheckerSetId())) {
                        checkerSetVO.setTaskUsing(true);
                        taskCheckerSets.add(checkerSetVO);
                    } else {
                        checkerSetVO.setTaskUsing(false);
                        otherCheckerSets.add(checkerSetVO);
                    }
                }

                // 加工具列表
                if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())) {
                    for (CheckerPropsEntity checkerPropsEntity : checkerSetEntity.getCheckerProps()) {
                        checkerSetVO.getToolList().add(checkerPropsEntity.getToolName());
                    }
                }

                // 加语言显示名称
                checkerSetVO.setCodeLangList(List2StrUtil.fromString(checkerSetEntity.getCheckerSetLang(), ","));

                //设置默认标签
                checkerSetVO.setDefaultCheckerSet((CheckerSetSource.DEFAULT.name().
                        equals(checkerSetVO.getCheckerSetSource())
                        && null == defaultCheckerSetMap.get(checkerSetVO.getCheckerSetId())
                        || (null != defaultCheckerSetMap.get(checkerSetVO.getCheckerSetId())
                        && defaultCheckerSetMap.get(checkerSetVO.getCheckerSetId()))));

                if (checkerSetVO.getVersion() == ToolIntegratedStatus.T.value()
                        || checkerSetVO.getVersion() == ToolIntegratedStatus.G.value()) {
                    CheckerSetVersionVO tCheckerSetVersionVO =
                            new CheckerSetVersionVO(ToolIntegratedStatus.T.value(), "测试");
                    CheckerSetVersionVO gCheckerSetVersionVO =
                            new CheckerSetVersionVO(ToolIntegratedStatus.G.value(), "灰度");
                    List<CheckerSetVersionVO> checkerSetVersionVOList;
                    if (checkerSetVO.getVersionList() == null) {
                        checkerSetVersionVOList = new ArrayList<>();
                    } else {
                        checkerSetVersionVOList = checkerSetVO.getVersionList();
                    }
                    checkerSetVersionVOList.add(tCheckerSetVersionVO);
                    checkerSetVersionVOList.add(gCheckerSetVersionVO);
                    checkerSetVO.setVersionList(checkerSetVersionVOList);
                }
            }

            // 任务使用的规则在前，未使用的规则在后，然后再按创建时间倒序
            if (CollectionUtils.isNotEmpty(taskCheckerSets)) {
                taskCheckerSets.sort(Comparator.comparingLong(o -> sortByOfficialProps(o) - o.getCreateTime()));
                result.addAll(taskCheckerSets);
            }
            if (CollectionUtils.isNotEmpty(otherCheckerSets)) {
                otherCheckerSets.sort(Comparator.comparingLong(o -> sortByOfficialProps(o) - o.getCreateTime()));
                result.addAll(otherCheckerSets);
            }
        }

        return result;
    }

    @Override
    public List<CheckerSetVO> getTaskCheckerSets(String projectId,
                                                 long taskId,
                                                 String toolName,
                                                 String dimension,
                                                 boolean needProps) {
        List<String> toolNameSet = ParamUtils.getToolsByDimension(toolName, dimension, taskId);

        //查出项目纬度的id集合
        List<CheckerSetProjectRelationshipEntity> projectRelationships =
                checkerSetProjectRelationshipRepository.findByProjectId(projectId);
        if (CollectionUtils.isEmpty(projectRelationships)) {
            return new ArrayList<>();
        }

        Map<String, CheckerSetProjectRelationshipEntity> projCheckerSetMap = projectRelationships.stream()
                .collect(Collectors.toMap(CheckerSetProjectRelationshipEntity::getCheckerSetId,
                        Function.identity(),
                        (k, v) -> v));

        //查出任务维度的id集合
        List<CheckerSetTaskRelationshipEntity> taskRelationships =
                checkerSetTaskRelationshipRepository.findByTaskId(taskId);

        Set<String> taskCheckerSetIds = taskRelationships.stream()
                .map(CheckerSetTaskRelationshipEntity::getCheckerSetId).collect(Collectors.toSet());

        //查出任务纬度下的包含指定工具的规则集
        List<CheckerSetEntity> checkerSetEntityList = checkerSetDao.findByComplexCheckerSetCondition(null,
                taskCheckerSetIds, null, null, new HashSet<>(toolNameSet), null,
                null, false, null, true);

        Result<GrayToolProjectVO> grayProjectresult = client.get(ServiceGrayToolProjectResource.class)
                .getGrayToolProjectInfoByProjrctId(projectId);

        if (grayProjectresult.isNotOk() || grayProjectresult.getData() == null) {
            log.info("get gray task info fail: {} {}", grayProjectresult, grayProjectresult.getData());
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
        }

        int toolIntegratedStatus = grayProjectresult.getData().getStatus();

        Map<String, List<CheckerSetEntity>> checkerSetVersionMap = checkerSetEntityList.stream()
                .collect(Collectors.groupingBy(CheckerSetEntity::getCheckerSetId));
        // 按照版本过滤规则集
        List<CheckerSetVO> result = checkerSetEntityList.stream()
                /*.filter(it -> it.getVersion() != null
                        && projCheckerSetMap.get(it.getCheckerSetId()) != null
                        && it.getVersion().equals(projCheckerSetMap.get(it.getCheckerSetId()).getVersion()))*/
                .filter(filterCheckerSetOfTask(projCheckerSetMap,
                        checkerSetVersionMap,
                        toolIntegratedStatus))
                /*.map(it -> {
                    CheckerSetVO checkerSetVO = new CheckerSetVO();
                    checkerSetVO.setCheckerSetId(it.getCheckerSetId());
                    checkerSetVO.setCheckerSetName(it.getCheckerSetName());
                    checkerSetVO.setVersion(it.getVersion());
                    checkerSetVO.setTaskUsing(true);
                    checkerSetVO.setCodeLang(it.getCodeLang());
                    checkerSetVO.setCheckerSetLang(it.getCheckerSetLang());
                    return checkerSetVO;
                })*/
                .map(it -> mapToVO(it, needProps))
                .sorted(Comparator.comparing(CheckerSetVO::getCodeLang)).collect(Collectors.toList());

        return result;
    }

    private CheckerSetVO mapToVO(CheckerSetEntity checkerSetEntity, boolean needProps) {
        CheckerSetVO checkerSetVO = new CheckerSetVO();
        if (needProps) {
            BeanUtils.copyProperties(checkerSetEntity, checkerSetVO);
            if(CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())){
                List<CheckerPropVO> checkerPropsVOS = checkerSetEntity.getCheckerProps()
                        .stream()
                        .map(checkerPropsEntity -> {
                            CheckerPropVO checkerPropVO = new CheckerPropVO();
                            BeanUtils.copyProperties(checkerPropsEntity, checkerPropVO);
                            return checkerPropVO;
                        }).collect(Collectors.toList());
                checkerSetVO.setCheckerProps(checkerPropsVOS);
            }
        } else {
            BeanUtils.copyProperties(checkerSetEntity, checkerSetVO, "checkerProps");
        }
        checkerSetVO.setTaskUsing(true);
        return checkerSetVO;
    }

    private Predicate<CheckerSetEntity> filterCheckerSetOfTask(
            Map<String, CheckerSetProjectRelationshipEntity> projCheckerSetMap,
            Map<String, List<CheckerSetEntity>> checkerSetVersionMap,
            int toolIntegratedStatus) {
        return new Predicate<CheckerSetEntity>() {
            @Override
            public boolean test(CheckerSetEntity checkerSetEntity) {
                CheckerSetProjectRelationshipEntity checkerSetRelationship =
                        projCheckerSetMap.get(checkerSetEntity.getCheckerSetId());
                if (checkerSetRelationship == null || checkerSetEntity.getVersion() == null
                        ) {
                    return false;
                }

                if (toolIntegratedStatus != ToolIntegratedStatus.P.value()
                        && checkerSetRelationship.getUselatestVersion() != null
                        && checkerSetRelationship.getUselatestVersion()
                        && findCheckerSetByVersion(checkerSetVersionMap.get(checkerSetEntity.getCheckerSetId()),
                        toolIntegratedStatus)) {
                        return checkerSetEntity.getVersion() == toolIntegratedStatus;
                    } else {
                    return checkerSetEntity.getVersion()
                            .equals(projCheckerSetMap.get(checkerSetEntity.getCheckerSetId()).getVersion());
                }
            }

            private Boolean findCheckerSetByVersion(List<CheckerSetEntity> checkerSetVersionList, int version) {
                return checkerSetVersionList.stream()
                        .map(it -> it.getVersion() != null && it.getVersion() == version)
                        .reduce((b1, b2) -> (b1 || b2))
                        .orElseGet(() -> Boolean.FALSE);
            }
        };
    }

    private boolean checkCheckerSetEntity(CheckerSetEntity checkerSetEntity, Map<String, Integer> officialMap,
                                          Map<String, Integer> checkerSetVersionMap, Set<String> taskCheckerSetIds) {
        //对应版本号
        Integer checkerSetVersion = checkerSetVersionMap.get(checkerSetEntity.getCheckerSetId());
        if (null != checkerSetVersionMap.get(checkerSetEntity.getCheckerSetId())
                && !checkerSetEntity.getVersion().equals(checkerSetVersion)) {
            return false;
        } else if (null != officialMap.get(checkerSetEntity.getCheckerSetId())
                && !checkerSetEntity.getVersion().equals(officialMap.get(checkerSetEntity.getCheckerSetId()))) {
            return false;
        }

        //如果是老规则集，且没有被该任务使用，且是多语言规则集，那么就不展示
        if (checkerSetEntity.getLegacy() != null
                && checkerSetEntity.getLegacy()
                && !taskCheckerSetIds.contains(checkerSetEntity.getCheckerSetId())
                && StringUtils.isNotEmpty(checkerSetEntity.getCheckerSetLang())
                && checkerSetEntity.getCheckerSetLang().contains(ComConstants.STRING_SPLIT)) {
            return false;
        }

        //规则数为空的不显示
        if (CollectionUtils.isEmpty(checkerSetEntity.getCheckerProps())) {
            return false;
        }

        return true;
    }

    @Override
    public Page<CheckerSetVO> getCheckerSetsOfTaskPage(CheckerSetListQueryReq queryCheckerSetReq) {
        if (null == queryCheckerSetReq.getSortType()) {
            queryCheckerSetReq.setSortType(Sort.Direction.DESC.name());
        }

        if (StringUtils.isEmpty(queryCheckerSetReq.getSortField())) {
            queryCheckerSetReq.setSortField("task_usage");
        }

        // 获取结果
        List<CheckerSetVO> result = getCheckerSetsOfTask(queryCheckerSetReq);

        log.info("finish to get checker set of project, task: {}, {}"
                , queryCheckerSetReq.getProjectId(), queryCheckerSetReq.getTaskId());

        //封装分页类
        int pageNum = Math.max(queryCheckerSetReq.getPageNum() - 1, 0);
        int pageSize = queryCheckerSetReq.getPageSize() <= 0 ? 10 : queryCheckerSetReq.getPageSize();
        Pageable pageable = PageRequest.of(pageNum, pageSize,
                Sort.by(queryCheckerSetReq.getSortType(), queryCheckerSetReq.getSortField()));
        long total = pageNum * pageSize + result.size() + 1;
        return new PageImpl<>(result, pageable, total);
    }

    /**
     * 查询规则集参数
     *
     * @param projectId
     * @return
     */
    @Override
    public CheckerSetParamsVO getParams(String projectId) {
        // 查询规则集类型列表
        CheckerSetParamsVO checkerSetParams = new CheckerSetParamsVO();
        checkerSetParams.setCatatories(Lists.newArrayList());
        for (CheckerSetCategory checkerSetCategory : CheckerSetCategory.values()) {
            CheckerSetCategoryVO categoryVO = new CheckerSetCategoryVO();
            categoryVO.setCnName(checkerSetCategory.getName());
            categoryVO.setEnName(checkerSetCategory.name());
            checkerSetParams.getCatatories().add(categoryVO);
        }

        // 查询规则集语言列表
        Result<List<BaseDataVO>> langsParamsResult =
                client.get(ServiceBaseDataResource.class).getParamsByType(KEY_LANG);
        if (langsParamsResult.isNotOk() || CollectionUtils.isEmpty(langsParamsResult.getData())) {
            log.error("checker set langs is empty!");
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        checkerSetParams.setCodeLangs(Lists.newArrayList());
        for (BaseDataVO baseDataVO : langsParamsResult.getData()) {
            CheckerSetCodeLangVO checkerSetCodeLangVO = new CheckerSetCodeLangVO();
            checkerSetCodeLangVO.setCodeLang(Integer.valueOf(baseDataVO.getParamCode()));
            checkerSetCodeLangVO.setDisplayName(baseDataVO.getParamName());
            checkerSetParams.getCodeLangs().add(checkerSetCodeLangVO);
        }

        // 查询项目下的规则集列表
        CheckerSetListQueryReq queryCheckerSetReq = new CheckerSetListQueryReq();
        queryCheckerSetReq.setProjectId(projectId);
        queryCheckerSetReq.setSortField(CheckerConstants.CheckerSetSortField.TASK_USAGE.value());
        queryCheckerSetReq.setSortType(Sort.Direction.DESC.name());
        List<CheckerSetVO> checkerSetVOS = getCheckerSetsOfProject(queryCheckerSetReq);
        checkerSetParams.setCheckerSets(checkerSetVOS);

        return checkerSetParams;
    }

    /**
     * 规则集ID
     *
     * @param checkerSetId
     */
    @Override
    public CheckerSetVO getCheckerSetDetail(String checkerSetId, int version) {
        CheckerSetEntity selectedCheckerSetEntity = null;
        if (version == Integer.MAX_VALUE) {
            List<CheckerSetEntity> checkerSetEntities =
                    checkerSetRepository.findByCheckerSetIdIn(Sets.newHashSet(checkerSetId));
            if (CollectionUtils.isNotEmpty(checkerSetEntities)) {
                int latestVersion = CheckerConstants.DEFAULT_VERSION;
                selectedCheckerSetEntity = checkerSetEntities.get(0);
                for (CheckerSetEntity checkerSetEntity : checkerSetEntities) {
                    if (checkerSetEntity.getVersion() > latestVersion) {
                        selectedCheckerSetEntity = checkerSetEntity;
                        latestVersion = selectedCheckerSetEntity.getVersion();
                    }
                }
            }
        } else {
            selectedCheckerSetEntity = checkerSetRepository.findFirstByCheckerSetIdAndVersion(checkerSetId, version);
        }
        CheckerSetVO checkerSetVO = new CheckerSetVO();
        if (selectedCheckerSetEntity != null) {
            BeanUtils.copyProperties(selectedCheckerSetEntity,checkerSetVO);
            checkerSetVO.setCodeLangList(List2StrUtil.fromString(selectedCheckerSetEntity.getCheckerSetLang(), ","));
        }

        // 加入工具列表
        Set<String> toolNames = Sets.newHashSet();
        if (selectedCheckerSetEntity != null) {
            if (CollectionUtils.isNotEmpty(selectedCheckerSetEntity.getCheckerProps())) {
                for (CheckerPropsEntity checkerPropsEntity : selectedCheckerSetEntity.getCheckerProps()) {
                    toolNames.add(checkerPropsEntity.getToolName());
                }
            }
        }
        checkerSetVO.setToolList(toolNames);

        return checkerSetVO;
    }

    /**
     * 修改规则集基础信息
     *
     * @param checkerSetId
     * @param updateCheckerSetReq
     */
    @Override
    public void updateCheckerSetBaseInfo(String checkerSetId, String projectId,
                                         V3UpdateCheckerSetReqVO updateCheckerSetReq) {
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetId(checkerSetId);
        if (CollectionUtils.isNotEmpty(checkerSetEntities)) {
            List<CheckerSetCatagoryEntity> catagoryEntities = getCatagoryEntities(updateCheckerSetReq.getCatagories());
            for (CheckerSetEntity checkerSetEntity : checkerSetEntities) {
                if (!projectId.equals(checkerSetEntity.getProjectId())) {
                    String errMsg = "不能修改其他项目的规则集！";
                    log.error(errMsg);
                    throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
                }
                checkerSetEntity.setCheckerSetName(updateCheckerSetReq.getCheckerSetName());
                checkerSetEntity.setDescription(updateCheckerSetReq.getDescription());
                checkerSetEntity.setCatagories(catagoryEntities);
            }
            checkerSetRepository.saveAll(checkerSetEntities);
        }
    }

    /**
     * 规则集关联到项目或任务
     *
     * @param checkerSetId
     * @param checkerSetRelationshipVO
     */
    @Override
    public void setRelationships(String checkerSetId, String user, CheckerSetRelationshipVO checkerSetRelationshipVO) {
        String projectId = checkerSetRelationshipVO.getProjectId();
        Long taskId = checkerSetRelationshipVO.getTaskId();

        CheckerSetProjectRelationshipEntity projectRelationshipEntity = null;
        List<CheckerSetProjectRelationshipEntity> projectRelationshipEntities =
                checkerSetProjectRelationshipRepository.findByProjectId(projectId);
        Map<String, Integer> checkerSetVersionMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(projectRelationshipEntities)) {
            for (CheckerSetProjectRelationshipEntity relationshipEntity : projectRelationshipEntities) {
                if (relationshipEntity.getCheckerSetId().equals(checkerSetId)) {
                    projectRelationshipEntity = relationshipEntity;
                }
                checkerSetVersionMap.put(relationshipEntity.getCheckerSetId(), relationshipEntity.getVersion());
            }
        }

        log.info("project relation ship entity is: {}, {}, {}", projectRelationshipEntity, projectId, taskId);

        if (CheckerConstants.CheckerSetRelationshipType.PROJECT.name().equals(checkerSetRelationshipVO.getType())) {

            projectRelationshipEntity =
                    checkerSetProjectRelationshipRepository.findFirstByCheckerSetIdAndProjectId(checkerSetId, projectId);
            if (projectRelationshipEntity != null) {
                log.error("关联已存在！: {}, {}, {}", checkerSetId, projectId, taskId);
                return;
            }
            CheckerSetProjectRelationshipEntity newProjectRelationshipEntity =
                    new CheckerSetProjectRelationshipEntity();
            newProjectRelationshipEntity.setCheckerSetId(checkerSetId);
            newProjectRelationshipEntity.setProjectId(projectId);
            newProjectRelationshipEntity.setUselatestVersion(true);
            newProjectRelationshipEntity.setDefaultCheckerSet(false);
            if (checkerSetRelationshipVO.getVersion() == null) {
                Map<String, Integer> latestVersionMap = getLatestVersionMap(Sets.newHashSet(checkerSetId));
                newProjectRelationshipEntity.setVersion(latestVersionMap.get(checkerSetId));
            } else {
                newProjectRelationshipEntity.setVersion(checkerSetRelationshipVO.getVersion());
            }
            checkerSetProjectRelationshipRepository.save(newProjectRelationshipEntity);
            log.info("set new task relation ship successfully: {}, {}, {}", checkerSetId, projectId, taskId);
        } else if (CheckerConstants.CheckerSetRelationshipType.TASK.name().equals(checkerSetRelationshipVO.getType())) {
            if (projectRelationshipEntity == null) {
                List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetId(checkerSetId);
                if (CollectionUtils.isNotEmpty(checkerSetEntities)) {
                    CheckerSetEntity latestVersionCheckerSet = checkerSetEntities.get(0);
                    for (CheckerSetEntity checkerSetEntity : checkerSetEntities) {
                        if (checkerSetEntity.getVersion() > latestVersionCheckerSet.getVersion()) {
                            latestVersionCheckerSet = checkerSetEntity;
                        }
                    }
                    if (Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name())
                            .contains(latestVersionCheckerSet.getCheckerSetSource())) {
                        projectRelationshipEntity = new CheckerSetProjectRelationshipEntity();
                        projectRelationshipEntity.setCheckerSetId(checkerSetId);
                        projectRelationshipEntity.setProjectId(projectId);
                        projectRelationshipEntity.setUselatestVersion(true);
                        //默认是默认规则集
                        if (CheckerSetSource.DEFAULT.name().equals(latestVersionCheckerSet.getCheckerSetSource())) {
                            projectRelationshipEntity.setDefaultCheckerSet(true);
                        } else {
                            projectRelationshipEntity.setDefaultCheckerSet(false);
                        }
                        projectRelationshipEntity.setVersion(latestVersionCheckerSet.getVersion());
                        checkerSetVersionMap.put(checkerSetId, latestVersionCheckerSet.getVersion());
                        checkerSetProjectRelationshipRepository.save(projectRelationshipEntity);
                    }
                }
                if (projectRelationshipEntity == null) {
                    String errMsg = "规则集没有安装到项目！";
                    log.error(errMsg);
                    throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
                }
            }

            CheckerSetTaskRelationshipEntity taskRelationshipEntity = null;
            Map<Long, Map<String, Integer>> currentTaskCheckerSetMap = Maps.newHashMap();
            List<CheckerSetTaskRelationshipEntity> taskRelationshipEntities =
                    checkerSetTaskRelationshipRepository.findByTaskId(taskId);
            if (CollectionUtils.isNotEmpty(taskRelationshipEntities)) {
                for (CheckerSetTaskRelationshipEntity relationshipEntity : taskRelationshipEntities) {
                    if (relationshipEntity.getCheckerSetId().equals(checkerSetId)) {
                        taskRelationshipEntity = relationshipEntity;
                    }
                    currentTaskCheckerSetMap.computeIfAbsent(relationshipEntity.getTaskId(), k -> Maps.newHashMap());
                    currentTaskCheckerSetMap.get(relationshipEntity.getTaskId()).put(relationshipEntity.getCheckerSetId(), checkerSetVersionMap.get(relationshipEntity.getCheckerSetId()));
                }
            }
            if (taskRelationshipEntity != null) {
                log.error("关联已存在！: {}, {}, {}", checkerSetId, projectId, taskId);
                return;
            }
            CheckerSetTaskRelationshipEntity newTaskRelationshipEntity = new CheckerSetTaskRelationshipEntity();
            newTaskRelationshipEntity.setCheckerSetId(checkerSetId);
            newTaskRelationshipEntity.setProjectId(projectId);
            newTaskRelationshipEntity.setTaskId(taskId);
            checkerSetTaskRelationshipRepository.save(newTaskRelationshipEntity);
            log.info("set new task relation ship successfully: {}, {}, {}", checkerSetId, projectId, taskId);

            // 任务关联规则集需要设置全量扫描
            CheckerSetEntity checkerSetEntity = checkerSetRepository.findFirstByCheckerSetIdAndVersion(checkerSetId,
                    projectRelationshipEntity.getVersion());
            if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())) {
                Set<String> toolSet = Sets.newHashSet();
                for (CheckerPropsEntity checkerPropsEntity : checkerSetEntity.getCheckerProps()) {
                    toolSet.add(checkerPropsEntity.getToolName());
                }
                toolBuildInfoService.setForceFullScan(taskId, Lists.newArrayList(toolSet));
            }

            // 设置强制全量扫描标志并刷新告警状态
            Map<Long, Map<String, Integer>> updatedTaskCheckerSetMap;
            try {
                updatedTaskCheckerSetMap = Maps.newHashMap();
                updatedTaskCheckerSetMap.put(taskId, CloneUtils.cloneObject(currentTaskCheckerSetMap.get(taskId)));
                if (null != updatedTaskCheckerSetMap.get(taskId)) {
                    updatedTaskCheckerSetMap.get(taskId).put(checkerSetId, checkerSetVersionMap.get(checkerSetId));
                }

                // 对各任务设置强制全量扫描标志，并修改告警状态
                Map<Long, Map<String, Integer>> finalUpdatedTaskCheckerSetMap = updatedTaskCheckerSetMap;
                ThreadPoolUtil.addRunnableTask(() -> {
                    setForceFullScanAndUpdateDefectAndToolStatus(currentTaskCheckerSetMap,
                            finalUpdatedTaskCheckerSetMap,
                            user);
                });
            } catch (CloneNotSupportedException e) {
                log.error("copy currentTaskCheckerSetMap fail!");
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }
        } else {
            String errMsg = "关联类型非法！";
            log.error(errMsg);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
        }
    }

    @Override
    public Boolean setRelationshipsOnce(String user, String projectId, long taskId, String toolName) {
        CheckerSetRelationshipVO projectCheckerSetRelationshipVO = new CheckerSetRelationshipVO();
        projectCheckerSetRelationshipVO.setType("PROJECT");
        projectCheckerSetRelationshipVO.setProjectId(projectId);
        projectCheckerSetRelationshipVO.setTaskId(taskId);

        CheckerSetRelationshipVO taskCheckerSetRelationshipVO = new CheckerSetRelationshipVO();
        taskCheckerSetRelationshipVO.setType("TASK");
        taskCheckerSetRelationshipVO.setProjectId(projectId);
        taskCheckerSetRelationshipVO.setTaskId(taskId);

        TaskDetailVO taskInfo = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId).getData();
        List<BaseDataVO> baseDataVOList =
                client.get(ServiceBaseDataResource.class).getParamsByType(ONCE_CHECKER_SET_KEY).getData();
        List<BaseDataVO> toolBaseData = Objects.requireNonNull(baseDataVOList).stream()
                .filter(it -> it.getParamName().equals(toolName)
                        && (Integer.valueOf(it.getParamCode()) & taskInfo.getCodeLang()) != 0)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(toolBaseData)) {
            throw new CodeCCException("该任务语言暂无合适规则集");
        }

        toolBaseData.forEach(baseDataVO -> {
            String checkerSetId = baseDataVO.getParamExtend1();
            log.info("start to open checker for task: {}, {}", taskId, checkerSetId);

            // 先安装
            setRelationships(checkerSetId, user, projectCheckerSetRelationshipVO);

            // 再关联
            setRelationships(checkerSetId, user, taskCheckerSetRelationshipVO);
        });
        return true;
    }

    /**
     * 任务批量关联规则集
     *
     * @param projectId
     * @param taskId
     * @param checkerSetList
     * @param user
     * @return
     */
    @Override
    public Boolean batchRelateTaskAndCheckerSet(String projectId,
                                                Long taskId,
                                                List<CheckerSetVO> checkerSetList,
                                                String user,
                                                Boolean isOpenSource) {
        List<CheckerSetProjectRelationshipEntity> projectRelationshipEntityList =
                checkerSetProjectRelationshipRepository.findByProjectId(projectId);
        Map<String, CheckerSetProjectRelationshipEntity> projInstallCheckerSetMap;
        if (CollectionUtils.isEmpty(projectRelationshipEntityList)) {
            projInstallCheckerSetMap = new HashMap<>();
        } else {
            projInstallCheckerSetMap = projectRelationshipEntityList.stream()
                    .collect(Collectors.toMap(it -> it.getCheckerSetId(), Function.identity(), (k, v) -> k));
        }

        List<CheckerSetTaskRelationshipEntity> existTaskRelationshipEntityList =
                checkerSetTaskRelationshipRepository.findByTaskId(taskId);
        Map<String, CheckerSetTaskRelationshipEntity> existTaskRelatedCheckerMap = null;
        if (CollectionUtils.isNotEmpty(existTaskRelationshipEntityList)) {
            existTaskRelatedCheckerMap = existTaskRelationshipEntityList.stream()
                    .collect(Collectors.toMap(
                            CheckerSetTaskRelationshipEntity::getCheckerSetId, Function.identity(), (k, v) -> v));
        }
        Set<String> checkerSetIds =
                checkerSetList.stream().map(CheckerSetVO::getCheckerSetId).collect(Collectors.toSet());
        List<CheckerSetEntity> checkerSetEntityList = checkerSetRepository.findByCheckerSetIdIn(checkerSetIds);

        // 找到每个规则集中版本号最大的一个规则集
        Map<String, CheckerSetEntity> maxCheckerSetEntityMap = checkerSetEntityList.stream()
                .collect(Collectors.groupingBy(CheckerSetEntity::getCheckerSetId))
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().max(
                                Comparator.comparingInt(CheckerSetEntity::getVersion)).orElse(new CheckerSetEntity())));

        // 项目还没安装的规则集是非法规则集
        List<String> invalidCheckerSet = new ArrayList<>();

        // 关联规则集需要设置全量扫描的任务工具
        Set<String> toolSet = new HashSet<>();
        List<CheckerSetTaskRelationshipEntity> taskRelationshipEntityList = new ArrayList<>();
        //如果是官方推荐和官方优选的话，还需要关联项目表
        List<CheckerSetProjectRelationshipEntity> projectRelationshipEntities = new ArrayList<>();
        long currTime = System.currentTimeMillis();
        for (CheckerSetVO checkerSetVO : checkerSetList) {
            String checkerSetId = checkerSetVO.getCheckerSetId();
            CheckerSetEntity maxVersionCheckerSet = maxCheckerSetEntityMap.get(checkerSetId);
            if (!projInstallCheckerSetMap.containsKey(checkerSetId)) {
                //如果是官方的话 需要关联
                boolean matchCheckerSetSource = StringUtils.isNotBlank(maxVersionCheckerSet.getCheckerSetSource())
                        && Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name())
                        .contains(maxVersionCheckerSet.getCheckerSetSource());
                if ((null != isOpenSource && isOpenSource)
                        || matchCheckerSetSource
                        || (maxVersionCheckerSet.getLegacy() != null && maxVersionCheckerSet.getLegacy()
                        && CheckerConstants.CheckerSetOfficial.OFFICIAL.code() == maxVersionCheckerSet.getOfficial())) {
                    //关联项目关联表
                    CheckerSetProjectRelationshipEntity checkerSetProjectRelationshipEntity =
                            new CheckerSetProjectRelationshipEntity();
                    checkerSetProjectRelationshipEntity.setProjectId(projectId);
                    checkerSetProjectRelationshipEntity.setCheckerSetId(checkerSetVO.getCheckerSetId());
                    //如果是开源扫描，并且版本不为空，则设置版本
                    if ((null != isOpenSource && isOpenSource)
                            && null != checkerSetVO.getVersion()
                            && Integer.MAX_VALUE != checkerSetVO.getVersion()) {
                        checkerSetProjectRelationshipEntity.setVersion(checkerSetVO.getVersion());
                        checkerSetProjectRelationshipEntity.setUselatestVersion(false);
                    } else {
                        checkerSetProjectRelationshipEntity.setVersion(maxVersionCheckerSet.getVersion());
                        checkerSetProjectRelationshipEntity.setUselatestVersion(true);
                    }
                    if (CheckerSetSource.DEFAULT.name().equals(checkerSetVO.getCheckerSetSource())) {
                        checkerSetProjectRelationshipEntity.setDefaultCheckerSet(true);
                    } else {
                        checkerSetProjectRelationshipEntity.setDefaultCheckerSet(false);
                    }
                    projectRelationshipEntities.add(checkerSetProjectRelationshipEntity);

                    //在关联任务的关联表
                    CheckerSetTaskRelationshipEntity newRelationshipEntity = new CheckerSetTaskRelationshipEntity();
                    newRelationshipEntity.setCheckerSetId(checkerSetId);
                    newRelationshipEntity.setProjectId(projectId);
                    newRelationshipEntity.setTaskId(taskId);
                    newRelationshipEntity.setCreatedBy(user);
                    newRelationshipEntity.setCreatedDate(currTime);
                    taskRelationshipEntityList.add(newRelationshipEntity);
                    if (CollectionUtils.isNotEmpty(checkerSetVO.getToolList())) {
                        toolSet.addAll(checkerSetVO.getToolList());
                    }
                } else {
                    invalidCheckerSet.add(checkerSetId);
                }
            } else {
                //如果是开源的，并且版本号与原来不一致，则需要更新版本号
                if ((null != isOpenSource && isOpenSource) && null != checkerSetVO.getVersion()) {
                    CheckerSetProjectRelationshipEntity checkerSetProjectRelationshipEntity =
                            projInstallCheckerSetMap.get(checkerSetId);

                    if (checkerSetProjectRelationshipEntity != null
                            && !checkerSetVO.getVersion().equals(checkerSetProjectRelationshipEntity.getVersion())) {
                        if (Integer.MAX_VALUE != checkerSetVO.getVersion()) {
                            checkerSetProjectRelationshipEntity.setVersion(checkerSetVO.getVersion());
                            checkerSetProjectRelationshipEntity.setUselatestVersion(false);
                        } else {
                            checkerSetProjectRelationshipEntity.setVersion(maxVersionCheckerSet.getVersion());
                            checkerSetProjectRelationshipEntity.setUselatestVersion(true);
                        }

                        checkerSetProjectRelationshipRepository.save(checkerSetProjectRelationshipEntity);
                    }
                }
                // 还没有被任务关联的规则集则创建关联
                if (MapUtils.isEmpty(existTaskRelatedCheckerMap)
                        || !existTaskRelatedCheckerMap.containsKey(checkerSetId)) {
                    CheckerSetTaskRelationshipEntity newRelationshipEntity = new CheckerSetTaskRelationshipEntity();
                    newRelationshipEntity.setCheckerSetId(checkerSetId);
                    newRelationshipEntity.setProjectId(projectId);
                    newRelationshipEntity.setTaskId(taskId);
                    newRelationshipEntity.setCreatedBy(user);
                    newRelationshipEntity.setCreatedDate(currTime);
                    taskRelationshipEntityList.add(newRelationshipEntity);
                    if (CollectionUtils.isNotEmpty(checkerSetVO.getToolList())) {
                        toolSet.addAll(checkerSetVO.getToolList());
                    }
                }
            }

            // 不在本次关联列表中的都要解除关联
            if (MapUtils.isNotEmpty(existTaskRelatedCheckerMap)) {
                existTaskRelatedCheckerMap.remove(checkerSetId);
            }
        }

        if (CollectionUtils.isNotEmpty(invalidCheckerSet)) {
            StringBuffer errMsg = new StringBuffer();
            errMsg.append("项目未安装规则集: ").append(JsonUtil.INSTANCE.toJson(invalidCheckerSet));
            log.error(errMsg.toString());
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg.toString()}, null);
        }

        checkerSetTaskRelationshipRepository.saveAll(taskRelationshipEntityList);

        //保存官方优选和官方推荐
        if (CollectionUtils.isNotEmpty(projectRelationshipEntities)) {
            checkerSetProjectRelationshipRepository.saveAll(projectRelationshipEntities);
        }

        // 解除规则集关联
        if (MapUtils.isNotEmpty(existTaskRelatedCheckerMap)) {
            Collection<CheckerSetTaskRelationshipEntity> needDeleteTaskRelationshens =
                    existTaskRelatedCheckerMap.values();
            checkerSetTaskRelationshipRepository.deleteAll(needDeleteTaskRelationshens);

            // 解除关联的规则集涉及的工具也需要强制全量扫描
            Set<String> needDeleteCheckerSeIds = needDeleteTaskRelationshens.stream()
                    .map(it -> it.getCheckerSetId()).collect(Collectors.toSet());
            List<CheckerSetEntity> needDeleteCheckerSets =
                    checkerSetRepository.findByCheckerSetIdIn(needDeleteCheckerSeIds);
            Map<String, List<CheckerSetEntity>> checkerSetMap =
                    needDeleteCheckerSets.stream().collect(Collectors.groupingBy(CheckerSetEntity::getCheckerSetId));
            checkerSetMap.forEach((checkerSetId, checkerSetEntities) -> {
                CheckerSetEntity selectCheckerSet = null;
                if (projInstallCheckerSetMap.get(checkerSetId) == null) {
                    log.info("projInstallCheckerSetMap get checkerSetId is null: {}", checkerSetId);
                }
                if (projInstallCheckerSetMap.get(checkerSetId).getUselatestVersion()) {
                    selectCheckerSet = checkerSetEntities.stream()
                            .max(Comparator.comparing(CheckerSetEntity::getVersion)).orElse(new CheckerSetEntity());
                } else {
                    for (CheckerSetEntity checkerSetEntity : checkerSetEntities) {
                        if (checkerSetEntity.getVersion()
                                .equals(projInstallCheckerSetMap.get(checkerSetId).getVersion())) {
                            selectCheckerSet = checkerSetEntity;
                        }
                    }
                }
                if (selectCheckerSet != null && CollectionUtils.isNotEmpty(selectCheckerSet.getCheckerProps())) {
                    Set<String> tools = selectCheckerSet.getCheckerProps().stream()
                            .map(CheckerPropsEntity::getToolName).collect(Collectors.toSet());
                    toolSet.addAll(tools);
                }
            });
        }

        // 关联规则集需要设置全量扫描
        toolBuildInfoService.setForceFullScan(taskId, Lists.newArrayList(toolSet));

        return true;
    }

    @Override
    public void management(String user, String checkerSetId, CheckerSetManagementReqVO checkerSetManagementReqVO) {

        // 校验规则集是否存在
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetId(checkerSetId);
        if (CollectionUtils.isEmpty(checkerSetEntities)) {
            String errMsg = "规则集不存在";
            log.error(errMsg);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
        }

        CheckerSetEntity firstCheckerSetEntity = checkerSetEntities.get(0);
        if (CheckerSetSource.DEFAULT.name().equals(firstCheckerSetEntity.getCheckerSetSource())
                || CheckerSetSource.RECOMMEND.name().equals(firstCheckerSetEntity.getCheckerSetSource())) {
            if (checkerSetManagementReqVO.getUninstallCheckerSet() != null
                    && checkerSetManagementReqVO.getUninstallCheckerSet()) {
                String errMsg = "官方推荐和官方优选规则集不能进行此项操作";
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
        }

        // 校验设置为公开的规则集名称是否与公共规则集重复
        if (checkerSetManagementReqVO.getScope() != null
                && checkerSetManagementReqVO.getScope() == CheckerConstants.CheckerSetScope.PUBLIC.code()) {
            checkNameExistInPublic(firstCheckerSetEntity.getCheckerSetName());
        }

        // 校验用户是否有权限
        boolean havePermission;
        if (checkerSetManagementReqVO.getDiscardFromTask() == null) {
            log.info("management checkerSet version auth user {} | project {}",
                    user, checkerSetManagementReqVO.getProjectId());
            havePermission = authExPermissionApi.authProjectManager(checkerSetManagementReqVO.getProjectId(), user);
        } else {
            Set<String> tasks = authExPermissionApi.queryTaskListForUser(user, checkerSetManagementReqVO.getProjectId(),
                    Sets.newHashSet(CodeCCAuthAction.TASK_MANAGE.getActionName()));
            havePermission = tasks.contains(String.valueOf(checkerSetManagementReqVO.getDiscardFromTask()));
            log.info("management checkSet auth user {} | task {} | set {}",
                    user, checkerSetManagementReqVO.getDiscardFromTask(), tasks);
        }
        if (!havePermission && !firstCheckerSetEntity.getCreator().equals(user)) {
            String errMsg = "当前用户不是项目管理员或者规则集创建者，无权进行此操作！";
            log.error(errMsg);
            throw new CodeCCException(CommonMessageCode.PERMISSION_DENIED, new String[]{"当前用户" + user}, null);
        }

        // 查询任务关联规则集记录
        List<CheckerSetTaskRelationshipEntity> taskRelationshipEntities =
                checkerSetTaskRelationshipRepository.findByProjectId(checkerSetManagementReqVO.getProjectId());
        //过滤限制当前规则集id的规则集
        List<CheckerSetTaskRelationshipEntity> selectCheckerSetEntities = new ArrayList<>();
        Map<Long, CheckerSetTaskRelationshipEntity> taskRelationshipEntityMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(taskRelationshipEntities)) {
            for (CheckerSetTaskRelationshipEntity taskRelationshipEntity : taskRelationshipEntities) {
                if (checkerSetId.equalsIgnoreCase(taskRelationshipEntity.getCheckerSetId())) {
                    taskRelationshipEntityMap.put(taskRelationshipEntity.getTaskId(), taskRelationshipEntity);
                    selectCheckerSetEntities.add(taskRelationshipEntity);
                }
            }
        }

        /*
         * 1、不允许删除非本项目的规则集，
         * 2、不允许卸载本项目的规则集
         * 3、已在任务中使用的规则集不允许删除或卸载
         */
        if (checkerSetManagementReqVO.getDeleteCheckerSet() != null
                && checkerSetManagementReqVO.getDeleteCheckerSet()) {
            if (!checkerSetEntities.get(0).getProjectId().equals(checkerSetManagementReqVO.getProjectId())) {
                String errMsg = "不允许删除非本项目的规则集";
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
            if (CollectionUtils.isNotEmpty(selectCheckerSetEntities)) {
                String errMsg = "该项目下还有任务使用此规则集，不允许删除或卸载";
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
        }
        if (checkerSetManagementReqVO.getUninstallCheckerSet() != null
                && checkerSetManagementReqVO.getUninstallCheckerSet()) {
            if (checkerSetEntities.get(0).getProjectId().equals(checkerSetManagementReqVO.getProjectId())) {
                String errMsg = "不允许卸载本项目的规则集";
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
            if (CollectionUtils.isNotEmpty(selectCheckerSetEntities)) {
                String errMsg = "该项目下还有任务使用此规则集，不允许删除或卸载";
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
        }

        // 查询当前项目关联规则集的列表
        List<CheckerSetProjectRelationshipEntity> projectRelationshipEntities =
                checkerSetProjectRelationshipRepository.findByProjectId(checkerSetManagementReqVO.getProjectId());
        Map<String, CheckerSetProjectRelationshipEntity> projectRelationshipEntityMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(projectRelationshipEntities)) {
            for (CheckerSetProjectRelationshipEntity projectRelationshipEntity : projectRelationshipEntities) {
                projectRelationshipEntityMap.put(projectRelationshipEntity.getCheckerSetId(),
                        projectRelationshipEntity);
            }
        }

        // 获取各任务当前使用的规则集列表
        Map<Long, Map<String, Integer>> currentTaskUseCheckerSetsMap = Maps.newHashMap();
        Map<Long, Map<String, Integer>> updatedTaskUseCheckerSetsMap = Maps.newHashMap();
        for (Map.Entry<Long, CheckerSetTaskRelationshipEntity> entry : taskRelationshipEntityMap.entrySet()) {
            CheckerSetProjectRelationshipEntity projectRelationshipEntity =
                    projectRelationshipEntityMap.get(entry.getValue().getCheckerSetId());
            currentTaskUseCheckerSetsMap.computeIfAbsent(entry.getKey(), k -> Maps.newHashMap());
            currentTaskUseCheckerSetsMap.get(entry.getKey()).put(projectRelationshipEntity.getCheckerSetId(),
                    projectRelationshipEntity.getVersion());
            taskRelationshipEntities.stream()
                    .filter(taskRelationshipEntity -> taskRelationshipEntity.getTaskId()
                            .equals(entry.getKey()))
                    .forEach(taskRelationshipEntity ->
                            currentTaskUseCheckerSetsMap.get(
                                    entry.getKey()).put(taskRelationshipEntity.getCheckerSetId(),
                            projectRelationshipEntityMap.get(
                                    taskRelationshipEntity.getCheckerSetId()).getVersion()));

            updatedTaskUseCheckerSetsMap.computeIfAbsent(entry.getKey(), k -> Maps.newHashMap());
            updatedTaskUseCheckerSetsMap.get(entry.getKey()).put(projectRelationshipEntity.getCheckerSetId(),
                    projectRelationshipEntity.getVersion());
            taskRelationshipEntities.stream()
                    .filter(taskRelationshipEntity -> taskRelationshipEntity
                            .getTaskId()
                            .equals(entry.getKey()))
                    .forEach(taskRelationshipEntity ->
                            updatedTaskUseCheckerSetsMap.get(
                                    entry.getKey()).put(taskRelationshipEntity.getCheckerSetId(),
                            projectRelationshipEntityMap.get(
                                    taskRelationshipEntity.getCheckerSetId()).getVersion()));
        }

        //兼容官方推荐官方优选
        CheckerSetProjectRelationshipEntity sourceProjectRelationEntity = null;

        // 设置项目维度的默认规则集
        if (checkerSetManagementReqVO.getDefaultCheckerSet() != null) {
            CheckerSetProjectRelationshipEntity projectRelationshipEntity =
                    projectRelationshipEntityMap.get(checkerSetId);
            if (null != projectRelationshipEntity) {
                projectRelationshipEntity.setDefaultCheckerSet(checkerSetManagementReqVO.getDefaultCheckerSet());
                checkerSetProjectRelationshipRepository.save(projectRelationshipEntity);
            } else {
                //兼容官方推荐官方优选
                CheckerSetEntity checkerSetEntity = checkerSetEntities.get(0);
                if (Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name())
                        .contains(checkerSetEntity.getCheckerSetSource())) {
                    sourceProjectRelationEntity = new CheckerSetProjectRelationshipEntity();
                    sourceProjectRelationEntity.setCheckerSetId(checkerSetId);
                    sourceProjectRelationEntity.setProjectId(checkerSetManagementReqVO.getProjectId());
                    sourceProjectRelationEntity.setDefaultCheckerSet(checkerSetManagementReqVO.getDefaultCheckerSet());
                }
            }
        }

        // 规则集的可见范围、是否设为默认都要更新到所有版本
        for (CheckerSetEntity checkerSetEntity : checkerSetEntities) {
            if (checkerSetManagementReqVO.getScope() != null) {
                checkerSetEntity.setScope(checkerSetManagementReqVO.getScope());
            }

            // 从本项目删除后，规则集需要设置为私有，这样其他没安装的项目就找不到了
            if (checkerSetManagementReqVO.getDeleteCheckerSet() != null
                    && checkerSetManagementReqVO.getDeleteCheckerSet()) {
                checkerSetEntity.setScope(CheckerConstants.CheckerSetScope.PRIVATE.code());
            }
        }
        checkerSetRepository.saveAll(checkerSetEntities);

        // 从本项目中卸载规则集，或者删除本项目的规则集，都要删除关联数据
        CheckerSetProjectRelationshipEntity relationshipEntity = projectRelationshipEntityMap.get(checkerSetId);
        if ((checkerSetManagementReqVO.getDeleteCheckerSet() != null && checkerSetManagementReqVO.getDeleteCheckerSet())
                || (checkerSetManagementReqVO.getUninstallCheckerSet() != null
                && checkerSetManagementReqVO.getUninstallCheckerSet())) {
            if (relationshipEntity != null) {
                checkerSetProjectRelationshipRepository.delete(relationshipEntity);
            }

            // 修改更新后的任务规则集列表
            for (Map.Entry<Long, Map<String, Integer>> entry : updatedTaskUseCheckerSetsMap.entrySet()) {
                if (entry.getValue().containsKey(checkerSetId)) {
                    entry.getValue().remove(checkerSetId);
                }
            }
        }

        // 切换项目关联的规则集版本
        if (checkerSetManagementReqVO.getVersionSwitchTo() != null) {
            if (relationshipEntity != null) {
                if (checkerSetManagementReqVO.getVersionSwitchTo() == Integer.MAX_VALUE) {
                    Map<String, Integer> latestVersionMap = getLatestVersionMap(Sets.newHashSet(checkerSetId));
                    relationshipEntity.setVersion(latestVersionMap.get(checkerSetId));
                    relationshipEntity.setUselatestVersion(true);
                } else {
                    relationshipEntity.setVersion(checkerSetManagementReqVO.getVersionSwitchTo());
                    relationshipEntity.setUselatestVersion(false);
                }
            } else {
                CheckerSetEntity checkerSetEntity = checkerSetEntities.get(0);
                if (Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name())
                        .contains(checkerSetEntity.getCheckerSetSource())) {
                    if (null == sourceProjectRelationEntity) {
                        sourceProjectRelationEntity = new CheckerSetProjectRelationshipEntity();
                    }
                    sourceProjectRelationEntity.setCheckerSetId(checkerSetId);
                    sourceProjectRelationEntity.setProjectId(checkerSetManagementReqVO.getProjectId());
                    if (checkerSetManagementReqVO.getVersionSwitchTo() == Integer.MAX_VALUE) {
                        Map<String, Integer> latestVersionMap =
                                getLatestVersionMap(Sets.newHashSet(checkerSetEntity.getCheckerSetId()));
                        sourceProjectRelationEntity.setVersion(latestVersionMap.get(checkerSetId));
                        sourceProjectRelationEntity.setUselatestVersion(true);
                    } else {
                        sourceProjectRelationEntity.setVersion(checkerSetManagementReqVO.getVersionSwitchTo());
                        sourceProjectRelationEntity.setUselatestVersion(false);
                    }
                    if (null == sourceProjectRelationEntity.getDefaultCheckerSet()) {
                        if (CheckerSetSource.DEFAULT.name().equals(checkerSetEntity.getCheckerSetSource())) {
                            sourceProjectRelationEntity.setDefaultCheckerSet(true);
                        } else {
                            sourceProjectRelationEntity.setDefaultCheckerSet(false);
                        }
                    }
                }
            }
            if (relationshipEntity != null) {
                checkerSetProjectRelationshipRepository.save(relationshipEntity);
            }

            // 修改更新后的任务规则集列表
            for (Map.Entry<Long, Map<String, Integer>> entry : updatedTaskUseCheckerSetsMap.entrySet()) {
                if (entry.getValue().containsKey(checkerSetId)) {
                    entry.getValue().put(checkerSetId, relationshipEntity.getVersion());
                }
            }
        }

        if (null != sourceProjectRelationEntity) {
            checkerSetProjectRelationshipRepository.save(sourceProjectRelationEntity);
        }

        // 任务不再使用该规则集
        if (checkerSetManagementReqVO.getDiscardFromTask() != null) {
            CheckerSetTaskRelationshipEntity taskRelationshipEntity =
                    taskRelationshipEntityMap.get(checkerSetManagementReqVO.getDiscardFromTask());
            if (taskRelationshipEntity != null) {
                List<CheckerSetTaskRelationshipEntity> currentTaskRelationships =
                        checkerSetTaskRelationshipRepository.findByTaskId(checkerSetManagementReqVO.getDiscardFromTask());
                if (currentTaskRelationships.size() == 1) {
                    String errMsg = "任务必须至少使用一个规则集！";
                    log.error(errMsg);
                    throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
                }
                checkerSetTaskRelationshipRepository.delete(taskRelationshipEntity);
            }

            // 修改更新后的任务规则集列表
            Map<String, Integer> taskCheckerSetVersionMap =
                    updatedTaskUseCheckerSetsMap.get(checkerSetManagementReqVO.getDiscardFromTask());
            taskCheckerSetVersionMap.remove(checkerSetId);
        }

        // 对各任务设置强制全量扫描标志，并修改告警状态
        ThreadPoolUtil.addRunnableTask(() -> {
            setForceFullScanAndUpdateDefectAndToolStatus(currentTaskUseCheckerSetsMap, updatedTaskUseCheckerSetsMap,
                    user);
        });
    }

    @Override
    public List<CheckerSetVO> queryCheckerSets(Set<String> checkerSetList, String projectId) {
        List<CheckerSetProjectRelationshipEntity> relationshipList =
                checkerSetProjectRelationshipRepository.findByCheckerSetIdInAndProjectId(checkerSetList, projectId);
        Set<String> relationshipSet;
        if (CollectionUtils.isNotEmpty(relationshipList)) {
            relationshipSet = relationshipList.stream()
                    .map(relationship -> relationship.getCheckerSetId() + "_" + relationship.getVersion())
                    .collect(Collectors.toSet());
        } else {
            relationshipSet = new HashSet<>();
        }

        List<CheckerSetEntity> checkerSets = checkerSetDao.findByComplexCheckerSetCondition(null,
                checkerSetList, null, null, null, null, null,
                true, null, true);

        //官方优选 官方推荐版本
        Map<String, Integer> officialMap = checkerSets.stream().filter(checkerSetEntity ->
                Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name())
                        .contains(checkerSetEntity.getCheckerSetSource())
                        && checkerSetList.contains(checkerSetEntity.getCheckerSetId()))
                .collect(Collectors.groupingBy(CheckerSetEntity::getCheckerSetId)).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().max(Comparator.comparingInt(CheckerSetEntity::getVersion))
                                .orElse(new CheckerSetEntity()).getVersion()));

        if (CollectionUtils.isNotEmpty(checkerSets)) {
            List<CheckerSetEntity> finalCheckerSet = new ArrayList<>();
            Set<String> finalCheckerSetId = new HashSet<>();
            checkerSets.forEach(checkerSetEntity -> {
                // 优先取关系表的规则集
                if (relationshipSet.contains(checkerSetEntity.getCheckerSetId()
                        + "_" + checkerSetEntity.getVersion())) {
                    finalCheckerSet.add(checkerSetEntity);
                    finalCheckerSetId.add(checkerSetEntity.getCheckerSetId());
                    return;
                }

                // 没在关系表，但在官方推荐的，版本号也对应上的， 也可以取
                if (!finalCheckerSetId.contains(checkerSetEntity.getCheckerSetId()) && checkerSetEntity.getVersion()
                        .equals(officialMap.get(checkerSetEntity.getCheckerSetId()))) {
                    finalCheckerSet.add(checkerSetEntity);
                    finalCheckerSetId.add(checkerSetEntity.getCheckerSetId());
                }
            });

            return finalCheckerSet.stream().map(checkerSetEntity ->
            {
                CheckerSetVO checkerSetVO = new CheckerSetVO();
                BeanUtils.copyProperties(checkerSetEntity, checkerSetVO);
                if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())) {
                    checkerSetVO.setToolList(checkerSetEntity.getCheckerProps().stream()
                            .map(CheckerPropsEntity::getToolName).collect(Collectors.toSet()));
                }
                return checkerSetVO;
            }).collect(Collectors.toList());
        }

        log.error("project {} has not install checker set: {}", projectId, checkerSetList);
        return null;
    }

    @Override
    public List<CheckerSetVO> getCheckerSetsByTaskId(Long taskId) {
        //查出任务维度的id集合
        List<CheckerSetTaskRelationshipEntity> checkerSetTaskRelationshipEntityList =
                checkerSetTaskRelationshipRepository.findByTaskId(taskId);
        if (CollectionUtils.isEmpty(checkerSetTaskRelationshipEntityList)) {
            return new ArrayList<>();
        }
        Set<String> checkerSetIds = checkerSetTaskRelationshipEntityList.stream().
                map(CheckerSetTaskRelationshipEntity::getCheckerSetId).
                collect(Collectors.toSet());
        String projectId = checkerSetTaskRelationshipEntityList.get(0).getProjectId();

        Result<GrayToolProjectVO> result = client.get(ServiceGrayToolProjectResource.class)
                .getGrayToolProjectInfoByProjrctId(projectId);

        if (result.isNotOk() || result.getData() == null) {
            log.error("getGrayToolProjectInfoByProjrctId fail. projectId: {}, taskId: {}", projectId, taskId);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        GrayToolProjectVO grayToolProjectVO = result.getData();
        int grayToolProjectStatus = grayToolProjectVO.getStatus();
        List<CheckerSetProjectRelationshipEntity> checherSetProjectRelateEntityList =
                checkerSetProjectRelationshipRepository.findByCheckerSetIdInAndProjectId(checkerSetIds, projectId);
        // 计算规则集的使用量
        Map<String, Long> checkerSetCountMap = checkerSetTaskRelationshipEntityList.stream().
                collect(Collectors.groupingBy(CheckerSetTaskRelationshipEntity::getCheckerSetId,
                        Collectors.counting()));

        Map<String, CheckerSetProjectRelationshipEntity> checkerSetRelationshipMap =
                checherSetProjectRelateEntityList.stream()
                        .collect(Collectors.toMap(it -> it.getCheckerSetId(), Function.identity(), (k, v) -> v));

        List<CheckerSetEntity> checkerSetEntityList = checkerSetRepository.findByCheckerSetIdIn(checkerSetIds);

        /* 过滤出符合项目的规则集：
         * 如果uselatestVersion为true是测试/灰度项目，并且是uselatestVersion为true，则优先取测试/灰度规则集；
         * 否则，取项目绑定的规则集版本
         */
        List<CheckerSetEntity> selectCheckerSetList = Lists.newArrayList();
        Map<String, List<CheckerSetEntity>> checkerSetMap =
                checkerSetEntityList.stream().collect(Collectors.groupingBy(CheckerSetEntity::getCheckerSetId));
        checkerSetMap.forEach((checkerSetId, checkerSetList) -> {
            CheckerSetEntity selectCheckerSet = null;
            for (CheckerSetEntity checkerSetEntity : checkerSetList) {
                CheckerSetProjectRelationshipEntity projectRelationship = checkerSetRelationshipMap.get(checkerSetId);
                if (projectRelationship == null) {
                    continue;
                }
                if (checkerSetEntity.getVersion() == null) {
                    continue;
                }
                if (((projectRelationship.getUselatestVersion() != null && projectRelationship.getUselatestVersion())
                        || grayToolProjectVO.isOpenSourceProject())
                        && grayToolProjectStatus == checkerSetEntity.getVersion()) {
                    selectCheckerSet = checkerSetEntity;
                    break;
                }

                if (checkerSetEntity.getVersion().equals(projectRelationship.getVersion())) {
                    selectCheckerSet = checkerSetEntity;
                }
            }
            if (selectCheckerSet != null) {
                selectCheckerSetList.add(selectCheckerSet);
            }
        });

        // 按版本过滤，按使用量排序
        return selectCheckerSetList.stream()
                .sorted(Comparator.comparingLong(o -> checkerSetCountMap.containsKey(o.getCheckerSetId())
                        ? -checkerSetCountMap.get(o.getCheckerSetId()) : 0L))
                .map(checkerSetEntity -> {
                    CheckerSetVO checkerSetVO = new CheckerSetVO();
                    BeanUtils.copyProperties(checkerSetEntity, checkerSetVO);
                    Integer useCount = checkerSetCountMap.get(checkerSetVO.getCheckerSetId()) == null ? 0 :
                            Integer.valueOf(checkerSetCountMap.get(checkerSetVO.getCheckerSetId()).toString());
                    checkerSetVO.setTaskUsage(useCount);
                    if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())) {
                        Set<String> toolList = new HashSet<>();
                        checkerSetVO.setCheckerProps(checkerSetEntity.getCheckerProps().stream()
                                .map(checkerPropsEntity ->
                                {
                                    toolList.add(checkerPropsEntity.getToolName());
                                    CheckerPropVO checkerPropVO = new CheckerPropVO();
                                    BeanUtils.copyProperties(checkerPropsEntity, checkerPropVO);
                                    return checkerPropVO;
                                }).collect(Collectors.toList()));
                        checkerSetVO.setToolList(toolList);
                    }

                    CheckerSetProjectRelationshipEntity projectRelationshipEntity =
                            checkerSetRelationshipMap.get(checkerSetEntity.getCheckerSetId());
                    if ((projectRelationshipEntity != null && null != projectRelationshipEntity.getDefaultCheckerSet()
                            && projectRelationshipEntity.getDefaultCheckerSet()) || (CheckerSetSource.DEFAULT.name().
                            equals(checkerSetEntity.getCheckerSetSource()) && null == projectRelationshipEntity)) {
                        checkerSetVO.setDefaultCheckerSet(true);
                    } else {
                        checkerSetVO.setDefaultCheckerSet(false);
                    }

                    return checkerSetVO;
                }).collect(Collectors.toList());
    }

    @Override
    public List<CheckerCommonCountVO> queryCheckerSetCountList(CheckerSetListQueryReq checkerSetListQueryReq) {

        String projectId = checkerSetListQueryReq.getProjectId();
        Result<GrayToolProjectVO> result = client.get(ServiceGrayToolProjectResource.class)
                .getGrayToolProjectInfoByProjrctId(projectId);

        if (result.isNotOk() || result.getData() == null) {
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
        }

        boolean isP = result.getData().getStatus() == ToolIntegratedStatus.P.value();
        int toolIntegratedStatus = result.getData().getStatus();

        //1. 语言数量map
        Map<String, Integer> langMap = new HashMap<>();
        List<String> langOrder =
                Arrays.asList(redisTemplate.opsForValue().get(RedisKeyConstants.KEY_LANG_ORDER).split(","));
        for (String codeLang : langOrder) {
            langMap.put(codeLang, 0);
        }
        //2.规则类别数量map
        CheckerSetCategory[] checkerSetCategoryList = CheckerSetCategory.values();
        Map<String, Integer> checkerSetCateMap = new HashMap<>();
        for (CheckerSetCategory checkerSetCategory : checkerSetCategoryList) {
            checkerSetCateMap.put(checkerSetCategory.name(), 0);
        }
        //3.工具类别数量map
        Map<String, Integer> toolMap = new HashMap<>();
        List<String> toolOrder =
                Arrays.asList(redisTemplate.opsForValue().get(RedisKeyConstants.KEY_TOOL_ORDER).split(","));
        for (String tool : toolOrder) {
            toolMap.put(tool, 0);
        }
        //4.来源数量筛选
        CheckerSetSource[] checkerSetSources = CheckerSetSource.values();
        Map<String, Integer> sourceMap = new HashMap<>();
        for (CheckerSetSource checkerSetSource : checkerSetSources) {
            sourceMap.put(checkerSetSource.name(), 0);
        }
        //5.总数
        List<CheckerSetEntity> totalList = new ArrayList<>();
        List<CheckerCommonCountVO> checkerCommonCountVOList = new ArrayList<>();
        if (StringUtils.isEmpty(projectId)) {
            log.error("project id is empty!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"project id"}, null);
        }
        List<CheckerSetProjectRelationshipEntity> checkerSetRelationshipRepositoryList =
                checkerSetProjectRelationshipRepository.findByProjectId(projectId);

        Map<String, Boolean> checkerLatestMap = checkerSetRelationshipRepositoryList.stream()
                .peek(checkerSetProjectRelationshipEntity -> {
                    if (checkerSetProjectRelationshipEntity.getUselatestVersion() == null) {
                        checkerSetProjectRelationshipEntity.setUselatestVersion(false);
                    }
                })
                .collect(Collectors.toMap(CheckerSetProjectRelationshipEntity::getCheckerSetId,
                        CheckerSetProjectRelationshipEntity::getUselatestVersion,
                        (v1, v2) -> v2));

        Set<String> checkerSetIds;
        if (CollectionUtils.isNotEmpty(checkerSetRelationshipRepositoryList)) {
            checkerSetIds = checkerSetRelationshipRepositoryList.stream()
                    .map(CheckerSetProjectRelationshipEntity::getCheckerSetId).collect(Collectors.toSet());
        } else {
            checkerSetIds = new HashSet<>();
        }

        List<CheckerSetEntity> checkerSetEntityList =
                checkerSetDao.findByComplexCheckerSetCondition(checkerSetListQueryReq.getKeyWord(),
                        checkerSetIds, null, null, null, null, null, true,
                        null, true);
        if (CollectionUtils.isNotEmpty(checkerSetEntityList)) {
            List<CheckerSetEntity> finalCheckerList = checkerSetEntityList.stream().collect(Collectors.groupingBy(
                    CheckerSetEntity::getCheckerSetId)).entrySet().stream().map(entry ->
            {
                List<CheckerSetEntity> checkerSetEntities = entry.getValue();
                Map<Integer, CheckerSetEntity> versionMap = checkerSetEntities.stream()
                        .collect(Collectors.toMap(CheckerSetEntity::getVersion, checkerSetEntity -> checkerSetEntity,
                                (v1, v2) -> v2));
                if (!isP && versionMap.get(toolIntegratedStatus) != null
                        && checkerLatestMap.get(entry.getKey()) != null
                        && checkerLatestMap.get(entry.getKey())) {
                    return versionMap.get(toolIntegratedStatus);
                }

                CheckerSetEntity finalCheckerSet = checkerSetEntities.stream()
                        .max(Comparator.comparing(CheckerSetEntity::getVersion))
                        .orElse(new CheckerSetEntity());
                if (isP && finalCheckerSet.getVersion() < 0) {
                    return null;
                } else {
                    return finalCheckerSet;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
            finalCheckerList.stream()
                    .filter(checkerSetEntity -> {
                        if (checkerSetEntity == null) {
                            return false;
                        } else if (!isP && checkerSetEntity.getVersion() < 0
                                && checkerSetEntity.getVersion() != toolIntegratedStatus) {
                            return false;
                        }
                        return !isP || checkerSetEntity.getVersion() >= 0;
                    })
                    .forEach(checkerSetEntity -> {
                        //1. 计算语言数量
                        if (judgeQualifiedCheckerSet(null, checkerSetListQueryReq.getCheckerSetCategory(),
                                checkerSetListQueryReq.getToolName(), checkerSetListQueryReq.getCheckerSetSource(),
                                checkerSetEntity) && StringUtils.isNotEmpty(checkerSetEntity.getCheckerSetLang())) {
                            //要分新插件和老插件
                            if (null != checkerSetEntity.getLegacy() && checkerSetEntity.getLegacy()) {
                                if (CollectionUtils.isNotEmpty(
                                        Arrays.asList(checkerSetEntity.getCheckerSetLang().split(",")))) {
                                    for (String lang : checkerSetEntity.getCheckerSetLang().split(",")) {
                                        langMap.compute(lang, (k, v) -> {
                                            if (null == v) {
                                                return 1;
                                            } else {
                                                v++;
                                                return v;
                                            }
                                        });
                                    }
                                }
                            } else {
                                langMap.compute(checkerSetEntity.getCheckerSetLang(), (k, v) -> {
                                    if (null == v) {
                                        return 1;
                                    } else {
                                        v++;
                                        return v;
                                    }
                                });
                            }
                        }
                        //2. 规则类别数量计算
                        if (judgeQualifiedCheckerSet(checkerSetListQueryReq.getCheckerSetLanguage(), null,
                                checkerSetListQueryReq.getToolName(),
                                checkerSetListQueryReq.getCheckerSetSource(), checkerSetEntity)
                                && CollectionUtils.isNotEmpty(checkerSetEntity.getCatagories())) {
                            checkerSetEntity.getCatagories().forEach(category ->
                                    checkerSetCateMap.compute(category.getEnName(), (k, v) -> {
                                        if (null == v) {
                                            return 1;
                                        } else {
                                            v++;
                                            return v;
                                        }
                                    })
                            );
                        }
                        //3. 工具数量计算
                        if (judgeQualifiedCheckerSet(checkerSetListQueryReq.getCheckerSetLanguage(),
                                checkerSetListQueryReq.getCheckerSetCategory(), null,
                                checkerSetListQueryReq.getCheckerSetSource(), checkerSetEntity)
                                && CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())) {
                            checkerSetEntity.getCheckerProps().stream().map(CheckerPropsEntity::getToolName).distinct()
                                    .forEach(tool -> {
                                        if (StringUtils.isBlank(tool)) {
                                            return;
                                        }
                                        toolMap.compute(tool, (k, v) -> {
                                            if (null == v) {
                                                return 1;
                                            } else {
                                                v++;
                                                return v;
                                            }
                                        });
                                    });
                        }
                        //4. 来源数量计算
                        if (judgeQualifiedCheckerSet(checkerSetListQueryReq.getCheckerSetLanguage(),
                                checkerSetListQueryReq.getCheckerSetCategory(),
                                checkerSetListQueryReq.getToolName(), null, checkerSetEntity)) {
                            sourceMap.compute(StringUtils.isBlank(checkerSetEntity.getCheckerSetSource())
                                    ? "SELF_DEFINED" : checkerSetEntity.getCheckerSetSource(), (k, v) ->
                            {
                                if (null == v) {
                                    return 1;
                                } else {
                                    v++;
                                    return v;
                                }
                            });
                        }

                        //5. 总数计算
                        if (judgeQualifiedCheckerSet(checkerSetListQueryReq.getCheckerSetLanguage(),
                                checkerSetListQueryReq.getCheckerSetCategory(),
                                checkerSetListQueryReq.getToolName(), checkerSetListQueryReq.getCheckerSetSource(),
                                checkerSetEntity)) {
                            totalList.add(checkerSetEntity);
                        }

                    });
        }

        //按照语言顺序
        List<CheckerCountListVO> checkerSetLangCountVOList = langMap.entrySet().stream().map(entry ->
                new CheckerCountListVO(entry.getKey(), null, entry.getValue())
        ).sorted(Comparator.comparingInt(o -> langOrder.indexOf(o.getKey()))).collect(Collectors.toList());
        //按照类别枚举排序
        List<CheckerSetCategory> categoryOrder = Arrays.asList(CheckerSetCategory.values());
        List<CheckerCountListVO> checkerSetCateCountVOList = checkerSetCateMap.entrySet().stream().map(entry ->
                new CheckerCountListVO(CheckerSetCategory.valueOf(entry.getKey()).name(),
                        CheckerSetCategory.valueOf(entry.getKey()).getName(), entry.getValue())
        ).sorted(Comparator.comparingInt(o -> categoryOrder.indexOf(CheckerSetCategory.valueOf(o.getKey())))).
                collect(Collectors.toList());
        //按照工具的排序
        List<CheckerCountListVO> checkerSetToolCountVOList = toolMap.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> new CheckerCountListVO(entry.getKey(), null, entry.getValue()))
                .sorted(Comparator.comparingInt(o -> toolOrder.indexOf(o.getKey()))).collect(Collectors.toList());

        List<CheckerSetSource> sourceOrder = Arrays.asList(CheckerSetSource.values());
        List<CheckerCountListVO> checkerSetSourceCountVOList = sourceMap.entrySet().stream().map(entry ->
                new CheckerCountListVO(CheckerSetSource.valueOf(entry.getKey()).name(),
                        CheckerSetSource.valueOf(entry.getKey()).getName(), entry.getValue())
        ).sorted(Comparator.comparingInt(o -> sourceOrder.indexOf(CheckerSetSource.valueOf(o.getKey())))).
                collect(Collectors.toList());
        List<CheckerCountListVO> checkerSetTotalCountVOList = Collections.singletonList(new CheckerCountListVO("total",
                null, totalList.size()));

        checkerCommonCountVOList.add(new CheckerCommonCountVO("checkerSetLanguage", checkerSetLangCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("checkerSetCategory", checkerSetCateCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("toolName", checkerSetToolCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("checkerSetSource", checkerSetSourceCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("total", checkerSetTotalCountVOList));
        return checkerCommonCountVOList;

    }

    @Override
    public List<CheckerSetEntity> findAvailableCheckerSetsByProject(String projectId,
                                                                    List<Boolean> legacy,
                                                                    int toolIntegratedStatus) {
        List<CheckerSetProjectRelationshipEntity> checkerSetProjectRelationshipEntityList =
                checkerSetProjectRelationshipRepository
                        .findByProjectId(projectId);
        Set<String> checkerSetIds;
        Map<String, Integer> checkerSetVersionMap;
        Map<String, Boolean> latestVersionCheckerSetMap;
        if (CollectionUtils.isEmpty(checkerSetProjectRelationshipEntityList)) {
            checkerSetIds = new HashSet<>();
            checkerSetVersionMap = new HashMap<>();
            latestVersionCheckerSetMap = new HashMap<>();
        } else {
            checkerSetIds = checkerSetProjectRelationshipEntityList.stream().
                    map(CheckerSetProjectRelationshipEntity::getCheckerSetId).
                    collect(Collectors.toSet());
            checkerSetVersionMap = checkerSetProjectRelationshipEntityList.stream().
                    filter(checkerSetProjectRelationshipEntity ->
                            StringUtils.isNotBlank(checkerSetProjectRelationshipEntity.getCheckerSetId()) &&
                                    null != checkerSetProjectRelationshipEntity.getVersion()
                    ).
                    collect(Collectors.toMap(CheckerSetProjectRelationshipEntity::getCheckerSetId,
                            CheckerSetProjectRelationshipEntity::getVersion, (k, v) -> v));
            latestVersionCheckerSetMap = checkerSetProjectRelationshipEntityList.stream()
                    .filter(Objects::nonNull)
                    .peek(checkerSetProjectRelationshipEntity -> {
                        if (checkerSetProjectRelationshipEntity.getUselatestVersion() == null) {
                            checkerSetProjectRelationshipEntity.setUselatestVersion(false);
                        }
                    })
                    .collect(Collectors.toMap(
                            CheckerSetProjectRelationshipEntity::getCheckerSetId,
                            CheckerSetProjectRelationshipEntity::getUselatestVersion,
                            (v1, v2) -> v2));
        }

        List<CheckerSetEntity> checkerSetEntityList = checkerSetDao.findByComplexCheckerSetCondition(null,
                checkerSetIds, null, null, null, null, null, true,
                null, true);

        boolean isP = toolIntegratedStatus == ToolIntegratedStatus.P.value();
        Map<String, CheckerSetEntity> checkerSetEntityMap = new HashMap<>();
        List<String> commonProps = Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name());
        for (CheckerSetEntity checkerSetEntity : checkerSetEntityList) {
            if (CollectionUtils.isEmpty(checkerSetEntity.getCheckerProps())) {
                continue;
            }

            if (!isP && checkerSetEntityMap.get(checkerSetEntity.getCheckerSetId()) != null
                    && checkerSetEntityMap.get(checkerSetEntity.getCheckerSetId())
                    .getVersion() == toolIntegratedStatus) {
                continue;
            }

            // checkerSetEntity.getLegacy() == legacy 或者 legacy == false时checkerSetEntity.getLegacy() == null
            boolean legacyCond = legacy.contains(checkerSetEntity.getLegacy())
                    || (CollectionUtils.isNotEmpty(legacy)
                    && legacy.contains(false)
                    && checkerSetEntity.getLegacy() == null);
            if (!legacyCond) {
                continue;
            }

            // 匹配规则
            String checkerSetId = checkerSetEntity.getCheckerSetId();
            CheckerSetEntity item = checkerSetEntityMap.get(checkerSetId);
            Integer checkerSetVersion = checkerSetVersionMap.get(checkerSetEntity.getCheckerSetId());

            // 灰度 项目，但规则集是测试 或 测试 项目，但规则集是会的
            if ((!isP && checkerSetEntity.getVersion() != null
                    && checkerSetEntity.getVersion() < 0
                    && checkerSetEntity.getVersion() != toolIntegratedStatus)
                    || isP && checkerSetEntity.getVersion() != null
                    && checkerSetEntity.getVersion() < 0) {
                continue;
            }

            if (!isP && checkerSetEntity.getVersion() != null
                    && checkerSetEntity.getVersion() == toolIntegratedStatus
                    && latestVersionCheckerSetMap.get(checkerSetId) != null
                    && latestVersionCheckerSetMap.get(checkerSetId)) {
                checkerSetEntityMap.put(checkerSetId, checkerSetEntity);
                continue;
            }
            boolean matchVersion = (null != checkerSetEntity.getVersion()
                    && null != checkerSetVersionMap.get(checkerSetEntity.getCheckerSetId())
                    && checkerSetEntity.getVersion().equals(checkerSetVersion));
            boolean matchCommonProp = commonProps.contains(checkerSetEntity.getCheckerSetSource());

            // 优先用版本匹配上的
            if (item != null) {
                if (matchVersion) {
                    checkerSetEntityMap.put(checkerSetId, checkerSetEntity);
                } else {
                    if (item.getVersion() < checkerSetEntity.getVersion()) {
                        checkerSetEntityMap.put(checkerSetId, checkerSetEntity);
                    }
                }
                continue;
            }

            if (matchVersion || matchCommonProp) {
                checkerSetEntityMap.put(checkerSetId, checkerSetEntity);
            }
        }

        return new ArrayList<>(checkerSetEntityMap.values());
    }

    @Override
    public Boolean updateCheckerSetAndTaskRelation(Long taskId, Long codeLang, String user) {
        List<CheckerSetTaskRelationshipEntity> checkerSetTaskRelationshipEntityList =
                checkerSetTaskRelationshipRepository.findByTaskId(taskId);
        List<CheckerSetEntity> projectCheckerSetList;
        Map<String, Integer> oldCheckerSetVersionMap = Maps.newHashMap();
        Map<String, Integer> newCheckerSetVersionMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(checkerSetTaskRelationshipEntityList)) {
            String projectId = checkerSetTaskRelationshipEntityList.get(0).getProjectId();
            List<CheckerSetProjectRelationshipEntity> checkerSetProjectRelationshipEntityList =
                    checkerSetProjectRelationshipRepository.findByProjectId(projectId);
            Set<String> checkerSetIds;

            if (CollectionUtils.isEmpty(checkerSetProjectRelationshipEntityList)) {
                projectCheckerSetList = new ArrayList<>();
                checkerSetIds = new HashSet<>();
            } else {
                checkerSetIds = checkerSetProjectRelationshipEntityList.stream()
                        .map(CheckerSetProjectRelationshipEntity::getCheckerSetId).collect(Collectors.toSet());

                Map<String, Integer> checkerSetVersionMap = checkerSetProjectRelationshipEntityList.stream().
                        collect(HashMap::new, (m, v) -> m.put(v.getCheckerSetId(), v.getVersion()), HashMap::putAll);

                List<CheckerSetEntity> checkerSetEntityList = checkerSetDao.findByComplexCheckerSetCondition(null,
                        checkerSetIds, null, null, null, null, null, true, null, true);

                projectCheckerSetList = checkerSetEntityList.stream().filter(it ->
                        it.getVersion().equals(checkerSetVersionMap.get(it.getCheckerSetId()))
                                || CheckerSetSource.DEFAULT.name().equals(it.getCheckerSetSource())
                                || CheckerSetSource.RECOMMEND.name().equals(it.getCheckerSetSource())
                ).collect(Collectors.toList());
                oldCheckerSetVersionMap.putAll(checkerSetTaskRelationshipEntityList.stream()
                        .collect(Collectors.toMap(CheckerSetTaskRelationshipEntity::getCheckerSetId,
                                checkerSetTaskRelationEntity -> checkerSetVersionMap
                                        .get(checkerSetTaskRelationEntity.getCheckerSetId()), (k, v) -> v)));
                newCheckerSetVersionMap.putAll(checkerSetTaskRelationshipEntityList.stream()
                        .collect(Collectors.toMap(CheckerSetTaskRelationshipEntity::getCheckerSetId,
                                checkerSetTaskRelationEntity -> checkerSetVersionMap
                                        .get(checkerSetTaskRelationEntity.getCheckerSetId()), (k, v) -> v)));
            }

            //官方优选 官方推荐版本
            Map<String, Integer> officialMap = projectCheckerSetList.stream().filter(it ->
                    CheckerSetSource.DEFAULT.name().equals(it.getCheckerSetSource())
                            || CheckerSetSource.RECOMMEND.name().equals(it.getCheckerSetSource()))
                    .collect(Collectors.groupingBy(CheckerSetEntity::getCheckerSetId))
                    .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                            entry -> entry.getValue().stream()
                                    .max(Comparator.comparingInt(CheckerSetEntity::getVersion))
                                    .orElse(new CheckerSetEntity()).getVersion()));

            List<CheckerSetEntity> taskCheckerSetList = projectCheckerSetList.stream().filter(checkerSetEntity ->
                    checkerSetTaskRelationshipEntityList.stream().anyMatch(it ->
                            it.getCheckerSetId().equals(checkerSetEntity.getCheckerSetId()))
            ).collect(Collectors.toList());

            //1. 解绑规则集
            Set<String> needToUnbindList = taskCheckerSetList.stream()
                    .filter(checkerSetEntity -> (codeLang & checkerSetEntity.getCodeLang()) == 0)
                    .map(CheckerSetEntity::getCheckerSetId).collect(Collectors.toSet());
            List<CheckerSetTaskRelationshipEntity> needToUnbindRelationEntityList =
                    checkerSetTaskRelationshipEntityList.stream()
                            .filter(it -> needToUnbindList.contains(it.getCheckerSetId())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(needToUnbindRelationEntityList)) {
                checkerSetTaskRelationshipRepository.deleteAll(needToUnbindRelationEntityList);
                for (CheckerSetTaskRelationshipEntity unbindRelationEntity : needToUnbindRelationEntityList) {
                    newCheckerSetVersionMap.remove(unbindRelationEntity.getCheckerSetId());
                }
            }

            //2. 新增语言自动绑定默认规则集
            Set<String> needToBindCheckerSet = new HashSet<>();
            Map<String, CheckerSetProjectRelationshipEntity> projectRelationshipEntityMap =
                    checkerSetProjectRelationshipEntityList.stream().collect(Collectors.toMap(
                            CheckerSetProjectRelationshipEntity::getCheckerSetId, Function.identity(), (k, v) -> v));
            String binaryCodeLang = Long.toBinaryString(codeLang);
            Long originalCodeLang = 1L << (binaryCodeLang.length() - 1);
            for (int i = 0; i < binaryCodeLang.length(); i++) {
                if ((binaryCodeLang.charAt(i) + "").equals("1")) {
                    Long selectedCodeLang = originalCodeLang >> i;
                    if (taskCheckerSetList.stream().allMatch(taskCheckerSetEntity ->
                            (selectedCodeLang & taskCheckerSetEntity.getCodeLang()) == 0L)) {
                        needToBindCheckerSet.addAll(projectCheckerSetList.stream().filter(checkerSet -> {
                            CheckerSetProjectRelationshipEntity projectRelationship =
                                    projectRelationshipEntityMap.get(checkerSet.getCheckerSetId());
                            //条件1, 符合相应语言的
                            return (checkerSet.getCodeLang() & selectedCodeLang) > 0L
                                    //条件2, 默认的
                                    && ((CheckerSetSource.DEFAULT.name().equals(checkerSet.getCheckerSetSource())
                                    && null == projectRelationship)
                                    || (null != projectRelationship
                                    && null != projectRelationship.getDefaultCheckerSet()
                                    && projectRelationship.getDefaultCheckerSet()))
                                    //条件3，非legacy
                                    && !(null != checkerSet.getLegacy() && checkerSet.getLegacy());
                        }).map(CheckerSetEntity::getCheckerSetId).collect(Collectors.toSet()));
                    }
                }
            }

            if (CollectionUtils.isNotEmpty(needToBindCheckerSet)) {
                checkerSetProjectRelationshipRepository.saveAll(
                        needToBindCheckerSet.stream().filter(checkerSetId -> !checkerSetIds.contains(checkerSetId))
                                .map(checkerSetId -> {
                                    CheckerSetProjectRelationshipEntity checkerSetProjectRelationshipEntity =
                                            new CheckerSetProjectRelationshipEntity();
                                    checkerSetProjectRelationshipEntity.setCheckerSetId(checkerSetId);
                                    checkerSetProjectRelationshipEntity.setProjectId(projectId);
                                    checkerSetProjectRelationshipEntity.setDefaultCheckerSet(true);
                                    checkerSetProjectRelationshipEntity.setUselatestVersion(true);
                                    checkerSetProjectRelationshipEntity.setVersion(officialMap.get(checkerSetId));
                                    return checkerSetProjectRelationshipEntity;
                                }).collect(Collectors.toList()));

                checkerSetTaskRelationshipRepository.saveAll(needToBindCheckerSet.stream().filter(StringUtils::isNotBlank)
                        .map(checkerSetId -> {
                            CheckerSetTaskRelationshipEntity checkerSetTaskRelationshipEntity =
                                    new CheckerSetTaskRelationshipEntity();
                            checkerSetTaskRelationshipEntity.setTaskId(taskId);
                            checkerSetTaskRelationshipEntity.setProjectId(projectId);
                            checkerSetTaskRelationshipEntity.setCheckerSetId(checkerSetId);
                            return checkerSetTaskRelationshipEntity;
                        }).collect(Collectors.toSet()));
                for (String checkerSetId : needToBindCheckerSet) {
                    if (officialMap.containsKey(checkerSetId)) {
                        newCheckerSetVersionMap.put(checkerSetId, officialMap.get(checkerSetId));
                    }
                }
            }
            // 对各任务设置强制全量扫描标志，并修改告警状态
            Map<Long, Map<String, Integer>> currentTaskUseCheckerSetsMap = Maps.newHashMap();
            currentTaskUseCheckerSetsMap.put(taskId, oldCheckerSetVersionMap);
            Map<Long, Map<String, Integer>> updatedTaskUseCheckerSetsMap = Maps.newHashMap();
            updatedTaskUseCheckerSetsMap.put(taskId, newCheckerSetVersionMap);
            ThreadPoolUtil.addRunnableTask(() -> setForceFullScanAndUpdateDefectAndToolStatus(
                    currentTaskUseCheckerSetsMap,
                    updatedTaskUseCheckerSetsMap, user));
        }
        return true;
    }

    @Override
    public TaskBaseVO getCheckerAndCheckerSetCount(Long taskId, String projectId) {
        TaskBaseVO taskBaseVO = new TaskBaseVO();
        taskBaseVO.setTaskId(taskId);
        List<CheckerSetTaskRelationshipEntity> checkerSetTaskRelationshipEntityList =
                checkerSetTaskRelationshipRepository.findByTaskId(taskId);
        if (CollectionUtils.isNotEmpty(checkerSetTaskRelationshipEntityList)) {
            Result<TaskDetailVO> taskDetailVOResult =
                    client.get(ServiceTaskRestResource.class).getTaskInfoWithoutToolsByTaskId(taskId);
            Long codeLang;
            if (taskDetailVOResult.isNotOk() || taskDetailVOResult.getData() == null) {
                log.error("task info empty! task id: {}", taskId);
                codeLang = 0L;
            } else {
                codeLang = taskDetailVOResult.getData().getCodeLang();
            }
            Set<String> taskCheckerSetList = checkerSetTaskRelationshipEntityList.stream()
                    .map(CheckerSetTaskRelationshipEntity::getCheckerSetId).collect(Collectors.toSet());
            List<CheckerSetEntity> checkerSetEntityList = findAvailableCheckerSetsByProject(projectId,
                    Arrays.asList(true, false), ToolIntegratedStatus.P.value());
            if (CollectionUtils.isNotEmpty(checkerSetEntityList)) {
                List<CheckerSetEntity> taskCheckerSetEntityList = checkerSetEntityList.stream()
                        .filter(checkerSetEntity -> taskCheckerSetList.contains(checkerSetEntity.getCheckerSetId())
                                && (codeLang & checkerSetEntity.getCodeLang()) > 0L)
                        .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(taskCheckerSetEntityList)) {
                    taskBaseVO.setCheckerSetName(taskCheckerSetEntityList.stream()
                            .map(CheckerSetEntity::getCheckerSetName)
                            .distinct().reduce((o1, o2) -> String.format("%s,%s", o1, o2)).get());
                    taskBaseVO.setCheckerCount(taskCheckerSetEntityList.stream()
                            .filter(checkerSetEntity -> CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps()))
                            .map(CheckerSetEntity::getCheckerProps)
                            .flatMap(Collection::stream).map(CheckerPropsEntity::getCheckerKey).distinct().count());
                    return taskBaseVO;
                }
            }
        }
        taskBaseVO.setCheckerCount(0L);
        taskBaseVO.setCheckerSetName("");
        return taskBaseVO;
    }

    private void setForceFullScanAndUpdateDefectAndToolStatus(
            Map<Long, Map<String, Integer>> currentTaskUseCheckerSetsMap,
            Map<Long, Map<String, Integer>> updatedTaskUseCheckerSetsMap, String user) {
        // 获取所有涉及的规则集Entity
        Set<String> checkerSetIds = Sets.newHashSet();
        for (Map.Entry<Long, Map<String, Integer>> entry : currentTaskUseCheckerSetsMap.entrySet()) {
            checkerSetIds.addAll(entry.getValue().keySet());
        }
        for (Map.Entry<Long, Map<String, Integer>> entry : updatedTaskUseCheckerSetsMap.entrySet()) {
            checkerSetIds.addAll(entry.getValue().keySet());
        }
        Map<String, CheckerSetEntity> checkerSetIdVersionMap = Maps.newHashMap();
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetIdIn(checkerSetIds);
        for (CheckerSetEntity checkerSetEntity : checkerSetEntities) {
            checkerSetIdVersionMap.put(checkerSetEntity.getCheckerSetId() + "_" + checkerSetEntity.getVersion(),
                    checkerSetEntity);
        }

        // 设置强制全量扫描标志并更新告警状态
        for (Map.Entry<Long, Map<String, Integer>> entry : currentTaskUseCheckerSetsMap.entrySet()) {
            List<CheckerSetEntity> fromCheckerSets = Lists.newArrayList();
            List<CheckerSetEntity> toCheckerSets = Lists.newArrayList();
            Map<String, Integer> updatedCheckerSetVersionMap = updatedTaskUseCheckerSetsMap.get(entry.getKey());
            if (MapUtils.isNotEmpty(entry.getValue())) {
                for (Map.Entry<String, Integer> checketSetIdVersionEntry : entry.getValue().entrySet()) {
                    fromCheckerSets.add(checkerSetIdVersionMap.get(checketSetIdVersionEntry.getKey() + "_" + checketSetIdVersionEntry.getValue()));
                }
            }
            if (MapUtils.isNotEmpty(updatedCheckerSetVersionMap)) {
                for (Map.Entry<String, Integer> checketSetIdVersionEntry : updatedCheckerSetVersionMap.entrySet()) {
                    toCheckerSets.add(checkerSetIdVersionMap.get(checketSetIdVersionEntry.getKey() + "_" + checketSetIdVersionEntry.getValue()));
                }
            }
            setForceFullScanAndUpdateDefectAndToolStatus(entry.getKey(), fromCheckerSets, toCheckerSets);
        }

        // 更新工具状态
        for (Map.Entry<Long, Map<String, Integer>> entry : updatedTaskUseCheckerSetsMap.entrySet()) {
            Set<String> updatedToolSet = Sets.newHashSet();
            if (MapUtils.isNotEmpty(entry.getValue())) {
                for (Map.Entry<String, Integer> checkerSetVersionEntry : entry.getValue().entrySet()) {
                    CheckerSetEntity checkerSetEntity = checkerSetIdVersionMap.get(checkerSetVersionEntry.getKey()
                            + "_" + checkerSetVersionEntry.getValue());
                    if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())) {
                        for (CheckerPropsEntity checkerPropsEntity : checkerSetEntity.getCheckerProps()) {
                            if (StringUtils.isNotBlank(checkerPropsEntity.getToolName())) {
                                updatedToolSet.add(checkerPropsEntity.getToolName());
                            }
                        }
                    }
                }
            }

            updateTools(user, entry.getKey(), updatedToolSet);
        }
    }

    private void setForceFullScanAndUpdateDefectAndToolStatus(long taskId, List<CheckerSetEntity> fromCheckerSets,
                                                              List<CheckerSetEntity> toCheckerSets) {
        // 初始化结果对象
        List<CheckerPropsEntity> openDefectCheckerProps = Lists.newArrayList();
        List<CheckerPropsEntity> closeDefectCheckeProps = Lists.newArrayList();
        List<CheckerPropsEntity> updatePropsCheckers = Lists.newArrayList();
        Set<String> toolNames = Sets.newHashSet();

        // 初始化校验用的切换后规则集临时Map
        Map<String, Map<String, CheckerPropsEntity>> toToolCheckersMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(toCheckerSets)) {
            for (CheckerSetEntity checkerSetVO : toCheckerSets) {

                if (CollectionUtils.isNotEmpty(checkerSetVO.getCheckerProps())) {
                    for (CheckerPropsEntity checkerPropVO : checkerSetVO.getCheckerProps()) {
                        toToolCheckersMap.computeIfAbsent(checkerPropVO.getToolName(), k -> Maps.newHashMap());
                        toToolCheckersMap.get(checkerPropVO.getToolName()).put(checkerPropVO.getCheckerKey(),
                                checkerPropVO);
                        toolNames.add(checkerPropVO.getToolName());
                    }
                }
            }
        }

        // 初始化校验用的切换前规则集临时Map，并记录需要关闭和需要更新的规则列表
        Map<String, Map<String, CheckerPropsEntity>> fromToolCheckersMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(fromCheckerSets)) {
            for (CheckerSetEntity checkerSetVO : fromCheckerSets) {

                if (CollectionUtils.isNotEmpty(checkerSetVO.getCheckerProps())) {
                    for (CheckerPropsEntity checkerPropVO : checkerSetVO.getCheckerProps()) {
                        fromToolCheckersMap.computeIfAbsent(checkerPropVO.getToolName(), k -> Maps.newHashMap());
                        fromToolCheckersMap.get(checkerPropVO.getToolName()).put(checkerPropVO.getCheckerKey(),
                                checkerPropVO);

                        if (toToolCheckersMap.get(checkerPropVO.getToolName()) == null
                                || !toToolCheckersMap.get(checkerPropVO.getToolName()).containsKey(checkerPropVO.getCheckerKey())) {
                            closeDefectCheckeProps.add(checkerPropVO);
                        } else {
                            updatePropsCheckers.add(toToolCheckersMap.get(checkerPropVO.getToolName()).get(checkerPropVO.getCheckerKey()));
                        }
                    }
                }
            }
        }

        // 记录需要打开的规则列表
        if (CollectionUtils.isNotEmpty(toCheckerSets)) {
            for (CheckerSetEntity checkerSetVO : toCheckerSets) {
                if (CollectionUtils.isNotEmpty(checkerSetVO.getCheckerProps())) {
                    for (CheckerPropsEntity checkerPropVO : checkerSetVO.getCheckerProps()) {
                        if (fromToolCheckersMap.get(checkerPropVO.getToolName()) == null
                                || !fromToolCheckersMap.get(checkerPropVO.getToolName()).containsKey(checkerPropVO.getCheckerKey())) {
                            openDefectCheckerProps.add(checkerPropVO);
                        }
                    }
                }
            }
        }

        // 设置强制全量扫描
        toolBuildInfoService.setForceFullScan(taskId, Lists.newArrayList(toolNames));

        // 刷新告警状态
        Map<String, ConfigCheckersPkgReqVO> toolDefectRefreshConfigMap = Maps.newHashMap();
        for (CheckerPropsEntity checkerPropsEntity : openDefectCheckerProps) {
            ConfigCheckersPkgReqVO configCheckersPkgReq = getConfigCheckersReqVO(taskId,
                    checkerPropsEntity.getToolName(), toolDefectRefreshConfigMap);
            configCheckersPkgReq.getOpenedCheckers().add(checkerPropsEntity.getCheckerKey());
        }
        for (CheckerPropsEntity checkerPropsEntity : closeDefectCheckeProps) {
            ConfigCheckersPkgReqVO configCheckersPkgReq = getConfigCheckersReqVO(taskId,
                    checkerPropsEntity.getToolName(), toolDefectRefreshConfigMap);
            configCheckersPkgReq.getClosedCheckers().add(checkerPropsEntity.getCheckerKey());
        }
        for (Map.Entry<String, ConfigCheckersPkgReqVO> entry : toolDefectRefreshConfigMap.entrySet()) {
            rabbitTemplate.convertAndSend(EXCHANGE_TASK_CHECKER_CONFIG, ROUTE_IGNORE_CHECKER, entry.getValue());
        }
    }

    private void updateTools(String user, long taskId, Set<String> updatedToolSet) {
        BatchRegisterVO batchRegisterVO = new BatchRegisterVO();
        batchRegisterVO.setTaskId(taskId);
        List<ToolConfigInfoVO> toolConfigInfoVOS = Lists.newArrayList();
        for (String toolName : updatedToolSet) {
            ToolConfigInfoVO toolConfigInfoVO = new ToolConfigInfoVO();
            toolConfigInfoVO.setTaskId(taskId);
            toolConfigInfoVO.setToolName(toolName);
            toolConfigInfoVOS.add(toolConfigInfoVO);
        }
        batchRegisterVO.setTools(toolConfigInfoVOS);
        client.get(ServiceToolRestResource.class).updateTools(taskId, user, batchRegisterVO);
    }

    private ConfigCheckersPkgReqVO getConfigCheckersReqVO(long taskId, String toolName, Map<String,
            ConfigCheckersPkgReqVO> toolDefectRefreshConfigMap) {
        ConfigCheckersPkgReqVO configCheckersPkgReq = toolDefectRefreshConfigMap.get(toolName);
        if (configCheckersPkgReq == null) {
            configCheckersPkgReq = new ConfigCheckersPkgReqVO();
            configCheckersPkgReq.setTaskId(taskId);
            configCheckersPkgReq.setToolName(toolName);
            configCheckersPkgReq.setOpenedCheckers(Lists.newArrayList());
            configCheckersPkgReq.setClosedCheckers(Lists.newArrayList());
            toolDefectRefreshConfigMap.put(toolName, configCheckersPkgReq);
        }
        return configCheckersPkgReq;
    }


    private Boolean judgeQualifiedCheckerSet(Set<String> checkerSetLanguage,
                                             Set<CheckerSetCategory> checkerSetCategorySet,
                                             Set<String> toolName, Set<CheckerSetSource> checkerSetSource,
                                             CheckerSetEntity checkerSetEntity) {
        //语言筛选要分新版本插件和老版本插件
        if (CollectionUtils.isNotEmpty(checkerSetLanguage)) {
            if (null != checkerSetEntity.getLegacy() && checkerSetEntity.getLegacy()) {
                if (checkerSetLanguage.stream().noneMatch(language ->
                        checkerSetEntity.getCheckerSetLang().contains(language))) {
                    return false;
                }
            } else {
                if (!checkerSetLanguage.contains(checkerSetEntity.getCheckerSetLang())) {
                    return false;
                }
            }
        }
        if (CollectionUtils.isNotEmpty(checkerSetCategorySet)
                && checkerSetCategorySet.stream().noneMatch(checkerSetCategory ->
                checkerSetEntity.getCatagories().stream()
                        .anyMatch(category -> checkerSetCategory.name().equalsIgnoreCase(category.getEnName())))) {
            return false;

        }
        if (CollectionUtils.isNotEmpty(toolName) && (CollectionUtils.isEmpty(checkerSetEntity.getCheckerProps()) ||
                toolName.stream().noneMatch(tool -> checkerSetEntity.getCheckerProps().stream().anyMatch(
                        checkerPropsEntity -> tool.equalsIgnoreCase(checkerPropsEntity.getToolName()))))) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(checkerSetSource) && !checkerSetSource.contains(CheckerSetSource.valueOf(
                StringUtils.isBlank(checkerSetEntity.getCheckerSetSource()) ? "SELF_DEFINED" :
                        checkerSetEntity.getCheckerSetSource()))) {
            return false;
        }
        return true;
    }


    /**
     * 校验规则集是否重复
     *  @param checkerSetId
     *
     */
    private void checkIdDuplicate(String checkerSetId) {
        boolean checkerSetIdDuplicate = false;
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetId(checkerSetId);
        for (CheckerSetEntity checkerSets : checkerSetEntities) {
            if (checkerSetIdDuplicate) {
                break;
            }
            if (checkerSets.getCheckerSetId().equals(checkerSetId)) {
                checkerSetIdDuplicate = true;
            }
        }
        StringBuffer errMsg = new StringBuffer();
        if (checkerSetIdDuplicate) {
            errMsg.append("规则集ID[").append(checkerSetId).append("]");
        }
        if (errMsg.length() > 0) {
            String errMsgStr = errMsg.toString();
            log.error("{}已存在", errMsgStr);
            throw new CodeCCException(CommonMessageCode.RECORD_EXIST, new String[]{errMsgStr}, null);
        }
    }

    /**
     * 校验规则集名称是否与公开规则集重复
     *
     * @param checkerSetName
     */
    private void checkNameExistInPublic(String checkerSetName) {
        boolean checkerSetNameDuplicate = false;
        List<CheckerSetEntity> checkerSetEntities =
                checkerSetRepository.findByScope(CheckerConstants.CheckerSetScope.PUBLIC.code());
        for (CheckerSetEntity checkerSets : checkerSetEntities) {
            if (checkerSetNameDuplicate) {
                break;
            }
            if (checkerSets.getCheckerSetName().equals(checkerSetName)) {
                checkerSetNameDuplicate = true;
            }
        }
        StringBuffer errMsg = new StringBuffer();
        if (checkerSetNameDuplicate) {
            errMsg.append("规则集名称[").append(checkerSetName).append("]");
        }
        if (errMsg.length() > 0) {
            String errMsgStr = errMsg.toString();
            log.error("{}已存在", errMsgStr);
            throw new CodeCCException(CommonMessageCode.RECORD_EXIST, new String[]{errMsgStr}, null);
        }
    }

    /**
     * 校验规则集名称是否与项目规则集重复
     *
     * @param checkerSetName
     * @param projectId
     */
    private void checkNameExistInProject(String checkerSetName, String projectId) {
        boolean checkerSetNameDuplicate = false;
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByProjectId(projectId);
        for (CheckerSetEntity checkerSets : checkerSetEntities) {
            if (checkerSetNameDuplicate) {
                break;
            }
            if (checkerSets.getCheckerSetName().equals(checkerSetName)) {
                checkerSetNameDuplicate = true;
            }
        }
        StringBuffer errMsg = new StringBuffer();
        if (checkerSetNameDuplicate) {
            errMsg.append("规则集名称[").append(checkerSetName).append("]");
        }
        if (errMsg.length() > 0) {
            String errMsgStr = errMsg.toString();
            log.error("{}已存在", errMsgStr);
            throw new CodeCCException(CommonMessageCode.RECORD_EXIST, new String[]{errMsgStr}, null);
        }
    }

    private Map<String, Integer> getLatestVersionMap(Set<String> checkerSetIds) {
        Map<String, Integer> latestVersionMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(checkerSetIds)) {
            List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetIdIn(checkerSetIds);
            for (CheckerSetEntity checkerSetEntity : checkerSetEntities) {
                if (latestVersionMap.get(checkerSetEntity.getCheckerSetId()) == null
                        || checkerSetEntity.getVersion() > latestVersionMap.get(checkerSetEntity.getCheckerSetId())) {
                    latestVersionMap.put(checkerSetEntity.getCheckerSetId(), checkerSetEntity.getVersion());
                }
            }
        }
        return latestVersionMap;
    }

    private List<CheckerSetCatagoryEntity> getCatagoryEntities(List<String> catatories) {
        List<CheckerSetCatagoryEntity> catagoryEntities = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(catatories)) {
            Map<String, String> catagoryNameMap = Maps.newHashMap();
            for (CheckerSetCategory checkerSetCategory : CheckerSetCategory.values()) {
                catagoryNameMap.put(checkerSetCategory.name(), checkerSetCategory.getName());
            }
            for (String categoryEnName : catatories) {
                CheckerSetCatagoryEntity catagoryEntity = new CheckerSetCatagoryEntity();
                catagoryEntity.setEnName(categoryEnName);
                catagoryEntity.setCnName(catagoryNameMap.get(categoryEnName));
                catagoryEntities.add(catagoryEntity);
            }
        }
        return catagoryEntities;
    }

    /**
     * 排序并分页
     *
     * @param pageNum
     * @param pageSize
     * @param sortField
     * @param sortType
     * @param defectVOs
     * @param <T>
     * @return
     */
    public <T> Page<T> sortAngPage(int pageNum, int pageSize, String sortField,
                                                                   Sort.Direction sortType, List<T> defectVOs) {
        if (null == sortType) {
            sortType = Sort.Direction.ASC;
        }

        // 严重程度要跟前端传入的排序类型相反
        if ("severity".equals(sortField)) {
            if (sortType.isAscending()) {
                sortType = Sort.Direction.DESC;
            } else {
                sortType = Sort.Direction.ASC;
            }
        }
        ListSortUtil.sort(defectVOs, sortField, sortType.name());
        int total = defectVOs.size();
        pageNum = pageNum - 1 < 0 ? 0 : pageNum - 1;
        pageSize = pageSize <= 0 ? 10 : pageSize;
        int subListBeginIdx = pageNum * pageSize;
        int subListEndIdx = subListBeginIdx + pageSize;
        if (subListBeginIdx > total) {
            subListBeginIdx = 0;
        }
        defectVOs = defectVOs.subList(subListBeginIdx, subListEndIdx > total ? total : subListEndIdx);

        //封装分页类
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(sortType, sortField));
        return new PageImpl<>(defectVOs, pageable, total);
    }

    @Override
    public List<CheckerSetVO> queryCheckerSetsForOpenScan(Set<CheckerSetVO> checkerSetList, String projectId) {
        if (CollectionUtils.isEmpty(checkerSetList)) {
            return new ArrayList<>();
        }
        Map<String, Integer> checkerSetVersionMap = checkerSetList.stream().
                filter(checkerSetVO -> null != checkerSetVO.getVersion()).collect(
                Collectors.toMap(CheckerSetVO::getCheckerSetId, CheckerSetVO::getVersion, (k, v) -> v));
        Set<String> checkerSetIdList =
                checkerSetList.stream().map(CheckerSetVO::getCheckerSetId).collect(Collectors.toSet());
        List<CheckerSetEntity> checkerSets = checkerSetDao.findByComplexCheckerSetCondition(null,
                checkerSetIdList, null, null, null, null, null, false,
                null, true);
        //用于装最新版本的map
        Map<String, Integer> latestVersionMap = new HashMap<>();
        //用于装当前版本的map
        Map<String, Integer> currentVersionMap = new HashMap<>();
        for (CheckerSetEntity checkerSetEntity : checkerSets) {
            /**
             * 1. 如果入参list中有固定版本号
             *    固定版本号为正常数字：表示固定用该版本号，用currentVersionMap
             *    固定版本号为整数最大值：表示用最新的版本，用latestVersionMap
             * 2. 如果入参list中无版本数字
             *    表示用最新版本，用latestVersionMap
             */
            if (checkerSetVersionMap.containsKey(checkerSetEntity.getCheckerSetId())
                    && Integer.MAX_VALUE != checkerSetVersionMap.get(checkerSetEntity.getCheckerSetId())) {
                currentVersionMap.put(checkerSetEntity.getCheckerSetId(),
                        checkerSetVersionMap.get(checkerSetEntity.getCheckerSetId()));
            } else if (latestVersionMap.get(checkerSetEntity.getCheckerSetId()) == null
                    || checkerSetEntity.getVersion() > latestVersionMap.get(checkerSetEntity.getCheckerSetId())) {
                latestVersionMap.put(checkerSetEntity.getCheckerSetId(), checkerSetEntity.getVersion());
            }
        }
        if (CollectionUtils.isNotEmpty(checkerSets)) {
            return checkerSets.stream()
                    .filter(checkerSetEntity ->
                            //最新版本号
                            (latestVersionMap.containsKey(checkerSetEntity.getCheckerSetId())
                                    && null != latestVersionMap.get(checkerSetEntity.getCheckerSetId())
                                    && (latestVersionMap.get(checkerSetEntity.getCheckerSetId())
                                            .equals(checkerSetEntity.getVersion())))
                                    //或者当前版本号
                                    || (currentVersionMap.containsKey(checkerSetEntity.getCheckerSetId())
                                            && null != currentVersionMap.get(checkerSetEntity.getCheckerSetId())
                                            && (currentVersionMap.get(checkerSetEntity.getCheckerSetId())
                                                    .equals(checkerSetEntity.getVersion())))
                    )
                    .map(checkerSetEntity ->
                    {
                        CheckerSetVO checkerSetVO = new CheckerSetVO();
                        BeanUtils.copyProperties(checkerSetEntity, checkerSetVO);
                        if (latestVersionMap.containsKey(checkerSetEntity.getCheckerSetId())) {
                            checkerSetVO.setVersion(Integer.MAX_VALUE);
                        }
                        if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())) {
                            checkerSetVO.setToolList(checkerSetEntity.getCheckerProps().stream().map(CheckerPropsEntity::getToolName).collect(Collectors.toSet()));
                        }
                        return checkerSetVO;
                    }).collect(Collectors.toList());
        }

        return null;
    }

    /**
     * 更新规则集信息
     *
     * @param userName     更新人
     * @param updateCheckerSetReqExtVO 基本信息
     * @return boolean
     */
    @Override
    public Boolean updateCheckerSetBaseInfoByOp(String userName,
            @NotNull V3UpdateCheckerSetReqExtVO updateCheckerSetReqExtVO) {
        boolean result = false;
        CheckerSetEntity checkerSetEntity = checkerSetRepository
                .findFirstByCheckerSetIdAndVersion(updateCheckerSetReqExtVO.getCheckerSetId(),
                        updateCheckerSetReqExtVO.getVersion());
        if (null != checkerSetEntity) {
            checkerSetEntity.setCatagories(getCatagoryEntities(updateCheckerSetReqExtVO.getCatagories()));
            checkerSetEntity.setCheckerSetSource(updateCheckerSetReqExtVO.getCheckerSetSource());
            checkerSetEntity.setDescription(updateCheckerSetReqExtVO.getDescription());
            checkerSetEntity.setUpdatedBy(userName);
            checkerSetEntity.setLastUpdateTime(System.currentTimeMillis());
            checkerSetRepository.save(checkerSetEntity);
            result = true;
        }

        return result;
    }

    /**
     * 获取规则集管理初始化参数选项
     *
     * @return
     */
    @Override
    public CheckerSetParamsVO getCheckerSetParams() {
        // 查询规则集类型列表
        CheckerSetParamsVO checkerSetParams = new CheckerSetParamsVO();
        // 类别
        checkerSetParams.setCatatories(Lists.newArrayList());
        for (CheckerSetCategory checkerSetCategory : CheckerSetCategory.values()) {
            CheckerSetCategoryVO categoryVO = new CheckerSetCategoryVO();
            categoryVO.setCnName(checkerSetCategory.getName());
            categoryVO.setEnName(checkerSetCategory.name());
            checkerSetParams.getCatatories().add(categoryVO);
        }

        // 来源
        checkerSetParams.setCheckerSetSource(Lists.newArrayList());
        for (CheckerSetSource checkerSetSource : CheckerSetSource.values()) {
            CheckerSetCategoryVO categoryVO = new CheckerSetCategoryVO();
            categoryVO.setCnName(checkerSetSource.getName());
            categoryVO.setEnName(checkerSetSource.name());
            checkerSetParams.getCheckerSetSource().add(categoryVO);
        }

        //适用语言
        Result<List<BaseDataVO>> langsParamsResult =
                client.get(ServiceBaseDataResource.class).getParamsByType(KEY_LANG);
        if (langsParamsResult.isNotOk() || CollectionUtils.isEmpty(langsParamsResult.getData())) {
            log.error("checker set langs is empty!");
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        checkerSetParams.setCodeLangs(Lists.newArrayList());
        for (BaseDataVO baseDataVO : langsParamsResult.getData()) {
            CheckerSetCodeLangVO checkerSetCodeLangVO = new CheckerSetCodeLangVO();
            checkerSetCodeLangVO.setDisplayName(baseDataVO.getParamName());
            checkerSetParams.getCodeLangs().add(checkerSetCodeLangVO);
        }

        return checkerSetParams;
    }

}
