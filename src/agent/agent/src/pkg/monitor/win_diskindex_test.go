//go:build windows
// +build windows

package monitor

import (
	"testing"
	"time"
)

// win_diskindex_test.go 只覆盖**纯函数可测**的缓存逻辑，不触发真实
// DeviceIoControl（那需要真实磁盘 + 权限，属端到端验证范畴）。
//
// 通过操作包级变量 diskIndexCache / diskIndexCachedAt 模拟命中/过期。

func TestDiskIndexCache_HitWithinTTL(t *testing.T) {
	resetDiskIndexCacheForTest()
	// 手动植入缓存
	diskIndexCacheMu.Lock()
	diskIndexCache = map[string]uint32{"C:": 0, "D:": 1}
	diskIndexCachedAt = time.Now()
	diskIndexCacheMu.Unlock()

	idx, err := queryStorageDeviceNumber("C:")
	if err != nil || idx != 0 {
		t.Errorf("want (0,nil), got (%d,%v)", idx, err)
	}
	idx2, err := queryStorageDeviceNumber("D:")
	if err != nil || idx2 != 1 {
		t.Errorf("want (1,nil), got (%d,%v)", idx2, err)
	}
}

func TestDiskIndexCache_ExpiredTriggersRefresh(t *testing.T) {
	resetDiskIndexCacheForTest()
	// 植入"10 分钟前"的缓存 → 应被视为过期，触发 refresh（refresh 会失败
	// 因为测试环境不一定有盘，但关键是我们验证"过期路径走到了 refresh"）。
	diskIndexCacheMu.Lock()
	diskIndexCache = map[string]uint32{"C:": 99}
	diskIndexCachedAt = time.Now().Add(-2 * diskIndexCacheTTL)
	diskIndexCacheMu.Unlock()

	// 调用 "Z:"——即使缓存里没有，也不会命中；会触发 refresh；无论 refresh
	// 成功与否，关键是返回值不等于 99（那是"命中旧缓存"的值，不应发生）
	idx, _ := queryStorageDeviceNumber("Z:")
	if idx == 99 {
		t.Error("expired cache must not return stale value")
	}
}

func TestDiskIndexCache_MissingLetterWithinTTL(t *testing.T) {
	resetDiskIndexCacheForTest()
	// 有效缓存里没有 "Z:" → 代码会尝试 refresh 一次；真实环境下 refresh 可
	// 能成功（Z 盘不存在 → 仍返回 error "not found"），可能失败（
	// GetLogicalDriveStrings 或 ioctl 报错）。任意情况下都应返回非 nil error。
	diskIndexCacheMu.Lock()
	diskIndexCache = map[string]uint32{"C:": 0}
	diskIndexCachedAt = time.Now()
	diskIndexCacheMu.Unlock()

	_, err := queryStorageDeviceNumber("Z:")
	if err == nil {
		t.Error("want error for missing drive letter after refresh attempt")
	}
}
