import { deepMerge } from '@/common/util';
import successInterceptor from './success-interceptor';
import errorInterceptor from './error-interceptor';
import RequestError from './request-error';

interface IFetchConfig extends RequestInit {
  responseType?: 'json' | 'text' | 'arrayBuffer' | 'blob' | 'formData',
  globalError?: Boolean
}

type HttpMethod = (url: string, payload?: any, config?: IFetchConfig) => Promise<any>;

interface IHttp {
  get?: HttpMethod,
  post?: HttpMethod,
  put?: HttpMethod,
  delete?: HttpMethod
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
  const execResult = /\/metrics\/([^\/]+)/.exec(location.href);
  // 合并配置
  let fetchConfig: IFetchConfig = deepMerge(
    {
      method: method.toLocaleUpperCase(),
      mode: 'cors',
      cache: 'default',
      credentials: 'include',
      headers: {
        'X-DEVOPS-PROJECT-ID': execResult?.[1] || '',
      },
      redirect: 'follow',
      referrerPolicy: 'no-referrer-when-downgrade',
      responseType: 'json',
      globalError: true,
    },
    config,
  );
  // set Content-Type
  fetchConfig.headers['Content-Type'] = config?.headers?.['Content-Type'] || contentTypeMap[fetchConfig.responseType];
  // merge payload
  if (methodsWithData.includes(method)) {
    fetchConfig = deepMerge(fetchConfig, { body: JSON.stringify(payload) });
  } else {
    fetchConfig = deepMerge(fetchConfig, payload);
  }
  return fetchConfig;
};

// 拼装发送请求 url
const getFetchUrl = (url, method, payload = {}) => {
  try {
    // 基础 url
    const baseUrl = /http(s)?:\/\//.test(import.meta.env.VITE_AJAX_URL_PREFIX)
      ? import.meta.env.VITE_AJAX_URL_PREFIX
      : location.origin + import.meta.env.VITE_AJAX_URL_PREFIX;
    // 构造 url 对象
    const urlObject: URL = new URL(url, baseUrl);
    if (methodsWithoutData.includes(method)) {
      Object.keys(payload).forEach((key) => {
        const value = payload[key];
        if (!['', undefined, null].includes(value)) {
          urlObject.searchParams.append(key, value);
        }
      });
    }
    return urlObject.href;
  } catch (error) {
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
