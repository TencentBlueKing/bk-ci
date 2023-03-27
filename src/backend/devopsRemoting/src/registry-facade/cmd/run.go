package cmd

import (
	"common/cli/check"
	"common/logs"
	"common/pprof"
	"common/watch"
	"context"
	"net"
	"net/http"
	"os"
	"os/signal"
	"path/filepath"
	"registry-facade/pkg/config"
	"registry-facade/pkg/registry"
	"sync"
	"syscall"
	"time"

	"github.com/containerd/containerd/remotes"
	"github.com/containerd/containerd/remotes/docker"
	"github.com/docker/cli/cli/config/configfile"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/collectors"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"github.com/spf13/cobra"
)

var runCmd = &cobra.Command{
	Use:   "run <config.json>",
	Short: "启动registry",
	Args:  check.ExactArgs(1),
	Run: func(_ *cobra.Command, args []string) {
		configPath := args[0]
		cfg, err := config.GetConfig(configPath)
		if err != nil {
			logs.WithError(err).WithField("filename", configPath).Fatal("cannot load config")
		}

		promreg := prometheus.NewRegistry()
		gpreg := prometheus.WrapRegistererWithPrefix("devops_registry_facade_", promreg)
		rtt, err := registry.NewMeasuringRegistryRoundTripper(newDefaultTransport(), prometheus.WrapRegistererWithPrefix("downstream_", gpreg))
		if err != nil {
			logs.WithError(err).Fatal("cannot register metrics")
		}
		if cfg.PrometheusAddr != "" {
			promreg.MustRegister(
				collectors.NewGoCollector(),
				collectors.NewProcessCollector(collectors.ProcessCollectorOpts{}),
				// TODO: http监控
			)

			handler := http.NewServeMux()
			handler.Handle("/metrics", promhttp.HandlerFor(promreg, promhttp.HandlerOpts{}))

			go func() {
				err := http.ListenAndServe(cfg.PrometheusAddr, handler)
				if err != nil {
					logs.WithError(err).Error("Prometheus metrics server failed")
				}
			}()
			logs.WithField("addr", cfg.PrometheusAddr).Info("started Prometheus metrics server")
		}
		if cfg.PProfAddr != "" {
			go pprof.Serve(cfg.PProfAddr)
		}

		var (
			dockerCfg   *configfile.ConfigFile
			dockerCfgMu sync.RWMutex
		)
		if cfg.AuthCfg != "" {
			dockerCfg = loadDockerCfg(cfg.AuthCfg)
		}

		resolverProvider := func() remotes.Resolver {
			client := registry.NewRetryableHTTPClient()
			client.Transport = rtt

			resolverOpts := docker.ResolverOptions{
				Client: client,
			}

			dockerCfgMu.RLock()
			defer dockerCfgMu.RUnlock()
			if dockerCfg != nil {
				resolverOpts.Hosts = docker.ConfigureDefaultRegistries(
					docker.WithAuthorizer(authorizerFromDockerConfig(dockerCfg)),
					docker.WithClient(client),
				)
			}

			return docker.NewResolver(resolverOpts)
		}

		registryDoneChan := make(chan struct{})
		reg, err := registry.NewRegistry(cfg.Registry, resolverProvider, prometheus.WrapRegistererWithPrefix("registry_", gpreg))
		if err != nil {
			logs.WithError(err).Fatal("cannot create registry")
		}

		ctx, cancel := context.WithCancel(context.Background())
		defer cancel()

		err = watch.File(ctx, configPath, func() {
			ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
			defer cancel()

			cfg, err := config.GetConfig(configPath)
			if err != nil {
				logs.WithError(err).Warn("cannot reload configuration")
				return
			}

			err = reg.UpdateStaticLayer(ctx, cfg.Registry.StaticLayer)
			if err != nil {
				logs.WithError(err).Warn("cannot reload configuration")
			}
		})
		if err != nil {
			logs.WithError(err).Fatal("cannot start watch of configuration file")
		}

		err = watch.File(ctx, cfg.AuthCfg, func() {
			dockerCfgMu.Lock()
			defer dockerCfgMu.Unlock()

			dockerCfg = loadDockerCfg(cfg.AuthCfg)
		})
		if err != nil {
			logs.WithError(err).Fatal("cannot start watch of Docker auth configuration file")
		}

		go func() {
			defer close(registryDoneChan)
			reg.MustServe()
		}()

		logs.Info("registry facade is running")
		sigChan := make(chan os.Signal, 1)
		signal.Notify(sigChan, os.Interrupt, syscall.SIGTERM)
		select {
		case <-sigChan:
		case <-registryDoneChan:
		}
	},
}

func loadDockerCfg(path string) *configfile.ConfigFile {
	if tproot := os.Getenv("TELEPRESENCE_ROOT"); tproot != "" {
		path = filepath.Join(tproot, path)
	}
	fr, err := os.OpenFile(path, os.O_RDONLY, 0)
	if err != nil {
		logs.WithError(err).Fatal("cannot read docker auth config")
	}

	dockerCfg := configfile.New(path)
	err = dockerCfg.LoadFromReader(fr)
	fr.Close()
	if err != nil {
		logs.WithError(err).Fatal("cannot read docker config")
	}
	logs.WithField("path", path).Info("using authentication for backing registries")

	return dockerCfg
}

func newDefaultTransport() *http.Transport {
	return &http.Transport{
		Proxy: http.ProxyFromEnvironment,
		DialContext: (&net.Dialer{
			Timeout:   30 * time.Second,
			KeepAlive: 30 * time.Second,
			DualStack: false,
		}).DialContext,
		MaxIdleConns:          0,
		MaxIdleConnsPerHost:   32,
		IdleConnTimeout:       30 * time.Second,
		TLSHandshakeTimeout:   10 * time.Second,
		ExpectContinueTimeout: 5 * time.Second,
		DisableKeepAlives:     true,
	}
}

// FromDockerConfig 把 docker client config 变成 docker registry hosts
func authorizerFromDockerConfig(cfg *configfile.ConfigFile) docker.Authorizer {
	return docker.NewDockerAuthorizer(docker.WithAuthCreds(func(host string) (user, pass string, err error) {
		auth, err := cfg.GetAuthConfig(host)
		if err != nil {
			return
		}
		user = auth.Username
		pass = auth.Password
		return
	}))
}
