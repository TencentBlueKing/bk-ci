package ports

import (
	commonTypes "common/types"
	"context"
	"remoting/pkg/types"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/google/go-cmp/cmp"
)

func TestPortsConfig(t *testing.T) {
	tests := []struct {
		Desc        string
		Devfile     *commonTypes.Devfile
		Expectation *PortConfigTestExpectations
	}{
		{
			Desc:        "无配置测试",
			Expectation: &PortConfigTestExpectations{},
		},
		{
			Desc: "Port端口配置测试",
			Devfile: &commonTypes.Devfile{
				Ports: []*commonTypes.Port{
					{
						Port:       123,
						OnOpen:     "ignore",
						Visibility: "public",
						Name:       "123",
						Desc:       "561231",
					},
				},
			},
			Expectation: &PortConfigTestExpectations{
				InstancePortConfigs: []*types.PortConfig{
					{
						Port:       123,
						OnOpen:     "ignore",
						Visibility: "public",
						Name:       "123",
						Desc:       "561231",
					},
				},
			},
		},
	}
	for _, test := range tests {
		t.Run(test.Desc, func(t *testing.T) {
			configService := &testRemotingConfigService{
				configs: make(chan *commonTypes.Devfile),
			}
			defer close(configService.configs)

			context, cancel := context.WithCancel(context.Background())
			defer cancel()

			ctrl := gomock.NewController(t)
			defer ctrl.Finish()

			service := NewConfigService(configService)
			updates, errors := service.Observe(context)

			actual := &PortConfigTestExpectations{}

			if test.Devfile != nil {
				go func() {
					configService.configs <- test.Devfile
				}()
				select {
				case err := <-errors:
					t.Fatal(err)
				case change := <-updates:
					for _, config := range change.instancePortConfigs {
						actual.InstancePortConfigs = append(actual.InstancePortConfigs, &config.Port)
					}
				}
			}

			if diff := cmp.Diff(test.Expectation, actual); diff != "" {
				t.Errorf("测试失败输出 (-want +got):\n%s", diff)
			}
		})
	}
}

type PortConfigTestExpectations struct {
	InstancePortConfigs []*types.PortConfig
}

type testRemotingConfigService struct {
	configs chan *commonTypes.Devfile
}

func (service *testRemotingConfigService) Watch(ctx context.Context) {
}

func (service *testRemotingConfigService) Observe(ctx context.Context) <-chan *commonTypes.Devfile {
	return service.configs
}
