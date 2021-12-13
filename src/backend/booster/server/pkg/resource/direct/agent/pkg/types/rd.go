/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package types

import (
	"fmt"

	"github.com/Tencent/bk-ci/src/booster/common/types"
)

// RoleType : string of role
type RoleType string

// define role strings
const (
	DistCCServerMaster  RoleType = "master"
	DistCCServerSlave   RoleType = "slave"
	DistCCServerUnknown RoleType = "unknown"
)

// RoleChangeEvent : Provides the master-change event notifications.
type RoleChangeEvent <-chan RoleType

// DistCCServerInfo : types.ServerInfo
type DistCCServerInfo struct {
	types.ServerInfo
}

// GetURI : return uri of server
func (di *DistCCServerInfo) GetURI() string {
	return fmt.Sprintf("%s://%s:%d", di.Scheme, di.IP, di.Port)
}

// define ServerElectionPath to "server"
const (
	ServerElectionPath = "server"
)
