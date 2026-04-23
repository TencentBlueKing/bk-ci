//go:build windows
// +build windows

package monitor

import (
	"strings"
	"sync"
	"syscall"
	"time"
	"unsafe"

	"github.com/pkg/errors"
	"golang.org/x/sys/windows"
)

// win_netadapter.go 通过 Win32 API GetAdaptersAddresses 查询网卡的
// FriendlyName -> Description 映射，用于把 monitor net metric 的 instance
// tag 从 gopsutil 的 FriendlyName（如 "以太网 5"）升级为 PDH
// Network Interface object 使用的 Description（如
// "Intel[R] Ethernet Connection [17] I219-LM"），对齐 telegraf
// win_perf_counters 的实例命名。
//
// 缓存：CI 构建机上网卡列表生命周期内基本不变，USB 网卡插拔罕见。
// 加 10 分钟 TTL 缓存；刷新失败时保留旧缓存（与 diskindex 的"失败清空"
// 策略不同——这里网卡 Description 偶发查询失败不应让 instance 突变回
// FriendlyName 再切回去，造成曲线断点）。

// adapterDescCacheTTL 与 diskIndexCacheTTL 保持一致（10 分钟）。
const adapterDescCacheTTL = 10 * time.Minute

var (
	adapterDescCacheMu  sync.Mutex
	adapterDescCache    map[string]string
	adapterDescCachedAt time.Time
)

// adapterDescriptions 返回 FriendlyName -> Description 映射。
// 命中缓存或成功刷新时返回 (map, nil)；刷新失败且无旧缓存时返回
// (nil, err)，调用方应 fallback 到 FriendlyName 作 instance。
func adapterDescriptions() (map[string]string, error) {
	adapterDescCacheMu.Lock()
	defer adapterDescCacheMu.Unlock()

	if adapterDescCache != nil && time.Since(adapterDescCachedAt) < adapterDescCacheTTL {
		return adapterDescCache, nil
	}

	fresh, err := scanAdapterDescriptions()
	if err != nil {
		if adapterDescCache != nil {
			// 刷新失败但保留旧缓存：业务侧可继续用近似正确的映射
			return adapterDescCache, nil
		}
		return nil, err
	}
	adapterDescCache = fresh
	adapterDescCachedAt = time.Now()
	return fresh, nil
}

// scanAdapterDescriptions 全量查询系统所有网卡，返回 FriendlyName -> Description。
//
// 调用模式：先用 size=0 拿长度（返回 ERROR_BUFFER_OVERFLOW），再分配缓
// 冲区二次调用。这是 GetAdaptersAddresses 的标准用法。
func scanAdapterDescriptions() (map[string]string, error) {
	var size uint32
	const family = syscall.AF_UNSPEC
	const flags uint32 = 0
	// 第一次探询需要的缓冲区大小
	err := windows.GetAdaptersAddresses(family, flags, 0, nil, &size)
	if err != nil && err != windows.ERROR_BUFFER_OVERFLOW {
		return nil, errors.Wrap(err, "netadapter: GetAdaptersAddresses size probe")
	}
	if size == 0 {
		return map[string]string{}, nil
	}

	buf := make([]byte, size)
	adapter := (*windows.IpAdapterAddresses)(unsafe.Pointer(&buf[0]))
	if err := windows.GetAdaptersAddresses(family, flags, 0, adapter, &size); err != nil {
		return nil, errors.Wrap(err, "netadapter: GetAdaptersAddresses fetch")
	}

	out := make(map[string]string, 8)
	for p := adapter; p != nil; p = p.Next {
		friendly := windows.UTF16PtrToString(p.FriendlyName)
		desc := windows.UTF16PtrToString(p.Description)
		if friendly == "" {
			continue
		}
		out[friendly] = pdhEscapeInstance(desc)
	}
	return out, nil
}

// pdhEscapeInstance 把驱动原始 Description 里的圆括号替换成方括号，对齐
// telegraf win_perf_counters 的 PDH instance 字符串（PDH 路径格式把 "("
// 和 ")" 视为特殊字符，Windows 内部会做这一步 escape）。
//
// 例：
//   Intel(R) Ethernet Connection (17) I219-LM
//     → Intel[R] Ethernet Connection [17] I219-LM
//
// 与 telegraf 历史 instance 字符串 100% 一致，让后端
// WHERE instance='Intel[R]...' 精确查询能命中 monitor 数据。
func pdhEscapeInstance(s string) string {
	if s == "" {
		return s
	}
	s = strings.ReplaceAll(s, "(", "[")
	s = strings.ReplaceAll(s, ")", "]")
	return s
}

// resetAdapterDescCacheForTest 供单测清理缓存，避免跨用例污染。
func resetAdapterDescCacheForTest() {
	adapterDescCacheMu.Lock()
	defer adapterDescCacheMu.Unlock()
	adapterDescCache = nil
	adapterDescCachedAt = time.Time{}
}
