import fetch from './fetch';
import {
  METRICS_API,
  PROCESS_API,
} from './constants';

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
    return fetch.get(`${METRICS_API}/thirdparty/overview/datas/summary/data/get`, params);
  },
  getPipelineSummaryData(params) {
    return fetch.post(`${METRICS_API}/pipeline/overview/datas/summary/data/get`, params);
  },
  getPipelineRunTimeTrend(params) {
    return fetch.post(`${METRICS_API}/pipeline/overview/datas/trend/info`, params);
  },
  getPipelineRunFailTrend(params) {
    return fetch.post(`${METRICS_API}/pipeline/fail/infos/trend/info`, params);
  },
  getPipelineStageTrend(params) {
    return fetch.post(`${METRICS_API}/pipeline/stage/statistics/trend/info`, params);
  },
  getErrorTypeList(params) {
    return fetch.get(`${METRICS_API}/project/info/pipeline/errorType/list`, params);
  },
  getErrorCodeList(params, atomCode) {
    return fetch.get(`${METRICS_API}/errorCode/infos/${atomCode}/list`, params);
  },
  getErrorTypeSummaryData(params) {
    return fetch.post(`${METRICS_API}/pipeline/fail/infos/errorType/summary/data/get`, params);
  },
  getPipelineFailDetail(params, page, pageSize) {
    return fetch.post(`${METRICS_API}/pipeline/fail/infos/details?page=${page}&pageSize=${pageSize}`, params);
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
    return fetch.post(`${METRICS_API}/pipeline/atom/fail/infos/errorCode/statistics/info`, params);
  },
  getErrorCodeInfoDetail(params, page, pageSize) {
    return fetch.post(`${METRICS_API}/pipeline/atom/fail/infos/details?page=${page}&pageSize=${pageSize}`, params);
  },
  getAtomStatisticsTrendInfo(params) {
    return fetch.post(`${METRICS_API}/atom/statistics/trend/info`, params);
  },
  getAtomStatisticsDetail(params, page, pageSize) {
    return fetch.post(`${METRICS_API}/atom/statistics/execute/info?page=${page}&pageSize=${pageSize}`, params);
  },
  getPipelineType({ projectId, pipelineId }) {
    return fetch.get(`${PROCESS_API}/pipelineInfos/${projectId}/searchByPipelineId?pipelineId=${pipelineId}`)
  },
};
