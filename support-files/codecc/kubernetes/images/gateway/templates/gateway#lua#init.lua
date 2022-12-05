--[[
Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸持续集成平台 available.

Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.

BK-CODECC 蓝鲸持续集成平台 is licensed under the MIT license.

A copy of the MIT License is included in this file.


Terms of the MIT License:
---------------------------------------------------
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and assoCODECCated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
]]

config = {
  env = "__BK_CI_ENV__",
  static_dir = "__BK_CI_HOME__/frontend",
  static_dir_gray = "__BK_CI_HOME__/frontend-gray",
  docs_dir = "__BK_CI_HOME__/docs",
  static_dir_codecc = "__BK_CODECC_HOME__/frontend",
  http_schema = "__BK_HTTP_SCHEMA__", -- 蓝鲸PaaS平台访问协议 http or https, 如果有对接才配置修改，开�有对接才配置修改，开源默认没对接
  paas_http_port = "__BK_PAAS_HTTPS_PORT__", -- 蓝鲸PaaS平台域�有对接才配置修改，开源默认没对接
  login_url = "__BK_CODECC_PAAS_LOGIN_URL__",   -- 蓝鲸PaaS平台登录URL, 如果有对接才配置修改，开溍务�对接�务，则该配置项为bk-CODECC, �务
  service_prefix = "__BK_CODECC_SERVICE_PREFIX__",
  allow_hosts = {
    "__BK_CODECC_GATEWAY_CORS_ALLOW_LIST__"
  },
  allow_headers = "Authorization,Content-Type,withcredentials,credentials,Accept,Origin,User-Agent,Cache-Control,Keep-Alive,X-Requested-With,If-Modified-Since,X-CSRFToken,X-DEVOPS-PROJECT-ID,X-DEVOPS-TASK-ID,X-DEVOPS-TOKEN",
  ns = {
    ip = {
      "__BK_CODECC_CONSUL_IP__"
    },
    port = __BK_CODECC_CONSUL_DNS_PORT__,
    http_port = __BK_CODECC_CONSUL_HTTP_PORT__,
    domain = "__BK_CODECC_CONSUL_DOMAIN__",
    tag = "__BK_CODECC_CONSUL_DISCOVERY_TAG__",
    suffix = "-__BK_CODECC_CONSUL_DISCOVERY_TAG__",
    nodes_url = "/v1/catalog/nodes"
  },
  oauth = {  -- 对接蓝鲸权陀要的配置
    ip = "__BK_SSM_HOST__",
    env = "__BK_CI_IAM_ENV__",
    port = "__BK_SSM_PORT__",
    host = "__BK_SSM_HOST__",
    url = "__BK_CI_GATEWAY_SSM_TOKEN_URL__",     -- 接口路径
    app_code = "__BK_CI_APP_CODE__",
    app_secret = "__BK_CI_APP_TOKEN__"
  },
  bkci = {host = "__BK_CI_FQDN__", port = 80},
}
  
require("init_common")
require("ip_whitelist")