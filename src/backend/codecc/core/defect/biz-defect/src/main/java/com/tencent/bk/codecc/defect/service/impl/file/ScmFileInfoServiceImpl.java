package com.tencent.bk.codecc.defect.service.impl.file;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.component.ScmJsonComponent;
import com.tencent.bk.codecc.defect.dao.mongorepository.CodeRepoInfoRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.ToolBuildStackRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.file.ScmFileInfoCacheRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.file.ScmFileInfoCacheDao;
import com.tencent.bk.codecc.defect.model.file.ScmFileInfoCacheEntity;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoEntity;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.pojo.FileMD5SingleModel;
import com.tencent.bk.codecc.defect.pojo.FileMD5TotalModel;
import com.tencent.bk.codecc.defect.service.file.ScmFileInfoService;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameChangeRecordVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.defect.vo.file.ScmFileMd5Info;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ScmFileInfoServiceImpl implements ScmFileInfoService {

    @Autowired
    private ScmFileInfoCacheDao scmFileInfoCacheDao;
    @Autowired
    private ScmJsonComponent scmJsonComponent;
    @Autowired
    private ScmFileInfoCacheRepository scmFileInfoCacheRepository;
    @Autowired
    private ToolBuildStackRepository toolBuildStackRepository;
    @Autowired
    private CodeRepoInfoRepository codeRepoInfoRepository;

    @Override
    public List<ScmFileMd5Info> listMd5FileInfos(long taskId, String toolName, String buildId)
    {
        log.info("listMd5FileInfos, taskId: {} toolName: {}, buildId: {}", taskId, toolName, buildId);

        // 如果前后两次分析的仓库地址或者分支发生了变化，则缓存失效
        boolean isCodeRepoChange = false;
        ToolBuildStackEntity toolBuildStack =
                toolBuildStackRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        if (toolBuildStack != null) {
            String lastBuildId = toolBuildStack.getBaseBuildId();
            if (StringUtils.isNotEmpty(lastBuildId)) {
                List<CodeRepoInfoEntity> codeRepoInfoList = codeRepoInfoRepository.findByTaskIdAndBuildIdIn(taskId,
                        Sets.newHashSet(buildId, lastBuildId));
                if (codeRepoInfoList.size() == 2) {
                    List<CodeRepoEntity> codeRepoInfo1 = codeRepoInfoList.get(0).getRepoList();
                    List<CodeRepoEntity> codeRepoInfo2 = codeRepoInfoList.get(1).getRepoList();
                    Set<String> codeRepoInfoKeys1 = codeRepoInfo1.stream().map(it -> {
                        if (StringUtils.isNotEmpty(it.getRepoId())) {
                            return String.format("%s_%s", it.getRepoId(), it.getBranch());
                        } else {
                            return String.format("%s_%s", it.getUrl(), it.getBranch());
                        }
                    }).collect(Collectors.toSet());
                    Set<String> codeRepoInfoKeys2 = codeRepoInfo2.stream().map(it -> {
                        if (StringUtils.isNotEmpty(it.getRepoId())) {
                            return String.format("%s_%s", it.getRepoId(), it.getBranch());
                        } else {
                            return String.format("%s_%s", it.getUrl(), it.getBranch());
                        }
                    }).collect(Collectors.toSet());

                    isCodeRepoChange = !CollectionUtils.isEqualCollection(codeRepoInfoKeys1, codeRepoInfoKeys2);
                }
            }
        }
        if (isCodeRepoChange) {
            return Collections.emptyList();
        }

        List<ScmFileInfoCacheEntity> result =
                scmFileInfoCacheRepository.findSimpleByTaskIdAndToolName(taskId, toolName);
        return result.stream().map((entity) ->
        {
            ScmFileMd5Info fileMd5Info = new ScmFileMd5Info();
            BeanUtils.copyProperties(entity, fileMd5Info);
            return fileMd5Info;
        }).collect(Collectors.toList());
    }

    @Override
    @Async("asyncTaskExecutor")
    public void parseFileInfo(long taskId, String streamName, String toolName, String buildId)
    {
        log.info("start to parse file md5 info: {}, {}, {}", taskId, toolName, buildId);
        try {
            doParseFileInfo(taskId, streamName, toolName, buildId);
        } catch (Exception e) {
            log.error("parse file info fail for {}, {}",taskId, toolName, e);
        }
    }

    private void doParseFileInfo(long taskId, String streamName, String toolName, String buildId)
    {
        Map<String, ScmBlameVO> fileChangeRecordsMap = loadAuthorInfoMap(taskId, streamName, toolName, buildId);

        List<ScmFileInfoCacheEntity> fileInfoEntities = fileChangeRecordsMap.values().stream().map(scmBlame -> {
            String md5 = scmBlame.getExtraInfoMap().get("md5");

            // 保存md5文件信息
            ScmFileInfoCacheEntity entity = new ScmFileInfoCacheEntity();
            BeanUtils.copyProperties(scmBlame, entity);

            entity.setTaskId(taskId);
            entity.setToolName(toolName);
            entity.setMd5(md5);
            entity.setBuildId(buildId);
            // 转化数据
            entity.setChangeRecords(scmBlame.getChangeRecords().stream().map((it) -> {
                ScmFileInfoCacheEntity.ScmBlameChangeRecordVO record = new ScmFileInfoCacheEntity.ScmBlameChangeRecordVO();
                record.setAuthor(it.getAuthor());
                record.setLines(it.getLines());
                record.setLineUpdateTime(it.getLineUpdateTime());
                return record;
            }).collect(Collectors.toList()));

            return entity;
        }).collect(Collectors.toList());

        scmFileInfoCacheDao.batchSave(fileInfoEntities);
        log.info("parse file md5 info size: {}, {}, {}", fileInfoEntities.size(), taskId, buildId);
    }

    @Override
    public Map<String, ScmBlameVO> loadAuthorInfoMap(long taskId, String streamName, String toolName, String buildId)
    {
        List<ScmBlameVO> fileChangeRecords = new ArrayList<>();

        // MD5文件是全量的，以这个为准
        FileMD5TotalModel fileMD5TotalModel = scmJsonComponent.loadFileMD5(streamName, toolName, buildId);
        Map<String, FileMD5SingleModel> fileMd5INfoMap = new HashMap<>();
        fileMD5TotalModel.getFileList().forEach((fileInfo) -> {
            String path = StringUtils.isNotEmpty(fileInfo.getFileRelPath()) ? fileInfo.getFileRelPath() : fileInfo.getFilePath();
            fileMd5INfoMap.put(path, fileInfo);
        });

        // 读之前缓存的文件
        fileChangeRecords.addAll(listScmBlameFileInfos(taskId, toolName));

        // 读最新的文件
        fileChangeRecords.addAll(scmJsonComponent.loadAuthorInfo(streamName, toolName, buildId));

        Map<String, ScmBlameVO> fileChangeRecordsMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(fileChangeRecords))
        {
            for (ScmBlameVO fileLineAuthor : fileChangeRecords)
            {
                FileMD5SingleModel md5Info = fileMd5INfoMap.get(fileLineAuthor.getFileRelPath()) != null ?
                        fileMd5INfoMap.get(fileLineAuthor.getFileRelPath()) : fileMd5INfoMap.get(fileLineAuthor.getFilePath());

                // 以md5.json的文件列表为准
                fileLineAuthor.setExtraInfoMap(new HashMap<>());
                if (md5Info != null)
                {
                    // md5.json文件才是最新的
                    fileLineAuthor.getExtraInfoMap().put("md5", md5Info.getMd5());
                    fileLineAuthor.setFilePath(md5Info.getFilePath());
                }

                fileChangeRecordsMap.put(fileLineAuthor.getFilePath(), fileLineAuthor);
            }
        }
        return fileChangeRecordsMap;
    }

    private List<ScmBlameVO> listScmBlameFileInfos(long taskId, String toolName)
    {
        List<ScmFileInfoCacheEntity> result = scmFileInfoCacheRepository.findByTaskIdAndToolName(taskId, toolName);
        if (result.isEmpty())
        {
            return Collections.emptyList();
        }
        return result.stream().map((entity) -> {
            ScmBlameVO scmBlameVO = new ScmBlameVO();
            BeanUtils.copyProperties(entity, scmBlameVO);
            if (CollectionUtils.isNotEmpty(entity.getChangeRecords()))
            {
                scmBlameVO.setChangeRecords(entity.getChangeRecords().stream().map((it) -> {
                    ScmBlameChangeRecordVO record = new ScmBlameChangeRecordVO();
                    record.setAuthor(it.getAuthor());
                    record.setLines(it.getLines());
                    record.setLineUpdateTime(it.getLineUpdateTime());
                    return record;
                }).collect(Collectors.toList()));
            }
            return scmBlameVO;
        }).collect(Collectors.toList());
    }
}
