package rsa

import (
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"encoding/pem"

	"github.com/pkg/errors"
)

func GenRsaKey() (prvkey, pubkey []byte, err error) {
	// 生成私钥文件
	privateKey, err := rsa.GenerateKey(rand.Reader, 1024)
	if err != nil {
		return nil, nil, err
	}
	derStream, err := x509.MarshalPKCS8PrivateKey(privateKey)
	if err != nil {
		return nil, nil, errors.Wrap(err, "MarshalPKCS8PrivateKey err")
	}
	block := &pem.Block{
		Type:  "PRIVATE KEY",
		Bytes: derStream,
	}
	prvkey = pem.EncodeToMemory(block)
	publicKey := &privateKey.PublicKey
	derPkix, err := x509.MarshalPKIXPublicKey(publicKey)
	if err != nil {
		return nil, nil, err
	}
	block = &pem.Block{
		Type:  "PUBLIC KEY",
		Bytes: derPkix,
	}
	pubkey = pem.EncodeToMemory(block)
	return prvkey, pubkey, nil
}

// RSA公钥加密
func RSAEncrypt(data, keyBytes []byte) ([]byte, error) {
	//解密pem格式的公钥
	block, _ := pem.Decode(keyBytes)
	if block == nil {
		return nil, errors.New("rsa publickey is null")
	}
	// 解析公钥
	pubInterface, err := x509.ParsePKIXPublicKey(block.Bytes)
	if err != nil {
		return nil, err
	}
	// 类型断言
	pub := pubInterface.(*rsa.PublicKey)
	//加密
	ciphertext, err := rsa.EncryptPKCS1v15(rand.Reader, pub, data)
	if err != nil {
		return nil, err
	}
	return ciphertext, nil
}

/*
 * RSA私钥解密
 */
func RSADecrypt(src []byte, keyBuf []byte) (string, error) {
	// 从数据中解析出pem块
	block, _ := pem.Decode(keyBuf)
	if block == nil {
		return "", errors.New("rsa privatekey is null")
	}

	// 解析出一个der编码的私钥
	privateKey, err := x509.ParsePKCS8PrivateKey(block.Bytes)
	if err != nil {
		return "", err
	}

	// 私钥解密
	result, err := rsa.DecryptPKCS1v15(rand.Reader, privateKey.(*rsa.PrivateKey), src)
	if err != nil {
		return "", err
	}
	return string(result), nil
}
