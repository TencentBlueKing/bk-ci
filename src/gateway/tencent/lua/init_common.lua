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
string = require("string")
math = require("math")
json = require("cjson.safe")
uuid = require("resty.jit-uuid")
resolver = require("resty.dns.resolver")
ck = require("resty.cookie")
http = require("resty.http")
jwt = require("resty.jwt")
stringUtil = require("util.string_util")
ipUtil = require("util.ip_util")
consulUtil = require("util.consul_util")
logUtil = require("util.log_util")
redisUtil = require("util.redis_util")
oauthUtil = require("util.oauth_util")
md5 = require("resty.md5")
arrayUtil = require("util.array_util")
cookieUtil = require("util.cookie_util")
itloginUtil = require("util.itlogin_util")
outerloginUtil = require("util.outerlogin_util")
urlUtil = require("util.url_util")
tagUtil = require("util.tag_util")
loadBalanceUtil = require("util.loadbalance_util")
accessControlUtil = require("util.access_control_util")
securityUtil = require("util.security_util")
ciAuthUtil = require("util.ci_auth_util")
buildUtil = require("util.build_util")
cjson = require("cjson")
resolvUtil = require("util.resolv_util")

local ok_table = {status = 0, data = true}

no_container_svr = {"scm", "sign", "config"}

response_ok = json.encode(ok_table)

