package types

type CommandStatus struct {
	Id       CommandType  `json:"id"`
	State    CommandState `json:"state"`
	Terminal string       `json:"terminal"`
}

type CommandState int

const (
	CommandOpening CommandState = iota
	CommandRunning
	CommandClosed
)

type CommandType string

const (
	PostCreateCommand CommandType = "postCreateCommand"
)
