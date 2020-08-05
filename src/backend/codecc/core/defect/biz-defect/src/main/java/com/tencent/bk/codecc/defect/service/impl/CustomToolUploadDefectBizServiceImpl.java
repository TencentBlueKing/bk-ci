package com.tencent.bk.codecc.defect.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.dao.mongorepository.*;
import com.tencent.bk.codecc.defect.dao.mongotemplate.FileCCNDao;
import com.tencent.bk.codecc.defect.dao.redis.StatisticDao;
import com.tencent.bk.codecc.defect.model.*;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.service.AbstractUploadDefectBizService;
import com.tencent.bk.codecc.defect.service.CCNUploadStatisticService;
import com.tencent.bk.codecc.defect.service.DUPCUploadStatisticService;
import com.tencent.bk.codecc.defect.service.file.ScmFileInfoService;
import com.tencent.bk.codecc.defect.vo.*;
import com.tencent.bk.codecc.defect.vo.customtool.*;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.IBizService;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import com.tencent.devops.common.util.CompressionUtils;
import com.tencent.devops.common.util.DateTimeUtils;
import com.tencent.devops.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户自定义工具上传告警实现类
 *
 * @version V4.0
 * @date 2019/10/16
 */
@Service("CUSTOMTOOLUploadDefectBizService")
@Slf4j
public class CustomToolUploadDefectBizServiceImpl extends AbstractUploadDefectBizService
{
    @Autowired
    private CheckerRepository checkerRepository;

    @Autowired
    protected StatisticDao statisticDao;

    @Autowired
    protected BuildRepository buildRepository;

    @Autowired
    protected CommonStatisticRepository commonStatisticRepository;

    @Autowired
    private CCNUploadStatisticService ccnUploadStatisticService;

    @Autowired
    private DUPCUploadStatisticService dupcUploadStatisticService;

    @Autowired
    private ToolMetaCacheService toolMetaCacheService;

    @Autowired
    private FileCCNRepository fileCCNRepository;

    @Autowired
    private FileCCNDao fileCCNDao;

    @Autowired
    private ToolBuildStackRepository toolBuildStackRepository;

    @Autowired
    protected ScmFileInfoService scmFileInfoService;
    /**
     * 上传告警
     *
     * @param uploadDefectVO
     * @return
     */
    @Override
    public CodeCCResult processBiz(UploadDefectVO uploadDefectVO)
    {
        // 获取文件作者信息
        Map<String, ScmBlameVO> fileChangeRecordsMap = scmFileInfoService.loadAuthorInfoMap(
            uploadDefectVO.getTaskId(),
            uploadDefectVO.getStreamName(),
            uploadDefectVO.getToolName(),
            uploadDefectVO.getBuildId());

        // 获取仓库信息
        JSONArray repoInfoJsonArr = scmJsonComponent.loadRepoInfo(uploadDefectVO.getStreamName(), uploadDefectVO.getToolName(), uploadDefectVO.getBuildId());
        Map<String, RepoSubModuleVO> codeRepoIdMap = Maps.newHashMap();
        if (repoInfoJsonArr != null && repoInfoJsonArr.length() > 0)
        {
            for (int i = 0; i < repoInfoJsonArr.length(); i++)
            {
                JSONObject codeRepoJson = repoInfoJsonArr.getJSONObject(i);
                ScmInfoVO codeRepoInfo = JsonUtil.INSTANCE.to(codeRepoJson.toString(), ScmInfoVO.class);
                //需要判断是svn还是git，svn采用rootUrl做key，git采用url做key
                RepoSubModuleVO repoSubModuleVO = new RepoSubModuleVO();
                repoSubModuleVO.setRepoId(codeRepoInfo.getRepoId());
                if(ComConstants.CodeHostingType.SVN.name().equalsIgnoreCase(codeRepoInfo.getScmType()))
                {
                    codeRepoIdMap.put(codeRepoInfo.getRootUrl(), repoSubModuleVO);
                }
                else
                {
                    codeRepoIdMap.put(codeRepoInfo.getUrl(), repoSubModuleVO);
                    if (CollectionUtils.isNotEmpty(codeRepoInfo.getSubModules()))
                    {
                        for (RepoSubModuleVO subModuleVO : codeRepoInfo.getSubModules())
                        {
                            RepoSubModuleVO subRepoSubModuleVO = new RepoSubModuleVO();
                            subRepoSubModuleVO.setRepoId(codeRepoInfo.getRepoId());
                            subRepoSubModuleVO.setSubModule(subModuleVO.getSubModule());
                            codeRepoIdMap.put(subModuleVO.getUrl(), subRepoSubModuleVO);
                        }
                    }
                }

            }
        }

        String toolPattern = toolMetaCacheService.getToolPattern(uploadDefectVO.getToolName());
        if (ComConstants.ToolPattern.LINT.name().equals(toolPattern))
        {
            uploadLintDefects(uploadDefectVO, fileChangeRecordsMap, codeRepoIdMap);
        }
        else if (ComConstants.ToolPattern.CCN.name().equals(toolPattern))
        {
            uploadCCNDefects(uploadDefectVO, fileChangeRecordsMap, codeRepoIdMap);
        }
        else if (ComConstants.ToolPattern.DUPC.name().equals(toolPattern))
        {
            uploadDUPCDefects(uploadDefectVO, fileChangeRecordsMap, codeRepoIdMap);
        }
        else
        {
            String errMsg = "tool pattern invalid!";
            log.error(errMsg);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
        }

        return new CodeCCResult(CommonMessageCode.SUCCESS, "upload defect ok");
    }

