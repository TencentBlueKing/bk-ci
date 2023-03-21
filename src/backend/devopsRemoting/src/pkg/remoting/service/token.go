package service

import (
	"context"
	"devopsRemoting/common/logs"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"sync"

	"github.com/pkg/errors"
)

type TokenService struct {
	bkticket Bkticket
}

func InitTokenService() *TokenService {
	return &TokenService{
		bkticket: Bkticket{
			cond:        sync.NewCond(&sync.Mutex{}),
			ticketReady: make(chan struct{}),
			ticket: &BkticktContent{
				Ticket:      "",
				User:        "",
				AccessToken: "",
			},
		},
	}
}

func (service *TokenService) UpdateBkTicket(ctx context.Context, ticket *BkticktRequestBody) error {
	logs.WithField("ticket", ticket).Debug("do update bkticket")

	service.bkticket.cond.L.Lock()
	defer service.bkticket.cond.L.Unlock()
	// preci已经兼容使用ticket作为凭据，不用去拿token了
	// cfg, err := config.GetConfig()
	// if err != nil {
	// 	logs.WithError(err).Error("UpdateBkTicket get config error")
	// 	return err
	// }

	// user, token, err := requestBkAcessToken(ctx, &BkticktContent{
	// 	Ticket:      ticket.Ticket,
	// 	User:        ticket.User,
	// 	AccessToken: "",
	// }, cfg.WorkSpace.PreciGateWayUrl)
	// if err != nil {
	// 	logs.WithError(err).Error("UpdateBkTicket requestBkAcessToken error")
	// 	return err
	// }

	// 重复的token不接受不进行重写
	if ticket.Ticket == service.bkticket.ticket.Ticket {
		logs.Debug("repeat bkticket")
		return nil
	}

	content := &BkticktContent{
		Ticket:      ticket.Ticket,
		User:        ticket.User,
		AccessToken: "",
	}

	service.bkticket.ticket = content
	service.bkticket.markReady()
	service.bkticket.cond.Broadcast()

	logs.WithField("ticket", service.bkticket.ticket).Debug("devops remoting bkticket watcher: updated")

	return nil
}

// ObserveBkticket 返回一个当ticket有更新时返回的chan
func (service *TokenService) ObserveBkticket(ctx context.Context) <-chan *BkticktContent {
	tickets := make(chan *BkticktContent)
	go func() {
		defer close(tickets)

		<-service.bkticket.ticketReady

		service.bkticket.cond.L.Lock()
		defer service.bkticket.cond.L.Unlock()
		for {
			tickets <- service.bkticket.ticket

			service.bkticket.cond.Wait()
			if ctx.Err() != nil {
				return
			}
		}
	}()
	return tickets
}

func requestBkAcessToken(ctx context.Context, ticket *BkticktContent, backendHost string) (user string, token string, err error) {
	url := fmt.Sprintf("%s/project/api/gw/user/accessToken", backendHost)

	req, err := http.NewRequestWithContext(ctx, "GET", url, nil)
	if err != nil {
		return "", "", errors.Wrap(err, "requestBkAcessToken create request error")
	}
	req.AddCookie(&http.Cookie{
		Name:  "bk_ticket",
		Value: ticket.Ticket,
	})

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return "", "", errors.Wrap(err, "requestBkAcessToken do request error")
	}

	respS, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return "", "", errors.Wrap(err, "requestBkAcessToken read resp body error")
	}
	data := &acessTokenRequestResp{}
	err = json.Unmarshal(respS, data)
	if err != nil {
		return "", "", errors.Wrap(err, "requestBkAcessToken json unmarshal")
	}

	if data.Uid == "" || data.AccessToken == "" {
		return "", "", fmt.Errorf("accesstoken resp error %v", data)
	}

	return data.Uid, data.AccessToken, nil
}

type acessTokenRequestResp struct {
	Uid         string `json:"uid"`
	AccessToken string `json:"accessToken"`
	BkTicket    string `json:"bk_ticket"`
}

type Bkticket struct {
	ticket *BkticktContent
	cond   *sync.Cond

	ticketReady chan struct{}
	readyOnce   sync.Once
}

type BkticktRequestBody struct {
	Ticket string `json:"ticket" binding:"required"`
	User   string `json:"user" binding:"required"`
}

type BkticktContent struct {
	Ticket      string `json:"ticket"`
	User        string `json:"user"`
	AccessToken string `json:"accessToken"`
}

// markReady 标志ticket已经准备好传输了，ready只会生效一次
func (service *Bkticket) markReady() {
	service.readyOnce.Do(func() {
		close(service.ticketReady)
	})
}
