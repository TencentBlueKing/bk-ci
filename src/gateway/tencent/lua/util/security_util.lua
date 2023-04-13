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
    if path == '/' then
        return true
    end

    -- external location 可以忽略
    local external_location = ngx.var.external_location
    if external_location ~= nil then
        return true
    end

    -- 外部链接安全检查
    local host = ngx.var.host
    local header_clb = ngx.var.http_x_clb_lbid
    if string.find(host, "bkdevops.qq.com") ~= nil or header_clb ~= nil then -- bkdevops.qq.com 相关域名或者传了lb的ID
        if string.find(path, "/api/app/") == nil -- app 路径
        and string.find(path, "/api/open/") == nil -- open路径
        and string.find(path, "/bkrepo/api/external/generic") == nil -- 仓库的external/generic路径
        and string.find(path, "/bkrepo/api/external/repository") == nil -- 仓库的external/repository路径
        and string.find(path, "/process/api/external/scm/codetgit/commit") == nil -- TGit回调
        and string.find(path, "/repository/api/external/github") == nil -- Github回调
        and string.find(path, "/external/api/external/github") == nil -- Github回调
        and string.find(path, "/process/api/external/scm/p4/commit") == nil -- p4回调
        and string.find(path, "/process/api/external/pipelines/projects/.+/.+/badge") == nil -- 勋章
        and string.find(path, "/stream/api/external/stream/projects/.+/.+/badge") == nil -- stream勋章
        then
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
