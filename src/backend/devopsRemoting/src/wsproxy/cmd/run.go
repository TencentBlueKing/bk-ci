package main

import (
	"common/logs"
	"errors"
	"net"
	"os"
	"path/filepath"
	"wsproxy/pkg/clients"
	"wsproxy/pkg/config"
	"wsproxy/pkg/constant"
	"wsproxy/pkg/proxy"
	"wsproxy/pkg/sshproxy"

	"github.com/bombsimon/logrusr/v2"
	"github.com/ci-plugins/crypto-go/ssh"
	"github.com/spf13/cobra"
	"k8s.io/apimachinery/pkg/runtime"
	utilruntime "k8s.io/apimachinery/pkg/util/runtime"
	clientgoscheme "k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/clientcmd"
	ctrl "sigs.k8s.io/controller-runtime"
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
			logs.Init(Service, Version, true, os.Getenv(constant.DebugModEnvName) == "true")
			cfg, err := config.GetConfig(args[0])
			if err != nil {
				logs.WithError(err).WithField("filename", args[0]).Fatal("cannot load config")
			}

			workspaceInfoProvider, mgr, err := initWorkspaceInfoProvider(cfg)
			if err != nil {
				logs.WithError(err).WithField("filename", args[0]).Fatal("initWorkspaceInfoProvider error")
			}

			var heartbeat sshproxy.Heartbeat
			backendCfg := cfg.DevRemotingBackend
			if backendCfg.HostName != "" && backendCfg.SHA1Key != "" {
				backendClient, err := clients.NewBackendClient(backendCfg.HostName, backendCfg.SHA1Key)
				if err != nil {
					logs.WithError(err).Fatal("create backendclient error")
				}
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
					logs.WithError(err).Fatal(err, "problem starting ws-proxy")
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
			ctrl.SetLogger(logrusr.New(logs.Logs))

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
					logs.WithError(err).Fatal(err, "unable to start manager")
				}
			} else {
				kubeConfig = ctrl.GetConfigOrDie()
			}

			mgr, err := ctrl.NewManager(kubeConfig, opts)
			if err != nil {
				logs.WithError(err).Fatal(err, "unable to start manager")
			}

			wsInfop := proxy.NewRemoteWorkspaceInfoProvider(mgr.GetClient(), mgr.GetScheme())
			err = wsInfop.SetupWithManager(mgr)
			if err != nil {
				logs.WithError(err).Fatal(err, "unable to create controller", "controller", "Pod")
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