    private void uploadLintDefects(UploadDefectVO uploadDefectVO, Map<String, ScmBlameVO> fileChangeRecordsMap, Map<String, RepoSubModuleVO> codeRepoIdMap)
    {
        log.info("start to upload lint defect, task id :{}, tool name: {}", uploadDefectVO.getTaskId(), uploadDefectVO.getToolName());
        // 获取文件告警映射
        Map<String, List<LintDefectEntity>> fileDefectsMap = Maps.newHashMap();
        if (StringUtils.isNotEmpty(uploadDefectVO.getDefectsCompress()))
        {
            String defectListJson = decompressDefects(uploadDefectVO.getDefectsCompress());
            List<CustomToolDefectVO> defectList = JsonUtil.INSTANCE.to(defectListJson, new TypeReference<List<CustomToolDefectVO>>()
            {
            });
            if (CollectionUtils.isNotEmpty(defectList))
            {
                // 获取规则列表
                List<CheckerDetailEntity> checkerDetailEntityList = checkerRepository.findByToolName(uploadDefectVO.getToolName());
                Map<String, CheckerDetailEntity> checkerDetailMap = Maps.newHashMap();
                if (CollectionUtils.isNotEmpty(checkerDetailEntityList))
                {
                    for (CheckerDetailEntity checkerDetail : checkerDetailEntityList)
                    {
                        checkerDetailMap.put(checkerDetail.getCheckerKey(), checkerDetail);
                    }
                }

                for (CustomToolDefectVO customToolDefectEntity : defectList)
                {
                    if (!fileDefectsMap.containsKey(customToolDefectEntity.getFilePath()))
                    {
                        fileDefectsMap.put(customToolDefectEntity.getFilePath(), Lists.newArrayList());
                    }
                    LintDefectEntity lintDefectEntity = new LintDefectEntity();
                    fillDefectAndAuthorInfo(lintDefectEntity, customToolDefectEntity, fileChangeRecordsMap, checkerDetailMap);

                    fileDefectsMap.get(customToolDefectEntity.getFilePath()).add(lintDefectEntity);
                }
            }
            log.info("get file defects map!");
        }

        // 告警转换为Lint工具所需数据并执行上报
        if (MapUtils.isNotEmpty(fileDefectsMap))
        {
            log.info("start to upload defect");
            // 获取Lint类工具告警上报实现类实例
            String beanName = String.format("%s%s%s", ComConstants.ToolPattern.LINT, ComConstants.BusinessType.UPLOAD_DEFECT.value(), ComConstants.BIZ_SERVICE_POSTFIX);
            IBizService uploadDefectService = SpringContextUtil.Companion.getBean(IBizService.class, beanName);

            for (Map.Entry<String, List<LintDefectEntity>> entry : fileDefectsMap.entrySet())
            {
                try{
                    UploadDefectVO uploadLintDefectVO = new UploadDefectVO();
                    BeanUtils.copyProperties(uploadDefectVO, uploadLintDefectVO);
                    uploadLintDefectVO.setFilePath(entry.getKey());
                    uploadLintDefectVO.setBuildId(uploadDefectVO.getBuildId());
                    String defectsCompress = CompressionUtils.compressAndEncodeBase64(JsonUtil.INSTANCE.toJson(entry.getValue()));
                    if (fileChangeRecordsMap.get(entry.getKey()) != null)
                    {
                        ScmBlameVO fileLineAuthorInfo = fileChangeRecordsMap.get(entry.getKey());
                        uploadLintDefectVO.setFileUpdateTime(fileLineAuthorInfo.getFileUpdateTime());
                        uploadLintDefectVO.setRevision(fileLineAuthorInfo.getRevision());
                        uploadLintDefectVO.setUrl(fileLineAuthorInfo.getUrl());
                        uploadLintDefectVO.setRelPath(fileLineAuthorInfo.getFileRelPath());
                        uploadLintDefectVO.setBranch(fileLineAuthorInfo.getBranch());
                        if (MapUtils.isNotEmpty(codeRepoIdMap))
                        {
                            //如果是svn用rootUrl关联
                            if(ComConstants.CodeHostingType.SVN.name().equalsIgnoreCase(fileLineAuthorInfo.getScmType()))
                            {
                                if(codeRepoIdMap.get(fileLineAuthorInfo.getRootUrl()) != null)
                                {
                                    RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(fileLineAuthorInfo.getRootUrl());
                                    uploadLintDefectVO.setRepoId(repoSubModuleVO.getRepoId());
                                }
                            }
                            //其他用rootUrl关联
                            else
                            {
                                if(codeRepoIdMap.get(uploadLintDefectVO.getUrl()) != null)
                                {
                                    RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(uploadLintDefectVO.getUrl());
                                    uploadLintDefectVO.setRepoId(repoSubModuleVO.getRepoId());
                                    if (StringUtils.isNotEmpty(repoSubModuleVO.getSubModule()))
                                    {
                                        uploadLintDefectVO.setSubModule(repoSubModuleVO.getSubModule());
                                    }
                                }
                            }
                        }
                    }
                    if (StringUtils.isEmpty(uploadLintDefectVO.getSubModule()))
                    {
                        uploadLintDefectVO.setSubModule("");
                    }
                    uploadLintDefectVO.setDefectsCompress(defectsCompress);
                    uploadDefectService.processBiz(uploadLintDefectVO);
                } catch (Exception e){
                    log.error("handle with lint defect fail! task id: {}, file path: {}",uploadDefectVO.getTaskId(), entry.getKey(), e);
                }
            }
        }
    }

