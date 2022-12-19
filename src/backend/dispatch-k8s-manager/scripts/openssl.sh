# 使用openssl生成pkcs8编码密匙用来给token加密

# 生成私钥
openssl genrsa -out rsa_private_key.pem 1024
# 生成公钥
openssl rsa -in rsa_private_key.pem -pubout -out rsa_public_key.pem
# 私钥pkcs8编码
openssl pkcs8 -topk8 -in rsa_private_key.pem -out  pkcs8_rsa_private_key.pem -nocrypt
