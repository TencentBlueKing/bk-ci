/*
* Tencent is pleased to support the open source community by making
* 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition) available.
*
* Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
*
* 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition) is licensed under the MIT License.
*
* License for 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition):
*
* ---------------------------------------------------
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
* documentation files (the "Software"), to deal in the Software without restriction, including without limitation
* the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
* to permit persons to whom the Software is furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all copies or substantial portions of
* the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
* THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
* CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
* IN THE SOFTWARE.
*/
import { createI18n } from 'vue-i18n';
import axios from 'axios';
import cookies from 'js-cookie';
import cnMessages from './zh-CN.json';
import enMessages from './en-US.json';

const DEFAULT_LOCALE = 'zh-CN';
const LS_KEY = 'blueking_language';
const localeAliasMap = {
  'zh-cn': 'zh-CN',
  cn: 'zh-CN',
  'en-us': 'en-US',
  en: 'en-US',
  us: 'en-US',
  // 设置蓝鲸cookie使用
  'zh-CN': 'zh-cn',
  'en-US': 'en',
};

type LocaleAliasUnion = 'zh-CN' | 'en-US' | 'zh-cn' | 'en';
type LocaleAliasMapKeyUnion = keyof typeof localeAliasMap;

const BK_CI_DOMAIN = location.host
  .split('.')
  .slice(1)
  .join('.');

function getLsLocale(): LocaleAliasUnion {
  try {
    const cookieLcale: LocaleAliasMapKeyUnion = (
      cookies.get(LS_KEY) || DEFAULT_LOCALE
    ).toLowerCase() as LocaleAliasMapKeyUnion;
    return (localeAliasMap[cookieLcale] as LocaleAliasUnion) || DEFAULT_LOCALE;
  } catch (error) {
    return DEFAULT_LOCALE;
  }
}

function setLsLocale(locale: LocaleAliasMapKeyUnion) {
  if (typeof cookies.set === 'function') {
    cookies.remove(LS_KEY, { domain: BK_CI_DOMAIN, path: '/' });
    cookies.set(LS_KEY, localeAliasMap[locale], {
      domain: BK_CI_DOMAIN,
      path: '/',
    });
  }
}

export default () => {
  const initLocale: LocaleAliasMapKeyUnion = getLsLocale();
  // export localeList
  const i18n = createI18n({
    locale: initLocale,
    fallbackLocale: DEFAULT_LOCALE,
    legacy: false,
    messages: {
      'zh-CN': cnMessages,
      'en-US': enMessages,
    },
  });

  setLocale(initLocale);

  function setLocale(localeLang: LocaleAliasMapKeyUnion) {
    setLsLocale(localeLang);
    axios.defaults.headers.common['Accept-Language'] = localeLang;
    document.querySelector('html')?.setAttribute('lang', localeLang);

    return localeLang;
  }

  return {
    i18n,
    setLocale,
  };
};
