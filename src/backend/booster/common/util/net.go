/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package util

import (
	"net"
)

var (
	_, classA, _  = net.ParseCIDR("10.0.0.0/8")
	_, classA1, _ = net.ParseCIDR("9.0.0.0/8")
	_, classAa, _ = net.ParseCIDR("100.64.0.0/10")
	_, classB, _  = net.ParseCIDR("172.16.0.0/12")
	_, classC, _  = net.ParseCIDR("192.168.0.0/16")
)

// GetIPAddress get local usable inner ip address
func GetIPAddress() (addrList []string) {
	address, err := net.InterfaceAddrs()
	if err != nil {
		return addrList
	}
	for _, addr := range address {
		if ip, ok := addr.(*net.IPNet); ok && !ip.IP.IsLoopback() && ip.IP.To4() != nil {
			if classA.Contains(ip.IP) {
				addrList = append(addrList, ip.IP.String())
				continue
			}
			if classA1.Contains(ip.IP) {
				addrList = append(addrList, ip.IP.String())
				continue
			}
			if classAa.Contains(ip.IP) {
				addrList = append(addrList, ip.IP.String())
				continue
			}
			if classB.Contains(ip.IP) {
				addrList = append(addrList, ip.IP.String())
				continue
			}
			if classC.Contains(ip.IP) {
				addrList = append(addrList, ip.IP.String())
				continue
			}
		}
	}
	return addrList
}
