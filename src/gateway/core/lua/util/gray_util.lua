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

_M = {}

function _M:get_gray()
    local devops_project = nil
    local get_from = nil

    --- 获取header中的devops_projectid
    if(devops_project == nil or devops_project == "") then
        devops_project = ngx.var.http_x_devops_project_id
        get_from = "get devops_project from header: "
    end

    --- 获取query中的devops_projectid
    if(devops_project == nil or devops_project == "") then
        devops_project = ngx.var["arg_x-devops-project-id"]
        get_from = "get devops_project from query: "
    end

    --- 获取redis中的devops_projectid
    if(devops_project == nil or devops_project == "") then
        devops_project = ngx.var.projectId
        get_from = "get devops_project from redis: "
    end

    --- 获取cookies中的devops_projectid
    if(devops_project == nil or devops_project == "") then
        devops_project = cookieUtil:get_cookie("X-DEVOPS-PROJECT-ID")
        get_from = "get devops_project from cookies: "
    end

    -- ngx.log(ngx.ERR, "devops_project: ", devops_project)
    local gray_flag = false
    if (devops_project ~= nil and devops_project ~= "") then
        -- ngx.log(ngx.ERR, get_from, devops_project)
        --- 先判断是否在
        local project_cache = ngx.shared.gray_project_store
        local project_cache_value = project_cache:get(devops_project)
        if project_cache_value == nil then
            -- ngx.log(ngx.ERR, "gray redis ")
            --- 查询redis的灰度情况
            local red = redisUtil:new()
            if not red then
                ngx.log(ngx.ERR, "gray failed to new redis ", err)
            else
                --- 获取对应的buildId
                -- local redRes, err = red:getset("project:setting:gray")
                -- local redRes, err = red:getset("project:setting:gray:" .. devops_project)

                -- ngx.log(ngx.ERR, "project:setting:gray:" .. devops_project)

                -- local redRes, err = red:smembers("project_setting_gray_" .. devops_project)
                -- local redRes, err = red:getset("project_setting_gray_" .. devops_project)
                local redRes, err = red:sismember("project:setting:gray",devops_project)
                -- ngx.log(ngx.ERR, "project:setting:gray")
                --- 将redis连接放回pool中
                local ok, err = red:set_keepalive(config.redis.max_idle_time, config.redis.pool_size)
                if not ok then
                    ngx.say("failed to set keepalive: ", err)
                    project_cache:set(devops_project,false,30)
                end

                if not redRes then
                        ngx.log(ngx.ERR, "gray failed to get redis result: ", err)
                        project_cache:set(devops_project,false,30)
                else
                    if redRes == ngx.null then
                        -- ngx.log(ngx.ERR, "gray redis result is null")
                        project_cache:set(devops_project,false,30)
                    else
                        -- ngx.log(ngx.ERR, "gray redRes ", redRes)
                        if redRes == 1 then
                            project_cache:set(devops_project,true,30)
                            gray_flag = true
                        end
                    end
                end
            end
        else
            if project_cache_value == true then
                gray_flag = true
            end
        end 
    end
    return gray_flag

end

return _M