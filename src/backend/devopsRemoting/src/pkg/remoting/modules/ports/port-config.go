package ports

import (
	"context"
	commonTypes "devopsRemoting/common/types"
	"devopsRemoting/src/pkg/remoting/config"
	"devopsRemoting/src/pkg/remoting/types"
	"fmt"
	"reflect"
	"strconv"
)

const NON_CONFIGED_BASIC_SCORE = 100000

type SortConfig struct {
	Port types.PortConfig
	Sort uint32
}

type PortConfigs struct {
	instancePortConfigs map[uint32]*SortConfig
}

func (configs *PortConfigs) ForEach(callback func(port uint32, config *SortConfig)) {
	if configs == nil {
		return
	}
	visited := make(map[uint32]struct{})
	for port, config := range configs.instancePortConfigs {
		_, exists := visited[port]
		if exists {
			continue
		}
		visited[port] = struct{}{}
		callback(port, config)
	}
}

func (configs *PortConfigs) Get(port uint32) (*SortConfig, bool) {
	if configs == nil {
		return nil, false
	}
	config, exists := configs.instancePortConfigs[port]
	if exists {
		return config, true
	}

	return nil, false
}

type ConfigInterace interface {
	Observe(ctx context.Context) (<-chan *PortConfigs, <-chan error)
}

type ConfigService struct {
	devfileService config.DevfileConfigInterface
}

func NewConfigService(devfileService config.DevfileConfigInterface) *ConfigService {
	return &ConfigService{
		devfileService: devfileService,
	}
}

func (service *ConfigService) Observe(ctx context.Context) (<-chan *PortConfigs, <-chan error) {
	updatesChan := make(chan *PortConfigs)
	errorsChan := make(chan error, 1)

	go func() {
		defer close(updatesChan)
		defer close(errorsChan)

		configs := service.devfileService.Observe(ctx)

		current := &PortConfigs{}

		for {
			select {
			case <-ctx.Done():
				return
			case config, ok := <-configs:
				if !ok {
					return
				}
				changed := service.update(config, current)
				if !changed {
					continue
				}
				updatesChan <- &PortConfigs{
					instancePortConfigs: current.instancePortConfigs,
				}
			}
		}
	}()
	return updatesChan, errorsChan
}

func (service *ConfigService) update(config *commonTypes.Devfile, current *PortConfigs) bool {
	currentPortConfigs := current.instancePortConfigs
	var ports []*commonTypes.Port
	if config != nil {
		ports = config.Ports
	}
	portConfigs := parseInstanceConfigs(ports)
	current.instancePortConfigs = portConfigs
	return !reflect.DeepEqual(currentPortConfigs, portConfigs)
}

func parseInstanceConfigs(ports []*commonTypes.Port) (portConfigs map[uint32]*SortConfig) {
	for index, config := range ports {
		if config == nil {
			continue
		}

		rawPort := fmt.Sprintf("%v", config.Port)
		Port, err := strconv.ParseUint(rawPort, 10, 16)
		if err == nil {
			if portConfigs == nil {
				portConfigs = make(map[uint32]*SortConfig)
			}
			port := uint32(Port)
			_, exists := portConfigs[port]
			if !exists {
				portConfigs[port] = &SortConfig{
					Port: types.PortConfig{
						OnOpen:     config.OnOpen,
						Port:       float64(Port),
						Visibility: config.Visibility,
						Desc:       config.Desc,
						Name:       config.Name,
					},
					Sort: uint32(index),
				}
			}
			continue
		}
	}
	return portConfigs
}
