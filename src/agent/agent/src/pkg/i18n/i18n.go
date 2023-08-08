/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package i18n

import (
	"sync"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/i18n/translation"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
	"github.com/nicksnyder/go-i18n/v2/i18n"
	"golang.org/x/text/language"
)

//go:generate go run ../../cmd/translation_generator/translation_generator.go $I18N_DIR

var localizer *localizerType

var defaultLocalTag = language.Make(config.DEFAULT_LANGUAGE_TYPE)

type localizerType struct {
	nowLocalizer language.Tag
	rwLock       sync.RWMutex
	localizers   map[language.Tag]*i18n.Localizer
}

func (l *localizerType) getLocalizer() *i18n.Localizer {
	l.rwLock.RLock()
	defer l.rwLock.RUnlock()
	local, ok := l.localizers[l.nowLocalizer]
	if !ok {
		// 未找到对应的本地化时默认使用中文
		logs.Warnf("not found nowLocalizer %s", l.nowLocalizer.String())
		return l.localizers[defaultLocalTag]
	}
	return local
}

func InitAgentI18n() {
	localizers := map[language.Tag]*i18n.Localizer{}
	bundle := i18n.NewBundle(language.SimplifiedChinese)
	for lanuage, messages := range translation.Translations {
		// 通过自动生成的校验之后不会出现err，但是如果出现了我们直接跳过
		tag, err := language.Parse(lanuage)
		if err != nil {
			continue
		}
		bundle.AddMessages(tag, messages...)
		localizers[tag] = i18n.NewLocalizer(bundle, tag.String())
	}

	localizer = &localizerType{
		// 初始化时默认为中文
		nowLocalizer: defaultLocalTag,
		rwLock:       sync.RWMutex{},
		localizers:   localizers,
	}
}

func Localize(messageId string, templateData map[string]interface{}) string {
	localizer.rwLock.RLock()
	defer localizer.rwLock.RUnlock()

	nowLocalizer := localizer.getLocalizer()
	if nowLocalizer == nil {
		logs.Error("Localize nowLocalizer is nil")
		return ""
	}

	translation, err := nowLocalizer.Localize(&i18n.LocalizeConfig{
		MessageID:    messageId,
		TemplateData: templateData,
	})
	if err != nil {
		logs.WithError(err).Error("Localize error")
		return ""
	}

	return translation
}

// CheckLocalizer 检查并且切换国际化语言
func CheckLocalizer() {
	newLocal := language.Make(config.GAgentConfig.Language)

	// 先用读锁看一眼，如果一样就不换了
	localizer.rwLock.RLock()
	if localizer.nowLocalizer == newLocal {
		localizer.rwLock.RUnlock()
		return
	}
	localizer.rwLock.RUnlock()

	localizer.rwLock.Lock()
	defer localizer.rwLock.Unlock()

	localizer.nowLocalizer = newLocal
}