    private void uploadCCNDefects(UploadDefectVO uploadDefectVO, Map<String, ScmBlameVO> fileChangeRecordsMap,
                                  Map<String, RepoSubModuleVO> codeRepoIdMap)
    {
        log.info("start to upload ccn defect, task id :{}, tool name: {}", uploadDefectVO.getTaskId(), uploadDefectVO.getToolName());
        // 获取告警数据
        String streamName = uploadDefectVO.getStreamName();
        String defectsCompress = scmJsonComponent.loadDefects(streamName, ComConstants.Tool.CCN.name(), uploadDefectVO.getBuildId());
        if (StringUtils.isEmpty(defectsCompress))
        {
            log.error("defect compress is empty!");
            return;
        }

        // 获取各文件告警列表，并加入告警作者
        Map<String, List<CCNDefectEntity>> fileCcnDefectsMap = Maps.newHashMap();
        Map<String, UploadDefectVO> updateCcnDefectVOMap = Maps.newHashMap();
        CCNFunctionsAndStatisticVO ccnFunctionsAndStatisticVO = JsonUtil.INSTANCE.to(defectsCompress,
                CCNFunctionsAndStatisticVO.class);
        if (StringUtils.isNotEmpty(ccnFunctionsAndStatisticVO.getDefectsCompress()))
        {
            String defectListJson = decompressDefects(ccnFunctionsAndStatisticVO.getDefectsCompress());
            List<CCNFunctionVO> ccnFunctionVOS = JsonUtil.INSTANCE.to(defectListJson,
                    new TypeReference<List<CCNFunctionVO>>(){});
            for (CCNFunctionVO ccnFunctionVO : ccnFunctionVOS)
            {
                try{
                    // 加入相对路径
                    ScmBlameVO fileLineAuthorInfo = fileChangeRecordsMap.get(ccnFunctionVO.getFilePath());
                /*if (fileLineAuthorInfo == null)
                {
                    String errMsg = String.format("cannot find defect author! filePath:%s", ccnFunctionVO.getFilePath());
                    logger.error(errMsg);
                    continue;
//                    throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL, new String[]{errMsg}, null);
                }*/
                    ccnFunctionVO.setRelPath(null == fileLineAuthorInfo ? null : fileLineAuthorInfo.getFileRelPath());

                    // 获取作者信息
                    if (MapUtils.isNotEmpty(fileChangeRecordsMap))
                    {
                        // 获取各文件代码行对应的作者信息映射
                        Map<String, Map<Integer, ScmBlameChangeRecordVO>> fileLineInfoeMap = getCodeLineAuthorMapping(fileLineAuthorInfo, ccnFunctionVO.getFilePath());

                        // 获取函数涉及的所有行中的最新修改作者作为告警作者
                        long functionLastUpdateTime = 0;
                        for (int i = ccnFunctionVO.getStartLines(); i <= ccnFunctionVO.getEndLines(); i++)
                        {
                            if(null != fileLineInfoeMap)
                            {
                                Map<Integer, ScmBlameChangeRecordVO> lineChangeRecordMap = fileLineInfoeMap.get(ccnFunctionVO.getFilePath());
                                if (lineChangeRecordMap != null)
                                {
                                    ScmBlameChangeRecordVO recordVO = lineChangeRecordMap.get(i);
                                    if (recordVO != null && recordVO.getLineUpdateTime() > functionLastUpdateTime)
                                    {
                                        functionLastUpdateTime = recordVO.getLineUpdateTime();
                                        ccnFunctionVO.setAuthor(recordVO.getAuthor());
                                        ccnFunctionVO.setLatestDateTime(recordVO.getLineUpdateTime());
                                    }
                                }
                            }
                        }
                    }

                    // 获取各文件圈复杂度告警列表
                    fileCcnDefectsMap.computeIfAbsent(ccnFunctionVO.getRelPath(), k -> Lists.newArrayList());
                    CCNDefectEntity ccnDefectEntity = new CCNDefectEntity();
                    BeanUtils.copyProperties(ccnFunctionVO, ccnDefectEntity);
                    fileCcnDefectsMap.get(ccnFunctionVO.getRelPath()).add(ccnDefectEntity);

                    // 保存上报圈复杂度告警的请求体
                    if (updateCcnDefectVOMap.get(ccnFunctionVO.getRelPath()) == null)
                    {
                        UploadDefectVO uploadCcnDefectVO = new UploadDefectVO();
                        BeanUtils.copyProperties(uploadDefectVO, uploadCcnDefectVO);
                        updateCcnDefectVOMap.put(ccnFunctionVO.getRelPath(), uploadCcnDefectVO);
                        uploadCcnDefectVO.setFilePath(ccnFunctionVO.getFilePath());
                        uploadCcnDefectVO.setRelPath(ccnFunctionVO.getRelPath());
                        uploadCcnDefectVO.setBuildId(uploadDefectVO.getBuildId());
                        uploadCcnDefectVO.setUrl(null == fileLineAuthorInfo ? null : fileLineAuthorInfo.getUrl());
                        uploadCcnDefectVO.setBranch(null == fileLineAuthorInfo ? null : fileLineAuthorInfo.getBranch());
                        uploadCcnDefectVO.setRevision(null == fileLineAuthorInfo ? null : fileLineAuthorInfo.getRevision());
                        if(null != fileLineAuthorInfo)
                        {
                            if(ComConstants.CodeHostingType.SVN.name().equalsIgnoreCase(fileLineAuthorInfo.getScmType()))
                            {
                                RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(fileLineAuthorInfo.getRootUrl());
                                uploadCcnDefectVO.setRepoId(repoSubModuleVO.getRepoId());
                            }
                            else
                            {
                                RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(fileLineAuthorInfo.getUrl());
                                if (repoSubModuleVO != null) {
                                    uploadCcnDefectVO.setRepoId(repoSubModuleVO.getRepoId());
                                    if (StringUtils.isNotEmpty(repoSubModuleVO.getSubModule()))
                                    {
                                        uploadCcnDefectVO.setSubModule(repoSubModuleVO.getSubModule());
                                    }
                                }
                            }
                        }
                        if (StringUtils.isEmpty(uploadCcnDefectVO.getSubModule()))
                        {
                            uploadCcnDefectVO.setSubModule("");
                        }
                        uploadCcnDefectVO.setBuildId(uploadDefectVO.getBuildId());
                    }
                } catch (Exception e){
                    log.error("handle with ccn defect fail! task id: {}, file path: {}", ccnFunctionVO.getTaskId(), ccnFunctionVO.getFilePath(), e);
                }

            }
        }

        // 每个文件分别调用圈复杂度告警告警上报处理类
        if (MapUtils.isNotEmpty(fileCcnDefectsMap))
        {
            String beanName = String.format("%s%s%s", ComConstants.ToolPattern.CCN, ComConstants.BusinessType.UPLOAD_DEFECT.value(), ComConstants.BIZ_SERVICE_POSTFIX);
            IBizService uploadDefectService = SpringContextUtil.Companion.getBean(IBizService.class, beanName);
            for (Map.Entry<String, List<CCNDefectEntity>> entry : fileCcnDefectsMap.entrySet())
            {
                UploadDefectVO uploadCcnDefectVO = updateCcnDefectVOMap.get(entry.getKey());
                String ccnDefectsCompress = CompressionUtils.compressAndEncodeBase64(JsonUtil.INSTANCE.toJson(entry.getValue()));
                uploadCcnDefectVO.setDefectsCompress(ccnDefectsCompress);
                uploadDefectService.processBiz(uploadCcnDefectVO);
            }
        }

        // 获取本次扫描是增量还是全量扫描
        TaskDetailVO taskDetailVO = getTaskDetail(uploadDefectVO.getTaskId());
        ToolBuildStackEntity toolBuildStackEntity = toolBuildStackRepository.findByTaskIdAndToolNameAndBuildId(taskDetailVO.getTaskId(),
                ComConstants.Tool.CCN.name(), uploadDefectVO.getBuildId());
        boolean isFullScan = toolBuildStackEntity != null ? toolBuildStackEntity.isFullScan() : true;

        // 如果本次是全量扫描，则要清除已有的文件圈复杂度列表，如果是增量扫描，则要先清理待删除文件
        if (isFullScan)
        {
            fileCCNRepository.deleteByTaskId(uploadDefectVO.getTaskId());
        }
        else
        {
            // 获取删除文件列表
            if (toolBuildStackEntity != null && CollectionUtils.isNotEmpty(toolBuildStackEntity.getDeleteFiles()))
            {
                fileCCNRepository.deleteByTaskIdIsAndFilePathIn(uploadDefectVO.getTaskId(), toolBuildStackEntity.getDeleteFiles());
            }
        }

        // 保存本次上报的文件圈复杂度列表
        if (CollectionUtils.isNotEmpty(ccnFunctionsAndStatisticVO.getFilesTotalCCN()))
        {
            List<FileCCNEntity> fileCCNEntities = Lists.newArrayList();
            for (FileCCNVO fileCCNVO : ccnFunctionsAndStatisticVO.getFilesTotalCCN())
            {
                FileCCNEntity fileCCNEntity = new FileCCNEntity();
                BeanUtils.copyProperties(fileCCNVO, fileCCNEntity);
                fileCCNEntity.setTaskId(uploadDefectVO.getTaskId());
                fileCCNEntities.add(fileCCNEntity);
            }

            fileCCNDao.upsertFileCCNList(fileCCNEntities);
        }

        // 统计平均圈复杂度
        float averageCCN = 0;
        BigDecimal fileCount = BigDecimal.ZERO;
        BigDecimal totalCCN = BigDecimal.ZERO;
        List<FileCCNEntity> fileCCNEntities = fileCCNRepository.findByTaskId(uploadDefectVO.getTaskId());
        for (FileCCNEntity fileCCNEntity : fileCCNEntities)
        {
            try
            {
                totalCCN = totalCCN.add(BigDecimal.valueOf(Double.parseDouble(fileCCNEntity.getTotalCCNCount())));
            }
            catch (Exception e)
            {
                log.error("totalCCNCount convert to double fail! fileCCNEntity{}", JsonUtil.INSTANCE.toJson(fileCCNEntity), e);
                continue;
            }
            fileCount = fileCount.add(BigDecimal.ONE);
        }
        log.info("file count is : {}", fileCount);

        // 避免除以0报错
        if (fileCount.compareTo(BigDecimal.ZERO) > 0)
        {
            averageCCN = totalCCN.divide(fileCount, 2, BigDecimal.ROUND_CEILING).floatValue();
        }

        // 上传数据
        CCNUploadStatisticVO uploadStatisticVO = new CCNUploadStatisticVO();
        uploadStatisticVO.setTaskId(uploadDefectVO.getTaskId());
        uploadStatisticVO.setStreamName(uploadDefectVO.getStreamName());
        uploadStatisticVO.setBuildId(uploadDefectVO.getBuildId());
        uploadStatisticVO.setAverageCCN(String.valueOf(averageCCN));
        ccnUploadStatisticService.uploadStatistic(uploadStatisticVO);
    }


