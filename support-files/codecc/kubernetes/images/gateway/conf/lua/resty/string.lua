-- Copyright (C) by Yichun Zhang (agentzh)


local ffi = require "ffi"
local ffi_new = ffi.new
local ffi_str = ffi.string
local C = ffi.C
--local setmetatable = setmetatable
--local error = error
local tonumber = tonumber


local _M = { _VERSION = '0.10' }


ffi.cdef[[
typedef unsigned char u_char;

u_char * ngx_hex_dump(u_char *dst, const u_char *src, size_t len);

intptr_t ngx_atoi(const unsigned char *line, size_t n);
]]

local str_type = ffi.typeof("uint8_t[?]")


function _M.str_to_hex(s)
    local len = #s * 2
    local buf = ffi_new(str_type, len)
    C.ngx_hex_dump(buf, s, #s)
    return ffi_str(buf, len)
end

function _M.hex_to_str(hex)
    local str, n = hex:gsub("(%x%x)[ ]?", function (word)
        return string.char(tonumber(word, 16))
    end)
    return str
end


function _M.atoi(s)
    return tonumber(C.ngx_atoi(s, #s))
end


return _M