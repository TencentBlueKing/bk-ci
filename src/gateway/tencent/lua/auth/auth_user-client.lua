-- Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
-- Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
-- BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
-- A copy of the MIT License is included in this file.
-- Terms of the MIT License:
-- ---------------------------------------------------
-- Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
-- The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
--- 蓝鲸平台登录对接
--- 获取Cookie中bk_token
local bk_token, err = cookieUtil:get_cookie("bk_ticket")
local devops_access_token = ngx.var.http_x_devops_access_token

if bk_token ~= nil or devops_access_token ~= nil then
    local ticket = nil
    if devops_access_token ~= nil then
        ticket = oauthUtil:verify_token(devops_access_token)
    else
        ticket = oauthUtil:get_ticket(bk_token)
    end
    if ticket ~= nil then
        --- 设置用户信息
        ngx.header["x-devops-uid"] = ticket.user_id
        ngx.header["x-devops-bk-token"] = bk_token
        ngx.header["x-devops-access-token"] = ticket.access_token
        ngx.exit(200)
    end
end

--- 移动网关登录对接(下面ms表示mobile site的意思)
local ms_timestamp = ngx.var.http_timestamp
local ms_signature = ngx.var.http_signature
local ms_staffid = ngx.var.http_staffid
local ms_staffname = ngx.var.http_staffname
local ms_x_ext_data = ngx.var.http_x_ext_data
local ms_x_rio_seq = ngx.var.http_x_rio_seq
if ms_timestamp ~= nil and ms_signature ~= nil and ms_staffid ~= nil and ms_staffname ~= nil and ms_x_ext_data ~= nil and
    ms_x_rio_seq ~= nil then
    local timestamp = ngx.time()
    local absTime = math.abs(tonumber(ms_timestamp) - timestamp)
    if absTime < 180 then
        local token = config.mobileSiteToken
        local input = ms_timestamp .. token .. ms_x_rio_seq .. "," .. ms_staffid .. "," .. ms_staffname .. "," ..
                          ms_x_ext_data .. ms_timestamp
        local resty_sha256 = require "resty.sha256"
        local resty_str = require "resty.string"
        local sha256 = resty_sha256:new()
        sha256:update(input)
        local digest = sha256:final()
        local my_signature = string.upper(resty_str.str_to_hex(digest))
        ngx.log(ngx.ERR, "my_signature : ", my_signature)
        if ms_signature == my_signature then
            --- 设置用户信息
            ngx.header["x-devops-uid"] = ms_staffname
            ngx.exit(200)
        end
    end
end

ngx.log(ngx.WARN, "auth user failed")
ngx.exit(401)
return
