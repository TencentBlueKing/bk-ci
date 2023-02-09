package ports

import (
	"context"
	"devopsRemoting/common/logs"
	"devopsRemoting/src/pkg/remoting/types"
	"fmt"
	"io"
	"net"
	"net/http"
	"net/http/httputil"
	"net/url"
	"reflect"
	"sort"
	"sync"
	"syscall"
	"time"

	"github.com/pkg/errors"
	"golang.org/x/net/nettest"
)

var workspaceIPAdress string

func init() {
	workspaceIPAdress = defaultRoutableIP()
}

type PortsManager struct {
	ExposeService  ExposedPortsInterface
	ServedService  ServedPortsInterface
	TunnelService  TunneledPortsInterface
	DevfileService ConfigInterace

	rwLock sync.RWMutex

	forceUpdates chan struct{}

	internal map[uint32]struct{}
	proxies  map[uint32]*localhostProxy
	// 方便单测
	proxyStarter func(port uint32) (proxy io.Closer, err error)
	autoExposed  map[uint32]*autoExposure

	autoTunneled      map[uint32]struct{}
	autoTunnelEnabled bool

	portConfigs *PortConfigs
	exposed     []ExposedPort
	served      []ServedPort
	tunneled    []PortTunnelState

	state map[uint32]*managedPort

	subscriptions map[*Subscription]struct{}
	closed        bool
}

type localhostProxy struct {
	io.Closer
	proxyPort uint32
}

type autoExposure struct {
	state  types.PortAutoExposure
	ctx    context.Context
	public bool
}

type managedPort struct {
	Served       bool
	Exposed      bool
	Visibility   types.PortVisibility
	Desc         string
	Name         string
	URL          string
	OnOpen       types.PortsStatusOnOpenAction
	AutoExposure types.PortAutoExposure

	LocalhostPort uint32

	Tunneled           bool
	TunneledTargetPort uint32
	TunneledVisibility types.TunnelVisiblity
	TunneledClients    map[string]uint32
}

func NewPortsManager(exposed ExposedPortsInterface, served ServedPortsInterface, devfileService ConfigInterace, tunneled TunneledPortsInterface, internalPorts ...uint32) *PortsManager {
	state := make(map[uint32]*managedPort)
	internal := make(map[uint32]struct{})
	for _, p := range internalPorts {
		internal[p] = struct{}{}
	}

	return &PortsManager{
		ExposeService:  exposed,
		ServedService:  served,
		TunnelService:  tunneled,
		DevfileService: devfileService,

		forceUpdates: make(chan struct{}, 1),

		internal:     internal,
		proxies:      make(map[uint32]*localhostProxy),
		autoExposed:  make(map[uint32]*autoExposure),
		autoTunneled: make(map[uint32]struct{}),

		state:         state,
		subscriptions: make(map[*Subscription]struct{}),
		proxyStarter:  startLocalhostProxy,

		autoTunnelEnabled: true,
	}
}

