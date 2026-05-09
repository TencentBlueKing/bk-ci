//go:build windows
// +build windows

package systemutil

import (
	"strings"
	"syscall"
	"unsafe"

	"github.com/pkg/errors"
	"golang.org/x/sys/windows"
)

func loadAgentInterfaceLookup() (agentInterfaceLookup, error) {
	lookup := agentInterfaceLookup{
		byIndex: map[int]agentInterfaceDetail{},
		byName:  map[string]agentInterfaceDetail{},
	}

	var size uint32
	const family = syscall.AF_UNSPEC
	const flags uint32 = 0
	err := windows.GetAdaptersAddresses(family, flags, 0, nil, &size)
	if err != nil && err != windows.ERROR_BUFFER_OVERFLOW {
		return lookup, errors.Wrap(err, "GetAdaptersAddresses size probe")
	}
	if size == 0 {
		return lookup, nil
	}

	buf := make([]byte, size)
	adapter := (*windows.IpAdapterAddresses)(unsafe.Pointer(&buf[0]))
	if err := windows.GetAdaptersAddresses(family, flags, 0, adapter, &size); err != nil {
		return lookup, errors.Wrap(err, "GetAdaptersAddresses fetch")
	}

	for p := adapter; p != nil; p = p.Next {
		friendly := windows.UTF16PtrToString(p.FriendlyName)
		description := windows.UTF16PtrToString(p.Description)
		detail := agentInterfaceDetail{
			friendlyName: friendly,
			description:  description,
			ifType:       p.IfType,
			isVirtual:    isVirtualWindowsAdapter(friendly, description, p.IfType),
		}
		lookup.byIndex[int(p.IfIndex)] = detail
		if friendly != "" {
			lookup.byName[strings.ToLower(friendly)] = detail
		}
	}

	return lookup, nil
}

func isVirtualWindowsAdapter(friendlyName, description string, ifType uint32) bool {
	if ifType == windows.IF_TYPE_SOFTWARE_LOOPBACK || ifType == windows.IF_TYPE_TUNNEL {
		return true
	}

	text := strings.ToLower(friendlyName + " " + description)
	keywords := []string{
		"vethernet",
		"default switch",
		"wsl",
		"docker",
		"container",
		"virtualbox",
		"host-only",
		"vmware",
		"loopback",
		"npcap",
		"isatap",
		"teredo",
		"wan miniport",
		"wireguard",
		"tap-",
		"ngnclient",
		"pangp",
		"vpn",
		"hyper-v",
	}
	for _, keyword := range keywords {
		if strings.Contains(text, keyword) {
			return true
		}
	}

	return false
}
