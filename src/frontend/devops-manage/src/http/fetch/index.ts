import { deepMerge } from '@/common/util';
import successInterceptor from './success-interceptor';
import errorInterceptor from './error-interceptor';
import RequestError from './request-error';

export interface IFetchConfig extends RequestInit {
  responseType?: 'json' | 'text' | 'arrayBuffer' | 'blob' | 'formData',
  globalError?: Boolean,
  disabledResponseType?: Boolean,
}

type HttpMethod = (url: string, payload?: any, config?: IFetchConfig) => Promise<any>;

interface IHttp {
  get?: HttpMethod;
  post?: HttpMethod;
  put?: HttpMethod;
  delete?: HttpMethod;
  head?: HttpMethod;
  options?: HttpMethod;
  patch?: HttpMethod;
}

// Content-Type
const contentTypeMap = {
  json: 'application/json',
  text: 'text/plain',
  formData: 'multipart/form-data',
};
const methodsWithoutData = ['delete', 'get', 'head', 'options'];
const methodsWithData = ['post', 'put', 'patch'];
const allMethods = [...methodsWithoutData, ...methodsWithData];

// 拼装发送请求配置
const getFetchConfig = (method: string, payload: any, config: IFetchConfig) => {
  let headers = {
    'X-Requested-With': 'fetch',
    'X-Gateway-Tag': 'kubernetes-dev-rbac',
    'Content-Type': contentTypeMap[config.responseType] || 'application/json'
  }
  if (config.disabledResponseType) {
    delete headers['Content-Type']
  }
  // 合并配置
  let fetchConfig: IFetchConfig = deepMerge(
    {
      method: method.toLocaleUpperCase(),
      mode: 'cors',
      cache: 'default',
      credentials: 'include',
      headers,
      redirect: 'follow',
      referrerPolicy: 'no-referrer-when-downgrade',
      responseType: 'json',
      globalError: true,
    },
    config,
  );
  // merge payload
  const body = config.disabledResponseType ? payload : JSON.stringify(payload)
  if (methodsWithData.includes(method)) {
    fetchConfig = deepMerge(fetchConfig, { body }, payload);
  } else {
    fetchConfig = deepMerge(fetchConfig, payload);
  }
  return fetchConfig;
};

// 拼装发送请求 url
const getFetchUrl = (url: string, method: string, payload = {}) => {
  try {
    // 基础 url
    const baseUrl = location.origin + process.env.BK_AJAX_URL_PREFIX;
    // 构造 url 对象
    const urlObject: URL = new URL(url, baseUrl);
    // get 请求需要将参数拼接到url上
    if (methodsWithoutData.includes(method)) {
      Object.keys(payload).forEach((key) => {
        const value = payload[key];
        if (!['', undefined, null].includes(value)) {
          urlObject.searchParams.append(key, value);
        }
      });
    }
    return urlObject.href;
  } catch (error: any) {
    throw new RequestError(-1, error.message);
  }
};

// 在自定义对象 http 上添加各请求方法
const http: IHttp = {};
allMethods.forEach((method) => {
  Object.defineProperty(http, method, {
    get() {
      return async (url: string, payload: any, config: IFetchConfig = {}) => {
        const fetchConfig: IFetchConfig = getFetchConfig(method, payload, config);
        try {
          const fetchUrl = getFetchUrl(url, method, payload);
          const response = await fetch(fetchUrl, fetchConfig);
          return await successInterceptor(response, fetchConfig);
        } catch (err) {
          return errorInterceptor(err, fetchConfig);
        }
      };
    },
  });
});

export default http;
