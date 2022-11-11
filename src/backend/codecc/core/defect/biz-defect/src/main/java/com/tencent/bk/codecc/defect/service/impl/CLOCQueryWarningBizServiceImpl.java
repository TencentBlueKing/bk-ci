package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongorepository.CLOCStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CodeRepoFromAnalyzeLogRepository;
import com.tencent.bk.codecc.defect.model.CLOCStatisticEntity;
import com.tencent.bk.codecc.defect.service.AbstractQueryWarningBizService;
import com.tencent.bk.codecc.defect.service.TaskLogService;
import com.tencent.bk.codecc.defect.service.TreeService;
import com.tencent.bk.codecc.defect.vo.CLOCDefectQueryRspInfoVO;
import com.tencent.bk.codecc.defect.vo.CLOCDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.CLOCDefectTreeRespVO;
import com.tencent.bk.codecc.defect.vo.CLOCTreeNodeVO;
import com.tencent.bk.codecc.defect.vo.TaskLogRepoInfoVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.QueryWarningPageInitRspVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.Tool;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@Service("CLOCQueryWarningBizService")
public class CLOCQueryWarningBizServiceImpl extends AbstractQueryWarningBizService
{

    @Autowired
    private CLOCStatisticRepository clocStatisticRepository;

    @Autowired
    CodeRepoFromAnalyzeLogRepository codeRepoFromAnalyzeLogRepository;

    @Autowired
    TaskLogService taskLogService;

    @Override
    public int getSubmitStepNum()
    {
        return ComConstants.Step4MutliTool.COMMIT.value();
    }

    @Autowired
    @Qualifier("CLOCTreeBizService")
    private TreeService treeService;

    /**
     * 获取CLOC告警视图信息
     *
     * @param taskId          任务ID
     * @param pageNum         分页
     * @param pageSize        分页
     * @param queryWarningReq 查询擦树实体类
     * @param sortField       排序字段
     * @param sortType        排序类型
     */
    @Override
    public CommonDefectQueryRspVO processQueryWarningRequest(long taskId, DefectQueryReqVO queryWarningReq, int pageNum, int pageSize, String sortField, Sort.Direction sortType)
    {
        CLOCDefectQueryRspVO clocDefectQueryRspVO = new CLOCDefectQueryRspVO();
        CLOCDefectTreeRespVO clocDefectTreeRespVO = new CLOCDefectTreeRespVO();

        switch (queryWarningReq.getOrder())
        {
            case FILE:
            {
                generateFileTree(clocDefectTreeRespVO, taskId);
                return clocDefectTreeRespVO;
            }
            case LANGUAGE:
            {
                generateLanguage(clocDefectQueryRspVO, taskId);
                return clocDefectQueryRspVO;
            }
            default:
            {
                return clocDefectQueryRspVO;
            }
        }
    }

    @Override
    public CommonDefectDetailQueryRspVO processQueryWarningDetailRequest(long taskId, String userId, CommonDefectDetailQueryReqVO queryWarningDetailReq, String sortField, Sort.Direction sortType)
    {
        return new CommonDefectDetailQueryRspVO();
    }

    @Override
    public QueryWarningPageInitRspVO processQueryWarningPageInitRequest(Long taskId, String toolName, String dimension,
            Set<String> statusSet, String checkerSet, String buildId) {
        return new QueryWarningPageInitRspVO();
    }

    @Override
    public ToolDefectRspVO processToolWarningRequest(Long taskId, DefectQueryReqVO queryWarningReq, Integer pageNum, Integer pageSize, String sortField, Sort.Direction sortType)
    {
        return new ToolDefectRspVO();
    }


