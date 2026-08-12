import fetch from './fetch';
import {
  METRICS_API,
  PROCESS_API,
} from './constants';
import { getUserTimeZone } from '../../../common-lib/time';

function withTimeZone(params = {}) {
  return {
    ...params,
    timeZone: params.timeZone || getUserTimeZone(),
  };
}

export default {
  getPipelineList(params) {
    params.channelCodes = 'GIT,BS'
    return fetch.get(`${PROCESS_API}/pipelineInfos/get/names`, params);
  },
  getPipelineLabels(params) {
    return fetch.get(`${METRICS_API}/project/info/pipeline/label/list`, params);
  },
  getThirdpartySummaryData(params) {
    if (!params.pipelineLabelIds && params.pipelineLabelIds.length) delete params.pipelineLabelIds
    if (!params.pipelineIds && params.pipelineIds.length) delete params.pipelineIds
    return fetch.get(`${METRICS_API}/thirdparty/overview/datas/summary/data/get`, withTimeZone(params));
  },
  getPipelineSummaryData(params) {
    return fetch.post(`${METRICS_API}/pipeline/overview/datas/summary/data/get`, withTimeZone(params));
  },
  getPipelineRunTimeTrend(params) {
    return fetch.post(`${METRICS_API}/pipeline/overview/datas/trend/info`, withTimeZone(params));
  },
  getPipelineRunFailTrend(params) {
    return fetch.post(`${METRICS_API}/pipeline/fail/infos/trend/info`, withTimeZone(params));
  },
  getPipelineStageTrend(params) {
    return fetch.post(`${METRICS_API}/pipeline/stage/statistics/trend/info`, withTimeZone(params));
  },
  getErrorTypeList(params) {
    return fetch.get(`${METRICS_API}/project/info/pipeline/errorType/list`, params);
  },
  getErrorCodeList(params, atomCode) {
    return fetch.get(`${METRICS_API}/errorCode/infos/${atomCode}/list`, params);
  },
  getErrorTypeSummaryData(params) {
    return fetch.post(`${METRICS_API}/pipeline/fail/infos/errorType/summary/data/get`, withTimeZone(params));
  },
  getPipelineFailDetail(params, page, pageSize) {
    return fetch.post(`${METRICS_API}/pipeline/fail/infos/details?page=${page}&pageSize=${pageSize}`, withTimeZone(params));
  },
  getProjectPluginList(params) {
    return fetch.get(`${METRICS_API}/project/info/atom/list`, params);
  },
  getProjectShowPluginList() {
    return fetch.get(`${METRICS_API}/atom/display/get`);
  },
  getProjectOptionPluginList(params) {
    return fetch.get(`${METRICS_API}/atom/display/optional/get`, params);
  },
  addProjectPlugin(params) {
    return fetch.post(`${METRICS_API}/atom/display/add`, params);
  },
  deleteProjectPlugin(params) {
    return fetch.post(`${METRICS_API}/atom/display/delete`, params);
  },
  getErrorCodeStatisticsInfo(params) {
    return fetch.post(`${METRICS_API}/pipeline/atom/fail/infos/errorCode/statistics/info`, withTimeZone(params));
  },
  getErrorCodeInfoDetail(params, page, pageSize) {
    return fetch.post(`${METRICS_API}/pipeline/atom/fail/infos/details?page=${page}&pageSize=${pageSize}`, withTimeZone(params));
  },
  getAtomStatisticsTrendInfo(params) {
    return fetch.post(`${METRICS_API}/atom/statistics/trend/info`, withTimeZone(params));
  },
  getAtomStatisticsDetail(params, page, pageSize) {
    return fetch.post(`${METRICS_API}/atom/statistics/execute/info?page=${page}&pageSize=${pageSize}`, withTimeZone(params));
  },
  getPipelineType({ projectId, pipelineId }) {
    return fetch.get(`${PROCESS_API}/pipelineInfos/${projectId}/searchByPipelineId?pipelineId=${pipelineId}`)
  },
};
