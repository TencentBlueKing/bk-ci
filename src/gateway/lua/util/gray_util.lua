_M = {}

function _M:get_gray()
    local devops_project = nil
    local get_from = nil

    --- op全部都走灰度
    local uri = ngx.var.request_uri
    local opFlag = string.find(uri, "/api/op/")
    if(opFlag ~= nil and config.env == "prod") then
        ngx.log(ngx.ERR, "This is a op uri:", ngx.var.request_uri)
        return true
    end


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