func (pm *PortsManager) Run(ctx context.Context, wg *sync.WaitGroup) {
	defer wg.Done()
	logs.Debug("portManager start")
	defer logs.Debug("portManager shutdown")

	ctx, cancel := context.WithCancel(ctx)
	defer func() {
		// 我们在关闭之前将订阅复制到一个列表中，以防止数据竞争。
		pm.rwLock.Lock()
		pm.closed = true
		subs := make([]*Subscription, 0, len(pm.subscriptions))
		for s := range pm.subscriptions {
			subs = append(subs, s)
		}
		pm.rwLock.Unlock()

		for _, s := range subs {
			_ = s.Close()
		}
	}()
	defer cancel()

	exposedUpdates, exposedErrors := pm.ExposeService.Observe(ctx)
	servedUpdates, servedErrors := pm.ServedService.Observe(ctx)
	configUpdates, configErrors := pm.DevfileService.Observe(ctx)
	tunneledUpdates, tunneledErrors := pm.TunnelService.Observe(ctx)

	for {
		var (
			exposed     []ExposedPort
			served      []ServedPort
			configured  *PortConfigs
			tunneled    []PortTunnelState
			forceUpdate bool
		)
		select {
		case <-pm.forceUpdates:
			forceUpdate = true
		case exposed = <-exposedUpdates:
			if exposed == nil {
				logs.Error("exposed ports observer stopped")
				return
			}
		case served = <-servedUpdates:
			if served == nil {
				logs.Error("served ports observer stopped")
				return
			}
		case configured = <-configUpdates:
			if configured == nil {
				logs.Error("configured ports observer stopped")
				return
			}
		case tunneled = <-tunneledUpdates:
			if tunneled == nil {
				logs.Error("tunneled ports observer stopped")
				return
			}

		case err := <-exposedErrors:
			if err == nil {
				logs.Error("exposed ports observer stopped")
				return
			}
			logs.WithError(err).Warn("error while observing exposed ports")
		case err := <-servedErrors:
			if err == nil {
				logs.Error("served ports observer stopped")
				return
			}
			logs.WithError(err).Warn("error while observing served ports")
		case err := <-configErrors:
			if err == nil {
				logs.Error("port configs observer stopped")
				return
			}
			logs.WithError(err).Warn("error while observing served port configs")
		case err := <-tunneledErrors:
			if err == nil {
				logs.Error("tunneled ports observer stopped")
				return
			}
			logs.WithError(err).Warn("error while observing tunneled ports")
		}

		if exposed == nil && served == nil && configured == nil && tunneled == nil && !forceUpdate {
			// we received just an error, but no update
			continue
		}
		pm.updateState(ctx, exposed, served, configured, tunneled)
	}
}

func (pm *PortsManager) Status() []*types.PortsStatus {
	pm.rwLock.RLock()
	defer pm.rwLock.RUnlock()

	return pm.getStatus()
}

func (pm *PortsManager) getStatus() []*types.PortsStatus {
	res := make([]*types.PortsStatus, 0, len(pm.state))
	for port := range pm.state {
		res = append(res, pm.getPortStatus(port))
	}
	sort.SliceStable(res, func(i, j int) bool {
		// Max number of port 65536
		score1 := NON_CONFIGED_BASIC_SCORE + res[i].LocalPort
		score2 := NON_CONFIGED_BASIC_SCORE + res[j].LocalPort
		if c, ok := pm.portConfigs.Get(res[i].LocalPort); ok {
			score1 = c.Sort
		}
		if c, ok := pm.portConfigs.Get(res[j].LocalPort); ok {
			score2 = c.Sort
		}
		if score1 != score2 {
			return score1 < score2
		}
		// Ranged ports
		return res[i].LocalPort < res[j].LocalPort
	})
	return res
}

func (pm *PortsManager) getPortStatus(port uint32) *types.PortsStatus {
	mp := pm.state[port]
	ps := &types.PortsStatus{
		LocalPort: mp.LocalhostPort,
		Served:    mp.Served,
		Desc:      mp.Desc,
		Name:      mp.Name,
		OnOpen:    mp.OnOpen,
	}
	if mp.Exposed && mp.URL != "" {
		ps.Exposed = &types.ExposedPortInfo{
			Visibility: mp.Visibility,
			Url:        mp.URL,
		}
	}
	ps.AutoExposure = mp.AutoExposure
	if mp.Tunneled {
		ps.Tunneled = &types.TunneledPortInfo{
			TargetPort: mp.TunneledTargetPort,
			Visibility: mp.TunneledVisibility,
			Clients:    mp.TunneledClients,
		}
	}
	return ps
}

