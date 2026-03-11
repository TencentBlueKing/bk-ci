package clients

var (
	Remoting *RemotingClient
)

func init() {
	Remoting = NewRemotingClient()
}
