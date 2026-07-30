-- Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
-- Copyright (C) 2019 Tencent.  All rights reserved.
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

-- 根据源请求域名判断是否为 creative 域名
function _M:is_creative_host()
    if config == nil or ngx.var.project == 'codecc' or type(config.kubernetes) ~= 'table' then
        return false
    end

    local creative_host_suffix = config.kubernetes.creative_host_suffix
    if type(creative_host_suffix) ~= 'string' or creative_host_suffix == '' then
        return false
    end

    local source_host = ngx.var.original_host
    if source_host == nil or source_host == '' then
        source_host = ngx.var.http_host
    end
    if source_host == nil or source_host == '' then
        source_host = ngx.var.host
    end
    if source_host == nil or source_host == '' then
        return false
    end

    source_host = string.lower(source_host)
    source_host = string.match(source_host, '^[^:]+') or source_host
    local first_label = string.match(source_host, '^[^%.]+')
    creative_host_suffix = string.lower(creative_host_suffix)

    if first_label == nil or string.sub(first_label, -string.len(creative_host_suffix)) ~= creative_host_suffix then
        return false
    end

    return true
end

-- 命中 creative 域名时返回目标 kubernetes namespace，否则返回 nil
function _M:get_namespace()
    if not self:is_creative_host() then
        return nil
    end

    local creative_namespace = config.kubernetes.creative_namespace
    if type(creative_namespace) ~= 'string' or creative_namespace == '' then
        return nil
    end

    return creative_namespace
end

return _M
