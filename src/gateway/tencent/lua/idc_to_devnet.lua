local timestamp = ngx.time()
local token = "6fb7eb9f0e213e126bde00720d5553c5b785a97b1f0400b0ac4e"
local sn = timestamp..token..timestamp
local host = "devcloud.esb.woa.com" --TODO 区分环境
local domain = "http://"..host.."/devops-idc2devnet/dev_devnet-backend_devops/"

local resty_sha256 = require "resty.sha256"
local resty_str = require "util.string_util"
local sha256 = resty_sha256:new()
sha256:update(sn)
local digest = sha256:final()
local signature = resty_str.to_hex(digest)

ngx.log(ngx.ERR , "t:"..timestamp.." , sn:"..sn.." , signature:"..signature)

ngx.var.timestamp = tostring(timestamp)
ngx.var.signature = signature
ngx.var.rio_domain = domain