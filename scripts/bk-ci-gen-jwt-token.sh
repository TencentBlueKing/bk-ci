#!/bin/bash

# 用法: ./bk-ci-gen-jwt-token.sh ${JWT_PRIVATE_KEY} ${module}

# 私钥文件路径
PRIVATE_KEY_FILE="private.key"

# 您提供的 Base64 编码的私钥字符串
BASE64_PRIVATE_KEY="$1"

# 清除Base64字符串中的换行符（如果存在）
CLEAN_BASE64_KEY=$(echo "$BASE64_PRIVATE_KEY" | tr -d '\n')

# 使用 echo 和 fold 命令来组装 PEM 格式
{
  echo "-----BEGIN RSA PRIVATE KEY-----"
  echo "$CLEAN_BASE64_KEY" | fold -w 64
  echo "-----END RSA PRIVATE KEY-----"
} > "$PRIVATE_KEY_FILE"

# 设置文件权限（非常重要）
chmod 600 "$PRIVATE_KEY_FILE"

HEADER='{"alg":"RS512","typ":"JWT"}'

# JWT Payload
# 当前时间戳 (Unix timestamp)
IAT=$(date +%s)
# 过期时间 (当前时间 + 10分钟)
EXP=$((IAT + 600)) # 600秒 = 10分钟
PAYLOAD='{"sub":"'$2'","exp":'$EXP'}'
# echo "PAYLOAD: $PAYLOAD"
# --- 函数：Base64url 编码 ---
base64url_encode() {
  # Base64 编码，然后替换 + 为 -，/ 为 _，并移除填充字符 =
  echo -n "$1" | openssl base64 -e -A | tr '+/' '-_' | tr -d '='
}

# --- 1. Base64url 编码 Header 和 Payload ---
ENCODED_HEADER=$(base64url_encode "$HEADER")
ENCODED_PAYLOAD=$(base64url_encode "$PAYLOAD")

# --- 2. 构造待签名字符串 ---
SIGNATURE_INPUT="$ENCODED_HEADER.$ENCODED_PAYLOAD"

# --- 3. 计算 SHA256 散列并进行 RSA 签名 ---
# 注意：openssl dgst -sha256 -sign 不支持直接处理字符串，它期望文件输入
# 我们可以将签名输入写入一个临时文件
echo -n "$SIGNATURE_INPUT" > /tmp/jwt_signature_input.tmp

# 使用 openssl dgst 进行签名
# -sha256 指定哈希算法
# -sign 指定签名私钥文件
# -out 指定输出文件
# /tmp/jwt_signature_input.tmp 是待签名的文件
# SIGNED_DATA=$(openssl dgst -sha512 -sign "$PRIVATE_KEY_FILE" -binary /tmp/jwt_signature_input.tmp)
openssl dgst -sha512 -sign "$PRIVATE_KEY_FILE" -binary /tmp/jwt_signature_input.tmp > signature.bin 2>>/dev/null
# 清理临时文件
# rm $PRIVATE_KEY_FILE
rm /tmp/jwt_signature_input.tmp

# --- 4. Base64url 编码签名结果 ---
ENCODED_SIGNATURE=$(base64 -w0 signature.bin | tr -d '=' | tr '+/' '-_')
rm -f signature.bin

# --- 5. 拼接最终的 JWT ---
JWT="$ENCODED_HEADER.$ENCODED_PAYLOAD.$ENCODED_SIGNATURE"

echo "$JWT"

