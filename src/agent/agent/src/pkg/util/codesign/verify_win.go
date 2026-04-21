//go:build windows
// +build windows

/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package codesign

import (
	"fmt"
	"strings"
	"unsafe"

	"github.com/pkg/errors"
	"golang.org/x/sys/windows"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
)

// Windows Authenticode 校验通过两步完成：
//  1. 调用 WinVerifyTrust 做完整的链校验（签名完整性 + 受信任根）
//  2. 从文件的 Authenticode 嵌入 PKCS7 消息中取出签名者（叶子）证书，
//     通过 CertGetNameString 提取 Subject 中的 O（organizationName，OID 2.5.4.10），
//     与 config.WinCertOrgName 做大小写不敏感比对，实现身份 pin。
//
// 吊销检查默认关闭（WTD_REVOKE_NONE），构建机常在内网、无法访问 CRL/OCSP。

var (
	modwintrust = windows.NewLazySystemDLL("wintrust.dll")
	modcrypt32  = windows.NewLazySystemDLL("crypt32.dll")

	procWinVerifyTrust          = modwintrust.NewProc("WinVerifyTrust")
	procCryptQueryObject        = modcrypt32.NewProc("CryptQueryObject")
	procCertGetNameStringW      = modcrypt32.NewProc("CertGetNameStringW")
	procCertFreeCertContext     = modcrypt32.NewProc("CertFreeCertificateContext")
	procCertCloseStore          = modcrypt32.NewProc("CertCloseStore")
	procCryptMsgClose           = modcrypt32.NewProc("CryptMsgClose")
	procCryptMsgGetParam        = modcrypt32.NewProc("CryptMsgGetParam")
	procCertFindCertInStore     = modcrypt32.NewProc("CertFindCertificateInStore")
)

const (
	// WinVerifyTrust 相关常量（来自 wintrust.h）
	wtdUIChoiceNone       = 2
	wtdRevokeNone         = 0
	wtdChoiceFile         = 1
	wtdStateActionVerify  = 1
	wtdStateActionClose   = 2
	wtdSafeToIgnoreRevoke = 0x00000100

	// CryptQueryObject 相关常量（来自 wincrypt.h）
	certQueryObjectFile               = 1
	certQueryContentFlagPKCS7Embedded = 0x00000400
	certQueryFormatFlagBinary         = 0x00000002

	// CMSG_SIGNER_INFO_PARAM：从 CryptMsg 获取签名者信息
	cmsgSignerInfoParam = 6

	// CertFindCertificateInStore 的 find 类型
	certFindSubjectCert = 0x000B0000

	// 证书编码：X509_ASN_ENCODING | PKCS_7_ASN_ENCODING
	encodingX509AndPKCS7 = 0x00010001

	// CertGetNameString 类型
	certNameAttrType = 3 // CERT_NAME_ATTR_TYPE

	// OID: organizationName（Subject 中的 O 字段）
	oidOrganizationName = "2.5.4.10"
)

// WINTRUST_ACTION_GENERIC_VERIFY_V2 {00AAC56B-CD44-11D0-8CC2-00C04FC295EE}
var wintrustActionGenericVerifyV2 = windows.GUID{
	Data1: 0x00AAC56B,
	Data2: 0xCD44,
	Data3: 0x11D0,
	Data4: [8]byte{0x8C, 0xC2, 0x00, 0xC0, 0x4F, 0xC2, 0x95, 0xEE},
}

type wintrustFileInfo struct {
	CbStruct     uint32
	FilePath     *uint16
	HFile        windows.Handle
	KnownSubject *windows.GUID
}

type wintrustData struct {
	CbStruct           uint32
	PolicyCallbackData uintptr
	SIPClientData      uintptr
	UIChoice           uint32
	RevocationChecks   uint32
	UnionChoice        uint32
	FileInfoOrCatalog  uintptr // 指向 wintrustFileInfo
	StateAction        uint32
	StateData          windows.Handle
	URLReference       *uint16
	ProvFlags          uint32
	UIContext          uint32
	SignatureSettings  uintptr
}

