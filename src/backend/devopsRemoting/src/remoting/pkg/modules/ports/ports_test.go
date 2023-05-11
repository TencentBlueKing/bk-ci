package ports

import (
	"common/logs"
	commonTypes "common/types"
	"context"
	"io"
	"net"
	"remoting/pkg/types"
	"sync"
	"testing"
	"time"

	"github.com/google/go-cmp/cmp"
	"github.com/google/go-cmp/cmp/cmpopts"
	"github.com/sirupsen/logrus"
	"golang.org/x/sync/errgroup"
)

func TestPortsUpdateState(t *testing.T) {
	type ExposureExpectation []ExposedPort
	type UpdateExpectation [][]*types.PortsStatus
	type ConfigChange struct {
		instance []*commonTypes.Port
	}
	type Change struct {
		Config      *ConfigChange
		Served      []ServedPort
		Exposed     []ExposedPort
		Tunneled    []PortTunnelState
		ConfigErr   error
		ServedErr   error
		ExposedErr  error
		TunneledErr error
	}
	tests := []struct {
		Desc             string
		InternalPorts    []uint32
		Changes          []Change
		ExpectedExposure ExposureExpectation
		ExpectedUpdates  UpdateExpectation
	}{
		{
			Desc: "本地被监听的端口自动放开测试",
			Changes: []Change{
				{Served: []ServedPort{{net.IPv4(127, 0, 0, 1), 8080, true}}},
				{Exposed: []ExposedPort{{LocalPort: 8080, URL: "test"}}},
				{Served: []ServedPort{{net.IPv4(127, 0, 0, 1), 8080, true}, {net.IPv4zero, 60000, false}}},
				{Served: []ServedPort{{net.IPv4zero, 60000, false}}},
				{Served: []ServedPort{}},
			},
			ExpectedExposure: []ExposedPort{
				{LocalPort: 8080},
				{LocalPort: 60000},
			},
			ExpectedUpdates: UpdateExpectation{
				{},
				[]*types.PortsStatus{{LocalPort: 8080, Served: true, OnOpen: types.PortsStatusNotifyPrivate}},
				[]*types.PortsStatus{{LocalPort: 8080, Served: true, OnOpen: types.PortsStatusNotifyPrivate, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPrivate, Url: "test"}}},
				[]*types.PortsStatus{{LocalPort: 8080, Served: true, OnOpen: types.PortsStatusNotifyPrivate, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPrivate, Url: "test"}}, {LocalPort: 60000, Served: true}},
				[]*types.PortsStatus{{LocalPort: 8080, Served: false, OnOpen: types.PortsStatusNotifyPrivate, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPrivate, Url: "test"}}, {LocalPort: 60000, Served: true}},
				[]*types.PortsStatus{{LocalPort: 8080, Served: false, OnOpen: types.PortsStatusNotifyPrivate, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPrivate, Url: "test"}}},
			},
		},
		{
			Desc: "0.0.0.0监听的的端口测试",
			Changes: []Change{
				{Served: []ServedPort{{net.IPv4zero, 8080, false}}},
				{Served: []ServedPort{}},
			},
			ExpectedExposure: []ExposedPort{
				{LocalPort: 8080},
			},
			ExpectedUpdates: UpdateExpectation{
				{},
				[]*types.PortsStatus{{LocalPort: 8080, Served: true, OnOpen: types.PortsStatusNotifyPrivate}},
				{},
			},
		},
		{
			Desc: "公开暴露的端口",
			Changes: []Change{
				{Served: []ServedPort{{Port: 8080}}},
				{Exposed: []ExposedPort{{LocalPort: 8080, Public: true, URL: "test"}}},
				{Exposed: []ExposedPort{{LocalPort: 8080, Public: false, URL: "test"}}},
			},
			ExpectedExposure: ExposureExpectation{
				{LocalPort: 8080},
			},
			ExpectedUpdates: UpdateExpectation{
				{},
				[]*types.PortsStatus{{LocalPort: 8080, Served: true, OnOpen: types.PortsStatusNotifyPrivate}},
				[]*types.PortsStatus{{LocalPort: 8080, Served: true, OnOpen: types.PortsStatusNotifyPrivate, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPublic, Url: "test"}}},
				[]*types.PortsStatus{{LocalPort: 8080, Served: true, OnOpen: types.PortsStatusNotifyPrivate, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPrivate, Url: "test"}}},
			},
		},
		{
			Desc:          "内部被监听的端口",
			InternalPorts: []uint32{8080},
			Changes: []Change{
				{Served: []ServedPort{}},
				{Served: []ServedPort{{net.IPv4zero, 8080, false}}},
			},
			ExpectedExposure: ExposureExpectation(nil),
			ExpectedUpdates:  UpdateExpectation{{}},
		},
		{
			Desc: "被配置自动暴露的端口",
			Changes: []Change{
				{
					Config: &ConfigChange{instance: []*commonTypes.Port{
						{Port: 8080},
					}},
				},
				{
					Exposed: []ExposedPort{{LocalPort: 8080, Public: false, URL: "test"}},
				},
				{
					Exposed: []ExposedPort{{LocalPort: 8080, Public: true, URL: "test"}},
				},
				{
					Served: []ServedPort{{net.IPv4(127, 0, 0, 1), 8080, true}},
				},
				{
					Exposed: []ExposedPort{{LocalPort: 8080, Public: true, URL: "test"}},
				},
				{
					Served: []ServedPort{{net.IPv4(127, 0, 0, 1), 8080, true}},
				},
				{
					Served: []ServedPort{},
				},
				{
					Served: []ServedPort{{net.IPv4(127, 0, 0, 1), 8080, false}},
				},
			},
			ExpectedExposure: []ExposedPort{
				{LocalPort: 8080, Public: false},
			},
			ExpectedUpdates: UpdateExpectation{
				{},
				[]*types.PortsStatus{{LocalPort: 8080, OnOpen: types.PortsStatusNotify}},
				[]*types.PortsStatus{{LocalPort: 8080, OnOpen: types.PortsStatusNotify, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPrivate, Url: "test"}}},
				[]*types.PortsStatus{{LocalPort: 8080, OnOpen: types.PortsStatusNotify, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPublic, Url: "test"}}},
				[]*types.PortsStatus{{LocalPort: 8080, Served: true, OnOpen: types.PortsStatusNotify, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPublic, Url: "test"}}},
				[]*types.PortsStatus{{LocalPort: 8080, OnOpen: types.PortsStatusNotify, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPublic, Url: "test"}}},
				[]*types.PortsStatus{{LocalPort: 8080, Served: true, OnOpen: types.PortsStatusNotify, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPublic, Url: "test"}}},
			},
		},
		{
			Desc: "为同一个端口监听事件启动多个代理",
			Changes: []Change{
				{
					Served: []ServedPort{{net.IPv4(127, 0, 0, 1), 8080, true}, {net.IPv4zero, 3000, true}},
				},
			},
			ExpectedExposure: []ExposedPort{
				{LocalPort: 8080},
				{LocalPort: 3000},
			},
			ExpectedUpdates: UpdateExpectation{
				{},
				{
					{LocalPort: 3000, Served: true, OnOpen: types.PortsStatusNotifyPrivate},
					{LocalPort: 8080, Served: true, OnOpen: types.PortsStatusNotifyPrivate},
				},
			},
		},
		{
			Desc: "在自动公开配置和公开更新之间端口被监听",
			Changes: []Change{
				{
					Config: &ConfigChange{instance: []*commonTypes.Port{
						{Port: 8080},
					}},
				},
				{
					Served: []ServedPort{{net.IPv4zero, 8080, false}},
				},
				{
					Exposed: []ExposedPort{{LocalPort: 8080, Public: false, URL: "test"}},
				},
			},
			ExpectedExposure: []ExposedPort{
				{LocalPort: 8080},
			},
			ExpectedUpdates: UpdateExpectation{
				{},
				{{LocalPort: 8080, OnOpen: types.PortsStatusNotify}},
				{{LocalPort: 8080, Served: true, OnOpen: types.PortsStatusNotify}},
				{{LocalPort: 8080, Served: true, OnOpen: types.PortsStatusNotify, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPrivate, Url: "test"}}},
			},
		},
		{
			Desc: "相同的端口在本地和0.0.0.0都被监听，倾向于0.0.0.0，在两次之间暴露",
			Changes: []Change{
				{
					Served: []ServedPort{{net.IPv4(127, 0, 0, 1), 5900, true}},
				},
				{
					Exposed: []ExposedPort{{LocalPort: 5900, URL: "test"}},
				},
				{
					Served: []ServedPort{{net.IPv4(127, 0, 0, 1), 5900, true}, {net.IPv4zero, 5900, false}},
				},
				{
					Exposed: []ExposedPort{{LocalPort: 5900, URL: "test"}},
				},
			},
			ExpectedExposure: []ExposedPort{
				{LocalPort: 5900},
			},
			ExpectedUpdates: UpdateExpectation{
				{},
				{{LocalPort: 5900, Served: true, OnOpen: types.PortsStatusNotifyPrivate}},
				{{LocalPort: 5900, Served: true, OnOpen: types.PortsStatusNotifyPrivate, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPrivate, Url: "test"}}},
			},
		},
		{
			Desc: "相同的端口在本地和0.0.0.0都被监听，倾向于0.0.0.0，在两次之后暴露",
			Changes: []Change{
				{
					Served: []ServedPort{{net.IPv4(127, 0, 0, 1), 5900, true}},
				},
				{
					Served: []ServedPort{{net.IPv4(127, 0, 0, 1), 5900, true}, {net.IPv4zero, 5900, false}},
				},
				{
					Exposed: []ExposedPort{{LocalPort: 5900, URL: "test"}},
				},
				{
					Exposed: []ExposedPort{{LocalPort: 5900, URL: "test"}},
				},
			},
			ExpectedExposure: []ExposedPort{
				{LocalPort: 5900},
			},
			ExpectedUpdates: UpdateExpectation{
				{},
				{{LocalPort: 5900, Served: true, OnOpen: types.PortsStatusNotifyPrivate}},
				{{LocalPort: 5900, Served: true, OnOpen: types.PortsStatusNotifyPrivate, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPrivate, Url: "test"}}},
			},
		},
		{
			Desc: "相同的端口在0.0.0.0和本地都被监听，倾向于0.0.0.0，在两次之间暴露",
			Changes: []Change{
				{
					Served: []ServedPort{{net.IPv4zero, 5900, false}},
				},
				{
					Exposed: []ExposedPort{{LocalPort: 5900, URL: "test"}},
				},
				{
					Served: []ServedPort{{net.IPv4zero, 5900, false}, {net.IPv4(127, 0, 0, 1), 5900, true}},
				},
			},
			ExpectedExposure: []ExposedPort{
				{LocalPort: 5900},
			},
			ExpectedUpdates: UpdateExpectation{
				{},
				{{LocalPort: 5900, Served: true, OnOpen: types.PortsStatusNotifyPrivate}},
				{{LocalPort: 5900, Served: true, OnOpen: types.PortsStatusNotifyPrivate, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPrivate, Url: "test"}}},
			},
		},
		{
			Desc: "相同的端口在0.0.0.0和本地都被监听，倾向于0.0.0.0，在两次之后暴露",
			Changes: []Change{
				{
					Served: []ServedPort{{net.IPv4zero, 5900, false}},
				},
				{
					Served: []ServedPort{{net.IPv4zero, 5900, false}, {net.IPv4(127, 0, 0, 1), 5900, true}},
				},
				{
					Exposed: []ExposedPort{{LocalPort: 5900, URL: "test"}},
				},
			},
			ExpectedExposure: []ExposedPort{
				{LocalPort: 5900},
			},
			ExpectedUpdates: UpdateExpectation{
				{},
				{{LocalPort: 5900, Served: true, OnOpen: types.PortsStatusNotifyPrivate}},
				{{LocalPort: 5900, Served: true, OnOpen: types.PortsStatusNotifyPrivate, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPrivate, Url: "test"}}},
			},
		},
		{
			Desc: "相同的端口在 ip4 本地监听，然后在 ip6 本地监听，首选最先监听的，在两者之间暴露",
			Changes: []Change{
				{
					Served: []ServedPort{{net.IPv4(127, 0, 0, 1), 5900, true}},
				},
				{
					Exposed: []ExposedPort{{LocalPort: 5900, URL: "test"}},
				},
				{
					Served: []ServedPort{{net.IPv4(127, 0, 0, 1), 5900, true}, {net.IPv6zero, 5900, true}},
				},
			},
			ExpectedExposure: []ExposedPort{
				{LocalPort: 5900},
			},
			ExpectedUpdates: UpdateExpectation{
				{},
				{{LocalPort: 5900, Served: true, OnOpen: types.PortsStatusNotifyPrivate}},
				{{LocalPort: 5900, Served: true, OnOpen: types.PortsStatusNotifyPrivate, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPrivate, Url: "test"}}},
			},
		},
		{
			Desc: "相同的端口在 ip4 本地监听，然后在 ip6 本地监听，首选最先监听的，在两者之后暴露",
			Changes: []Change{
				{
					Served: []ServedPort{{net.IPv4(127, 0, 0, 1), 5900, true}},
				},
				{
					Served: []ServedPort{{net.IPv4(127, 0, 0, 1), 5900, true}, {net.IPv6zero, 5900, true}},
				},
				{
					Exposed: []ExposedPort{{LocalPort: 5900, URL: "test"}},
				},
			},
			ExpectedExposure: []ExposedPort{
				{LocalPort: 5900},
			},
			ExpectedUpdates: UpdateExpectation{
				{},
				{{LocalPort: 5900, Served: true, OnOpen: types.PortsStatusNotifyPrivate}},
				{{LocalPort: 5900, Served: true, OnOpen: types.PortsStatusNotifyPrivate, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPrivate, Url: "test"}}},
			},
		},
		{
			Desc: "相同的端口在 ip4 0.0.0.0监听，然后在 ip6 监听，首选最先监听的，在两者之间暴露",
			Changes: []Change{
				{
					Served: []ServedPort{{net.IPv4zero, 5900, false}},
				},
				{
					Exposed: []ExposedPort{{LocalPort: 5900, URL: "test"}},
				},
				{
					Served: []ServedPort{{net.IPv4zero, 5900, false}, {net.IPv6zero, 5900, false}},
				},
			},
			ExpectedExposure: []ExposedPort{
				{LocalPort: 5900},
			},
			ExpectedUpdates: UpdateExpectation{
				{},
				{{LocalPort: 5900, Served: true, OnOpen: types.PortsStatusNotifyPrivate}},
				{{LocalPort: 5900, Served: true, OnOpen: types.PortsStatusNotifyPrivate, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPrivate, Url: "test"}}},
			},
		},
		{
			Desc: "相同的端口在 ip4 0.0.0.0监听，然后在 ip6 监听，首选最先监听的，在两者之后暴露",
			Changes: []Change{
				{
					Served: []ServedPort{{net.IPv4zero, 5900, false}},
				},
				{
					Served: []ServedPort{{net.IPv4zero, 5900, false}, {net.IPv6zero, 5900, false}},
				},
				{
					Exposed: []ExposedPort{{LocalPort: 5900, URL: "test"}},
				},
			},
			ExpectedExposure: []ExposedPort{
				{LocalPort: 5900},
			},
			ExpectedUpdates: UpdateExpectation{
				{},
				{{LocalPort: 5900, Served: true, OnOpen: types.PortsStatusNotifyPrivate}},
				{{LocalPort: 5900, Served: true, OnOpen: types.PortsStatusNotifyPrivate, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPrivate, Url: "test"}}},
			},
		},
		{
			Desc: "如果配置了描述，端口状态会在端口公开后立即设置描述",
			Changes: []Change{
				{
					Config: &ConfigChange{instance: []*commonTypes.Port{
						{Port: 8080, Visibility: "private", Desc: "Development server"},
					}},
				},
				{
					Served: []ServedPort{{net.IPv4zero, 8080, false}},
				},
				{
					Exposed: []ExposedPort{{LocalPort: 8080, Public: false, URL: "test"}},
				},
			},
			ExpectedExposure: []ExposedPort{
				{LocalPort: 8080},
			},
			ExpectedUpdates: UpdateExpectation{
				{},
				{{LocalPort: 8080, Desc: "Development server", OnOpen: types.PortsStatusNotify}},
				{{LocalPort: 8080, Desc: "Development server", Served: true, OnOpen: types.PortsStatusNotify}},
				{{LocalPort: 8080, Desc: "Development server", Served: true, OnOpen: types.PortsStatusNotify, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPrivate, Url: "test"}}},
			},
		},
		{
			Desc: "如果在工作区中配置了名称，端口状态会在端口公开后立即设置名称属性",
			Changes: []Change{
				{
					Config: &ConfigChange{instance: []*commonTypes.Port{
						{Port: 3000, Name: "react"},
					}},
				},
				{
					Served: []ServedPort{{net.IPv4zero, 3000, false}},
				},
				{
					Exposed: []ExposedPort{{LocalPort: 3000, Public: false, URL: "test"}},
				},
			},
			ExpectedExposure: []ExposedPort{
				{LocalPort: 3000},
			},
			ExpectedUpdates: UpdateExpectation{
				{},
				{{LocalPort: 3000, Name: "react", OnOpen: types.PortsStatusNotify}},
				{{LocalPort: 3000, Name: "react", Served: true, OnOpen: types.PortsStatusNotify}},
				{{LocalPort: 3000, Name: "react", Served: true, OnOpen: types.PortsStatusNotify, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPrivate, Url: "test"}}},
			},
		},
		{
			Desc: "改变devfile的port排序",
			Changes: []Change{
				{
					Config: &ConfigChange{instance: []*commonTypes.Port{
						{Port: 3001, Visibility: "private", Name: "react"},
						{Port: 3000, Visibility: "private", Name: "react"},
					}},
				},
				{
					Config: &ConfigChange{instance: []*commonTypes.Port{
						{Port: 3001, Visibility: "private", Name: "react"},
						{Port: 3000, Visibility: "private", Name: "react"},
					}},
				},
				{
					Served: []ServedPort{{net.IPv4zero, 5002, false}},
				},
				{
					Served: []ServedPort{{net.IPv4zero, 5002, false}, {net.IPv4zero, 5001, false}},
				},
				{
					Config: &ConfigChange{instance: []*commonTypes.Port{
						{Port: 3000, Visibility: "private", Name: "react"},
						{Port: 3001, Visibility: "private", Name: "react"},
					}},
				},
				{
					Served: []ServedPort{{net.IPv4zero, 5001, false}, {net.IPv4zero, 3000, false}},
				},
				{
					Exposed: []ExposedPort{{LocalPort: 3000, Public: false, URL: "test"}},
				},
			},
			ExpectedExposure: []ExposedPort{
				{LocalPort: 5002},
				{LocalPort: 5001},
				{LocalPort: 3000},
				{LocalPort: 3001},
			},
			ExpectedUpdates: UpdateExpectation{
				{},
				{
					{LocalPort: 3001, Name: "react", OnOpen: types.PortsStatusNotify},
					{LocalPort: 3000, Name: "react", OnOpen: types.PortsStatusNotify},
				},
				{
					{LocalPort: 3001, Name: "react", OnOpen: types.PortsStatusNotify},
					{LocalPort: 3000, Name: "react", OnOpen: types.PortsStatusNotify},
				},
				{
					{LocalPort: 3001, Name: "react", OnOpen: types.PortsStatusNotify},
					{LocalPort: 3000, Name: "react", OnOpen: types.PortsStatusNotify},
					{LocalPort: 5002, Served: true, OnOpen: types.PortsStatusNotifyPrivate},
				},
				{
					{LocalPort: 3001, Name: "react", OnOpen: types.PortsStatusNotify},
					{LocalPort: 3000, Name: "react", OnOpen: types.PortsStatusNotify},
					{LocalPort: 5001, Served: true, OnOpen: types.PortsStatusNotifyPrivate},
					{LocalPort: 5002, Served: true, OnOpen: types.PortsStatusNotifyPrivate},
				},
				{
					{LocalPort: 3000, Name: "react", OnOpen: types.PortsStatusNotify},
					{LocalPort: 3001, Name: "react", OnOpen: types.PortsStatusNotify},
					{LocalPort: 5001, Served: true, OnOpen: types.PortsStatusNotifyPrivate},
					{LocalPort: 5002, Served: true, OnOpen: types.PortsStatusNotifyPrivate},
				},
				{
					{LocalPort: 3000, Name: "react", Served: true, OnOpen: types.PortsStatusNotify},
					{LocalPort: 3001, Name: "react", OnOpen: types.PortsStatusNotify},
					{LocalPort: 5001, Served: true, OnOpen: types.PortsStatusNotifyPrivate},
				},
				{
					{LocalPort: 3000, Name: "react", Served: true, OnOpen: types.PortsStatusNotify, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPrivate, Url: "test"}},
					{LocalPort: 3001, Name: "react", OnOpen: types.PortsStatusNotify},
					{LocalPort: 5001, Served: true, OnOpen: types.PortsStatusNotifyPrivate},
				},
			},
		},
		{
			Desc: "改变devfile的port排序",
			Changes: []Change{
				{
					Config: &ConfigChange{
						instance: []*commonTypes.Port{
							{Port: 3001, Visibility: "private", Name: "react"},
							{Port: 3000, Visibility: "private", Name: "react"},
						},
					},
				},
				{
					Config: &ConfigChange{
						instance: []*commonTypes.Port{
							{Port: 3003, Visibility: "private", Name: "react"},
							{Port: 3001, Visibility: "private", Name: "react"},
							{Port: 3000, Visibility: "private", Name: "react"},
						},
					},
				},
				{
					Served: []ServedPort{{net.IPv4zero, 3000, false}},
				},
				{
					Served: []ServedPort{{net.IPv4zero, 3000, false}, {net.IPv4zero, 3001, false}, {net.IPv4zero, 3002, false}},
				},
				{
					Config: &ConfigChange{
						instance: []*commonTypes.Port{
							{Port: 3003, Visibility: "private", Name: "react"},
							{Port: 3000, Visibility: "private", Name: "react"},
						},
					},
				},
				{
					Config: &ConfigChange{
						instance: []*commonTypes.Port{
							{Port: 3003, Visibility: "private", Name: "react"},
							{Port: 3000, Visibility: "private", Name: "react"},
						},
					},
				},
			},
			ExpectedExposure: []ExposedPort{
				{LocalPort: 3000},
				{LocalPort: 3001},
				{LocalPort: 3002},
				{LocalPort: 3003},
			},
			ExpectedUpdates: UpdateExpectation{
				{},
				{
					{LocalPort: 3001, Name: "react", OnOpen: types.PortsStatusNotify},
					{LocalPort: 3000, Name: "react", OnOpen: types.PortsStatusNotify},
				},
				{
					{LocalPort: 3003, Name: "react", OnOpen: types.PortsStatusNotify},
					{LocalPort: 3001, Name: "react", OnOpen: types.PortsStatusNotify},
					{LocalPort: 3000, Name: "react", OnOpen: types.PortsStatusNotify},
				},
				{
					{LocalPort: 3003, Name: "react", OnOpen: types.PortsStatusNotify},
					{LocalPort: 3001, Name: "react", OnOpen: types.PortsStatusNotify},
					{LocalPort: 3000, Served: true, Name: "react", OnOpen: types.PortsStatusNotify},
				},
				{
					{LocalPort: 3003, Name: "react", OnOpen: types.PortsStatusNotify},
					{LocalPort: 3001, Served: true, Name: "react", OnOpen: types.PortsStatusNotify},
					{LocalPort: 3000, Served: true, Name: "react", OnOpen: types.PortsStatusNotify},
					{LocalPort: 3002, Served: true, OnOpen: types.PortsStatusNotifyPrivate},
				},
				{
					{LocalPort: 3003, Name: "react", OnOpen: types.PortsStatusNotify},
					{LocalPort: 3000, Served: true, Name: "react", OnOpen: types.PortsStatusNotify},
					{LocalPort: 3001, Served: true, OnOpen: types.PortsStatusNotifyPrivate},
					{LocalPort: 3002, Served: true, OnOpen: types.PortsStatusNotifyPrivate},
				},
				{

					{LocalPort: 3003, Name: "react", OnOpen: types.PortsStatusNotify},
					{LocalPort: 3000, Served: true, Name: "react", OnOpen: types.PortsStatusNotify},
					{LocalPort: 3001, Served: true, OnOpen: types.PortsStatusNotifyPrivate},
					{LocalPort: 3002, Served: true, OnOpen: types.PortsStatusNotifyPrivate},
				},
			},
		},
		{
			// Please make sure this test pass for code browser resolveExternalPort
			// see also https://github.com/gitpod-io/openvscode-server/blob/5ab7644a8bbf37d28e23212bc6f1529cafd8bf7b/extensions/gitpod-web/src/extension.ts#L310-L339
			Desc: "在没有服务的情况下公开端口，应该为 openvscode-server 的用例响应端口",
			Changes: []Change{
				{
					Exposed: []ExposedPort{{LocalPort: 3000, Public: false, URL: "test"}},
				},
			},
			// this will not exposed because test manager didn't implement it properly
			// ExpectedExposure: []ExposedPort{
			// 	{LocalPort: 3000},
			// },
			ExpectedUpdates: UpdateExpectation{
				{},
				{
					{LocalPort: 3000, OnOpen: types.PortsStatusNotifyPrivate, Exposed: &types.ExposedPortInfo{Visibility: types.PortVisibilityPrivate, Url: "test"}},
				},
			},
		},
	}

	logs.Logs.Logger.SetLevel(logrus.FatalLevel)

	for _, test := range tests {
		t.Run(test.Desc, func(t *testing.T) {
			var (
				exposed = &testExposedPorts{
					Changes: make(chan []ExposedPort),
					Error:   make(chan error, 1),
				}
				served = &testServedPorts{
					Changes: make(chan []ServedPort),
					Error:   make(chan error, 1),
				}
				config = &testConfigService{
					Changes: make(chan *PortConfigs),
					Error:   make(chan error, 1),
				}
				tunneled = &testTunneledPorts{
					Changes: make(chan []PortTunnelState),
					Error:   make(chan error, 1),
				}

				pm    = NewPortsManager(exposed, served, config, tunneled, test.InternalPorts...)
				updts [][]*types.PortsStatus
			)
			pm.proxyStarter = func(port uint32) (io.Closer, error) {
				return io.NopCloser(nil), nil
			}

			ctx, cancel := context.WithCancel(context.Background())
			defer cancel()
			var wg sync.WaitGroup
			wg.Add(3)
			go pm.Run(ctx, &wg)
			sub, err := pm.Subscribe()
			if err != nil {
				t.Fatal(err)
			}
			go func() {
				defer wg.Done()
				defer sub.Close()

				for up := range sub.Updates() {
					updts = append(updts, up)
				}
			}()
			go func() {
				defer wg.Done()
				defer close(config.Error)
				defer close(config.Changes)
				defer close(served.Error)
				defer close(served.Changes)
				defer close(exposed.Error)
				defer close(exposed.Changes)
				defer close(tunneled.Error)
				defer close(tunneled.Changes)

				for _, c := range test.Changes {
					if c.Config != nil {
						change := &PortConfigs{}
						portConfigs := parseInstanceConfigs(c.Config.instance)
						change.instancePortConfigs = portConfigs
						config.Changes <- change
					} else if c.ConfigErr != nil {
						config.Error <- c.ConfigErr
					} else if c.Served != nil {
						served.Changes <- c.Served
					} else if c.ServedErr != nil {
						served.Error <- c.ServedErr
					} else if c.Exposed != nil {
						exposed.Changes <- c.Exposed
					} else if c.ExposedErr != nil {
						exposed.Error <- c.ExposedErr
					} else if c.Tunneled != nil {
						tunneled.Changes <- c.Tunneled
					} else if c.TunneledErr != nil {
						tunneled.Error <- c.TunneledErr
					}
				}
			}()

			wg.Wait()

			var (
				sortExposed      = cmpopts.SortSlices(func(x, y ExposedPort) bool { return x.LocalPort < y.LocalPort })
				ignoreUnexported = cmpopts.IgnoreUnexported(
					types.PortsStatus{},
					types.ExposedPortInfo{},
				)
			)
			if diff := cmp.Diff(test.ExpectedExposure, ExposureExpectation(exposed.Exposures), sortExposed, ignoreUnexported); diff != "" {
				t.Errorf("测试失败输出 (-want +got):\n%s", diff)
			}

			if diff := cmp.Diff(test.ExpectedUpdates, UpdateExpectation(updts), ignoreUnexported); diff != "" {
				t.Errorf("测试失败输出 (-want +got):\n%s", diff)
			}
		})
	}
}