    /**
     * 统计重复率信息
     * @param uploadDefectVO
     * @param fileChangeRecordsMap
     * @param codeRepoIdMap
     */
    private void uploadDUPCDefects(UploadDefectVO uploadDefectVO, Map<String, ScmBlameVO> fileChangeRecordsMap,
                                   Map<String, RepoSubModuleVO> codeRepoIdMap){
        log.info("start to upload dupc defect, task id :{}, tool name: {}", uploadDefectVO.getTaskId(), uploadDefectVO.getToolName());
        Long curTime = System.currentTimeMillis();
        String defectsCompress = uploadDefectVO.getDefectsCompress();
        String uncompressedDefects = CompressionUtils.decodeBase64AndDecompress(defectsCompress);
        if (StringUtils.isEmpty(defectsCompress))
        {
            return;
        }

        List<DUPCFileParseVO> dupcFileParseVOList = JsonUtil.INSTANCE.to(uncompressedDefects, new TypeReference<List<DUPCFileParseVO>>() {
        });
        if(CollectionUtils.isNotEmpty(dupcFileParseVOList))
        {
            //按文件为维度调用处理
            dupcFileParseVOList.forEach(dupcFileParseVO -> {
                try{
                    ScmBlameVO fileLineAuthorInfo = fileChangeRecordsMap.get(dupcFileParseVO.getFilePath());
                    /*if(null == fileLineAuthorInfo)
                    {
                        return;
                    }*/
                    DUPCDefectEntity dupcDefectEntity = new DUPCDefectEntity();
                    //设置基础信息
                    dupcDefectEntity.setTaskId(uploadDefectVO.getTaskId());
                    dupcDefectEntity.setToolName(ComConstants.Tool.DUPC.name());
                    dupcDefectEntity.setFilePath(dupcFileParseVO.getFilePath());
                    dupcDefectEntity.setTotalLines(dupcFileParseVO.getTotalLines());
                    dupcDefectEntity.setDupLines(dupcFileParseVO.getDupLines());
                    dupcDefectEntity.setDupRate(dupcFileParseVO.getDupRate());
                    dupcDefectEntity.setDupRateValue(Float.parseFloat(dupcFileParseVO.getDupRate().replace("%", "")) / 100);
                    dupcDefectEntity.setBlockNum(dupcFileParseVO.getBlockNum());
                    dupcDefectEntity.setLastUpdateTime(curTime);

                    //设置相应文件路径及代码库信息
                    dupcDefectEntity.setRelPath(null == fileLineAuthorInfo ? null : fileLineAuthorInfo.getFileRelPath());
                    dupcDefectEntity.setUrl(null == fileLineAuthorInfo ? null : fileLineAuthorInfo.getUrl());
                    dupcDefectEntity.setFileChangeTime(getFileChangeTime(fileLineAuthorInfo));
                    dupcDefectEntity.setRevision(null == fileLineAuthorInfo ? null : fileLineAuthorInfo.getRevision());
                    dupcDefectEntity.setBranch(null == fileLineAuthorInfo ? null : fileLineAuthorInfo.getBranch());
                    if(null != fileLineAuthorInfo)
                    {
                        if(ComConstants.CodeHostingType.SVN.name().equalsIgnoreCase(fileLineAuthorInfo.getScmType()))
                        {
                            RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(fileLineAuthorInfo.getRootUrl());
                            dupcDefectEntity.setRepoId(repoSubModuleVO.getRepoId());
                        }
                        else
                        {
                            RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(fileLineAuthorInfo.getUrl());
                            dupcDefectEntity.setRepoId(repoSubModuleVO.getRepoId());
                            if (StringUtils.isNotEmpty(repoSubModuleVO.getSubModule()))
                            {
                                dupcDefectEntity.setSubModule(repoSubModuleVO.getSubModule());
                            }
                        }
                    }



                    /**
                     * 获取作者信息
                     */
                    // 获取各文件代码行对应的作者信息映射
                    Map<String, Map<Integer, ScmBlameChangeRecordVO>> fileLineInfoMap = getCodeLineAuthorMapping(fileLineAuthorInfo, dupcFileParseVO.getFilePath());
                    //设置代码块清单
                    List<DUPCBlockParseVO> blockList = dupcFileParseVO.getBlockList();
                    if(CollectionUtils.isNotEmpty(blockList)){
                        dupcDefectEntity.setBlockList(blockList.stream().map(dupcBlockParseVO -> {
                            CodeBlockEntity codeBlockEntity = new CodeBlockEntity();
                            codeBlockEntity.setSourceFile(dupcFileParseVO.getFilePath());
                            codeBlockEntity.setStartLines(dupcBlockParseVO.getStartLines());
                            codeBlockEntity.setEndLines(dupcBlockParseVO.getEndLines());
                            codeBlockEntity.setFingerPrint(dupcBlockParseVO.getFingerPrint());
                            if(null != fileLineInfoMap)
                            {
                                Map<Integer, ScmBlameChangeRecordVO> lineChangeRecordMap = fileLineInfoMap.get(dupcFileParseVO.getFilePath());
                                if(null != lineChangeRecordMap)
                                {
                                    Long functionLastUpdateTime = 0L;
                                    for (int i = dupcBlockParseVO.getStartLines().intValue(); i <= dupcBlockParseVO.getEndLines().intValue(); i++)
                                    {
                                        ScmBlameChangeRecordVO recordVO = lineChangeRecordMap.get(i);
                                        if (recordVO != null && recordVO.getLineUpdateTime() > functionLastUpdateTime)
                                        {
                                            functionLastUpdateTime = recordVO.getLineUpdateTime();
                                            codeBlockEntity.setAuthor(recordVO.getAuthor());
                                            codeBlockEntity.setLatestDatetime(recordVO.getLineUpdateTime());
                                        }
                                    }
                                }
                            }
                            return codeBlockEntity;
                        }).collect(Collectors.toList()));
                        //设置作者清单
                        dupcDefectEntity.setAuthorList(dupcDefectEntity.getBlockList().stream().map(CodeBlockEntity::getAuthor).
                                filter(StringUtils::isNotBlank).distinct().reduce((o1, o2) -> String.format("%s;%s", o1, o2)).orElse(""));
                    }


                    UploadDefectVO uploadDupcDefectVO = new UploadDefectVO();
                    BeanUtils.copyProperties(uploadDefectVO, uploadDupcDefectVO);
                    uploadDupcDefectVO.setFilePath(dupcFileParseVO.getFilePath());
                    uploadDupcDefectVO.setRelPath(dupcDefectEntity.getRelPath());
                    uploadDupcDefectVO.setBuildId(uploadDefectVO.getBuildId());
                    uploadDupcDefectVO.setUrl(dupcDefectEntity.getUrl());
                    uploadDupcDefectVO.setBranch(dupcDefectEntity.getBranch());
                    uploadDupcDefectVO.setRevision(dupcDefectEntity.getRevision());
                    uploadDupcDefectVO.setRepoId(dupcDefectEntity.getRepoId());
                    if (StringUtils.isNotEmpty(dupcDefectEntity.getSubModule()))
                    {
                        uploadDupcDefectVO.setSubModule(dupcDefectEntity.getSubModule());
                    }
                    else
                    {
                        uploadDupcDefectVO.setSubModule("");
                    }
                    String dupcDefectsCompress = CompressionUtils.compressAndEncodeBase64(JsonUtil.INSTANCE.toJson(dupcDefectEntity));
                    uploadDupcDefectVO.setDefectsCompress(dupcDefectsCompress);


                    //调用告警上报逻辑
                    String beanName = String.format("%s%s%s", ComConstants.ToolPattern.DUPC, ComConstants.BusinessType.UPLOAD_DEFECT.value(), ComConstants.BIZ_SERVICE_POSTFIX);
                    IBizService uploadDefectService = SpringContextUtil.Companion.getBean(IBizService.class, beanName);
                    uploadDefectService.processBiz(uploadDupcDefectVO);
                } catch (Exception e){
                    log.error("handle with dupc defect fail! task id: {}, file path: {}", uploadDefectVO.getTaskId(), dupcFileParseVO.getFilePath(), e);
                }

            });


            //处理重复率统计信息
            DUPCScanSummaryVO dupcScanSummaryVO = new DUPCScanSummaryVO();
            dupcScanSummaryVO.setRawlineCount(uploadDefectVO.getTotalLineCount());
            dupcScanSummaryVO.setDupLineCount(uploadDefectVO.getDupcLineCount());

            UploadDUPCStatisticVO uploadDUPCStatisticVO = new UploadDUPCStatisticVO();
            uploadDUPCStatisticVO.setTaskId(uploadDefectVO.getTaskId());
            uploadDUPCStatisticVO.setBuildId(uploadDefectVO.getBuildId());
            uploadDUPCStatisticVO.setStreamName(uploadDefectVO.getStreamName());
            uploadDUPCStatisticVO.setScanSummary(dupcScanSummaryVO);
            dupcUploadStatisticService.uploadStatistic(uploadDUPCStatisticVO);
        }
        else
        {
            //如果没有告警则设置为0
            DUPCScanSummaryVO dupcScanSummaryVO = new DUPCScanSummaryVO();
            dupcScanSummaryVO.setRawlineCount(0L);
            dupcScanSummaryVO.setDupLineCount(0L);

            UploadDUPCStatisticVO uploadDUPCStatisticVO = new UploadDUPCStatisticVO();
            uploadDUPCStatisticVO.setTaskId(uploadDefectVO.getTaskId());
            uploadDUPCStatisticVO.setBuildId(uploadDefectVO.getBuildId());
            uploadDUPCStatisticVO.setStreamName(uploadDefectVO.getStreamName());
            uploadDUPCStatisticVO.setScanSummary(dupcScanSummaryVO);
            dupcUploadStatisticService.uploadStatistic(uploadDUPCStatisticVO);
        }


    }

