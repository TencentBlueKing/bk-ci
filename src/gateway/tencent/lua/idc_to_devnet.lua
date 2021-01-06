local timestamp = ngx.time()
local token = "6fb7eb9f0e213e126bde00720d5553c5b785a97b1f0400b0ac4e"
local sn = timestamp..token..timestamp
local riohost = "devcloud.esb.woa.com"
local target = "dev_devnet-backend_devops"--TODO 区分环境
local riodomain = riohost.."/devops-idc2devnet/"..target

local resty_sha256 = require "resty.sha256"
local resty_str = require "resty.string"
local sha256 = resty_sha256:new()
sha256:update(sn)
local digest = sha256:final()
local signature = resty_str.str_to_hex(digest)

ngx.var.riodomain = riodomain
ngx.var.signature = signature
ngx.var.timestamp = tostring(timestamp)