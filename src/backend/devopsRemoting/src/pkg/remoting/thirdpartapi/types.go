package thirdpartapi

type DevopsHttpResult struct {
	Data    any    `json:"data"`
	Status  int    `json:"status"`
	Message string `json:"message"`
}

type GetUserGitCredResp struct {
	Cred string `json:"value"`
	Host string `json:"host"`
}

type BackendWorkspaceDetail struct {
	EnvironmentHost string `json:"environmentHost"`
}