type testTunneledPorts struct {
	Changes chan []PortTunnelState
	Error   chan error
}

func (tep *testTunneledPorts) Observe(ctx context.Context) (<-chan []PortTunnelState, <-chan error) {
	return tep.Changes, tep.Error
}
func (tep *testTunneledPorts) Tunnel(ctx context.Context, options *TunnelOptions, descs ...*PortTunnelDescription) ([]uint32, error) {
	return nil, nil
}
func (tep *testTunneledPorts) CloseTunnel(ctx context.Context, localPorts ...uint32) ([]uint32, error) {
	return nil, nil
}
func (tep *testTunneledPorts) EstablishTunnel(ctx context.Context, clientID string, localPort uint32, targetPort uint32) (net.Conn, error) {
	return nil, nil
}

type testConfigService struct {
	Changes chan *PortConfigs
	Error   chan error
}

func (tep *testConfigService) Observe(ctx context.Context) (<-chan *PortConfigs, <-chan error) {
	return tep.Changes, tep.Error
}

type testExposedPorts struct {
	Changes chan []ExposedPort
	Error   chan error

	Exposures []ExposedPort
	mu        sync.Mutex
}

func (tep *testExposedPorts) Observe(ctx context.Context) (<-chan []ExposedPort, <-chan error) {
	return tep.Changes, tep.Error
}