// certContext 对应 Windows CERT_CONTEXT（仅用于传递指针）。
type certContext struct {
	dwCertEncodingType uint32
	pbCertEncoded      *byte
	cbCertEncoded      uint32
	pCertInfo          uintptr
	hCertStore         windows.Handle
}

// Verify 使用 Authenticode 校验 path 对应文件的签名链，并核对签发证书的
// Subject O 字段是否匹配 config.WinCertOrgName。
//
// 若 config.WinCertOrgName 为空，跳过校验并返回 nil。
func Verify(path string) error {
	if strings.TrimSpace(config.WinCertOrgName) == "" {
		logDisabledOnce("WinCertOrgName empty")
		return nil
	}
	if err := preCheckPath(path); err != nil {
		return err
	}

	if err := winVerifyTrustChain(path); err != nil {
		return errors.Wrapf(err, "WinVerifyTrust failed for %s", path)
	}

	org, err := extractSignerOrg(path)
	if err != nil {
		return errors.Wrapf(err, "extract signer org from %s failed", path)
	}

	expected := strings.TrimSpace(config.WinCertOrgName)
	if !strings.EqualFold(strings.TrimSpace(org), expected) {
		return fmt.Errorf("codesign: signer org mismatch: expected %q, got %q", expected, org)
	}

	logs.Infof("codesign|signature check passed for %s, signer O=%s", path, org)
	return nil
}

// winVerifyTrustChain 调用 WinVerifyTrust 做完整链校验，不做吊销检查。
func winVerifyTrustChain(path string) error {
	pathPtr, err := windows.UTF16PtrFromString(path)
	if err != nil {
		return err
	}

	fileInfo := wintrustFileInfo{FilePath: pathPtr}
	fileInfo.CbStruct = uint32(unsafe.Sizeof(fileInfo))

	wd := wintrustData{
		UIChoice:          wtdUIChoiceNone,
		RevocationChecks:  wtdRevokeNone,
		UnionChoice:       wtdChoiceFile,
		FileInfoOrCatalog: uintptr(unsafe.Pointer(&fileInfo)),
		StateAction:       wtdStateActionVerify,
		ProvFlags:         wtdSafeToIgnoreRevoke,
	}
	wd.CbStruct = uint32(unsafe.Sizeof(wd))

	ret, _, _ := procWinVerifyTrust.Call(
		0, // hwnd = NULL
		uintptr(unsafe.Pointer(&wintrustActionGenericVerifyV2)),
		uintptr(unsafe.Pointer(&wd)),
	)

	// 关闭状态（无论校验成功与否都必须调用以释放资源）
	wd.StateAction = wtdStateActionClose
	procWinVerifyTrust.Call(
		0,
		uintptr(unsafe.Pointer(&wintrustActionGenericVerifyV2)),
		uintptr(unsafe.Pointer(&wd)),
	)

	// WinVerifyTrust 成功返回 0，失败返回 HRESULT（非 0）
	if ret != 0 {
		return fmt.Errorf("WinVerifyTrust HRESULT=0x%x", uint32(ret))
	}
	return nil
}

// cryptoAPIBlob 对应 Windows 的 CRYPT_INTEGER_BLOB / CERT_NAME_BLOB 结构：
//
//	typedef struct {
//	    DWORD cbData;
//	    BYTE  *pbData;
//	} CRYPT_INTEGER_BLOB;
type cryptoAPIBlob struct {
	CbData uint32
	PbData *byte
}

// cmsgSignerInfo 对应 CMSG_SIGNER_INFO 的前缀字段（前两个字段是我们需要的
// Issuer 和 SerialNumber）。真实结构后面还有 HashAlgorithm、EncryptedHash、
// AuthAttrs、UnauthAttrs，但我们只用前两个，所以这里只定义到 SerialNumber。
// 因为 Go 的结构体指针 + unsafe.Pointer 会按声明顺序布局，保持前缀对齐即可
// 安全读取。
type cmsgSignerInfo struct {
	DwVersion uint32
	Issuer    cryptoAPIBlob
	// SerialNumber 对应的是 CRYPT_INTEGER_BLOB（ASN.1 编码的整数）
	SerialNumber cryptoAPIBlob
	// 以下字段存在但我们不读：
	// HashAlgorithm CRYPT_ALGORITHM_IDENTIFIER
	// HashEncryptionAlgorithm CRYPT_ALGORITHM_IDENTIFIER
	// EncryptedHash CRYPT_HASH_BLOB
	// AuthAttrs CRYPT_ATTRIBUTES
	// UnauthAttrs CRYPT_ATTRIBUTES
}

