-- copy from https://git.woa.com/tapisix/tapisix/blob/79510f3312bea495a5a57ae4001730c32e0bef01/apisix/plugins/zhiyan/log/client.lua
local os                   = os
local ngx                  = ngx
local unpack               = unpack
local core                 = require("apisix.core")
local polaris_client       = require("apisix.utils.polaris_client")
local http                 = require("resty.http")

local _M = {
    config = {},
}

local polaris_api = polaris_client.new()
assert(polaris_api ~= nil)

local node = nil
local mdata = nil
local http_report_url = ""
local node_instance_req

local connect

-- tcp连接
local function tcp_connect()
    local sock = ngx.socket.tcp()
    local ok, err = sock:connect(node.host, node.port)
    if not ok then
        return nil, "[zhiyan] client init tcp connect failed: " .. err
    end

    sock:settimeout(1000 * _M.config.max_report_time * 2)
    return sock, nil
end

-- udp连接
local function udp_connect()
    local sock = ngx.socket.udp()
    local ok, err = sock:setpeername(node.host, node.port)
    if not ok then
        return nil, "[zhiyan] client init udp connect failed: " .. err
    end

    sock:settimeout(1000 * _M.config.max_report_time * 2)
    return sock, nil
end

-- tcp udp 上报
local function socket_report(msgpack)
    if msgpack.msglen == 0 then
        return
    end

    local sock, err = connect()
    if err ~= nil then
        core.log.error(err)
        return
    end
    local ok, err = sock:send(msgpack.marshal())
    if not ok then
        core.log.error("[zhiyan] socket report failed: " .. err)
        return
    end

    -- 放回连接池
    if _M.config.proto == "TCP" then
        local ok, err = sock:setkeepalive(100, 5)
        if not ok then
            core.log.error("tcp set keepalive failed: " .. err)
        end
    end
end

-- http请求
local function http_request(body)
    local http_cli, err = http.new()
    if err then
        return -1, "http client init failed: " .. err
    end

    local headers = { ["Content-Type"] = "application/json; charset=utf-8" }

    http_cli:set_timeout(3 * 1000)
    local res, err = http_cli:request_uri(http_report_url, {
        method            = "POST",
        body              = body,
        ssl_verify        = false,
        keepalive         = true,
        headers           = headers,
        keepalive_timeout = 60000,
        keepalive_pool    = 100,
    })
    if err then
        return -1, "http request failed: " .. err
    end
    return res.status, res.body
end

-- http上报
local function http_report(msgpack)
    if msgpack.msgnum == 0 then
        return
    end

    local data = {
        topic = _M.config.topic,
        host  = _M.config.host,
        data  = msgpack.msg
    }
    -- 必须先刷库再上报
    msgpack.clean()

    -- 批量上报
    local code, body = http_request(core.json.encode(data))
    if code ~= 200 then
        core.log.error(body)
    end
end

local PROTO_MDATA = {
    TCP = {
        upstream = {
            idc    = { "Production", "zhiyan_log_collect_proxy_common_tcp" },
            dev    = { "Production", "zhiyan_log_collect_proxy_devnet_tcp" },
            public = { "Production", "zhiyan_log_collect_proxy_public_tcp" }
        },
        connect = tcp_connect,
        report  = socket_report
    },
    UDP = {
        upstream = {
            idc    = { "Production", "zhiyan_log_collect_proxy_common_udp" },
        },
        connect = udp_connect,
        report  = socket_report
    },
    HTTP = {
        upstream = {
            idc = "http://log-report-sz.zhiyan.tencent-cloud.net:13001/collect"
        },
        report = http_report
    }
}

-- socket connect
connect = function()

    if node == nil then
        node = polaris_client.get_one_instance(polaris_api, node_instance_req, true)
    end

    return mdata.connect(node)
end

function _M.init(config)
    _M.config   = config

    if not _M.config.host then
        _M.config.host = os.getenv("POD_IP")
    end

    if _M.config.host == nil then
        _M.config.host = "127.0.0.1"
    end

    mdata = PROTO_MDATA[_M.config.proto]
    if mdata == nil then
        return "proto must in TCP/UDP/HTTP"
    end

    if config.env == nil then
        config.env = "idc"
    end

    local report_upstream = mdata.upstream[config.env]
    if report_upstream == nil then
        return config.proto .. " does not support " .. config.env .. " environment"
    end

    _M.report = mdata.report
    if config.proto == "HTTP" then
        http_report_url = report_upstream
    else
        node_instance_req = polaris_client.get_one_instance_req(unpack(report_upstream))
    end

end

return _M