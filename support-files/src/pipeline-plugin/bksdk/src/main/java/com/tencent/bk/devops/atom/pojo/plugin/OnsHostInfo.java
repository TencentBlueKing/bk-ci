package com.tencent.bk.devops.atom.pojo.plugin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnsHostInfo {

    private String ip; //IP地址
    private int port; //端口号

    public OnsHostInfo() {
    }

    public OnsHostInfo(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }
}
