/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package protocol

import (
	"fmt"
	"math/rand"
	"strconv"
	"strings"
	"time"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

// define const vars
const (
	UnknownMessage = "unknown"
)

// CompressType define compress types
type CompressType int32

// define compress types
const (
	CompressNone CompressType = iota
	CompressLZO
	CompressLZ4
	CompressUnknown = 99
)

var (
	compressType2StringMap = map[CompressType]string{
		CompressNone:    "none",
		CompressLZO:     "lzo",
		CompressLZ4:     "lz4",
		CompressUnknown: UnknownMessage,
	}
)

// String return the string of CompressType
func (r CompressType) String() string {
	if v, ok := compressType2StringMap[r]; ok {
		return v
	}

	return UnknownMessage
}

// HostType indicate host is remote or local
type HostType string

// const vars
const (
	HostRemote HostType = "remote"
	HostLocal  HostType = "local"

	LocalHostToken = "localhost"
)

// STring return the string of HostType
func (h HostType) String() string {
	return string(h)
}

// Host define remote host
type Host struct {
	Server       string       `json:"server"`
	TokenString  string       `json:"token_string"`
	Hosttype     HostType     `json:"host_type"`
	Jobs         int          `json:"jobs"`
	Compresstype CompressType `json:"compress_type"`
	Protocol     string       `json:"protocol"`
	TimeDelta    int64        `json:"time_delta"`
}

// Equal to judge whether equal
func (h *Host) Equal(other *Host) bool {
	if other == nil {
		return false
	}

	return h.Server == other.Server &&
		h.Protocol == other.Protocol &&
		h.Compresstype == other.Compresstype
}

// String return the string of host info, generated
func (h *Host) String() string {
	return fmt.Sprintf("server: %s, tokenString: %s, hostType: %s, jobs: %d, compressType: %s, protocol: %s",
		h.Server, h.TokenString, h.Hosttype.String(), h.Jobs, h.Compresstype.String(), h.Protocol)
}

// ObtainCCHosts resolve cc host
func ObtainCCHosts(hoststr string) ([]Host, error) {
	list := ""
	if hoststr != "" {
		list = hoststr
	} else {
		list = env.GetEnv(env.HostList)
	}

	if list == "" {
		return nil, fmt.Errorf("failed to get host by env")
	}

	fields := strings.Fields(list)
	var needrand bool
	hosts := []Host{}
	for _, v := range fields {
		if v == "--randomize" {
			needrand = true
			continue
		}

		// only tcp,not ssh or localhost
		distccfields := strings.FieldsFunc(v, func(s rune) bool {
			if s == '/' || s == ',' {
				return true
			}
			return false
		})

		if len(distccfields) == 0 {
			continue
		}

		host := Host{
			Server:       distccfields[0],
			TokenString:  distccfields[0],
			Hosttype:     HostRemote,
			Jobs:         8,
			Compresstype: CompressNone,
			Protocol:     "tcp",
		}

		if len(distccfields) > 1 {
			jobs, err := strconv.Atoi(distccfields[1])
			if err != nil {
				blog.Errorf("got invalid server [%s]", v)
				continue
			}
			host.Jobs = jobs
			if len(distccfields) > 2 && distccfields[2] == "lzo" {
				host.Compresstype = CompressLZO
			}
		}
		hosts = append(hosts, host)
	}

	if needrand && len(hosts) > 1 {
		rand.Seed(time.Now().UnixNano())
		newindex := rand.Perm(len(hosts))

		newhosts := []Host{}
		for _, v := range newindex {
			newhosts = append(newhosts, hosts[v])
		}
		hosts = newhosts
	}
	return hosts, nil
}
