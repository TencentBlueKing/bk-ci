package com.tencent.bk.codecc.task.service;

import com.tencent.bk.codecc.task.pojo.DailyDataReportReqModel;
import com.tencent.bk.codecc.task.pojo.NodeServerRespModel;
import com.tencent.devops.common.api.annotation.ServiceInterface;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@ServiceInterface(value = "node-api")
@Consumes("application/json")
@Produces("application/json")
public interface EmailRenderingService {
    /**
     * 获取邮件内容
     *
     * @param reqVo
     * @return
     */
    @POST
    @Path("/dailymail/content")
    NodeServerRespModel getEmailContent(DailyDataReportReqModel reqVo);
}