// certInfoIssuerSerial 对应 CERT_INFO 中用于 CertFindCertificateInStore
// (CERT_FIND_SUBJECT_CERT) 查找时需要匹配的 SerialNumber 和 Issuer 字段。
//
// CERT_FIND_SUBJECT_CERT 的文档说明：在 CERT_INFO 中只使用 Issuer 和
// SerialNumber 两个字段做匹配，因此这里只需要填这两项即可，其他字段留零值。
// 但 CERT_INFO 整体布局必须匹配，前置字段 dwVersion 和 SerialNumber 要按
// 顺序放在正确偏移上。
type certInfoIssuerSerial struct {
	DwVersion            uint32
	SerialNumber         cryptoAPIBlob
	SignatureAlgorithm   cryptAlgIdentifier // 空占位保持结构对齐
	Issuer               cryptoAPIBlob
	NotBefore            windows.Filetime
	NotAfter             windows.Filetime
	Subject              cryptoAPIBlob
	SubjectPublicKeyInfo certPublicKeyInfo
	IssuerUniqueID       cryptoAPIBlob
	SubjectUniqueID      cryptoAPIBlob
	CExtension           uint32
	RgExtension          uintptr
}

// cryptAlgIdentifier 对应 CRYPT_ALGORITHM_IDENTIFIER：{ LPSTR pszObjId; CRYPT_OBJID_BLOB Parameters; }
type cryptAlgIdentifier struct {
	PszObjID   *byte
	Parameters cryptoAPIBlob
}

// certPublicKeyInfo 对应 CERT_PUBLIC_KEY_INFO。
type certPublicKeyInfo struct {
	Algorithm cryptAlgIdentifier
	PublicKey cryptBitBlob
}

// cryptBitBlob 对应 CRYPT_BIT_BLOB：{ DWORD cbData; BYTE *pbData; DWORD cUnusedBits; }
type cryptBitBlob struct {
	CbData      uint32
	PbData      *byte
	CUnusedBits uint32
}

// extractSignerOrg 从 path 对应的 Authenticode 签名中精确定位签名者证书，
// 并返回 Subject 中 O 字段（organizationName）的内容。
//
// 精确定位的做法：
//  1. CryptQueryObject 取出嵌入的 PKCS7 消息（hMsg）和包含所有证书的 store（hStore）
//  2. CryptMsgGetParam(CMSG_SIGNER_INFO_PARAM) 从 hMsg 读出签名者的 Issuer 和 SerialNumber
//  3. 以 Issuer+SerialNumber 构造 CERT_INFO，调 CertFindCertificateInStore
//     (CERT_FIND_SUBJECT_CERT) 从 hStore 精确匹配到签名者证书
//
// 这样即使攻击者在 PKCS7 包里塞入多张证书（比如一张合法的 O=Tencent 放在前面
// 做障眼法，真正做签名的是另一张），我们也只会校验真正参与签名的那张的 O。
func extractSignerOrg(path string) (string, error) {
	pathPtr, err := windows.UTF16PtrFromString(path)
	if err != nil {
		return "", err
	}

	var (
		encoding    uint32
		contentType uint32
		formatType  uint32
		hStore      windows.Handle
		hMsg        windows.Handle
	)

	ret, _, callErr := procCryptQueryObject.Call(
		uintptr(certQueryObjectFile),
		uintptr(unsafe.Pointer(pathPtr)),
		uintptr(certQueryContentFlagPKCS7Embedded),
		uintptr(certQueryFormatFlagBinary),
		0,
		uintptr(unsafe.Pointer(&encoding)),
		uintptr(unsafe.Pointer(&contentType)),
		uintptr(unsafe.Pointer(&formatType)),
		uintptr(unsafe.Pointer(&hStore)),
		uintptr(unsafe.Pointer(&hMsg)),
		0,
	)
	if ret == 0 {
		return "", fmt.Errorf("CryptQueryObject failed: %v", callErr)
	}
	defer procCertCloseStore.Call(uintptr(hStore), 0)
	defer procCryptMsgClose.Call(uintptr(hMsg))

	signerInfoBuf, err := getSignerInfoBytes(hMsg)
	if err != nil {
		return "", err
	}
	if len(signerInfoBuf) < int(unsafe.Sizeof(cmsgSignerInfo{})) {
		return "", fmt.Errorf("CMSG_SIGNER_INFO too short: %d bytes", len(signerInfoBuf))
	}
	signer := (*cmsgSignerInfo)(unsafe.Pointer(&signerInfoBuf[0]))

	// 用 Issuer + SerialNumber 精确定位签名者证书
	findCriteria := certInfoIssuerSerial{
		Issuer:       signer.Issuer,
		SerialNumber: signer.SerialNumber,
	}
	leafRaw, _, callErr := procCertFindCertInStore.Call(
		uintptr(hStore),
		uintptr(encodingX509AndPKCS7),
		0,
		uintptr(certFindSubjectCert),
		uintptr(unsafe.Pointer(&findCriteria)),
		0, // prevCertContext = NULL
	)
	if leafRaw == 0 {
		return "", fmt.Errorf("CertFindCertificateInStore failed: %v", callErr)
	}
	leaf := (*certContext)(unsafe.Pointer(leafRaw))
	defer procCertFreeCertContext.Call(uintptr(unsafe.Pointer(leaf)))

	return certGetNameString(leaf, oidOrganizationName)
}

