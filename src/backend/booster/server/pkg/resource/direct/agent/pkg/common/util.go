/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package common

import (
	"fmt"
	"net"
	"os"
	"os/exec"

	"github.com/Tencent/bk-ci/src/booster/common/blog"

	"github.com/shirou/gopsutil/cpu"
	"github.com/shirou/gopsutil/mem"
)

const (
	LocalIPKKey = "BK_DISTCC_LOCAL_IP"
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
	addrs, err := net.InterfaceAddrs()
	if err != nil {
		return addrList
	}
	for _, addr := range addrs {
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

// get local ip from env first, if not exists or empty, then get from net
func GetLocalIP() (string, error) {
	ip := os.Getenv(LocalIPKKey)
	if ip != "" {
		return ip, nil
	}

	ips := GetIPAddress()
	if len(ips) == 0 {
		return "", fmt.Errorf("get local IP failed, the client will exit")
	}

	return ips[0], nil
}

// ListApplicationByNameWindows
func ListApplicationByNameWindows(processName string) (string, error) {
	blog.Infof("ListApplicationByNameWindows with process[%s]", processName)

	condition := fmt.Sprintf("imagename eq %s", processName)
	cmd := exec.Command("tasklist", "/fi", condition)
	//buf := bytes.NewBuffer(make([]byte, 10240))
	//cmd.Stdout = buf
	data, err := cmd.Output()
	if err != nil {
		blog.Infof("failed to tasklist /fi %s for err[%v]", condition, err)
		return "", err
	} else {
		return (string)(data), nil
	}
}

// ListApplicationByNameUnix
func ListApplicationByNameUnix(processName string) (string, error) {
	blog.Infof("ListApplicationByNameUnix with process[%s]", processName)

	command := fmt.Sprintf("ps aux|grep -F \"%s\"", processName)
	cmd := exec.Command("/bin/bash", "-c", command)
	data, err := cmd.Output()
	if err != nil {
		blog.Infof("failed to execute %s for err[%v]", command, err)
		return "", err
	}
	return (string)(data), nil
}

// ListApplicationByNameAndPidWindows
func ListApplicationByNameAndPidWindows(processName string, pid string) (string, error) {
	blog.Infof("ListApplicationByNameAndPidWindows with process[%s] pid[%s]", processName, pid)

	condition1 := fmt.Sprintf("imagename eq %s", processName)
	condition2 := fmt.Sprintf("pid eq %s", pid)
	cmd := exec.Command("tasklist", "/fi", condition1, "/fi", condition2)

	data, err := cmd.Output()
	if err != nil {
		blog.Infof("failed to tasklist /fi %s /fi %s for err[%v]", condition1, condition2, err)
		return "", err
	} else {
		return (string)(data), nil
	}
}

// ListApplicationByNameAndPidUnix
func ListApplicationByNameAndPidUnix(processName string, pid string) (string, error) {
	blog.Infof("ListApplicationByNameAndPidUnix with process[%s] pid[%s]", processName, pid)

	command := fmt.Sprintf("ps aux|grep -F \"%s\"|grep -F \"%s\"|grep -v grep", processName, pid)
	cmd := exec.Command("/bin/bash", "-c", command)
	data, err := cmd.Output()
	if err != nil {
		blog.Infof("failed to execute %s for err[%v]", command, err)
		return "", err
	}
	return (string)(data), nil
}

// KillApplicationByNameWindows
func KillApplicationByNameWindows(processName string) error {
	blog.Infof("KillApplicationByNameWindows with process[%s]", processName)

	condition := fmt.Sprintf("imagename eq %s", processName)
	cmd := exec.Command("taskkill", "/f", "/fi", condition)
	err := cmd.Run()
	if err != nil {
		blog.Infof("failed to taskkill /f /fi %s for err[%v]", condition, err)
		return err
	} else {
		blog.Infof("succeed to taskkill /f /fi %s", condition)
		return nil
	}
}

// KillApplicationByNameUnix
func KillApplicationByNameUnix(processName string) error {
	blog.Infof("KillApplicationByNameUnix with process[%s]", processName)

	command := fmt.Sprintf("pkill \"%s\"", processName)
	cmd := exec.Command("/bin/bash", "-c", command)
	err := cmd.Run()
	if err != nil {
		blog.Infof("failed to pkill %s for err[%v]", processName, err)
		return err
	} else {
		blog.Infof("succeed to pkill %s for err[%v]", processName, err)
		return nil
	}
}

// GetTotalMemory get total memory(KB) for windows
func GetTotalMemory() (uint64, error) {
	v, err := mem.VirtualMemory()
	if err != nil {
		return 0, err
	} else {
		return v.Total / 1024, nil
	}
}

// between 0 ~ 100
func GetTotalCPUUsage() (float64, error) {
	per, err := cpu.Percent(0, false)
	if err != nil {
		return 0.0, err
	}

	return per[0], err
}

// Exist check file exist
func Exist(filename string) (bool, error) {
	_, err := os.Stat(filename)
	if err == nil || os.IsExist(err) {
		return true, nil
	}

	return false, err
}