    /**
     * 按语言聚类CLOC扫描结果
     *
     * @param taskId               任务ID
     * @param clocDefectQueryRspVO CLOC告警统计响应体实体类
     */
    private void generateLanguage(CLOCDefectQueryRspVO clocDefectQueryRspVO, long taskId)
    {
        String lastBuildId = null;
        CLOCStatisticEntity lastStatisticEntity =
                clocStatisticRepository.findFirstByTaskIdAndToolNameOrderByUpdatedDateDesc(taskId, Tool.CLOC.name());

        if (lastStatisticEntity != null && StringUtils.isNotBlank(lastStatisticEntity.getBuildId()))
        {
            lastBuildId = lastStatisticEntity.getBuildId();
        }
        else if (lastStatisticEntity == null)
        {
            return;
        }

        log.info("query cloc info, taskId: {} | lastBuildId: {}", taskId, lastBuildId);

        List<CLOCStatisticEntity> clocStatisticEntities;
        if (StringUtils.isNotBlank(lastBuildId))
        {
            clocStatisticEntities = clocStatisticRepository.findByTaskIdAndToolNameAndBuildId(
                    taskId, Tool.CLOC.name(), lastBuildId);
        }
        else
        {
            clocStatisticEntities = clocStatisticRepository.findByTaskIdAndToolName(taskId, Tool.CLOC.name());
        }

        if (CollectionUtils.isEmpty(clocStatisticEntities))
        {
            return;
        }

        List<CLOCDefectQueryRspInfoVO> clocDefectQueryRspInfoVOS = new ArrayList<>(clocStatisticEntities.size() + 2);
        long totalBlank = 0;
        long totalCode = 0;
        long totalComment = 0;

        for (CLOCStatisticEntity clocStatisticEntity : clocStatisticEntities
        )
        {
            totalBlank += clocStatisticEntity.getSumBlank();
            totalCode += clocStatisticEntity.getSumCode();
            totalComment += clocStatisticEntity.getSumComment();
        }

        long totalLines = totalBlank + totalCode + totalComment;

        // 注入总计信息
        CLOCDefectQueryRspInfoVO totalInfo = new CLOCDefectQueryRspInfoVO();
        totalInfo.setLanguage("总计");
        totalInfo.setSumBlank(totalBlank);
        totalInfo.setSumCode(totalCode);
        totalInfo.setSumComment(totalComment);
        totalInfo.setSumLines(totalLines);
        totalInfo.setProportion(100);
        clocDefectQueryRspVO.setTotalInfo(totalInfo);

        // 计算其他小比例语言统计信息
        AtomicLong otherSumBlank = new AtomicLong();
        AtomicLong otherSumCode = new AtomicLong();
        AtomicLong otherSumComment = new AtomicLong();
        AtomicLong otherSumLines = new AtomicLong();
        int otherProPortion = 0;

        List<CLOCDefectQueryRspInfoVO> losts = new LinkedList<>();
        // 计算各语言统计信息
        List<CLOCDefectQueryRspInfoVO> finalClocDefectQueryRspInfoVOS = clocDefectQueryRspInfoVOS;
        clocStatisticEntities.forEach(clocStatisticEntity ->
        {
            CLOCDefectQueryRspInfoVO clocDefectQueryRspInfoVO = new CLOCDefectQueryRspInfoVO();
            clocDefectQueryRspInfoVO.setLanguage(clocStatisticEntity.getLanguage());
            long sumBlank = clocStatisticEntity.getSumBlank();
            long sumCode = clocStatisticEntity.getSumCode();
            long sumComment = clocStatisticEntity.getSumComment();
            long sumLines = sumBlank + sumCode + sumComment;
            double proportion = (((double) sumLines / (double) totalLines)) * 100;

            // 四舍五入精度丢失记录
            if ((proportion + 0.5) >= ((int) proportion + 1))
            {
                losts.add(clocDefectQueryRspInfoVO);
            }

            if (proportion < 1)
            {
                otherSumBlank.addAndGet(sumBlank);
                otherSumCode.addAndGet(sumCode);
                otherSumComment.addAndGet(sumComment);
                otherSumLines.addAndGet(sumLines);
            }
            else
            {
                clocDefectQueryRspInfoVO.setSumBlank(sumBlank);
                clocDefectQueryRspInfoVO.setSumCode(sumCode);
                clocDefectQueryRspInfoVO.setSumComment(sumComment);
                clocDefectQueryRspInfoVO.setSumLines(sumLines);
                clocDefectQueryRspInfoVO.setProportion((int) proportion);
                finalClocDefectQueryRspInfoVOS.add(clocDefectQueryRspInfoVO);
            }
        });

        // 计算其他小比例语言行数占比
        AtomicInteger allProportion = new AtomicInteger(clocDefectQueryRspInfoVOS.stream()
                .mapToInt(CLOCDefectQueryRspInfoVO::getProportion)
                .sum());
        if (otherSumLines.get() > 0)
        {
            otherProPortion = 100 - allProportion.get();
        }
        else if ((100 - allProportion.get()) > 0)
        {
            // 上面在计算集合中的语言行数所占百分比时精度丢失导致总百分比不到 100 的情况
            losts.forEach(lost ->
            {
                if (100 - allProportion.get() > 0)
                {
                    lost.setProportion(lost.getProportion() + 1);
                    allProportion.getAndIncrement();
                }
            });
        }
        // 注入其他小比例语言统计信息
        CLOCDefectQueryRspInfoVO otherInfo = new CLOCDefectQueryRspInfoVO();
        otherInfo.setLanguage("Others");
        otherInfo.setSumBlank(otherSumBlank.get());
        otherInfo.setSumCode(otherSumCode.get());
        otherInfo.setSumComment(otherSumComment.get());
        otherInfo.setSumLines(otherSumLines.get());
        otherInfo.setProportion(otherProPortion);
        clocDefectQueryRspVO.setOtherInfo(otherInfo);

        clocDefectQueryRspVO.setTaskId(taskId);
        clocDefectQueryRspVO.setNameEn(Tool.CLOC.name());
        clocDefectQueryRspVO.setToolName(Tool.CLOC.name());
        // 按照代码行百分比排序，注入各语言统计信息
        clocDefectQueryRspInfoVOS = clocDefectQueryRspInfoVOS.stream()
                .sorted((x, y) -> -(x.getProportion() - y.getProportion()))
                .collect(Collectors.toList());
        clocDefectQueryRspVO.setLanguageInfo(clocDefectQueryRspInfoVOS);
    }

