package imagedebug

import (
	"fmt"
	"math/rand"
	"net"
	"strconv"
	"strings"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config"
	"github.com/pkg/errors"
)

const (
	MIN_PORT = 0
	MAX_PORT = 65535
)

type PortAllocator struct {
	maxNodePortAllocRetries int
	nodePortRangeSize       int
	nodePortRangeStart      int
}

func NewPortAllocator() *PortAllocator {
	fi, si, ok := checkPortRangeFormat(config.GAgentConfig.ImageDebugPortRange)
	if !ok {
		fi, si, _ = checkPortRangeFormat(config.DEFAULT_IMAGE_DEBUG_PORT_RANGE)
	}

	return &PortAllocator{
		maxNodePortAllocRetries: si - fi + 1,
		nodePortRangeSize:       si - fi,
		nodePortRangeStart:      fi,
	}
}

func checkPortRangeFormat(rg string) (int, int, bool) {
	rgsub := strings.Split(rg, "-")
	if len(rgsub) != 2 {
		return 0, 0, false
	}

	fi, err := strconv.Atoi(rgsub[0])
	if err != nil {
		return 0, 0, false
	}

	if fi < MIN_PORT {
		return 0, 0, false
	}

	si, err := strconv.Atoi(rgsub[1])
	if err != nil {
		return 0, 0, false
	}

	if si > MAX_PORT {
		return 0, 0, false
	}

	if fi >= si {
		return 0, 0, false
	}

	return fi, si, true
}

func (p *PortAllocator) AllocateNodePort() (int, error) {
	// Allocate a random port within the configured range
	port, err := p.randomUnusedNodePort()
	if err != nil {
		return 0, err
	}

	// Check if the port is already in use
	if isNodePortInUse(port) {
		return 0, fmt.Errorf("node port %d is in use after random", port)
	}

	return port, nil
}

func (p *PortAllocator) randomUnusedNodePort() (int, error) {
	// Allocate a random port within the configured range
	for i := 0; i < p.maxNodePortAllocRetries; i++ {
		port := rand.Intn(int(p.nodePortRangeSize)) + int(p.nodePortRangeStart)
		if !isNodePortInUse(port) {
			return port, nil
		}
	}

	return 0, errors.New("unable to allocate a free node port")
}

func isNodePortInUse(port int) bool {
	// Check if the port is already in use
	listener, err := net.Listen("tcp", fmt.Sprintf(":%d", port))
	if err != nil {
		imageDebugLogs.Infof("Port %d is already in use", port)
		return true
	}
	listener.Close()
	imageDebugLogs.Infof("Port %d is available", port)

	return false
}
