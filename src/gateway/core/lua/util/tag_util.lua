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

    local devops_project = projectUtil:get_project()
    local default_tag = ns_config.tag
    local tag = default_tag

    if devops_project ~= nil and devops_project ~= '' then
        -- 先从本地缓存获取
        local tag_cache = ngx.shared.tag_project_store
        local tag_cache_value = tag_cache:get(devops_project)
        if tag_cache_value ~= nil then
            tag = tag_cache_value
        else
            -- 再从redis获取
            local red_key = "project:setting:tag:v2"
            local red, err = redisUtil:new()
            if not red then
                -- redis 获取连接失败
                ngx.log(ngx.ERR, "tag failed to new redis ", err)
            else
                -- 从redis获取tag
                local redRes, err = red:hget(red_key, devops_project)
                if not redRes then
                    ngx.log(ngx.ERR, "tag failed to get redis result: ", err)
                    tag_cache:set(devops_project, default_tag, 30)
                else
                    if redRes == ngx.null then
                        tag_cache:set(devops_project, default_tag, 30)
                    else
                        tag_cache:set(devops_project, redRes, 30)
                        tag = redRes
                    end
                end

                -- 是否只能用默认tag
                if tag ~= ns_config.tag then
                    local pattern = red:get("project:setting:tag:pattern:v2")
                    if pattern then
                        if string.find(ngx.var.uri, pattern) then
                            tag = default_tag
                            tag_cache:set(devops_project, default_tag, 30)
                        end
                    end
                end

                --- 将redis连接放回pool中
                red:set_keepalive(config.redis.max_idle_time, config.redis.pool_size)
            end
        end
    end

    -- 设置tag到http请求头
    ngx.header["X-DEVOPS-TAG"] = tag

    return tag
end

return _M