    /**
     * 按路径聚类CLOC扫描结果
     *
     * @param taskId               任务ID
     * @param clocDefectTreeRespVO CLOC告警树响应体实体类
     */
    private void generateFileTree(CLOCDefectTreeRespVO clocDefectTreeRespVO, long taskId)
    {
        List<String> toolNames = Arrays.asList(Tool.CLOC.name(), null);

        // 生成文件树
        CLOCTreeNodeVO root = (CLOCTreeNodeVO) treeService.getTreeNode(taskId, toolNames);
        clocDefectTreeRespVO.setClocTreeNodeVO(root);
        clocDefectTreeRespVO.setNameEn(Tool.CLOC.name());
        clocDefectTreeRespVO.setToolName(Tool.CLOC.name());
        clocDefectTreeRespVO.setTaskId(taskId);

        // 注入代码库信息
        List<CLOCDefectTreeRespVO.CodeRepo> repoInfoList = new LinkedList<>();
        Map<String, TaskLogRepoInfoVO> repoInfoMap = taskLogService.getLastAnalyzeRepoInfo(taskId);
        if (repoInfoMap != null && !repoInfoMap.isEmpty())
        {
            repoInfoMap.forEach(
                (repoUrl, taskLogRepoInfoVO) -> {
                if (StringUtils.isNotBlank(repoUrl)) {
                    repoInfoList.add(
                            new CLOCDefectTreeRespVO.CodeRepo(repoUrl, taskLogRepoInfoVO.getBranch()));
                }
            });
        }

        if (repoInfoList.size() == 0)
        {
            log.info("get analyzed repo info fail, taskId: {}", taskId);
            repoInfoList.add(new CLOCDefectTreeRespVO.CodeRepo("", "master"));
        }
        clocDefectTreeRespVO.setCodeRepo(repoInfoList);
    }
}