func (pm *PortsManager) updateState(ctx context.Context, exposed []ExposedPort, served []ServedPort, configured *PortConfigs, tunneled []PortTunnelState) {
	pm.rwLock.Lock()
	defer pm.rwLock.Unlock()

	if exposed != nil && !reflect.DeepEqual(pm.exposed, exposed) {
		pm.exposed = exposed
	}

	if tunneled != nil && !reflect.DeepEqual(pm.tunneled, tunneled) {
		pm.tunneled = tunneled
	}

	if served != nil {
		servedMap := make(map[uint32]ServedPort)

		for _, port := range served {
			if _, existProxy := pm.proxies[port.Port]; existProxy && port.Address.String() == workspaceIPAdress {
				// 忽略绑定到工作区 ip 地址的条目
				// 因为它们是由内部反向代理创建的
				continue
			}

			current, exists := servedMap[port.Port]
			if !exists || (!port.BoundToLocalhost && current.BoundToLocalhost) {
				servedMap[port.Port] = port
			}
		}

		var servedKeys []uint32
		for k := range servedMap {
			servedKeys = append(servedKeys, k)
		}
		sort.Slice(servedKeys, func(i, j int) bool {
			return servedKeys[i] < servedKeys[j]
		})

		var newServed []ServedPort
		for _, key := range servedKeys {
			newServed = append(newServed, servedMap[key])
		}

		if !reflect.DeepEqual(pm.served, newServed) {
			logs.WithField("served", newServed).Debug("updating served ports")
			pm.served = newServed
			pm.updateProxies()
			pm.autoTunnel(ctx)
		}
	}

	if configured != nil {
		pm.portConfigs = configured
	}

	newState := pm.nextState(ctx)
	stateChanged := !reflect.DeepEqual(newState, pm.state)
	pm.state = newState

	if !stateChanged && configured == nil {
		return
	}

	status := pm.getStatus()
	logs.WithField("ports", fmt.Sprintf("%+v", status)).Debug("ports changed")
	for sub := range pm.subscriptions {
		select {
		case sub.updates <- status:
		case <-time.After(5 * time.Second):
			logs.Error("ports subscription droped out")
			_ = sub.Close()
		}
	}
}

func (pm *PortsManager) updateProxies() {
	servedPortMap := map[uint32]bool{}
	for _, s := range pm.served {
		servedPortMap[s.Port] = s.BoundToLocalhost
	}

	// 在代理中还有但是在已经端口已经被关闭的则关闭代理
	for port, proxy := range pm.proxies {
		if boundToLocalhost, exists := servedPortMap[port]; !exists || !boundToLocalhost {
			delete(pm.proxies, port)
			err := proxy.Close()
			if err != nil {
				logs.WithError(err).WithField("localPort", port).Warn("cannot stop localhost proxy")
			} else {
				logs.WithField("localPort", port).Info("localhost proxy has been stopped")
			}
		}
	}

	for _, served := range pm.served {
		localPort := served.Port
		_, exists := pm.proxies[localPort]
		if exists || !served.BoundToLocalhost {
			continue
		}

		proxy, err := pm.proxyStarter(localPort)
		if err != nil {
			logs.WithError(err).WithField("localPort", localPort).Warn("cannot start localhost proxy")
			continue
		}
		logs.WithField("localPort", localPort).Info("localhost proxy has been started")

		pm.proxies[localPort] = &localhostProxy{
			Closer:    proxy,
			proxyPort: localPort,
		}
	}
}

// startLocalhostProxy 将所有启动的端口代理到localhost
// 并为每个端口本机IP启动http tcp服务用来访问
func startLocalhostProxy(port uint32) (io.Closer, error) {
	host := fmt.Sprintf("localhost:%d", port)

	dsturl, err := url.Parse("http://" + host)
	if err != nil {
		return nil, errors.Errorf("cannot produce proxy destination URL: %s", err.Error())
	}

	proxy := httputil.NewSingleHostReverseProxy(dsturl)
	originalDirector := proxy.Director
	proxy.Director = func(req *http.Request) {
		req.Host = host
		originalDirector(req)
	}
	proxy.ErrorHandler = func(rw http.ResponseWriter, req *http.Request, err error) {
		rw.WriteHeader(http.StatusBadGateway)

		// avoid common warnings

		if errors.Is(err, context.Canceled) {
			return
		}

		if errors.Is(err, io.EOF) {
			return
		}

		if errors.Is(err, syscall.ECONNREFUSED) {
			return
		}

		var netOpErr *net.OpError
		if errors.As(err, &netOpErr) {
			if netOpErr.Op == "read" {
				return
			}
		}

		logs.WithError(err).WithField("local-port", port).WithField("url", req.URL.String()).Warn("localhost proxy request failed")
	}

	proxyAddr := fmt.Sprintf("%v:%d", workspaceIPAdress, port)
	lis, err := net.Listen("tcp", proxyAddr)
	if err != nil {
		return nil, errors.Errorf("cannot listen on proxy port %d: %s", port, err.Error())
	}

	srv := &http.Server{
		Addr:    proxyAddr,
		Handler: proxy,
	}
	go func() {
		err := srv.Serve(lis)
		if err == http.ErrServerClosed {
			return
		}
		logs.WithError(err).WithField("local-port", port).Error("localhost proxy failed")
	}()

	return srv, nil
}

