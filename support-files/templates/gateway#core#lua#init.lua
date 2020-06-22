--[[
Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.

Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.

BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.

A copy of the MIT License is included in this file.


Terms of the MIT License:
---------------------------------------------------
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
]]

config = {
  env = "__BKCI_ENV__",
  static_dir = "__INSTALL_PATH__/__MODULE__/frontend",
  static_dir_gray = "__INSTALL_PATH__/__MODULE__/frontend-gray",
  docs_dir = "__INSTALL_PATH__/__MODULE__/docs",
  static_dir_codecc = "__INSTALL_PATH__/codecc/frontend",
  http_schema = "__HTTP_SCHEMA__", -- 蓝鲸PaaS平台访问协议 http or https, 如果有对接才配置修改，开源默认没对接
  paas_host = "__PAAS_FQDN__", -- 蓝鲸PaaS平台域名, 如果有对接才配置修改，开源默认没对接
  paas_http_port = "__PAAS_HTTPS_PORT__", -- 蓝鲸PaaS平台域名的端口, 如果有对接才配置修改，开源默认没对接
  login_url = "__PAAS_LOGIN_URL__",   -- 蓝鲸PaaS平台域名, 如果有对接才配置修改，开源默认没对接
  service_name = "",  -- 指定后台微服务名称，如果对接后端是boot-assembly的单体微服务，则该配置项为bk-ci, 否则请置空会自动路由相应微服务
  allow_hosts = {
    "__BKCI_ALLOW_HOST__"
  },
  allow_headers = "Authorization,Content-Type,withcredentials,credentials,Accept,Origin,User-Agent,Cache-Control,Keep-Alive,X-Requested-With,If-Modified-Since,X-CSRFToken,X-DEVOPS-PROJECT-ID,X-DEVOPS-TASK-ID",
  ns = {
    ip = {
      "127.0.0.1"
    },
    port = __BKCI_CONSUL_DNS_PORT__,
    http_port = __BKCI_CONSUL_PORT__,
    domain = "__BKCI_CONSUL_DOMAIN__",
    tag = "__BKCI_CONSUL_TAG__",
    suffix = "-__BKCI_CONSUL_TAG__",
    nodes_url = "/v1/catalog/nodes"
  },
  ns_gray = {
    ip = {
      "__BKCI_CONSUL_GRAY_IP__"
    },
    port = __BKCI_CONSUL_DNS_PORT__,
    http_port = __BKCI_CONSUL_PORT__,
    domain = "__BKCI_CONSUL_DOMAIN__",
    tag = "__BKCI_CONSUL_TAG__",
    suffix = "-__BKCI_CONSUL_TAG__",
    nodes_url = "/v1/catalog/nodes"
  },
  ns_devnet = {
    ip = {
      "127.0.0.1"
    },
    port = __BKCI_CONSUL_DNS_PORT__,
    http_port = __BKCI_CONSUL_PORT__,
    domain = "__BKCI_CONSUL_DOMAIN__",
    tag = "__BKCI_CONSUL_TAG__",
    suffix = "-__BKCI_CONSUL_TAG__",
    nodes_url = "/v1/catalog/nodes"
  },
  ns_devnet_gray = {
    ip = {
      "__BKCI_CONSUL_DEVNET_GRAY_IP__"
    },
    port = __BKCI_CONSUL_DNS_PORT__,
    http_port = __BKCI_CONSUL_PORT__,
    domain = "__BKCI_CONSUL_DOMAIN__",
    tag = "__BKCI_CONSUL_TAG__",
    suffix = "-__BKCI_CONSUL_TAG__",
    nodes_url = "/v1/catalog/nodes"
  },
  paasCIDomain = "__BKCI_PAASCI_FQDN__",
  job = {
    domain = "__BKCI_JOB_FQDN__"
  },
  redis = {
    host = "__REDIS_IP__",
    port = __REDIS_PORT__,
    pass = "__REDIS_PASS__",  -- redis 密码，没有密码的话，把这行注释掉
    database = __REDIS_DB__,         -- 默认选择db0
    max_idle_time = 600000, -- 保留在连接池的时间
    pool_size = 10         -- 连接池的大小
  },
  oauth = {  -- 对接蓝鲸权限中心才需要的配置
    ip = "__IAM_IP0__",
    env = "__IAM_ENV__",
    port = "__IAM_HTTP_PORT__",
    host = "__IAM_HOST__",
    url = "__IAM_TOKEN_URL__",     -- 接口路径
    app_code = "__APP_CODE__",
    app_secret = "__APP_TOKEN__",
  },
  artifactory = {
    port = "__JFROG_HTTP_PORT__",
    docker = "__DOCKER_PORT__",
    host = "__BKCI_GATEWAY_IP0__",
    domain = "__JFROG_FQDN__",
    user = "__JFROG_USERNAME__",
    password = "__JFROG_PASSWORD__"
  },
  influxdb = {
    ip = "__INFLUXDB_IP0__",
    port = "__INFLUXDB_PORT__",
    db = "__INFLUXDB_DB__",
    user = "__INFLUXDB_USERNAME__",
    password = "__INFLUXDB_PASSWORD__"
  },
  bkrepo = {
    domain = "__BKREPO_HOST__"
  },
  jwtPublicKey = "-----BEGIN RSA PRIVATE KEY-----\nMIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDi4fICYzKEtm3l\nZoSPg5/Iox31QLYgVYZbaOEgzvZvjil/dq7qPKBlbIsJc68FA+7Qkpw957Jj1S9A\ndNi2Ci3yHy1PzyVcJ1qLKrRGVjBD1ZpKJP9WH9Q7L8FZZxkbuAm1FDHIMy46v1Hl\nPWc3Ir7Ai8J0tX8tjnuHRuWnv7y/Ro6Y1MJ/K6yU1yQOLYFSOZnWhTgM6TUZ7h4L\nJdeZ0KA3BxYUNtEU9Q8XFGE2hnb5IkIHeTk3gXTm8Y1Xdq9CbZe+jN7dWtR0uE/6\nmAx71X0qQ2bwa0lvR983ocm4noQlhROdIneZ6cxs0u7bRW+z0FADAmN78D33Pur8\n9wQFmiBrAgMBAAECggEBAJMSamHX0eCrrVN+gEHTzhkue/YGi8ksB5trwjwVTTSF\nUCs7USmwQT1d/kcTQYobwYxc7YFHl6EVibrbw7tFoAEK6sGIgyxYql36QcAykLj4\nVzrm/snieh4f19NPfLw2Mby7KYYgf0A/0yOqCSV5lXOVZWloWde7PCI1+BsktIK8\nTo3lVCTKBVtYhHalm5W4TzLncsKe2UQN3k17d50YAQeG+3G1v42MlJCvVDmTQvFz\neGZHqZQvBK0gaSnkcLL/IahJPvZxlJRHI3EzJVMPSwIoGzV0hG223JktGqSrRm7Z\n1ZA0UVWVmTnmdT4wufDb4A+osiEMLbey4b7V0Pl0UukCgYEA+EnFDC6rNEeHh+gK\njj1nn2kHbZPTjJdwYi3PVC9TfI2+CY/w98PPKFHDoIY4TGdF4j3S2+DQXNb85Y+d\nIbUTDc5ES6ceiPe8rvbV4sELYILT4F4wkpBw/Oh33ufqwqn41CPph1gjOgSa4jYe\nSiILu0ARACl0QGoqVCEzOvl4fmUCgYEA6e343hpgN91KHY4W7rLQootKsWYND1ZC\ni3+QS6YHBKFozLvlqtM3l8mEIuyUcyE9U5AEdFi7IMZj9v1kho83edjJ9/MJeNJ8\nl39oPaNbX3FexwJcl8Z06ZOHoj19LBY1P7t/FU4Uv2AtHIgYlM4CpIpLZhJTBYAD\nOEh1EdO6Do8CgYEA6zXPYxQPAk7E+R38afWH5f80lz0UirqoL4ogQCs5VuRcZGil\nKcKozBRxU+/zA4ZOMN7Kk5wtJ9ZO7BYaEGWesFR4ZIbkKXMvnzydMNwaMAqgN4xj\nTWVidGSxskxYHKOy4x1GTP5VGNBl/eiw1x/bpz8xG8spoyAwC1UMWFEfMfUCgYA4\nfkrywGXqN9vVRWJOZQqzpnX0X5PZ11gcvkLHsiHRwXVAtEPjvDyZwIXTtVSodSeN\nTaN1wZP3d1He7RTg1idsmqkz4xKvhg2mvJMdB51icEuWPgDEep3zZriDutvG0Not\nOQeYypGCIiTi1g8xqIrE180bqmM4WdDtP7peAsbVCwKBgQDmpHEcEi56Ng8KscJ+\nHk8PTt+BPbLUK1tGqIsuiYh67PIfYLKA2E9Mply3f89OAA8TDj4ZemiO6919N3Lr\n3kFlOWcs9knWrF/ICcz7wPfPVvU4YI28CVMd9BYn95ArbrIY1Cj13XpPc6da6NSe\nXhT2x6QN+NfcgSLjqGo0oEMg0w==\n-----END RSA PRIVATE KEY-----" 
}
  
require("init_common")
require("ip_whitelist")
  