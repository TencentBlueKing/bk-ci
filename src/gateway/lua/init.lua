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
  static_dir = "/data/bkee/bkci/frontend",
  docs_dir = "/data/bkee/bkci/docs",
  http_schema = "https",   -- 蓝鲸PaaS平台访问协议 http or https, 如果有对接才配置修改，开源默认没对接,设置无效
  paas_domain = "__PAAS_FQDN__",  -- 蓝鲸PaaS平台域名, 如果有对接才配置修改，开源默认没对接,设置无效
  service_name = "",  -- 指定后台微服务名称，如果对接后端是boot-assembly的单体微服务，则该配置项为bk-ci, 否则请置空会自动路由相应微服务
  allow_hosts = {
  },
  allow_headers = "Authorization,Content-Type,Accept,Origin,User-Agent,Cache-Control,Keep-Alive,X-Requested-With,If-Modified-Since,X-CSRFToken,X-DEVOPS-PROJECT-ID",
  ns = {
    ip = {
      "127.0.0.1"
    },
    port = 8600,
    http_port = 8500,
    domain = "bkdevops",
    tag = "devops",
    nodes_url = "/v1/catalog/nodes"
  },
  ns = {
    ip = {
      "127.0.0.1"
    },
    port = 8600,
    http_port = 8500,
    domain = "bkdevops",
    tag = "devops",
    nodes_url = "/v1/catalog/nodes"
  },
  redis = {
    host = "__REDIS_IP0__",
    port = __REDIS_PORT__,
    pass = "__REDIS_PASS__",  -- redis 密码，没有密码的话，把这行注释掉
    database = 1,
    max_idle_time = 600000, -- 保留在连接池的时间
    pool_size = 10         -- 连接池的大小
  },
  service_ip_whitelist = {
    -- local
    "127.0.0.1",
  }
}

require("init_common")
