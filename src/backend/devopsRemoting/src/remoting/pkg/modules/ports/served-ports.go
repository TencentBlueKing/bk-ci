package ports

import (
	"bufio"
	"bytes"
	"common/logs"
	"context"
	"encoding/hex"
	"fmt"
	"io"
	"net"
	"os"
	"sort"
	"strconv"
	"strings"
	"time"
)

type ServedPort struct {
	Address          net.IP
	Port             uint32
	BoundToLocalhost bool
}

type ServedPortsInterface interface {
	Observe(ctx context.Context) (<-chan []ServedPort, <-chan error)
}

const (
	fnNetTCP  = "/proc/net/tcp"
	fnNetTCP6 = "/proc/net/tcp6"
)

// PollingServedPortsObserver 定期轮询“/proc”以观察端口变化。
type PollingServedPortsObserver struct {
	RefreshInterval time.Duration

	fileOpener func(fn string) (io.ReadCloser, error)
}

func (p *PollingServedPortsObserver) Observe(ctx context.Context) (<-chan []ServedPort, <-chan error) {
	if p.fileOpener == nil {
		p.fileOpener = func(fn string) (io.ReadCloser, error) {
			return os.Open(fn)
		}
	}

	var (
		errchan = make(chan error, 1)
		reschan = make(chan []ServedPort)
		ticker  = time.NewTicker(p.RefreshInterval)
	)

	go func() {
		defer close(errchan)
		defer close(reschan)

		for {
			select {
			case <-ctx.Done():
				logs.Info("Port observer stopped")
				return
			case <-ticker.C:
			}

			var (
				visited = make(map[string]struct{})
				ports   []ServedPort
			)

			var protos []string
			for _, path := range []string{fnNetTCP, fnNetTCP6} {
				if _, err := os.Stat(path); err == nil {
					protos = append(protos, path)
				}
			}

			for _, fn := range protos {
				fc, err := p.fileOpener(fn)
				if err != nil {
					errchan <- err
					continue
				}
				ps, err := readNetTCPFile(fc, true)
				fc.Close()

				if err != nil {
					errchan <- err
					continue
				}
				for _, port := range ps {
					key := fmt.Sprintf("%s:%d", hex.EncodeToString(port.Address), port.Port)
					_, exists := visited[key]
					if exists {
						continue
					}
					visited[key] = struct{}{}
					ports = append(ports, port)
				}
			}

			if len(ports) > 0 {
				reschan <- ports
			}
		}
	}()

	return reschan, errchan
}

func readNetTCPFile(fc io.Reader, listeningOnly bool) (ports []ServedPort, err error) {
	scanner := bufio.NewScanner(fc)
	for scanner.Scan() {
		fields := strings.Fields(scanner.Text())
		if len(fields) < 4 {
			continue
		}
		if listeningOnly && fields[3] != "0A" {
			continue
		}

		segs := strings.Split(fields[1], ":")
		if len(segs) < 2 {
			continue
		}
		addrHex, portHex := segs[0], segs[1]

		port, err := strconv.ParseUint(portHex, 16, 32)
		if err != nil {
			logs.WithError(err).WithField("port", portHex).Warn("cannot parse port entry from /proc/net/tcp* file")
			continue
		}
		ipAddress := hexDecodeIP([]byte(addrHex))

		ports = append(ports, ServedPort{
			BoundToLocalhost: ipAddress.IsLoopback(),
			Address:          ipAddress,
			Port:             uint32(port),
		})

		sort.Slice(ports, func(i, j int) bool {
			if ports[i].Address.Equal(ports[j].Address) {
				return ports[i].Port < ports[j].Port
			}
			return bytes.Compare(ports[i].Address, ports[j].Address) < 0
		})

		sort.Slice(ports, func(i, j int) bool {
			return ports[i].Port < ports[j].Port
		})
	}
	if err = scanner.Err(); err != nil {
		return nil, err
	}

	return
}

func hexDecodeIP(src []byte) net.IP {
	buf := make(net.IP, net.IPv6len)

	blocks := len(src) / 8
	for block := 0; block < blocks; block++ {
		for i := 0; i < 4; i++ {
			a := fromHexChar(src[block*8+i*2])
			b := fromHexChar(src[block*8+i*2+1])
			buf[block*4+3-i] = (a << 4) | b
		}
	}
	return buf[:blocks*4]
}

// Converts a hex character into its value.
func fromHexChar(c byte) uint8 {
	switch {
	case '0' <= c && c <= '9':
		return c - '0'
	case 'a' <= c && c <= 'f':
		return c - 'a' + 10
	case 'A' <= c && c <= 'F':
		return c - 'A' + 10
	}
	return 0
}
