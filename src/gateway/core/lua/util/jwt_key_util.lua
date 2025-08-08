-- 将jwtPrivateKey转换为PEM格式
local single_line_b64_private_key = config.jwtPrivateKey
if single_line_b64_private_key == '' or single_line_b64_private_key == nil then
    return
end
-- 组装 PEM 格式的私钥
local private_key_pem_parts = {}
table.insert(private_key_pem_parts, "-----BEGIN RSA PRIVATE KEY-----")
-- 每 64 个字符添加一个换行符
local len = #single_line_b64_private_key
local chunk_size = 64
for i = 1, len, chunk_size do
table.insert(private_key_pem_parts, string.sub(single_line_b64_private_key, i, i + chunk_size - 1))
end
table.insert(private_key_pem_parts, "-----END RSA PRIVATE KEY-----")
config.jwtPrivateKey = table.concat(private_key_pem_parts, "\n")
