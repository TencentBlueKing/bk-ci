package ports

import (
	"context"
	"devopsRemoting/src/pkg/remoting/types"
	"net"
	"strconv"
	"sync"

	"github.com/pkg/errors"
)

type PortTunnelDescription struct {
	LocalPort  uint32
	TargetPort uint32
	Visibility types.TunnelVisiblity
}

type PortTunnelState struct {
	Desc    PortTunnelDescription
	Clients map[string]uint32
}

type TunnelOptions struct {
	SkipIfExists bool
}

// TunneledPortsInterface 观察tunnel使用的接口
type TunneledPortsInterface interface {
	Observe(ctx context.Context) (<-chan []PortTunnelState, <-chan error)

	// Tunnel 通知客户端安装监听器，并通过EstablishTunnel转发客户端链接
	Tunnel(ctx context.Context, options *TunnelOptions, descs ...*PortTunnelDescription) ([]uint32, error)

	CloseTunnel(ctx context.Context, localPorts ...uint32) ([]uint32, error)

	// EstablishTunnel 为远程机器传入的链接建立通道
	EstablishTunnel(ctx context.Context, clientID string, localPort uint32, targetPort uint32) (net.Conn, error)
}

type TunneledPortsService struct {
	rwLock  *sync.RWMutex
	cond    *sync.Cond
	tunnels map[uint32]*PortTunnel
}

type PortTunnel struct {
	State PortTunnelState
	Conns map[string]map[net.Conn]struct{}
}

func NewTunneledPortsService(_ bool) *TunneledPortsService {
	var rwLock sync.RWMutex
	return &TunneledPortsService{
		rwLock:  &rwLock,
		cond:    sync.NewCond(&rwLock),
		tunnels: make(map[uint32]*PortTunnel),
	}
}

func (p *TunneledPortsService) Observe(ctx context.Context) (<-chan []PortTunnelState, <-chan error) {
	var (
		errchan = make(chan error, 1)
		reschan = make(chan []PortTunnelState)
	)

	go func() {
		defer close(errchan)
		defer close(reschan)

		p.cond.L.Lock()
		defer p.cond.L.Unlock()
		for {
			var i int
			res := make([]PortTunnelState, len(p.tunnels))
			for _, port := range p.tunnels {
				res[i] = port.State
				i++
			}
			reschan <- res

			p.cond.Wait()
			if ctx.Err() != nil {
				return
			}
		}
	}()

	return reschan, errchan
}

func (p *TunneledPortsService) Tunnel(ctx context.Context, options *TunnelOptions, descs ...*PortTunnelDescription) (tunneled []uint32, err error) {
	var shouldNotify bool
	p.cond.L.Lock()
	defer p.cond.L.Unlock()
	for _, desc := range descs {
		descErr := desc.validate()
		if descErr != nil {
			if err == nil {
				err = descErr
			} else {
				err = errors.Errorf("%s\n%s", err, descErr)
			}
			continue
		}
		tunnel, tunnelExists := p.tunnels[desc.LocalPort]
		if !tunnelExists {
			tunnel = &PortTunnel{
				State: PortTunnelState{
					Clients: make(map[string]uint32),
				},
				Conns: make(map[string]map[net.Conn]struct{}),
			}
			p.tunnels[desc.LocalPort] = tunnel
		} else if options.SkipIfExists {
			continue
		}
		tunnel.State.Desc = *desc
		shouldNotify = true
		tunneled = append(tunneled, desc.LocalPort)
	}
	if shouldNotify {
		p.cond.Broadcast()
	}
	return tunneled, err
}

func (desc *PortTunnelDescription) validate() (err error) {
	if desc.LocalPort <= 0 || desc.LocalPort > 0xFFFF {
		return errors.Errorf("bad local port: %d", desc.LocalPort)
	}
	if desc.TargetPort > 0xFFFF {
		return errors.Errorf("bad target port: %d", desc.TargetPort)
	}
	return nil
}

func (p *TunneledPortsService) CloseTunnel(ctx context.Context, localPorts ...uint32) (closedPorts []uint32, err error) {
	var closed []*PortTunnel
	p.cond.L.Lock()
	for _, localPort := range localPorts {
		tunnel, existsTunnel := p.tunnels[localPort]
		if !existsTunnel {
			continue
		}
		delete(p.tunnels, localPort)
		closed = append(closed, tunnel)
		closedPorts = append(closedPorts, localPort)
	}
	if len(closed) > 0 {
		p.cond.Broadcast()
	}
	p.cond.L.Unlock()
	for _, tunnel := range closed {
		for _, conns := range tunnel.Conns {
			for conn := range conns {
				closeErr := conn.Close()
				if closeErr == nil {
					continue
				}
				if err == nil {
					err = closeErr
				} else {
					err = errors.Errorf("%s\n%s", err, closeErr)
				}
			}
		}
	}
	return closedPorts, err
}

type tunnelConn struct {
	net.Conn
	once       sync.Once
	closeErr   error
	onDidClose func()
}

func (p *TunneledPortsService) EstablishTunnel(ctx context.Context, clientID string, localPort uint32, targetPort uint32) (net.Conn, error) {
	p.cond.L.Lock()
	defer p.cond.L.Unlock()

	tunnel, tunnelExists := p.tunnels[localPort]
	if tunnelExists {
		expectedTargetPort, clientExists := tunnel.State.Clients[clientID]
		if clientExists && expectedTargetPort != targetPort {
			return nil, errors.Errorf("client '%s': %d:%d is already tunneling", clientID, localPort, targetPort)
		}
	} else {
		return nil, errors.Errorf("client '%s': '%d' tunnel does not exist", clientID, localPort)
	}

	addr := net.JoinHostPort("localhost", strconv.FormatInt(int64(localPort), 10))
	conn, err := net.Dial("tcp", addr)
	if err != nil {
		return nil, err
	}
	var result net.Conn
	result = &tunnelConn{
		Conn: conn,
		onDidClose: func() {
			p.cond.L.Lock()
			defer p.cond.L.Unlock()
			_, existsTunnel := p.tunnels[localPort]
			if !existsTunnel {
				return
			}
			delete(tunnel.Conns[clientID], result)
			if len(tunnel.Conns[clientID]) == 0 {
				delete(tunnel.State.Clients, clientID)
			}
			p.cond.Broadcast()
		},
	}
	if tunnel.Conns[clientID] == nil {
		tunnel.Conns[clientID] = make(map[net.Conn]struct{})
	}
	tunnel.Conns[clientID][result] = struct{}{}
	tunnel.State.Clients[clientID] = targetPort
	p.cond.Broadcast()
	return result, nil
}
