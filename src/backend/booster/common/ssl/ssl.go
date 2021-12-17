/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package ssl

import (
	"crypto/tls"
	"crypto/x509"
	"encoding/pem"
	"fmt"
	"io/ioutil"
)

// ClientTSLConfNoVerity get a empty tls.Config with insecure skip verify for client side.
func ClientTSLConfNoVerity() *tls.Config {
	return &tls.Config{
		InsecureSkipVerify: true,
	}
}

// ClientTSLConfVerityServer get a new tls.Config and will check given ca for client side.
func ClientTSLConfVerityServer(caFile string) (*tls.Config, error) {
	caPool, err := loadCa(caFile)
	if err != nil {
		return nil, err
	}

	conf := &tls.Config{
		RootCAs: caPool,
	}

	return conf, nil
}

// ClientTSLConfVerity receive certs files and return a tls.Config for client side.
func ClientTSLConfVerity(caFile, certFile, keyFile, passwd string) (*tls.Config, error) {
	caPool, err := loadCa(caFile)
	if err != nil {
		return nil, err
	}

	cert, err := loadCertificates(certFile, keyFile, passwd)
	if err != nil {
		return nil, err
	}

	conf := &tls.Config{
		InsecureSkipVerify: true,
		RootCAs:            caPool,
		Certificates:       []tls.Certificate{*cert},
	}

	return conf, nil
}

// ServerTSLConf receive certs files and return a tls.Config for server side.
func ServerTSLConf(caFile, certFile, keyFile, passwd string) (*tls.Config, error) {
	if "" == caFile {
		return ServerTSLConfVerity(certFile, keyFile, passwd)
	}

	return ServerTSLConfVerityClient(caFile, certFile, keyFile, passwd)
}

// ServerTSLConfVerity receive certs files without ca and return a tls.Config for server side.
func ServerTSLConfVerity(certFile, keyFile, passwd string) (*tls.Config, error) {

	cert, err := loadCertificates(certFile, keyFile, passwd)
	if err != nil {
		return nil, err
	}

	conf := &tls.Config{
		Certificates: []tls.Certificate{*cert},
	}

	return conf, nil
}

// ServerTSLConfVerityClient receive certs files with ca and return a tls.Config for server side.
func ServerTSLConfVerityClient(caFile, certFile, keyFile, passwd string) (*tls.Config, error) {
	caPool, err := loadCa(caFile)
	if err != nil {
		return nil, err
	}

	cert, err := loadCertificates(certFile, keyFile, passwd)
	if err != nil {
		return nil, err
	}

	conf := &tls.Config{
		ClientCAs:    caPool,
		Certificates: []tls.Certificate{*cert},
		ClientAuth:   tls.RequireAndVerifyClientCert,
	}

	return conf, nil
}

func loadCa(caFile string) (*x509.CertPool, error) {
	ca, err := ioutil.ReadFile(caFile)
	if err != nil {
		return nil, err
	}

	caPool := x509.NewCertPool()
	if ok := caPool.AppendCertsFromPEM(ca); ok != true {
		return nil, fmt.Errorf("append ca cert failed")
	}

	return caPool, nil
}

func loadCertificates(certFile, keyFile, passwd string) (*tls.Certificate, error) {
	//key file
	priKey, err := ioutil.ReadFile(keyFile)
	if err != nil {
		return nil, err
	}

	if "" != passwd {
		priPem, _ := pem.Decode(priKey)
		if priPem == nil {
			return nil, fmt.Errorf("decode private key failed")
		}

		priDecrPem, err := x509.DecryptPEMBlock(priPem, []byte(passwd))
		if err != nil {
			return nil, err
		}

		priKey = pem.EncodeToMemory(&pem.Block{
			Type:  "RSA PRIVATE KEY",
			Bytes: priDecrPem,
		})
	}

	//certificate
	certData, err := ioutil.ReadFile(certFile)
	if err != nil {
		return nil, err
	}

	tlsCert, err := tls.X509KeyPair(certData, priKey)
	if err != nil {
		return nil, err
	}

	return &tlsCert, nil
}
