package middleware

import (
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"disaptch-k8s-manager/pkg/apiserver/apis"
	"disaptch-k8s-manager/pkg/config"
	"disaptch-k8s-manager/pkg/logs"
	"disaptch-k8s-manager/pkg/types"
	"encoding/pem"
	"errors"
	"net/http"

	"github.com/gin-gonic/gin"
)

func InitApiAuth() gin.HandlerFunc {

	return func(c *gin.Context) {
		// 放过一些url
		for _, url := range apis.NoAuthUrls {
			if c.FullPath() == apis.ApiPrefix+url {
				c.Next()
				return
			}
		}

		// 判断是否是来自蓝盾的token
		// 通过是否配置了加密信息判断是否加密
		token := c.GetHeader(config.Config.ApiServer.Auth.ApiToken.Key)
		if config.Config.ApiServer.Auth.RsaPrivateKey == "" {
			if token != "" && token == config.Config.ApiServer.Auth.ApiToken.Value {
				c.Next()
				return
			}
		} else {
			decryptToken, err := RSADecrypt([]byte(token), []byte(config.Config.ApiServer.Auth.RsaPrivateKey))
			if err != nil {
				logs.Error("decryptToken error", err)
			} else if decryptToken != "" && decryptToken == config.Config.ApiServer.Auth.ApiToken.Value {
				c.Next()
				return
			}
		}

		c.AbortWithStatus(http.StatusUnauthorized)
		c.JSON(http.StatusUnauthorized, &types.Result{
			Data:    nil,
			Status:  http.StatusUnauthorized,
			Message: "no auth",
		})
	}
}

/*
 * RSA私钥解密
 */
func RSADecrypt(src []byte, keyBuf []byte) (string, error) {
	// 从数据中解析出pem块
	block, _ := pem.Decode(keyBuf)
	if block == nil {
		return "", errors.New("auth privatekey is null")
	}

	// 解析出一个der编码的私钥
	privateKey, err := x509.ParsePKCS1PrivateKey(block.Bytes)

	// 私钥解密
	result, err := rsa.DecryptPKCS1v15(rand.Reader, privateKey, src)
	if err != nil {
		return "", err
	}
	return string(result), nil
}
