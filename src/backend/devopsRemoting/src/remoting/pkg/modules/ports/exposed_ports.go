package ports

import (
	"common/logs"
	commonTypes "common/types"
	"context"
	"fmt"
	"net/url"
	"remoting/pkg/constant"
	"remoting/pkg/thirdpartapi"
)

type ExposedPort struct {
	LocalPort uint32
	URL       string
	Public    bool
}

type ExposedPortsInterface interface {
	// Observe 观察已经裸露的端口
	Observe(ctx context.Context) (<-chan []ExposedPort, <-chan error)

	// Run 监听需要公开暴露的端口的请求
	// TODO: 暂时先不实现，看后续开发进度
	Run(ctx context.Context)

	// Expose 发送暴露一个端口到外部的请求，根据公开类型的不同，私有的由obverve同步列表后由autoexpose处理，公开的需要调用ServerApi由run处理
	Expose(ctx context.Context, port uint32, public bool) <-chan error
}

type RemotingExposedPorts struct {
	WorkspaceId  string
	WorkspaceUrl string

	thirdApi *thirdpartapi.ThirdPartApi

	localExposedPort   []uint32
	localExposedNotice chan struct{}

	requests chan *exposePortRequest
}

type exposePortRequest struct {
	port *commonTypes.WorkspaceInstancePort
	ctx  context.Context
	done chan error
}

func NewRemotingExposedPorts(workspaceId, workspaceUrl string, thirdPartApi *thirdpartapi.ThirdPartApi) *RemotingExposedPorts {
	return &RemotingExposedPorts{
		WorkspaceId:  workspaceId,
		WorkspaceUrl: workspaceUrl,

		thirdApi: thirdPartApi,

		requests:           make(chan *exposePortRequest, 30),
		localExposedNotice: make(chan struct{}, 30),
	}
}

func (g *RemotingExposedPorts) Observe(ctx context.Context) (<-chan []ExposedPort, <-chan error) {
	var (
		reschan = make(chan []ExposedPort)
		errchan = make(chan error, 1)
	)

	go func() {
		defer close(reschan)
		defer close(errchan)

		mixin := func(localExposedPort []uint32) []ExposedPort {
			res := make(map[uint32]ExposedPort)
			for _, port := range g.localExposedPort {
				res[port] = ExposedPort{
					LocalPort: port,
					Public:    false,
					URL:       g.getPortUrl(port),
				}
			}

			exposedPort := make([]ExposedPort, 0, len(res))
			for _, p := range res {
				exposedPort = append(exposedPort, p)
			}
			return exposedPort
		}
		for {
			select {
			case <-g.localExposedNotice:
				res := mixin(g.localExposedPort)
				reschan <- res
			case <-ctx.Done():
				return
			}
		}
	}()

	return reschan, errchan
}

func (g *RemotingExposedPorts) getPortUrl(port uint32) string {
	var u *url.URL
	var err error
	if g.WorkspaceUrl != "" {
		u, err = url.Parse(g.WorkspaceUrl)
		if err != nil {
			return ""
		}
	} else {
		detail, err := g.thirdApi.Server.GetWorkspaceDetail(context.Background(), g.WorkspaceId)
		if err != nil {
			logs.WithError(err).Errorf("port %d request workspace detail error", port)
			return ""
		} else if detail.EnvironmentHost == "" {
			logs.Warnf("port %d request workspace detail host null", port)
			return ""
		}
		wurl := fmt.Sprintf("%s%s", constant.WorkspaceSchema, detail.EnvironmentHost)
		g.WorkspaceUrl = wurl
		u, err = url.Parse(g.WorkspaceUrl)
		if err != nil {
			logs.WithError(err).Errorf("parse workspace url %s error", g.WorkspaceUrl)
			return ""
		}
	}

	u.Host = fmt.Sprintf("%d-%s", port, u.Host)
	return u.String()
}

func (g *RemotingExposedPorts) Expose(ctx context.Context, local uint32, public bool) <-chan error {
	if !public {
		if !g.existInLocalExposed(local) {
			g.localExposedPort = append(g.localExposedPort, local)
			g.localExposedNotice <- struct{}{}
		}
		c := make(chan error)
		close(c)
		return c
	}
	req := &exposePortRequest{
		port: &commonTypes.WorkspaceInstancePort{
			Port:       float64(local),
			Visibility: "public",
		},
		ctx:  ctx,
		done: make(chan error),
	}
	g.requests <- req
	return req.done
}

func (g *RemotingExposedPorts) existInLocalExposed(port uint32) bool {
	for _, p := range g.localExposedPort {
		if p == port {
			return true
		}
	}
	return false
}

func (g *RemotingExposedPorts) Run(ctx context.Context) {
	for {
		select {
		case <-ctx.Done():
			return
		case req := <-g.requests:
			// TODO: 暂时先不实现，看后续开发进度
			logs.WithField("req", req).Warn("recived public ports expose request")
		}
	}
}
