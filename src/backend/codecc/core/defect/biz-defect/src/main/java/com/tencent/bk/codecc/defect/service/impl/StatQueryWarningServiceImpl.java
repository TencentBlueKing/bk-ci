package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongotemplate.StatDefectDao;
import com.tencent.bk.codecc.defect.model.StatDefectEntity;
import com.tencent.bk.codecc.defect.service.IStatQueryWarningService;
import com.tencent.bk.codecc.defect.vo.StatDefectQueryRespVO;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.util.JsonUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StatQueryWarningServiceImpl implements IStatQueryWarningService {

    @Autowired
    StatDefectDao statDefectDao;

    @Override
    public List<StatDefectQueryRespVO> processQueryWarningRequest(long taskId, String toolName, long startTime, long endTime) {
        // 时间范围默认为当天
        if (startTime == 0 || endTime == 0) {
            LocalDateTime begin = LocalDateTime.of(LocalDate.now(), LocalTime.of(0, 0, 0, 0));
            LocalDateTime end = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(0, 0, 0, 0));
            startTime = begin.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            endTime = end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }

        List<Document> defectEntityList = statDefectDao.getByTaskIdAndToolNameAndTime(taskId, toolName);
        long finalEndTime = endTime;
        long finalStartTime = startTime;
        defectEntityList = defectEntityList.stream()
                .filter(document -> document.getInteger("time_stamp") >= finalStartTime && document.getInteger("time_stamp") < finalEndTime)
                .collect(Collectors.toList());

        List<StatDefectQueryRespVO> statDefectQueryRespVOList = new ArrayList<>(defectEntityList.size());
        defectEntityList.forEach(statDefectEntity -> {
            StatDefectQueryRespVO respVO = new StatDefectQueryRespVO();
            respVO.setStatInfo(JsonUtil.INSTANCE.toMap(JsonUtil.INSTANCE.toJson(statDefectEntity)));
            statDefectQueryRespVOList.add(respVO);
        });
        return statDefectQueryRespVOList;
    }

    public Long getLastestMsgTime(long taskId) {
        List<StatDefectEntity> lastDefectList = statDefectDao
                .getByTaskIdAndToolNameOrderByTime(taskId, Tool.GITHUBSTATISTIC.name());
        if (lastDefectList == null || lastDefectList.isEmpty()) {
            log.warn("last githubstatistic defect of {} is null", taskId);
            return 0L;
        }

        return Long.parseLong(lastDefectList.get(0).getString("time_stamp"));
    }
}
