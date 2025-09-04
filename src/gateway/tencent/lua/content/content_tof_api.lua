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
local querysArgs = urlUtil:parseUrl(ngx.var.request_uri)
local code = querysArgs["code"]

if code == nil then
    ngx.log(ngx.ERR, "Can not get tof api code")
    ngx.redirect("https://" .. config.bkci.host, 302)
end

-- TOF RIO 网关签名算法
local timestamp = ngx.time()
local token = config.itlogin.token
local nonce = uuid()
local sn = timestamp .. token .. nonce .. timestamp
local resty_sha256 = require "resty.sha256"
local resty_str = require "resty.string"
local sha256 = resty_sha256:new()
sha256:update(sn)
local digest = sha256:final()
local signature = resty_str.str_to_hex(digest)


local httpc = http.new()
if not httpc then
    ngx.log(ngx.ERR, "failed to create httpc")
    ngx.exit(500)
    return
end

local res, err = httpc:request_uri("http://" .. config.itlogin.host ..
    "/ebus/tof4/api/v1/passport/AccessToken?code=" .. code, {
        method = "GET",
        ssl_verify = false,
        headers = {
            ["x-rio-paasid"] = config.itlogin.paasId,
            ["x-rio-signature"] = signature,
            ["x-rio-timestamp"] = tostring(timestamp),
            ["x-rio-nonce"] = nonce,
        }
    })

--- 设置HTTP返回连接池
httpc:set_keepalive(60000, 5)


if not res then
    ngx.log(ngx.ERR, "failed to get tof4 access token: ", err)
    ngx.exit(500)
    return
end

if res.status ~= 200 then
    ngx.log(ngx.ERR, "failed to get tof4 access token, status: ", res.status)
    ngx.exit(res.status)
    return
end

--- 获取所有回复
local responseBody = res.body
--- 转换JSON的返回数据为TABLE
local result = json.decode(responseBody)
if result.ErrCode ~= 0 then
    ngx.log(ngx.ERR, "ErrMsg : ", result.ErrMsg)
    ngx.exit(500)
end

local loginName = result.Data.LoginName

local encrypt = aesUtil.encrypt(loginName, config.itlogin.aseKey)
local expires = ngx.cookie_time(ngx.time() + 604800) -- 7天后过期,跟bk_ticket一致
cookieUtil:set_cookie({
    key = "x-devops-identity",
    value = encrypt,
    path = "/",
    domain = 'devops.woa.com', -- 只写在主域名
    httponly = true,
    expires = expires
})

-- 跳转到指定的url
ngx.redirect(ngx.unescape_uri(querysArgs["url"]), 302)
