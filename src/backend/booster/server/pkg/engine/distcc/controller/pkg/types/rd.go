package types

import (
	"fmt"

	"github.com/Tencent/bk-ci/src/booster/common/types"
)

type RoleType string

const (
	DistCCControllerMaster  RoleType = "master"
	DistCCControllerSlave   RoleType = "slave"
	DistCCControllerUnknown RoleType = "unknown"
)

// Provides the master-change event notifications.
type RoleChangeEvent <-chan RoleType

// DistCCControllerInfo describe the server info of controller
type DistCCControllerInfo struct {
	types.ServerInfo
}

// GetURI return the server url
func (di *DistCCControllerInfo) GetURI() string {
	return fmt.Sprintf("%s://%s:%d", di.Scheme, di.IP, di.Port)
}

const (
	DistCCControllerElectionPath = "controller"
)
