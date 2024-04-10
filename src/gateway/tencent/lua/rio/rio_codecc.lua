local timestamp = ngx.time()
local token = "024afc6a7a3261c051f8ed8595af069ca64e8857a012fd561b42"
local sn = timestamp .. token .. timestamp

local target = "v2_codecc"
if config.env == 'dev' then
    target = 'v2_dev_codecc'
elseif config.env == 'test' then
    target = 'v2_test_codecc'
end

local riodomain = "apidev-idc.sgw.woa.com/CodeCC/" .. target

local resty_sha256 = require "resty.sha256"
local resty_str = require "resty.string"
local sha256 = resty_sha256:new()
sha256:update(sn)
local digest = sha256:final()
local signature = resty_str.str_to_hex(digest)

ngx.var.riodomain = riodomain
ngx.var.signature = signature
ngx.var.timestamp = tostring(timestamp)