    private Long getFileChangeTime(ScmBlameVO fileLineAuthorInfo) {
        if (null == fileLineAuthorInfo) {
            return 0L;
        } else {
            return fileLineAuthorInfo.getFileUpdateTime() / ComConstants.COMMON_NUM_1000L;
        }
    }

    /**
     * 获取各文件代码行对应的作者信息映射
     * @param fileLineAuthorInfo
     * @param filePath
     * @return
     */
    private Map<String, Map<Integer, ScmBlameChangeRecordVO>> getCodeLineAuthorMapping(ScmBlameVO fileLineAuthorInfo, String filePath){
        Map<String, Map<Integer, ScmBlameChangeRecordVO>> fileLineInfoeMap = Maps.newHashMap();
        if(null == fileLineAuthorInfo)
        {
            return null;
        }
        if (fileLineInfoeMap.get(filePath) == null)
        {
            fileLineInfoeMap.put(filePath, Maps.newHashMap());
            List<ScmBlameChangeRecordVO> changeRecords = fileLineAuthorInfo.getChangeRecords();
            if (CollectionUtils.isNotEmpty(changeRecords))
            {
                for (ScmBlameChangeRecordVO changeRecord : changeRecords)
                {
                    List<Object> lines = changeRecord.getLines();
                    if (lines != null && lines.size() > 0)
                    {
                        for (Object line : lines)
                        {
                            if (line instanceof Integer)
                            {
                                fileLineInfoeMap.get(filePath).put((int)line, changeRecord);
                            }
                            else if (line instanceof List)
                            {
                                List<Integer> lineScope = (List<Integer>) line;
                                for (int i = lineScope.get(0); i <= lineScope.get(1); i++)
                                {
                                    fileLineInfoeMap.get(filePath).put(i, changeRecord);
                                }
                            }
                        }
                    }
                }
            }
        }
        return fileLineInfoeMap;
    }


