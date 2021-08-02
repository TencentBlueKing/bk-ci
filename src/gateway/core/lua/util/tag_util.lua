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

    local devops_project = ngx.var.project_id
    local devops_service = ngx.var.service
    local default_tag = ns_config.tag
    local tag = default_tag
    local header_tag = ngx.var.http_x_gateway_tag

    -- 根据header强制路由tag
    if ngx.var.http_x_gateway_tag ~= nil then
        return ngx.var.http_x_gateway_tag
    end

    -- 获取redis连接
    local red, err = redisUtil:new()
    if not red then
        ngx.log(ngx.ERR, "tag failed to new redis ", err)
        return tag
    end
    -- 获取本地缓存
    local tag_cache = ngx.shared.tag_project_store

    -- 根据service路由
    local useServiceTag = false
    if devops_service ~= nil and devops_service ~= '' then
        local service_local_cache_key = 'tag_local_cache_key_' .. devops_service
        local service_local_cache_value = tag_cache:get(service_local_cache_key)
        if service_local_cache_value ~= '-1' then
            if service_local_cache_value ~= nil then
                tag = service_local_cache_value
                useServiceTag = true
            else
                local service_redis_cache_value = red:get("project:setting:service:tag:" .. devops_service)
                if service_redis_cache_value and service_redis_cache_value ~= ngx.null then
                    tag_cache:set(service_local_cache_key, service_redis_cache_value, 60)
                    tag = service_redis_cache_value
                    useServiceTag = true
                else
                    tag_cache:set(service_local_cache_key, '-1', 60)
                end
            end
        end
    end

    -- 根据project_id路由
    if useServiceTag == false and devops_project ~= nil and devops_project ~= '' then
        local tag_cache_value = tag_cache:get(devops_project)
        if tag_cache_value ~= nil then
            tag = tag_cache_value
        else
            local redis_key = nil
            if ngx.var.project == 'codecc' then
                redis_key = 'project:setting:tag:codecc:v2'
            else
                redis_key = "project:setting:tag:v2"
            end
            -- 从redis获取tag
            local hash_key = '\xAC\xED\x00\x05t\x00' .. string.char(devops_project:len()) .. devops_project -- 兼容Spring Redis的hashKey的默认序列化
            local redRes, err = red:hget(redis_key, hash_key)
            if not redRes then
                ngx.log(ngx.ERR, "tag failed to get redis result: ", err)
                tag_cache:set(devops_project, default_tag, 30)
            else
                if redRes == ngx.null then
                    tag_cache:set(devops_project, default_tag, 30)
                else
                    local hash_val = redRes:sub(8) -- 兼容Spring Redis的hashValue的默认序列化
                    tag_cache:set(devops_project, hash_val, 30)
                    tag = hash_val
                end
            end
        end
    end

    --- 将redis连接放回pool中
    red:set_keepalive(config.redis.max_idle_time, config.redis.pool_size)

    -- 设置tag到http请求头
    ngx.header["X-GATEWAY-TAG"] = tag

    return tag
end

-- 获取tag对应的路径
function _M:get_sub_path(tag)
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
