/*
* Tencent is pleased to support the open source community by making
* 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition) available.
*
* Copyright (C) 2021 Tencent.  All rights reserved.
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
import router from '@/router';
import axios, { AxiosRequestConfig, AxiosResponse } from 'axios';
import { IS_CI_MODE } from '.';

interface ResponseData {
  code: number,
  result: boolean,
  data: any,
  message: string,
}

const instance = axios.create({
  baseURL: '/web',
  validateStatus: (status) => {
    if (status > 400) {
      console.warn(`HTTP 请求出错 status: ${status}`);
    }
    return status >= 200 && status <= 503;
  },
  withCredentials: true,
});

export const pendingRequestMap = new Map();

export function getRequestUnique(config: AxiosRequestConfig) {
  return [
    config.method,
    config.url,
    JSON.stringify(config.params),
    JSON.stringify(config.data),
  ].join('&');
}

function addPendingRequest(request: AxiosRequestConfig) {
  const url = getRequestUnique(request);
  const controller = new AbortController();
  let { signal } = request;
  if (!signal) {
    signal = controller.signal;
  }
  if (!pendingRequestMap.has(url)) { // 如果 pending 中不存在当前请求，则添加进去
    pendingRequestMap.set(url, controller);
  }
  Object.assign(request, {
    signal,
  });
  return controller;
}

function removePendingRequest(request: AxiosRequestConfig) {
  const url = getRequestUnique(request);
  if (pendingRequestMap.has(url)) {
    const controller = pendingRequestMap.get(url);
    controller?.abort();
    pendingRequestMap.delete(url);
  }
}

export function clearPendingRequest() {
  for (const [, controller] of pendingRequestMap) {
    controller?.abort();
  }
  pendingRequestMap.clear();
}

instance.interceptors.request.use((request: AxiosRequestConfig) => {
  removePendingRequest(request);
  addPendingRequest(request);

  return request;
});

instance.interceptors.response.use<ResponseData>((response: AxiosResponse) => {
  const { data, config, status } = response;
  if (status === 401 || status === 402) {
    if (IS_CI_MODE) {
      window.postMessage({
        action: 'toggleLoginDialog',
      }, '*');
      // TODO: 后续优化
      // @ts-ignore
      location.href = window.getLoginUrl();
    } else {
      router.replace({
        name: 'login',
      });
    }
    return Promise.reject({
      message: data.message,
    });
  } else if (data.code !== 0 && config.method !== 'head') {
    return Promise.reject({
      message: data.message,
    });
  }
  return response.data instanceof Blob ? response.data : data.data ?? response;
}, (error) => {
  if (axios.isCancel(error)) {
    console.log('request cancel,', error);
    return Promise.resolve(null);
  }
  return Promise.reject(error);
});

export default instance;

