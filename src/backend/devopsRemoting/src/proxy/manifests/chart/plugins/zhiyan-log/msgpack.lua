-- copy from https://git.woa.com/tapisix/tapisix/raw/79510f3312bea495a5a57ae4001730c32e0bef01/apisix/plugins/zhiyan/log/msgpack.lua
local bit       = bit
local ngx       = ngx
local table     = table
local string    = string
local int64     = require("int64")

-- 消息版本
local PKG_VER     = 0x02
-- 魔数
local MAGIC_HEAD  = 0x09010203
-- 包最小长度
local PKG_MIN_LEN = 4 + 1 + 1 + 1
-- 压缩选项
local COMPRESS    = {
    -- gzip
    [true]  = 1,
    -- 不压缩
    [false] = 0
}

local _M = {
    -- 消息体
    msg = {},
    -- 消息长度
    msglen = 0,
    -- 消息数量
    msgnum = 0,
    -- 是否压缩
    compress = false,
    -- config
    config = {}
}

local function bigEndianPutInt32(num)
    return bit.rshift(num, 24) % 256, bit.rshift(num, 16) % 256, bit.rshift(num, 8) % 256, num % 256
end

function _M.clean()
    _M.msg    = {}
    _M.msglen = 0
    _M.msgnum = 0
end

-- 编码
function _M.marshal()

    local payload = {
        -- 魔数头
        string.char(bigEndianPutInt32(MAGIC_HEAD)),
        -- 总长度
        string.char(bigEndianPutInt32(PKG_MIN_LEN + _M.msglen)),
        -- 包版本
        string.char(PKG_VER),
        -- host 长度
        string.char(#_M.config.host),
        -- host
        _M.config.host,
        -- topic 长度
        string.char(#_M.config.topic),
        -- topic
        _M.config.topic,
        -- 压缩选项
        string.char(COMPRESS[_M.compress]),
        -- 包
        table.concat(_M.msg)
    }
    _M.clean()
    return table.concat(payload)
end

-- 添加msg
local function add_socket_msg(msg)
    ngx.update_time()
    local msglen = #msg
    table.insert(
        _M.msg,
        table.concat({
            int64.tostringBigEndian(ngx.now() * 1000),
            string.char(bigEndianPutInt32(msglen)),
            msg
        })
    )
    _M.msgnum = _M.msgnum + 1
    _M.msglen = _M.msglen + msglen
    return _M.msgnum
end

-- 添加http消息
local function add_http_msg(msg)
    ngx.update_time()
    table.insert(
        _M.msg,
        {
            message = msg,
            timestamp = ngx.now() * 1000
        }
    )
    _M.msgnum = _M.msgnum + 1
    return _M.msgnum
end

function _M.add(msg)
    local _add = {
        ["TCP"]  = add_socket_msg,
        ["UDP"]  = add_socket_msg,
        ["HTTP"] = add_http_msg,
    }
    _M.add = _add[_M.config.proto]
    return _M.add(msg)
end

return _M