func (pm *PortsManager) autoTunnel(ctx context.Context) {
	if !pm.autoTunnelEnabled {
		var localPorts []uint32
		for localPort := range pm.autoTunneled {
			localPorts = append(localPorts, localPort)
		}
		// CloseTunnel ensures that everything is closed
		pm.autoTunneled = make(map[uint32]struct{})
		_, err := pm.TunnelService.CloseTunnel(ctx, localPorts...)
		if err != nil {
			logs.WithError(err).Error("cannot close auto tunneled ports")
		}
		return
	}
	var descs []*PortTunnelDescription
	for _, served := range pm.served {
		if pm.boundInternally(served.Port) {
			continue
		}

		_, autoTunneled := pm.autoTunneled[served.Port]
		if !autoTunneled {
			descs = append(descs, &PortTunnelDescription{
				LocalPort:  served.Port,
				TargetPort: served.Port,
				Visibility: types.TunnelVisiblityHost,
			})
		}
	}
	autoTunneled, err := pm.TunnelService.Tunnel(ctx, &TunnelOptions{
		SkipIfExists: true,
	}, descs...)
	if err != nil {
		logs.WithError(err).Error("cannot auto tunnel ports")
	}
	for _, localPort := range autoTunneled {
		pm.autoTunneled[localPort] = struct{}{}
	}
}

func (pm *PortsManager) boundInternally(port uint32) bool {
	_, exists := pm.internal[port]
	return exists
}

func (pm *PortsManager) nextState(ctx context.Context) map[uint32]*managedPort {
	state := make(map[uint32]*managedPort)

	genManagedPort := func(port uint32) *managedPort {
		if mp, exists := state[port]; exists {
			return mp
		}
		config, exists := pm.portConfigs.Get(port)
		var portConfig *types.PortConfig
		if exists && config != nil {
			portConfig = &config.Port
		}
		mp := &managedPort{
			LocalhostPort: port,
			OnOpen:        getOnOpenAction(portConfig, port),
		}
		if exists {
			mp.Name = config.Port.Name
			mp.Desc = config.Port.Desc
		}
		state[port] = mp
		return mp
	}

	// 1. 先处理已经暴露的或者tunnel的端口，因为他们不依赖devfile和served的端口
	for _, exposed := range pm.exposed {
		port := exposed.LocalPort
		if pm.boundInternally(port) {
			continue
		}
		Visibility := types.PortVisibilityPrivate
		if exposed.Public {
			Visibility = types.PortVisibilityPublic
		}
		mp := genManagedPort(port)
		mp.Exposed = true
		mp.Visibility = Visibility
		mp.URL = exposed.URL
	}

	for _, tunneled := range pm.tunneled {
		port := tunneled.Desc.LocalPort
		if pm.boundInternally(port) {
			continue
		}
		mp := genManagedPort(port)
		mp.Tunneled = true
		mp.TunneledTargetPort = tunneled.Desc.TargetPort
		mp.TunneledVisibility = tunneled.Desc.Visibility
		mp.TunneledClients = tunneled.Clients
	}

	// 2. second 处理已经在devfile中配置的端口，防止自动打开再把它打开一遍
	if pm.portConfigs != nil {
		pm.portConfigs.ForEach(func(port uint32, config *SortConfig) {
			if pm.boundInternally(port) {
				return
			}
			mp := genManagedPort(port)
			autoExpose, autoExposed := pm.autoExposed[port]
			if autoExposed {
				mp.AutoExposure = autoExpose.state
			}
			if mp.Exposed || autoExposed {
				return
			}

			mp.Visibility = types.PortVisibilityPrivate
			if config.Port.Visibility == types.PortVisibilityPublicName {
				mp.Visibility = types.PortVisibilityPublic
			}
			public := mp.Visibility == types.PortVisibilityPublic
			mp.AutoExposure = pm.autoExpose(ctx, mp.LocalhostPort, public).state
		})
	}

	// 3. 最后处理served端口
	// 因为我们不想在同一端口上自动公开已经公开的端口
	// 并且需要配置以正确决定默认可见性
	for _, served := range pm.served {
		port := served.Port
		if pm.boundInternally(port) {
			continue
		}
		mp := genManagedPort(port)
		mp.Served = true

		autoExposure, autoExposed := pm.autoExposed[port]
		if autoExposed {
			mp.AutoExposure = autoExposure.state
			continue
		}

		var public bool
		config, exists := pm.portConfigs.Get(mp.LocalhostPort)

		if mp.Exposed || exists {
			public = mp.Visibility == types.PortVisibilityPublic
		} else {
			public = exists && config.Port.Visibility == types.PortVisibilityPublicName
		}

		if mp.Exposed && ((mp.Visibility == types.PortVisibilityPublic && public) || (mp.Visibility == types.PortVisibilityPrivate && !public)) {
			continue
		}

		mp.AutoExposure = pm.autoExpose(ctx, mp.LocalhostPort, public).state
	}

	var ports []uint32
	for port := range state {
		ports = append(ports, port)
	}

	sort.Slice(ports, func(i, j int) bool {
		return ports[i] < ports[j]
	})

	newState := make(map[uint32]*managedPort)
	for _, mp := range ports {
		newState[mp] = state[mp]
	}

	return newState
}