    /**
     * 填入作者信息
     *
     * @param lintDefectEntity
     * @param customToolDefectEntity
     * @param fileChangeRecordsMap
     */
    private void fillDefectAndAuthorInfo(LintDefectEntity lintDefectEntity, CustomToolDefectVO customToolDefectEntity,
                                         Map<String, ScmBlameVO> fileChangeRecordsMap, Map<String, CheckerDetailEntity> checkerDetailMap)
    {
        BeanUtils.copyProperties(customToolDefectEntity, lintDefectEntity);
        CheckerDetailEntity checkerDetail = checkerDetailMap.get(customToolDefectEntity.getChecker());
        if (checkerDetail != null)
        {
            lintDefectEntity.setSeverity(checkerDetail.getSeverity());
        }
        if (MapUtils.isNotEmpty(fileChangeRecordsMap) && fileChangeRecordsMap.get(customToolDefectEntity.getFilePath()) != null)
        {
            ScmBlameVO fileLineAuthorInfo = fileChangeRecordsMap.get(customToolDefectEntity.getFilePath());
            List<ScmBlameChangeRecordVO> changeRecords = fileLineAuthorInfo.getChangeRecords();
            if (CollectionUtils.isNotEmpty(changeRecords))
            {
                int defectLine = customToolDefectEntity.getLineNum();
                // 告警中的行号为0的改成1
                if (defectLine == 0)
                {
                    defectLine = 1;
                    lintDefectEntity.setLineNum(defectLine);
                }
                for (ScmBlameChangeRecordVO changeRecord : changeRecords)
                {
                    boolean isFound = false;
                    List<Object> lines = changeRecord.getLines();
                    if (lines != null && lines.size() > 0)
                    {
                        for (Object line : lines)
                        {
                            if (line instanceof Integer && defectLine == (int) line)
                            {
                                isFound = true;
                            }
                            else if (line instanceof List)
                            {
                                List<Integer> lineScope = (List<Integer>) line;
                                if (CollectionUtils.isNotEmpty(lineScope) && lineScope.size() > 1)
                                {
                                    if (lineScope.get(0) <= defectLine && lineScope.get(lineScope.size() - 1) >= defectLine)
                                    {
                                        isFound = true;
                                    }
                                }
                            }
                            if (isFound)
                            {
                                lintDefectEntity.setAuthor(changeRecord.getAuthor());
                                long lineUpdateTime = DateTimeUtils.getThirteenTimestamp(changeRecord.getLineUpdateTime());
                                lintDefectEntity.setLineUpdateTime(lineUpdateTime);
                                break;
                            }
                        }
                    }
                    if (isFound)
                    {
                        break;
                    }
                }
            }
        }
    }
}
