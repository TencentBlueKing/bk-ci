-- Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
-- Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
-- BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
-- A copy of the MIT License is included in this file.
-- Terms of the MIT License:
-- ---------------------------------------------------
-- Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
-- The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

-- 二进制网关转发
local x_gw_token = ngx.var.http_x_devops_token -- 二进制网关转发
if x_gw_token ~= nil and x_gw_token == config.gw_token and ngx.var.http_x_devops_uid ~= nil then
    ngx.header["x-devops-uid"] = ngx.var.http_x_devops_uid
    if ngx.var.http_x_devops_bk_token ~= nil then
        ngx.header["x-devops-bk-token"] = ngx.var.http_x_devops_bk_token
    end
    if ngx.var.http_x_devops_access_token ~= nil then
        ngx.header["x-devops-access-token"] = ngx.var.http_x_devops_access_token
    end
    return
end

-- 判断devx访问
local is_devx = string.find(ngx.var.http_host, "devx") ~= nil or ngx.var.http_x_devops_env == "devx"

if is_devx then
    --- 太湖登录
    local bk_token, err = cookieUtil:get_cookie("bk_token")
    if bk_token == nil then
        bk_token = ngx.var.http_x_devops_bk_token
    end
    if bk_token == nil then
        bk_token = urlUtil:parseUrl(ngx.var.request_uri)["x-devops-bk-token"]
    end
    if bk_token ~= nil then
        local ticket = oauthUtil:verify_tai_token(bk_token)
        if ticket ~= nil then
            ngx.header["x-devops-uid"] = ticket.username
            ngx.header["x-devops-bk-token"] = bk_token
            ngx.exit(200)
        end
    end
else
    --- 移动网关登录(下面ms表示mobile site的意思)
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
            if ms_signature == my_signature then
                --- 设置用户信息
                ngx.header["x-devops-uid"] = ms_staffname
                ngx.exit(200)
            end
        end
    end
    local double_check = true and ngx.var.http_host == config.bkci.host and
        not string.find(ngx.var.request_uri, '^/prebuild') and not string.find(ngx.var.request_uri, '^/ms/prebuild') and
        not string.find(ngx.var.request_uri, '^/remotedev') and not string.find(ngx.var.request_uri, '^/ms/remotedev') and
        not string.find(ngx.var.request_uri, '^/websocket') and not string.find(ngx.var.request_uri , '^/ms/project/api/user/users') and
        not string.find(ngx.var.request_uri, "^/project/api/user/projects")
    local tof_staffname
    if double_check then
        --- TOF登录
        local base64 = require "ngx.base64"
        local dec = base64.decode_base64url
        local resty_sha256 = require "resty.sha256"
        local resty_str = require "resty.string"
        local cjson = require "cjson"
        local key = config.tof_token -- token变量

        local function check_sign(key)
            local timestamp = ngx.var.http_timestamp
            if timestamp == nil then
                return false
            end
            local ts = tonumber(timestamp)
            local now = ngx.time()
            if now - ts > 180 or ts - now > 180 then
                ngx.log(ngx.WARN, "tof timestamp error , ", timestamp, " , ts: ", ts)
                return false
            end
            -- check timestamp 180
            local signature = ngx.var.http_signature
            local nonce = ngx.var.http_x_rio_seq
            local signstr = string.format('%s%s%s,%s,%s,%s%s', timestamp, key, nonce, '', '', '', timestamp)
            local hash = resty_sha256:new()
            hash:update(signstr)
            local str = resty_str.str_to_hex(hash:final())
            if string.upper(str) ~= signature then
                ngx.log(ngx.WARN, "tof signature error , ", signature, " , str: ", str)
                return false
            end
            return true
        end

        if not check_sign(key) then
            ngx.log(ngx.WARN, 'check smartgate signature fail')
            ngx.exit(401)
        end

        local str = ngx.var.http_x_tai_identity
        if str then
            local header, enckey, iv, ciphertext, tag = str:match("(.-)%.(.-)%.(.-)%.(.-)%.(.*)")
            local cipher = assert(require("resty.openssl.cipher").new("aes-256-gcm"))
            local plaintext, err = cipher:decrypt(key, dec(iv), dec(ciphertext), false, header, dec(tag))
            if err ~= nil then
                ngx.log(ngx.WARN, "auth tof failed")
                ngx.exit(401)
            end
            local res = cjson.decode(plaintext)
            tof_staffname = res.LoginName
        else
            tof_staffname = ngx.req.get_headers()['staffname']
        end
    end

    --- 内部蓝鲸登录
    local bk_token, err = cookieUtil:get_cookie("bk_ticket")
    if bk_token == nil then
        bk_token = ngx.var.http_x_devops_bk_ticket
    end
    if bk_token == nil then
        bk_token = urlUtil:parseUrl(ngx.var.request_uri)["x-devops-bk-ticket"]
    end
    local devops_access_token = ngx.var.http_x_devops_access_token

    if bk_token ~= nil or devops_access_token ~= nil then
        local ticket = nil
        if devops_access_token ~= nil then
            ticket = oauthUtil:verify_token(devops_access_token)
        else
            ticket = oauthUtil:get_ticket(bk_token)
        end
        if ticket ~= nil then
            if double_check and ticket.user_id ~= tof_staffname then -- 双重校验, 蓝鲸用户必须等于TOF账户
                ngx.log(ngx.WARN, "tof user not eq bk user")
                ngx.exit(401)
            end
            --- 设置用户信息
            ngx.header["x-devops-uid"] = ticket.user_id
            ngx.header["x-devops-bk-token"] = bk_token
            ngx.header["x-devops-access-token"] = ticket.access_token
            ngx.exit(200)
        end
    end
end

ngx.log(ngx.WARN, "auth user failed")
ngx.exit(401)
return
