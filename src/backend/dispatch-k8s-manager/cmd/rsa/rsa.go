package main

import (
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"encoding/pem"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strconv"

	"github.com/pkg/errors"
)

// usage：rsa-generate [输出文件路径] [密匙位数(不填为2048)]
func main() {
	if len(os.Args) < 2 {
		io.WriteString(os.Stderr, "需求参数：输出文件夹路径。\n")
		os.Exit(1)
	}
	bitsS := "2048"
	if len(os.Args) > 2 {
		bitsS = os.Args[2]
	}
	err := RsaGenKey(bitsS, filepath.Join(os.Args[1], "private-key.pem"), filepath.Join(os.Args[1], "public-key.pem"))
	if err != nil {
		io.WriteString(os.Stderr, fmt.Sprintf("执行错误: %s。\n", err.Error()))
		os.Exit(1)
	}
	io.WriteString(os.Stdout, "执行成功。\n")
}

/*
 * 生成RSA公钥和私钥并保存在对应的目录文件下
 * 参数bits: 指定生成的秘钥的长度, 单位: bit
 */
func RsaGenKey(bitsS string, privatePath, pubulicPath string) error {
	bits, err := strconv.Atoi(bitsS)
	if err != nil {
		return err
	}

	path, err := filepath.Abs(privatePath)
	if err != nil {
		errors.Wrap(err, "get key abs path error")
	}
	err = os.MkdirAll(filepath.Dir(path), 0666)
	if err != nil {
		errors.Wrap(err, "create key dir error")
	}

	// 1. 生成私钥文件
	// GenerateKey函数使用随机数据生成器random生成一对具有指定字位数的RSA密钥
	privateKey, err := rsa.GenerateKey(rand.Reader, bits)
	if err != nil {
		return err
	}
	// 2. MarshalPKCS1PrivateKey将rsa私钥序列化为ASN.1 PKCS#1 DER编码
	derPrivateStream := x509.MarshalPKCS1PrivateKey(privateKey)

	// 3. Block代表PEM编码的结构, 对其进行设置
	block := pem.Block{
		Type:  "RSA PRIVATE KEY",
		Bytes: derPrivateStream,
	}

	// 4. 创建文件
	privateFile, err := os.Create(privatePath)
	defer privateFile.Close()

	if err != nil {
		return err
	}

	// 5. 使用pem编码, 并将数据写入文件中
	err = pem.Encode(privateFile, &block)
	if err != nil {
		return err
	}

	// 1. 生成公钥文件
	publicKey := privateKey.PublicKey
	derPublicStream := x509.MarshalPKCS1PublicKey(&publicKey)

	block = pem.Block{
		Type:  "RSA PUBLIC KEY",
		Bytes: derPublicStream,
	}

	publicFile, err := os.Create(pubulicPath)
	defer publicFile.Close()

	if err != nil {
		return err
	}

	// 2. 编码公钥, 写入文件
	err = pem.Encode(publicFile, &block)
	if err != nil {
		return err
	}

	return nil
}
