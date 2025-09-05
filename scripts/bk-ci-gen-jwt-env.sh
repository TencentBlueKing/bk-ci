#!/bin/bash
desc="generate jwt rsa key, and convert to special form."

# 原始的RSA私钥, 需要为PKCS#8封装.
rsa_private_key=$(openssl genrsa 2048 2>/dev/null | openssl pkcs8 -nocrypt -topk8 2>/dev/null)
# 基于私钥生成公钥
rsa_public_key=$(openssl rsa -pubout 2>/dev/null <<< "$rsa_private_key")

# 提取私钥内容
rsa_private_key_content=${rsa_private_key#*$'\n'}
rsa_private_key_content=${rsa_private_key_content%$'\n'*}
# 同理处理公钥.
rsa_public_key_content=${rsa_public_key#*$'\n'}
rsa_public_key_content=${rsa_public_key_content%$'\n'*}

# 最终渲染成一行
rsa_public_key_content_oneline=${rsa_public_key_content//$'\n'/''}
rsa_private_key_content_oneline=${rsa_private_key_content//$'\n'/''}
# 输出变量赋值语句
echo BK_CI_JWT_RSA_PRIVATE_KEY=\"$rsa_private_key_content_oneline\"
echo BK_CI_JWT_RSA_PUBLIC_KEY=\"$rsa_public_key_content_oneline\"