func (tep *testExposedPorts) Run(ctx context.Context) {
}

func (tep *testExposedPorts) Expose(ctx context.Context, local uint32, public bool) <-chan error {
	tep.mu.Lock()
	defer tep.mu.Unlock()

	tep.Exposures = append(tep.Exposures, ExposedPort{
		LocalPort: local,
		Public:    public,
	})
	return nil
}

type testServedPorts struct {
	Changes chan []ServedPort
	Error   chan error
}

func (tps *testServedPorts) Observe(ctx context.Context) (<-chan []ServedPort, <-chan error) {
	return tps.Changes, tps.Error
}

// testing for deadlocks between subscribing and processing events
func TestPortsConcurrentSubscribe(t *testing.T) {
	var (
		subscribes  = 100
		subscribing = make(chan struct{})
		exposed     = &testExposedPorts{
			Changes: make(chan []ExposedPort),
			Error:   make(chan error, 1),
		}
		served = &testServedPorts{
			Changes: make(chan []ServedPort),
			Error:   make(chan error, 1),
		}
		config = &testConfigService{
			Changes: make(chan *PortConfigs),
			Error:   make(chan error, 1),
		}
		tunneled = &testTunneledPorts{
			Changes: make(chan []PortTunnelState),
			Error:   make(chan error, 1),
		}
		pm = NewPortsManager(exposed, served, config, tunneled)
	)
	pm.proxyStarter = func(local uint32) (io.Closer, error) {
		return io.NopCloser(nil), nil
	}

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()
	var wg sync.WaitGroup
	wg.Add(2)
	go pm.Run(ctx, &wg)
	go func() {
		defer wg.Done()
		defer close(config.Error)
		defer close(config.Changes)
		defer close(served.Error)
		defer close(served.Changes)
		defer close(exposed.Error)
		defer close(exposed.Changes)
		defer close(tunneled.Error)
		defer close(tunneled.Changes)

		var j uint32
		for {

			select {
			case <-time.After(50 * time.Millisecond):
				served.Changes <- []ServedPort{{Port: j}}
				j++
			case <-subscribing:
				return
			}
		}
	}()

	eg, _ := errgroup.WithContext(context.Background())
	for i := 0; i < maxSubscriptions; i++ {
		eg.Go(func() error {
			for j := 0; j < subscribes; j++ {
				sub, err := pm.Subscribe()
				if err != nil {
					return err
				}
				// status
				select {
				case <-sub.Updates():
				// update
				case <-sub.Updates():
				}
				sub.Close()
			}
			return nil
		})
	}
	err := eg.Wait()
	close(subscribing)
	if err != nil {
		t.Fatal(err)
	}

	wg.Wait()
}

