local auth_header = ngx.req.get_headers()["Authorization"]
if auth_header == "Bearer "..config.auth then
    ngx.exit(ngx.HTTP_OK)
else
    ngx.exit(ngx.HTTP_UNAUTHORIZED)
end
