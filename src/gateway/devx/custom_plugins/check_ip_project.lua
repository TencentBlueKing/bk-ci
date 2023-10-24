    local core = require("apisix.core")
    local http = require("resty.http")
    local plugin_name = "check-ip-project"

    local schema = {type = "object", properties = {host = {type = "string"}}, required = {"host"}}

    local _M = {
    version = 0.1,
    name = plugin_name,
    priority = 1,
    schema = schema,
    }
    function _M.check_schema(conf)
        return core.schema.check(schema, conf)
    end

    function _M.rewrite(conf, ctx)
        local project = core.request.header(ctx, "X-DEVOPS-PROJECT-ID")
        local ip = core.request.header(ctx, "X-Real-IP")

        if project == nil then
            core.log.error("project is null")
            return
        end

        if ip == nil then
            core.log.error("ip is null")
            return
        end

        core.log.error("project is [" .. project .. "] , ip is [" .. ip .. "] , host is ["..conf.host.."]")

        local httpc = http.new()
        httpc:set_timeout(3000)
        local ok, err = httpc:connect(conf.host, 80)
        if not ok then
            return false, "failed to connect to host[" .. host .. "], " .. err
        end

        local httpc_res, httpc_err = httpc:request({
            method = "GET",
            path = "/api/service/remotedev/checkWorkspaceProject?project_id=" .. project .. "&ip=" .. ip
        })

        if not httpc_res then
            core.log.error("empty http res , ".. httpc_err)
            return 401 , "check project and ip failed, "..httpc_err
        end

        local body,body_err = core.json.decode(httpc_res:read_body())
        if not body then
            core.log.error("decode json error , "..body_err)
            return 401 , "check project and ip failed, "..body_err
        end

        if body.data == false then
            core.log.error("data is false , project : "..project.." , ip : "..ip)
            return 401 , "check project and ip failed"
        end
    end
    return _M
