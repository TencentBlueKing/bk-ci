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
-- 浏览器访问公开 static 仓库时，平台凭证会被 bkrepo 拒绝并带上 WWW-Authenticate，Chrome 会弹出登录框。
-- 只对 user 层 GET 计算跳转地址；计算失败则返回 nil，调用方继续使用已经设好的 target 做 proxy_pass。
local function public_static_logo_location()
    if ngx.var.access_type ~= "user" then
        return nil
    end
    if ngx.req.get_method() ~= "GET" then
        return nil
    end

    local function static_rest(path)
        local rest = string.match(path, "^/bkrepo/api/user/generic/[^/]*bk%-store/static/(.+)$")
        if rest == nil then
            rest = string.match(path, "^/ms/bkrepo/api/user/generic/[^/]*bk%-store/static/(.+)$")
        end
        return rest
    end

    local path = ngx.unescape_uri(ngx.var.uri or "")
    path = string.gsub(path, "/+", "/")
    local rest = static_rest(path)
    if rest == nil then
        local raw = ngx.var.request_uri or ""
        local query_at = string.find(raw, "?", 1, true)
        if query_at then
            raw = string.sub(raw, 1, query_at - 1)
        end
        raw = ngx.unescape_uri(raw)
        raw = string.gsub(raw, "/+", "/")
        rest = static_rest(raw)
    end
    if rest == nil or rest == "" or string.sub(rest, 1, 1) == "/" then
        return nil
    end
    if string.find(rest, "..", 1, true)
        or string.find(rest, "\\", 1, true)
        or string.find(rest, "://", 1, true)
        or string.find(rest, "\r", 1, true)
        or string.find(rest, "\n", 1, true) then
        return nil
    end

    local prefix = ngx.var.bk_ci_public_path or ""
    if string.find(prefix, "__BK_", 1, true) then
        prefix = ""
    end
    prefix = string.gsub(prefix, "/+$", "")
    return prefix .. "/bk-plugin-fe/" .. rest
end

-- 获取服务名称
local service_name = ngx.var.service
if config.service_name ~= nil and config.service_name ~= "" then
    service_name = config.service_name
end

if not service_name or service_name == "" then
    ngx.log(ngx.STDERR, "failed with service name :", tostring(service_name))
    ngx.exit(503)
    return
end

-- 当服务器为job的时候指向job的域名
if service_name == "job" then
    ngx.var.target = config.job.domain
    return
end

if service_name == "bkrepo" then
    ngx.var.target = config.bkrepo.domain
    local ok, location = pcall(public_static_logo_location)
    if ok and location ~= nil and location ~= "" then
        return ngx.redirect(location, ngx.HTTP_MOVED_TEMPORARILY)
    end
    return
end

-- 服务重写
if service_name == "dispatch-docker" or service_name == "dispatch-kubernetes" then
    service_name = "dispatch"
elseif service_name == "image" or service_name == "monitoring" or service_name == "plugin" then
    service_name = "misc"
end

-- 获取灰度设置
local cache_tail = ""
local ns_config = config.ns

if ngx.var.devops_region ~= "DEVNET" then
    cache_tail = ".normal.idc"
else
    cache_tail = ".normal.devnet"
end

if not ns_config.ip then
    ngx.log(ngx.ERR, "DNS ip not exist!")
    ngx.exit(503)
    return
end

-- 获取tag设置
local devops_tag = tagUtil:get_tag(ns_config)
if devops_tag == nil then
    devops_tag = ns_config.tag
end

-- 设置 rid
if ngx.var.http_x_devops_rid == nil then
    ngx.header["X-DEVOPS-RID"] = ngx.var.uuid
end

-- 负载均衡
local target = loadBalanceUtil:getTarget(devops_tag, service_name, cache_tail, ns_config)
if target == nil then
    if target == nil then
        ngx.exit(503)
    end
end

ngx.var.target = target

-- 特殊逻辑
if ngx.var.url_prefix ~= nil then
    if config.artifactory.realm == "local" then
        ngx.var.url_prefix = "http://" .. ngx.var.target .. "/resource/bk-plugin-fe/"
    else
        local bkStoreId = "bk-store"
        -- 与初始化、后端共用 BK_CI_BKREPO_PROJECT_PREFIX。空前缀保持 bk-store，有前缀则为 {前缀}.bk-store
        local prefix = config.bkrepo.projectPrefix
        if prefix ~= nil and prefix ~= "" and not string.find(prefix, "__BK_", 1, true) then
            prefix = string.gsub(prefix, "%.$", "")
            if prefix ~= "" then
                bkStoreId = prefix .. "." .. bkStoreId
            end
        end
        ngx.var.url_prefix = "http://" .. config.bkrepo.domain .. "/generic/" .. bkStoreId .. "/static/"
    end
end