// getSignerInfoBytes 从 CryptMsg 读取 CMSG_SIGNER_INFO 的原始字节。
// 注意：Issuer/SerialNumber 等字段内的指针指向此 buffer 内部，因此
// 调用方必须在 buffer 生命周期内使用返回值。
func getSignerInfoBytes(hMsg windows.Handle) ([]byte, error) {
	var size uint32
	ret, _, callErr := procCryptMsgGetParam.Call(
		uintptr(hMsg),
		uintptr(cmsgSignerInfoParam),
		0, // signer index 0（即第一个签名者）
		0,
		uintptr(unsafe.Pointer(&size)),
	)
	if ret == 0 || size == 0 {
		return nil, fmt.Errorf("CryptMsgGetParam size query failed: %v", callErr)
	}
	buf := make([]byte, size)
	ret, _, callErr = procCryptMsgGetParam.Call(
		uintptr(hMsg),
		uintptr(cmsgSignerInfoParam),
		0,
		uintptr(unsafe.Pointer(&buf[0])),
		uintptr(unsafe.Pointer(&size)),
	)
	if ret == 0 {
		return nil, fmt.Errorf("CryptMsgGetParam failed: %v", callErr)
	}
	return buf, nil
}

// certGetNameString 调用 CertGetNameStringW 提取证书 Subject 中指定 OID 的属性值。
func certGetNameString(cert *certContext, oid string) (string, error) {
	oidPtr, err := windows.BytePtrFromString(oid)
	if err != nil {
		return "", err
	}

	// 第一次调用：查询所需字符数（含结尾 NUL）
	size, _, _ := procCertGetNameStringW.Call(
		uintptr(unsafe.Pointer(cert)),
		uintptr(certNameAttrType),
		0,
		uintptr(unsafe.Pointer(oidPtr)),
		0,
		0,
	)
	if size <= 1 {
		return "", fmt.Errorf("CertGetNameString: attribute %q not found", oid)
	}

	buf := make([]uint16, size)
	ret, _, _ := procCertGetNameStringW.Call(
		uintptr(unsafe.Pointer(cert)),
		uintptr(certNameAttrType),
		0,
		uintptr(unsafe.Pointer(oidPtr)),
		uintptr(unsafe.Pointer(&buf[0])),
		size,
	)
	if ret == 0 {
		return "", fmt.Errorf("CertGetNameString failed")
	}
	return windows.UTF16ToString(buf), nil
}
