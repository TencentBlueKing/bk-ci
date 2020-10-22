package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.task.api.ServiceGongfengTaskRestResource;
import com.tencent.devops.common.auth.pojo.GongfengBaseInfo;
import com.tencent.devops.common.auth.service.GongfengAuthTaskService;
import com.tencent.devops.common.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JobGongfengAuthTaskServiceImpl implements GongfengAuthTaskService {
    @Autowired
    private Client client;

    @Override
    public GongfengBaseInfo getGongfengProjInfo(long taskId) {
        return client.get(ServiceGongfengTaskRestResource.class).getGongfengBaseInfo(taskId).getData();
    }

    @Override
    public GongfengBaseInfo getGongfengCIProjInfo(int gongfengId) {
        return client.get(ServiceGongfengTaskRestResource.class).getGongfengCIBaseInfo(gongfengId).getData();
    }
}
