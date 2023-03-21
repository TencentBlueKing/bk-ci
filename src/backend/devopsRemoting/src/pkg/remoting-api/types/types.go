package types

type DevopsHttpResult struct {
	Data    any    `json:"data"`
	Status  int    `json:"status"`
	Message string `json:"message"`
}
