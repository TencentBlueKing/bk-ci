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
  static_dir = "__BK_REPO_HOME__/frontend",
  home_dir = "__BK_REPO_HOME__",
  service_name = "__BK_REPO_SERVICE_NAME__",  -- 指定后台微服务名称，如果对接后端是boot-assembly的单体微服务，则该配置项为bk-ci, 否则请置空会自动路由相应微服务
  service_prefix = "__BK_REPO_SERVICE_PREFIX__", -- 微服务前缀
  allow_hosts = {
    "__BK_REPO_GATEWAY_CORS_ALLOW_LIST__"
  },
  allow_headers = "Authorization,Content-Type,withcredentials,credentials,Accept,Origin,User-Agent,Cache-Control,Keep-Alive,X-Requested-With,If-Modified-Since,X-CSRFToken,X-DEVOPS-PROJECT-ID,X-DEVOPS-TASK-ID,X-BKREPO-UID,X-BKREPO-API-TYPE",
  ns = {
    ip = {
      "__BK_REPO_CONSUL_DNS_HOST__"
    },
    port = "__BK_REPO_CONSUL_DNS_PORT__",
    http_port = "__BK_REPO_CONSUL_SERVER_PORT__",
    domain = "__BK_REPO_CONSUL_DOMAIN__",
    tag = "__BK_REPO_CONSUL_TAG__",
    nodes_url = "/v1/catalog/nodes"
  },
  oauth = {  -- 对接蓝鲸权限中心才需要的配置
    ip = "__BK_REPO_SSM_IP0__",
    env = "__BK_REPO_SSM_ENV__",
    port = "__BK_REPO_SSM_HTTP_PORT__",
    host = "__BK_REPO_SSM_HOST__",
    url = "__BK_REPO_SSM_TOKEN_URL__",     -- 接口路径
    app_code = "__BK_REPO_APP_CODE__",
    app_secret = "__BK_REPO_APP_TOKEN__",
  },
  bkrepo = {
    authorization = "__BK_REPO_AUTHORIZATION__",
    domain = "__BK_REPO_DOMAIN__"
  },
  mode = "__BK_REPO_DEPLOY_MODE__",
  router = {
    project = {
      {
        name = "devops",
        tag = "devops"
      }
    }
  }
}
  
require("init_common")
require("ip_whitelist")
