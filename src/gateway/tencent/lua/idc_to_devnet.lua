local hmac = require "resty.hmac"
local str_util = require "resty.string"

local timestamp = ngx.time()
local token = "6fb7eb9f0e213e126bde00720d5553c5b785a97b1f0400b0ac4e"
local sn = timestamp..token..timestamp
local host = "devcloud.esb.woa.com" --TODO 区分环境
local domain = "http://"..host.."/devops-idc2devnet/dev_devnet-backend_devops/"
local signature = str_util.to_hex(hmac:new(sn, hmac.ALGOS.SHA256):final())

ngx.log(ngx.ERR , "t:"..timestamp.." , signature:"..signature)

ngx.var.timestamp = timestamp
ngx.var.signature = signature
ngx.var.rio_domain = domain