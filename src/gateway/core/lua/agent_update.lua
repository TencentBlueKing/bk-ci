local md5 = ngx.var.arg_eTag
local file_name = ngx.var.arg_file
local bkrepo_url = "http://"..config.bkrepo.domain.."/generic/bkdevops/static/gw/dispatch/"..ngx.var.grayDir.."/"..file_name
local httpc = http.new()
httpc:set_timeout(3000)
local res, err = httpc:request_uri(bkrepo_url , {method = "HEAD"})
httpc:set_keepalive(60000, 5)

-- 判断仓库是否存在
if res == nil or res.status ~= 200 then
    ngx.log(ngx.ERR , "HEAD "..bkrepo_url.." failed , err: "..tostring(err))

    -- 使用本地文件
    local local_file_name = config.internal_file_dir.."/dispatch/"..ngx.var.grayDir.."/"..file_name
    local local_file = io.open(local_file_name)
    if local_file then
        local_file:close()
    else
        ngx.log(ngx.ERR, "Local file "..local_file_name.." not exist")
        ngx.exit(304)
        return
    end

    local handle = io.popen("md5sum "..local_file_name.." | awk '{print $1}'")
    local local_md5 = handle:read("*line")
    handle:close()
    if local_md5 == md5 then
        ngx.exit(304)
        return
    end
    ngx.header["X-Checksum-Md5"]=local_md5
    ngx.exec("/internal/file/dispatch/"..ngx.var.grayDir.."/"..file_name)
else
   -- 判断md5是否相等
    if md5 and res.headers["X-Checksum-Md5"] == md5 then
        ngx.exit(304)
        return
    end
end