func getOnOpenAction(config *types.PortConfig, port uint32) types.PortsStatusOnOpenAction {
	if config == nil {
		// anything above 32767 seems odd (e.g. used by language servers)
		unusualRange := !(0 < port && port < 32767)
		wellKnown := port <= 10000
		if unusualRange || !wellKnown {
			return types.PortsStatusIgnore
		}
		return types.PortsStatusNotifyPrivate
	}
	if config.OnOpen == types.PortsStatusIgnoreName {
		return types.PortsStatusIgnore
	}
	if config.OnOpen == types.PortsStatusOpenBrowserName {
		return types.PortsStatusOpenBrowser
	}
	if config.OnOpen == types.PortsStatusOpenPreviewName {
		return types.PortsStatusOpenPreview
	}
	return types.PortsStatusNotify
}

// clients should guard a call with check whether such port is already exposed or auto exposed
func (pm *PortsManager) autoExpose(ctx context.Context, localPort uint32, public bool) *autoExposure {
	exposingErr := pm.ExposeService.Expose(ctx, localPort, public)
	autoExpose := &autoExposure{
		state:  types.PortAutoExposureTrying,
		ctx:    ctx,
		public: public,
	}
	go func() {
		err := <-exposingErr
		if err != nil {
			if err != context.Canceled {
				autoExpose.state = types.PortAutoExposureFailed
				logs.WithError(err).WithField("localPort", localPort).Warn("cannot auto-expose port")
			}
			return
		}
		autoExpose.state = types.PortAutoExposureSucceeded
		logs.WithField("localPort", localPort).Info("auto-exposed port")
	}()
	pm.autoExposed[localPort] = autoExpose
	logs.WithField("localPort", localPort).Info("auto-exposing port")
	return autoExpose
}

func defaultRoutableIP() string {
	iface, err := nettest.RoutedInterface("ip", net.FlagUp|net.FlagBroadcast)
	if err != nil {
		return ""
	}

	iface, err = net.InterfaceByName(iface.Name)
	if err != nil {
		return ""
	}

	addresses, err := iface.Addrs()
	if err != nil {
		return ""
	}

	return addresses[0].(*net.IPNet).IP.String()
}
