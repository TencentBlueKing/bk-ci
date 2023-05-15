-- copy from https://git.woa.com/tapisix/tapisix/blob/f65e9e11a376de6480ca36c3698caf81e7123899/apisix/plugins/zhiyan-log.lua
local ngx = ngx
local pairs = pairs
local string = string
local str_byte = string.byte
local ngx_time = ngx.time
local ngx_timer_at = ngx.timer.at
local process = require("ngx.process")
local core = require("apisix.core")
local log_util = require("apisix.utils.log-util")
local local_conf = require("apisix.core.config_local").local_conf()
local msgpack = require("apisix.plugins.zhiyan.log.msgpack")
local client = require("apisix.plugins.zhiyan.log.client")
local timers = require("apisix.timers")

local plugin_name = "zhiyan-log"
local schema = {
    type = "object",
    properties = {
        limit_req_size = { type = "integer", default = 1048576 },
        limit_resp_size = { type = "integer", default = 1048576 },
        include_resp_body = { type = "boolean", default = false },
        include_req_body = { type = "boolean", default = false },
        labels = { type = "object" },
    },
    additionalProperties = false,
}

local _M = {
    version = 0.1,
    priority = 504,
    name = plugin_name,
    schema = schema,
}

local log_format
local config = local_conf.zhiyan_log


local function gen_log_format(format)
    local log_format = {}
    if format == nil then
        return log_format
    end

    for k, var_name in pairs(format) do
        if var_name:byte(1, 1) == str_byte("$") then
            log_format[k] = { true, var_name:sub(2) }
        else
            log_format[k] = { false, var_name }
        end
    end
    core.log.info("log_format: ", core.json.delay_encode(log_format))
    return log_format
end


-- 定时上报
local function report(premature)
    client.report(msgpack)
end


function _M.body_filter(conf, ctx)
    if conf.include_resp_body then
        local resp_body = core.response.hold_body_chunk(ctx, true, conf.limit_resp_size)

        if resp_body and resp_body ~= "" then
            ctx.resp_body = resp_body
        end
    end
end


-- 日志上报
function _M.log(conf, ctx)

    local entry = {}
    if log_format then
        for k, var_attr in pairs(log_format) do
            if var_attr[1] then
                entry[k] = ctx.var[var_attr[2]]
            else
                entry[k] = var_attr[2]
            end
        end
    else
        entry = log_util.get_full_log(ngx, conf)
    end

    if conf.labels then
        entry.labels = conf.labels
    end

    if ctx.matched_route and ctx.matched_route.value.labels then
        entry.route = {}
        entry.route.labels = ctx.matched_route.value.labels
    end

    local msg_num = msgpack.add(core.json.encode(entry))
    -- 数量超过最大限制强制上报
    if msg_num >= config.max_batch_number then
        ngx_timer_at(0, report)
    end
end


function _M.init()
    if config == nil then
        core.log.error("there is no zhiyan configuration, please check config.yaml")
        return
    end

    if process.type() ~= "worker" then
        return
    end

    if config.log_format then
        log_format = gen_log_format(core.json.decode(config.log_format))
    end

    config.compress = false

    config.proto = string.upper(config.proto)
    msgpack.config = config
    local err = client.init(config)
    if err ~= nil then
        core.log.error(err)
        return
    end

    -- 注册上报timer
    local report_start_at = ngx_time()
    local report_interval = config.maximum_write_delay or 1
    local report_fn = function()
        local now = ngx_time()
        if now - report_start_at >= report_interval then
            report_start_at = now
            report(nil)
        end
    end

    timers.register_timer("plugin#zhiyan-log", report_fn)
end


function _M.destroy()
    if process.type() ~= "worker" then
        return
    end

    timers.unregister_timer("plugin#zhiyan-log")
end


return _M