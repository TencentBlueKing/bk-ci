package monitor

import "strings"

// net_filter.go 按前缀黑名单过滤虚接口与仅内核路由使用的伪接口。
//
// 设计取舍：
//   - 采用"前缀匹配"而非"精确匹配"：大量虚接口名带随机/递增后缀（br-abc123、
//     utun7、vEthernet (WSL Hyper-V firewall)），前缀足以识别
//   - 保留物理网卡及常见别名（eth* / ens* / enp* / em* / en* / 以太网 /
//     Intel... I219-LM）不被过滤
//   - 不从 agent 配置读取；黑名单硬编码，以保证三平台行为一致，后续有需要
//     再开放 .agent.properties 覆盖
//   - Windows 的 Loopback 伪接口名严格为 "Loopback Pseudo-Interface 1"，
//     前缀 "Loopback" 足够
//
// 与 diskio 过滤（input_diskio_filter.go）互不依赖，各自维护自己的黑名单。
var defaultNetIfaceSkipPrefixes = []string{
	// Linux
	"lo",      // lo / lo0
	"br-",     // docker bridge: br-abc123
	"veth",    // container veth pair
	"docker",  // docker0
	"cali",    // calico / CNI
	"flannel", // flannel / weave
	"weave",   // weave
	"cni",     // generic cni
	"virbr",   // libvirt
	"tun",     // tun*（macOS utun* 也命中；需早于 en* 判断）
	"tap",     // tap 虚接口
	// macOS 专属伪接口
	"gif",    // generic ip tunnel
	"stf",    // 6to4 tunnel
	"anpi",   // Apple Mobile Net
	"ap",     // AirPort
	"awdl",   // Apple Wireless Direct
	"llw",    // Link-Layer Wireless
	"bridge", // bridge0
	"utun",   // user tunnel
	// Windows 伪接口
	"Loopback Pseudo-Interface",
	"vEthernet",    // vEthernet (WSL (Hyper-V firewall)) 等
	"isatap.",      // ISATAP tunnel
	"Teredo",       // IPv6 teredo tunnel
	"NGNClient",    // VPN 隧道虚拟网卡
	"TAP-",         // OpenVPN TAP-Windows Adapter Vx
	"WireGuard",    // WireGuard 隧道
	"Hyper-V",      // Hyper-V Virtual Ethernet Adapter
	"WAN Miniport", // Windows 拨号/VPN 虚接口
	"PANGP",        // GlobalProtect VPN
}

// shouldSkipNetInterface 返回 true 表示该接口应被过滤，不产出 metric。
// 大小写不敏感以兼容 Windows 不同语言版本的接口名（实际是 FriendlyName）。
func shouldSkipNetInterface(name string) bool {
	lower := strings.ToLower(name)
	for _, p := range defaultNetIfaceSkipPrefixes {
		if strings.HasPrefix(lower, strings.ToLower(p)) {
			return true
		}
	}
	return false
}
