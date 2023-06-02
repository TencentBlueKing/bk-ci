-- Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
-- Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
-- BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
-- A copy of the MIT License is included in this file.
-- Terms of the MIT License:
-- ---------------------------------------------------
-- Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
-- documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
-- rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
-- permit persons to whom the Software is furnished to do so, subject to the following conditions:
-- The above copyright notice and this permission notice shall be included in all copies or substantial portions of
-- the Software.
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
-- LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
-- NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
-- WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
-- SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
config = {
    env = "__BK_CI_ENV__",
    internal_file_dir = "__BK_CI_DATA_DIR__/gateway/files/",
    static_dir = "__BK_CI_HOME__/frontend",
    static_dir_gray = "__BK_CI_HOME__/frontend-gray",
    docs_dir = "__BK_CI_HOME__/docs",
    static_dir_codecc = "__BK_CODECC_HOME__/frontend",
    static_dir_codecc_gray = "__BK_CODECC_HOME__/frontend-gray",
    http_schema = "__BK_HTTP_SCHEMA__", -- 蓝鲸PaaS平台访问协议 http or https, 如果有对接才配置修改，开源默认没对接
    paas_host = "__BK_PAAS_FQDN__", -- 蓝鲸PaaS平台域名, 如果有对接才配置修改，开源默认没对接
    paas_http_port = "__BK_PAAS_HTTPS_PORT__", -- 蓝鲸PaaS平台域名的端口, 如果有对接才配置修改，开源默认没对接
    login_url = "__BK_CI_PAAS_LOGIN_URL__", -- 蓝鲸PaaS平台登录URL, 如果有对接才配置修改，开源默认没对接
    service_name = "", -- 指定后台微服务名称，如果对接后端是boot-assembly的单体微服务，则该配置项为bk-ci, 否则请置空会自动路由相应微服务
    allow_hosts = {"__BK_CI_GATEWAY_CORS_ALLOW_LIST__"},
    allow_headers = "Authorization,Content-Type,withcredentials,credentials,Accept,Origin,User-Agent,Cache-Control,Keep-Alive,X-Requested-With,If-Modified-Since,X-CSRFToken,X-DEVOPS-PROJECT-ID,X-DEVOPS-TASK-ID,X-DEVOPS-TOKEN",
    ns = {
        ip = {"__BK_CI_CONSUL_IP__"},
        port = __BK_CI_CONSUL_DNS_PORT__,
        http_port = __BK_CI_CONSUL_HTTP_PORT__,
        domain = "__BK_CI_CONSUL_DOMAIN__",
        tag = "__BK_CI_CONSUL_DISCOVERY_TAG__",
        suffix = "-__BK_CI_CONSUL_DISCOVERY_TAG__",
        nodes_url = "/v1/catalog/nodes"
    },
    ns_devnet = {
        ip = {"__BK_CI_CONSUL_DEVNET_IP__"},
        port = __BK_CI_CONSUL_DNS_PORT__,
        http_port = __BK_CI_CONSUL_PORT__,
        domain = "__BK_CI_CONSUL_DOMAIN__",
        tag = "__BK_CI_CONSUL_DISCOVERY_TAG__",
        suffix = "-__BK_CI_CONSUL_DISCOVERY_TAG__",
        nodes_url = "/v1/catalog/nodes"
    },
    paasCIDomain = "__BK_CI_PAASCI_FQDN__",
    job = {domain = "__BK_CI_JOB_FQDN__"},
    redis = {
        host = "__BK_CI_REDIS_HOST__",
        port = __BK_CI_REDIS_PORT__,
        pass = "__BK_CI_REDIS_PASSWORD__", -- redis 密码，没有密码的话，把这行注释掉
        database = __BK_CI_REDIS_DB__, -- 默认选择db0
        max_idle_time = 600000, -- 保留在连接池的时间
        pool_size = 5, -- 连接池的大小
        backlog = 100, -- 连接等待队列
        ssl = __BK_CI_REDIS_SSL__
    },
    oauth = { -- 对接蓝鲸权限中心才需要的配置
        ip = "__BK_SSM_HOST__",
        env = "__BK_CI_IAM_ENV__",
        port = "__BK_SSM_PORT__",
        host = "__BK_SSM_HOST__",
        url = "__BK_CI_GATEWAY_SSM_TOKEN_URL__", -- 接口路径
        app_code = "__BK_CI_APP_CODE__",
        app_secret = "__BK_CI_APP_TOKEN__"
    },
    artifactory = {
        port = "__BK_CI_JFROG_HTTP_PORT__",
        docker = "__BK_CI_DOCKER_PORT__",
        host = "__BK_CI_HOST__",
        domain = "__BK_CI_JFROG_FQDN__",
        user = "__BK_CI_JFROG_USER__",
        password = "__BK_CI_JFROG_PASSWORD__",
        realm = "__BK_CI_ARTIFACTORY_REALM__"
    },
    influxdb = {
        ip = "__BK_CI_INFLUXDB_HOST__",
        port = "__BK_CI_INFLUXDB_PORT__",
        db = "__BK_CI_INFLUXDB_DB__",
        user = "__BK_CI_INFLUXDB_USER__",
        password = "__BK_CI_INFLUXDB_PASSWORD__"
    },
    bkrepo = {
        domain = "__BK_REPO_FQDN__",
        authorization = "__BK_CI_BKREPO_AUTHORIZATION__"
    },
    bkci = {host = "__BK_CI_FQDN__", port = 80},
    kubernetes = {
        domain = "kubernetes.demo.com",
        switchAll = false,
        codecc = {domain = "kubernetes.demo.com"},
        useForceHeader = false,
        tags = {},
        codeccTags = {},
        api = {
            host = "kubernetes.demo.com",
            port = 6443 ,
            token = ""
        }
    }
}

require("init_common")
require("ip_whitelist")

