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
local _M = {}
function _M:isSafe()
    if ngx.var.safe == 'true' then
        return true
    end

    -- 根请求直接忽略
    local path = ngx.var.uri
    if path == '/index.html' or path == '/' then
        return true
    end

    -- auth location 可以忽略
    local auth_location = ngx.var.auth_location
    if auth_location == "1" then
        return true
    end

    -- external location 可以忽略
    local external_location = ngx.var.external_location
    if external_location == "1" then
        return true
    end

    -- 外部链接安全检查
    local host = ngx.var.host
    local header_clb = ngx.var.http_x_clb_lbid
    if string.find(host, "bkdevops.qq.com") ~= nil or header_clb ~= nil then -- bkdevops.qq.com 相关域名或者传了lb的ID
        local security_paths = {
            "/([%w-_]+)/api/app/", -- app 路径
            "/([%w-_]+)/api/desktop/", -- 离岸开发 路径
            "/websocket/ws/desktop", -- 离岸开发 ws
            "/experience/api/open/experiences/appstore/redirect", -- 跳转到AppStore
            "/experience/api/open/experiences/outerLogin", -- 外部用户登录
            "/process/api/external/scm/codetgit/commit", -- 仓库的external/generic路径
            "/repository/api/external/github", -- Github回调
            "/process/api/external/scm/p4/commit", -- p4回调
            "/process/api/external/pipelines/projects/.+/.+/badge", -- 勋章
            "/artifactory/api/external/url/visit", -- 短链接
            "/bkrepo/bkci%-desktop" -- 蓝盾桌面端
        }
        local is_secure = false
        for _, item in ipairs(security_paths) do
            if string.find(path, "^" .. item) ~= nil or string.find(path, "^/ms" .. item) ~= nil then
                is_secure = true
            end
        end
        if not is_secure then
            ngx.log(ngx.ERR, "it is unsafe , host : ", host, " , path : ", path)
            return false
        end

        -- 防止spring actuator被调用
        if string.find(path, "^/actuator/") ~= nil or string.find(path, "^/management/") ~= nil then
            ngx.log(ngx.ERR, "it is unsafe , host : ", host, " , path : ", path)
            return false
        end
    end
    return true
end
return _M
