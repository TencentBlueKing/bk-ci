package main

import (
	"common/devops"
	"common/logs"
	"errors"
	"fmt"
	"net"
	"os"
	"path/filepath"
	"time"
	"wsproxy/pkg/config"
	"wsproxy/pkg/constant"
	"wsproxy/pkg/proxy"
	"wsproxy/pkg/sshproxy"

	"github.com/ci-plugins/crypto-go/ssh"
	"github.com/spf13/cobra"
	uzap "go.uber.org/zap"
	"go.uber.org/zap/zapcore"
	"k8s.io/apimachinery/pkg/runtime"
	utilruntime "k8s.io/apimachinery/pkg/util/runtime"
	clientgoscheme "k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/clientcmd"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/log/zap"
)

var scheme = runtime.NewScheme()

func init() {
	utilruntime.Must(clientgoscheme.AddToScheme(scheme))
}

func newCommandRun() *cobra.Command {
	cmd := &cobra.Command{
		Use:   "run <config.json>",
		Short: "Starts ws-proxy",
		Args:  cobra.ExactArgs(1),
		Run: func(_ *cobra.Command, args []string) {
			cfg, err := config.GetConfig(args[0])
			if err != nil {
				fmt.Printf("can not load config %s %s", args[0], err.Error())
				os.Exit(1)
			}

			if cfg.Proxy.LogConfig != nil && cfg.KubemanagerType == string(config.Backend) {
				logs.DefaultInitFileLog(cfg.Proxy.LogConfig.ServiceLogPath, cfg.Proxy.LogConfig.ErrorLogPath, Service, Version, os.Getenv(constant.DebugModEnvName) == "true")
			} else {
				logs.DeafultInitStd(Service, Version, os.Getenv(constant.DebugModEnvName) == "true")
			}

			workspaceInfoProvider, mgr, err := initWorkspaceInfoProvider(cfg)
			if err != nil {
				logs.Fatal("initWorkspaceInfoProvider error", logs.Err(err), logs.String("filename", args[0]))
			}

			var heartbeat sshproxy.Heartbeat
			backendCfg := cfg.DevRemotingBackend
			if backendCfg.HostName != "" && backendCfg.SHA1Key != "" {
				backendClient := devops.NewRemoteDevClient(backendCfg.HostName, backendCfg.SHA1Key)
				heartbeat = &sshproxy.BackendHeartbeat{
					Client: backendClient,
				}
			}

			// ssh网关
			var signers []ssh.Signer
			flist, err := os.ReadDir(cfg.SSHHostKeyPath)
			if err == nil && len(flist) > 0 {
				for _, f := range flist {
					if f.IsDir() {
						continue
					}
					b, err := os.ReadFile(filepath.Join(cfg.SSHHostKeyPath, f.Name()))
					if err != nil {
						continue
					}
					hostSigner, err := ssh.ParsePrivateKey(b)
					if err != nil {
						continue
					}
					signers = append(signers, hostSigner)
				}
				if len(signers) > 0 {
					server := sshproxy.New(signers, workspaceInfoProvider, heartbeat)
					l, err := net.Listen("tcp", ":2200")
					if err != nil {
						panic(err)
					}
					go server.Serve(l)
					logs.Info("SSHGateway is up and running")
				}
			}

			if cfg.KubemanagerType == string(config.KubeApi) {
				go proxy.NewWorkspaceProxy(
					cfg.Ingress,
					cfg.Proxy,
					cfg.DevRemotingBackend,
					proxy.HostBasedRouter(cfg.Ingress.Header, cfg.DevRemotingBackend.WorkspaceHostSuffix),
					workspaceInfoProvider,
					signers,
				).MustServe()
				logs.Infof("started proxying on %s", cfg.Ingress.HTTPAddress)

				if err := mgr.Start(ctrl.SetupSignalHandler()); err != nil {
					logs.Fatal("problem starting ws-proxy", logs.Err(err))
				}

				logs.Info("Received SIGINT - shutting down")
			} else {
				logs.Infof("started proxying on %s", cfg.Ingress.HTTPAddress)
				proxy.NewWorkspaceProxy(
					cfg.Ingress,
					cfg.Proxy,
					cfg.DevRemotingBackend,
					proxy.HostBasedRouter(cfg.Ingress.Header, cfg.DevRemotingBackend.WorkspaceHostSuffix),
					workspaceInfoProvider,
					signers,
				).MustServe()
			}
		},
	}

	return cmd
}

func initWorkspaceInfoProvider(cfg *config.WsPorxyConfig) (proxy.WorkspaceInfoProvider, ctrl.Manager, error) {
	var err error
	switch cfg.KubemanagerType {
	case string(config.KubeApi):
		{
			zapcfg := uzap.NewProductionConfig()
			zapcfg.EncoderConfig.TimeKey = "time"
			zapcfg.EncoderConfig.EncodeTime = func(t time.Time, enc zapcore.PrimitiveArrayEncoder) {
				enc.AppendString(t.Format(time.RFC3339Nano))
			}
			zapopts := zap.Options{
				Development: false,
				Encoder:     zapcore.NewJSONEncoder(zapcfg.EncoderConfig),
			}
			ctrl.SetLogger(zap.New(zap.UseFlagOptions(&zapopts)))

			opts := ctrl.Options{
				Scheme:         scheme,
				Namespace:      cfg.KubeConfig.NameSpace,
				LeaderElection: false,
			}

			var kubeConfig *rest.Config

			checkConfig := func(config string) bool {
				if _, err := os.Stat(config); os.IsNotExist(err) {
					return false
				}
				return true
			}

			if cfg.KubeConfig.KubeConfig != "" && checkConfig(cfg.KubeConfig.KubeConfig) {
				kubeConfig, err = clientcmd.BuildConfigFromFlags("", cfg.KubeConfig.KubeConfig)
				if err != nil {
					logs.Fatal("unable to start manager", logs.Err(err))
				}
			} else {
				kubeConfig = ctrl.GetConfigOrDie()
			}

			mgr, err := ctrl.NewManager(kubeConfig, opts)
			if err != nil {
				logs.Fatal("unable to start manager", logs.Err(err))
			}

			wsInfop := proxy.NewRemoteWorkspaceInfoProvider(mgr.GetClient(), mgr.GetScheme())
			err = wsInfop.SetupWithManager(mgr)
			if err != nil {
				logs.Fatal("unable to create controller", logs.Err(err))
			}

			return wsInfop, mgr, nil
		}
	case string(config.Backend):
		{
			wsInfop, err := proxy.NewBackendWorkspaceInfoProvider(cfg)
			if err != nil {
				return nil, nil, err
			}
			return wsInfop, nil, nil
		}
	default:
		return nil, nil, errors.New("KubemanagerType config error")
	}
}
