//go:build darwin && cgo
// +build darwin,cgo

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

/*
#cgo LDFLAGS: -framework CoreFoundation -framework Security

#include <CoreFoundation/CoreFoundation.h>
#include <Security/Security.h>
#include <stdlib.h>

// verify_with_requirement 使用 SecStaticCode 接口校验 path 对应的二进制：
// 1) 构造 SecStaticCodeRef
// 2) 用 Designated Requirement 表达式构造 SecRequirementRef（绑定 anchor + Team ID）
// 3) 调 SecStaticCodeCheckValidityWithErrors 做完整校验
//
// 使用的 flag 组合：
//   - kSecCSStrictValidate：拒绝任何资源目录被篡改的情况（比默认更严）
//   - kSecCSCheckAllArchitectures：若是 fat binary，要求所有架构都通过校验，
//     防止攻击者只篡改其中一种架构的 slice
//   - kSecCSCheckNestedCode：校验所有嵌套签名组件（如内嵌 framework）
//
// 返回 OSStatus；出错时可选通过 outErrMsg 带回人类可读的错误信息（调用方 free）
static OSStatus verify_with_requirement(const char *path, const char *requirement, char **outErrMsg) {
    OSStatus status = errSecSuccess;
    CFStringRef cfPath = NULL;
    CFURLRef cfURL = NULL;
    CFStringRef cfReqStr = NULL;
    SecRequirementRef req = NULL;
    SecStaticCodeRef code = NULL;
    CFErrorRef cfErr = NULL;

    // 严格校验标志位组合。
    SecCSFlags strictFlags = kSecCSStrictValidate
                           | kSecCSCheckAllArchitectures
                           | kSecCSCheckNestedCode;

    cfPath = CFStringCreateWithCString(kCFAllocatorDefault, path, kCFStringEncodingUTF8);
    if (!cfPath) { status = errSecAllocate; goto done; }

    cfURL = CFURLCreateWithFileSystemPath(kCFAllocatorDefault, cfPath, kCFURLPOSIXPathStyle, false);
    if (!cfURL) { status = errSecAllocate; goto done; }

    status = SecStaticCodeCreateWithPath(cfURL, kSecCSDefaultFlags, &code);
    if (status != errSecSuccess) goto done;

    cfReqStr = CFStringCreateWithCString(kCFAllocatorDefault, requirement, kCFStringEncodingUTF8);
    if (!cfReqStr) { status = errSecAllocate; goto done; }

    status = SecRequirementCreateWithString(cfReqStr, kSecCSDefaultFlags, &req);
    if (status != errSecSuccess) goto done;

    status = SecStaticCodeCheckValidityWithErrors(code, strictFlags, req, &cfErr);
    if (status != errSecSuccess && cfErr != NULL && outErrMsg != NULL) {
        CFStringRef desc = CFErrorCopyDescription(cfErr);
        if (desc) {
            CFIndex len = CFStringGetMaximumSizeForEncoding(CFStringGetLength(desc), kCFStringEncodingUTF8) + 1;
            char *buf = (char*)malloc(len);
            if (buf && CFStringGetCString(desc, buf, len, kCFStringEncodingUTF8)) {
                *outErrMsg = buf;
            } else if (buf) {
                free(buf);
            }
            CFRelease(desc);
        }
    }

done:
    if (cfErr) CFRelease(cfErr);
    if (req) CFRelease(req);
    if (code) CFRelease(code);
    if (cfReqStr) CFRelease(cfReqStr);
    if (cfURL) CFRelease(cfURL);
    if (cfPath) CFRelease(cfPath);
    return status;
}
*/
import "C"

import (
	"fmt"
	"strings"
	"unsafe"

	"github.com/pkg/errors"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
)

// Verify 使用 macOS Security.framework 校验 path 对应文件的签名，
// 并通过 Designated Requirement 要求叶子证书 OU（Team ID）匹配 config.MacosTeamId。
//
// 若 config.MacosTeamId 为空，跳过校验并返回 nil。
func Verify(path string) error {
	teamID := strings.TrimSpace(config.MacosTeamId)
	if teamID == "" {
		logDisabledOnce("MacosTeamId empty")
		return nil
	}
	if err := preCheckPath(path); err != nil {
		return err
	}

	// Designated Requirement：要求 Apple 根 + 叶子证书 OU 等于预期 Team ID
	requirement := fmt.Sprintf(
		`anchor apple generic and certificate leaf[subject.OU] = "%s"`,
		teamID,
	)

	cPath := C.CString(path)
	defer C.free(unsafe.Pointer(cPath))
	cReq := C.CString(requirement)
	defer C.free(unsafe.Pointer(cReq))

	var cErrMsg *C.char
	status := C.verify_with_requirement(cPath, cReq, &cErrMsg)
	var errMsg string
	if cErrMsg != nil {
		errMsg = C.GoString(cErrMsg)
		C.free(unsafe.Pointer(cErrMsg))
	}

	if status != 0 {
		if errMsg == "" {
			errMsg = fmt.Sprintf("OSStatus=%d", int32(status))
		}
		return errors.Errorf(
			"codesign: SecStaticCodeCheckValidity failed for %s (expected teamId=%s): %s",
			path, teamID, errMsg,
		)
	}

	logs.Infof("codesign|signature check passed for %s, teamId=%s", path, teamID)
	return nil
}