func TestManager_getStatus(t *testing.T) {
	type portState struct {
		port      uint32
		notServed bool
	}
	type fields struct {
		orderInYaml []int
		state       []portState
	}
	tests := []struct {
		name   string
		fields fields
		want   []uint32
	}{
		{
			name: "正常测试",
			fields: fields{
				// The port number (e.g. 1337) or range (e.g. 3000-3999) to expose.
				orderInYaml: []int{1002, 1000, 1001},
				state:       []portState{{port: 1000}, {port: 1001}, {port: 1002}, {port: 3003}, {port: 3001}, {port: 3002}, {port: 4002}, {port: 4000}, {port: 5000}, {port: 5005}},
			},
			want: []uint32{1002, 1000, 1001, 3001, 3002, 3003, 4000, 4002, 5000, 5005},
		},
		{
			name: "对配置的port ASC排序",
			fields: fields{
				orderInYaml: []int{1002, 1009},
				state:       []portState{{port: 5000}, {port: 1000}, {port: 1009}, {port: 4000}, {port: 4001}, {port: 3000}, {port: 3009}},
			},
			want: []uint32{1009, 1000, 3000, 3009, 4000, 4001, 5000},
		},
		{
			name: "对监听的port ASC排序",
			fields: fields{
				orderInYaml: []int{},
				state:       []portState{{port: 4000}, {port: 4003}, {port: 4007}, {port: 4001}, {port: 4006}},
			},
			want: []uint32{4000, 4001, 4003, 4006, 4007},
		},
		{
			// Please make sure this test pass for code browser resolveExternalPort
			// see also https://github.com/gitpod-io/openvscode-server/blob/5ab7644a8bbf37d28e23212bc6f1529cafd8bf7b/extensions/gitpod-web/src/extension.ts#L310-L339
			name: "公开未服务的端口应响应其状态",
			fields: fields{
				orderInYaml: []int{},
				state:       []portState{{port: 4000, notServed: true}},
			},
			want: []uint32{4000},
		},
		// It will not works because we do not `Run` ports Manger
		// As ports Manger will autoExpose those ports (but not ranged port) in yaml
		// and they will exists in state
		// {
		// 	name: "not ignore ports that not served but exists in yaml",
		// 	fields: fields{
		// 		orderInYaml: []any{1002, 1000, 1001},
		// 		state:       []uint32{},
		// 	},
		// 	want: []uint32{1002, 1000, 1001},
		// },
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			state := make(map[uint32]*managedPort)
			for _, s := range tt.fields.state {
				state[s.port] = &managedPort{
					Served:             !s.notServed,
					LocalhostPort:      s.port,
					TunneledTargetPort: s.port,
					TunneledClients:    map[string]uint32{},
				}
			}
			portsItems := []*commonTypes.Port{}
			for _, port := range tt.fields.orderInYaml {
				portsItems = append(portsItems, &commonTypes.Port{Port: int(port)})
			}
			portsConfig := parseInstanceConfigs(portsItems)
			pm := &PortsManager{
				portConfigs: &PortConfigs{
					instancePortConfigs: portsConfig,
				},
				state: state,
			}
			got := pm.getStatus()
			if len(got) != len(tt.want) {
				t.Errorf("Manager.getStatus() length = %v, want %v", len(got), len(tt.want))
			}
			gotPorts := []uint32{}
			for _, g := range got {
				gotPorts = append(gotPorts, g.LocalPort)
			}
			if diff := cmp.Diff(gotPorts, tt.want); diff != "" {
				t.Errorf("测试失败输出 (-want +got):\n%s", diff)
			}
		})
	}
}
