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
_M = {}
-- 判断当前请求属于哪个tag
function _M:get_tag(ns_config)
    if ngx.var.use_default_tag == 'true' then
        return ns_config.tag
    end

    local devops_project_id = ngx.var.project_id
    local devops_project = ngx.var.project
    local devops_service = ngx.var.service
    local default_tag = ns_config.tag
    local tag = nil

    -- 根据header强制路由tag
    if ngx.var.http_x_gateway_tag ~= nil then
        tag = ngx.var.http_x_gateway_tag
        if not string.find(tag, '^kubernetes-') and self:switch_kubernetes(devops_project, tag) then
            tag = "kubernetes-" .. tag
        end
    else
        -- 获取本地缓存
        local tag_cache = ngx.shared.tag_project_store
        local tag_cache_key = 'tag_cache_' .. tostring(devops_project_id) .. '_' .. tostring(devops_service) .. '_' ..
                                  tostring(devops_project)
        local tag_cache_value = tag_cache:get(tag_cache_key)

        -- 如果有缓存 ,则使用缓存变量
        if tag_cache_value ~= nil and tag_cache_value ~= '' then
            tag = tag_cache_value
        else -- 否则从redis中拿到策略
            local red, err = redisUtil:new()
            if not red then
                ngx.log(ngx.ERR, "tag failed to new redis ", err)
                return tag
            end
            -- 根据project_id路由
            if devops_project_id ~= nil and devops_project_id ~= '' then
                local redis_key = nil
                if devops_project == 'codecc' then
                    redis_key = 'project:setting:tag:codecc:v2'
                else
                    redis_key = "project:setting:tag:v2"
                end
                -- 从redis获取tag
                local hash_key = '\xAC\xED\x00\x05t\x00' .. string.char(devops_project_id:len()) .. devops_project_id -- 兼容Spring Redis的hashKey的默认序列化
                local redRes = red:hget(redis_key, hash_key)
                if redRes and redRes ~= ngx.null then
                    local hash_val = redRes:sub(8) -- 兼容Spring Redis的hashValue的默认序列化
                    tag_cache:set(devops_project_id, hash_val, 5)
                    tag = hash_val
                end
            end
            -- 根据service路由
            if tag == nil and devops_service ~= '' then
                local service_redis_cache_value = red:get("project:setting:service:tag:" .. devops_service)
                if service_redis_cache_value and service_redis_cache_value ~= ngx.null then
                    tag = service_redis_cache_value
                end
            end
            -- 根据ngx.var.project路由
            if tag == nil and devops_project then
                local project_redis_cache_value = red:get("project:setting:project:tag:" .. devops_project)
                if project_redis_cache_value and project_redis_cache_value ~= ngx.null then
                    tag = project_redis_cache_value
                end
            end
            -- 使用默认值
            if tag == nil then
                tag = default_tag
            end
            -- 是否使用kubernetes
            if not string.find(tag, '^kubernetes-') then
                if self:switch_kubernetes(devops_project, tag) then
                    tag = "kubernetes-" .. tag
                else
                    local k8s_redis_key = nil
                    if devops_project == 'codecc' then
                        k8s_redis_key = 'project:setting:k8s:codecc'
                    else
                        k8s_redis_key = "project:setting:k8s"
                    end
                    local k8s_redRes = red:sismember(k8s_redis_key, devops_project_id)
                    if k8s_redRes == 1 then
                        tag = "kubernetes-" .. tag
                    end
                end
            end
            --- 将redis连接放回pool中
            red:set_keepalive(config.redis.max_idle_time, config.redis.pool_size)

            -- 将redis拿到的tag保存在缓存
            tag_cache:set(tag_cache_key, tag, 5)
        end
    end

    -- 客户端选择路由到容器环境
    if type(tag) == "string" and not string.find(tag, '^kubernetes-') and config.kubernetes.useForceHeader then
        if devops_project == 'codecc' then
            if ngx.var.http_x_gateway_codecc_force_k8s == 'true' then
                tag = "kubernetes-" .. tag
            end
        else
            if ngx.var.http_x_gateway_force_k8s == 'true' then
                tag = "kubernetes-" .. tag
            end
        end
    end

    -- 设置tag到http请求头
    self:set_header(tag)

    return tag
end

function _M:switch_kubernetes(devops_project, tag)
    if config.kubernetes.switchAll == true then
        return true
    end
    local isInList = false
    local tags = nil
    if devops_project == 'codecc' then
        tags = config.kubernetes.codeccTags
    else
        tags = config.kubernetes.tags
    end
    for _, v in ipairs(tags) do
        if v == tag then
            isInList = true
            break
        end
    end
    return isInList
end

-- 设置tag到http请求头
function _M:set_header(tag)
    if ngx.var.http_x_gateway_tag == nil then
        ngx.header["X-GATEWAY-TAG"] = tag
    end
    ngx.var.route_tag = tag
end

-- 获取前端目录
function _M:get_frontend_path(tag, project)
    if string.find(tag, '^kubernetes-') then
        tag = string.sub(tag, 12) -- 去掉 "kubernetes-" 头部
    end
    local frontend_path_cache = ngx.shared.tag_frontend_path_store
    local local_cache_key = "ci_" .. tag
    if project == "codecc" then
        local_cache_key = "codecc_" .. tag
    end
    local frontend_path = frontend_path_cache:get(local_cache_key)
    if frontend_path == nil then
        -- 从redis获取
        local red, err = redisUtil:new()
        if not red then
            ngx.log(ngx.ERR, "tag failed to new redis ", err)
            return config.static_dir
        end
        local red_key = "ci:frontend:path:" .. tag
        if project == "codecc" then
            red_key = "codecc:frontend:path:" .. tag
        end
        frontend_path = red:get(red_key)
        if not frontend_path or frontend_path == ngx.null then
            frontend_path = ""
        end
        frontend_path_cache:set(local_cache_key, frontend_path, 30)
        red:set_keepalive(config.redis.max_idle_time, config.redis.pool_size)
    end

    local suffix = config.static_dir
    if project == "codecc" then
        suffix = config.static_dir_codecc
    end

    if frontend_path == nil or frontend_path == "" then
        return suffix
    end

    return suffix .. "-" .. frontend_path
end

-- 获取tag对应的下载路径
function _M:get_sub_path(tag)
    if string.find(tag, '^kubernetes-') then
        tag = string.sub(tag, 12) -- 去掉 "kubernetes-" 头部
    end
    -- 从缓存获取
    local sub_path_cache = ngx.shared.tag_sub_path_store
    local sub_path = sub_path_cache:get(tag)
    if sub_path == nil then
        -- 从redis获取
        local red, err = redisUtil:new()
        if not red then
            ngx.log(ngx.ERR, "tag failed to new redis ", err)
            return tag
        end
        sub_path = red:get("gw:sub:path:" .. tag)
        if not sub_path or sub_path == ngx.null then
            sub_path = "prod"
        end
        sub_path_cache:set(tag, sub_path, 30)
        red:set_keepalive(config.redis.max_idle_time, config.redis.pool_size)
        return sub_path
    else
        return sub_path
    end
end

return _M
