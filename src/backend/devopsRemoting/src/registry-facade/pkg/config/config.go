package config

import (
	"encoding/json"
	"os"
)

type ServiceConfig struct {
	Registry       Config `json:"registry"`
	AuthCfg        string `json:"dockerAuth"`
	PProfAddr      string `json:"pprofAddr"`
	PrometheusAddr string `json:"prometheusAddr"`
}

type Config struct {
	Port               int              `json:"port"`
	Prefix             string           `json:"prefix"`
	StaticLayer        []StaticLayerCfg `json:"staticLayer"`
	RemoteSpecProvider []*RSProvider    `json:"remoteSpecProvider,omitempty"`
	FixedSpecProvider  string           `json:"fixedSpecFN,omitempty"`
	Store              string           `json:"store"`
	RequireAuth        bool             `json:"requireAuth"`
	TLS                *TLS             `json:"tls"`

	// IPFSCache *IPFSCacheConfig `json:"ipfs,omitempty"`
	// RedisCache *RedisCacheConfig `json:"redis,omitempty"`
}

type StaticLayerCfg struct {
	Ref  string `json:"ref"`
	Type string `json:"type"`
}

type RSProvider struct {
	Addr string `json:"addr"`
	TLS  *TLS   `json:"tls,omitempty"`
}
type TLS struct {
	Authority   string `json:"ca"`
	Certificate string `json:"crt"`
	PrivateKey  string `json:"key"`
}

func GetConfig(path string) (*ServiceConfig, error) {
	fc, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}

	var cfg ServiceConfig
	err = json.Unmarshal(fc, &cfg)
	if err != nil {
		return nil, err
	}

	// if cfg.Registry.IPFSCache != nil && cfg.Registry.IPFSCache.Enabled {
	// 	if cfg.Registry.RedisCache == nil || !cfg.Registry.RedisCache.Enabled {
	// 		return nil, xerrors.Errorf("IPFS cache requires Redis")
	// 	}
	// }

	// if cfg.Registry.RedisCache != nil {
	// 	rd := cfg.Registry.RedisCache
	// 	rd.Password = os.Getenv("REDIS_PASSWORD")
	// 	cfg.Registry.RedisCache = rd
	// }

	return &cfg, nil
}
