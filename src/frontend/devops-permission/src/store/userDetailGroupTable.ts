import http from '@/http/api';
import { defineStore } from 'pinia';
import { ref } from 'vue';
import pipelineIcon from '@/css/svg/color-logo-pipeline.svg';
import codelibIcon from '@/css/svg/color-logo-codelib.svg';
import codeccIcon from '@/css/svg/color-logo-codecc.svg';
import environmentIcon from '@/css/svg/color-logo-environment.svg';
import experienceIcon from '@/css/svg/color-logo-experience.svg';
import qualityIcon from '@/css/svg/color-logo-quality.svg';
import ticketIcon from '@/css/svg/color-logo-ticket.svg';
import turboIcon from '@/css/svg/color-logo-turbo.svg';

enum HandoverType {
  AUTHORIZATION = 'AUTHORIZATION',
  GROUP = 'GROUP'
}
interface GroupTableType {
  groupName?: String;
  iamGroupId?: Number;
  projectCode?: String;
  resourceCode: String;
  resourceName: String;
  handoverType?: HandoverType;
  handoverFrom?: String;
  groupDesc?: String;

};
interface Pagination {
  limit: number;
  current: number;
  count: number;
}

interface SourceType {
  activeFlag?: boolean;
  count?: number;
  pagination: Pagination;
  resourceType: string;
  resourceTypeName?: string;
  tableData: GroupTableType[];
  tableLoading?: boolean;
  type: HandoverType;
}

interface CollapseListType {
  resourceType: string;
  resourceTypeName: string;
  count: number;
  type: HandoverType;
}

interface DetailParams {
  projectCode?: String;
  resourceType?: String,
  batchOperateType?: String;
  previewConditionReq?: String;
  queryChannel?: String;
  page?: Number,
  pageSize?: Number,
  flowNo?: String
}

export default defineStore('userDetailGroupTable', () => {
  const isLoading = ref(true);
  const detailSourceList = ref<SourceType[]>([]);
  const collapseList = ref<CollapseListType[]>([]);
  let currentRequestId = 0;
  const detailParams = ref<DetailParams>();

  /**
   * 获取资源授权管理数量
   */
  async function getCollapseList() {
    try {
      const params = detailParams.value;
      const res = await http.getResourceType2CountOfHandover(params);
      collapseList.value = res;
      detailSourceList.value = collapseList.value.map(item => ({
        ...item,
        tableLoading: false,
        pagination: { count: 0, current: 1, limit: 10 },
        tableData: [],
      }));
    } catch (error) {
      console.log(error);
    }
  }

  async function fetchListData(fetchFunction: (item: DetailParams) => Promise<any>, item: SourceType) {
    if (!collapseList.value.some(collapseItem => collapseItem.resourceType === item.resourceType)) {
      return {};
    }
    try {
      const params: DetailParams = {
        ...detailParams.value,
        resourceType: item.resourceType,
        page: item.pagination.current,
        pageSize: item.pagination.limit,
      };
      return await fetchFunction(params);
    } catch (error) {
      console.log(error);
      return {};
    }
  }
  /**
   *  获取交接单中授权相关
   * @param resourceType 资源类型
   */
  async function getAuthorizationsList(item: SourceType) {
    return fetchListData(http.listAuthorizationsOfHandover, item);
  }
  /**
   * 获取交接单中用户组相关
   * @param resourceType 资源类型
   */
  async function getGroupList(item: SourceType) {
    return fetchListData(http.listGroupsOfHandover, item);
  }
  /**
   * 获取页面数据
   */
  async function fetchDetailList(projectIdParam: DetailParams) {
    // 初始化数据
    detailSourceList.value = [];

    detailParams.value = projectIdParam;
    const requestId = ++currentRequestId;

    // 加载权限管理数量
    await getCollapseList();

    try {
      isLoading.value = true;

      const [authorizationItem, userGroupItem] = [
        detailSourceList.value.find(item => item.type === HandoverType.AUTHORIZATION),
        detailSourceList.value.find(item => item.type === HandoverType.GROUP)
      ];
      // 同时获取授权列表和用户组列表
      const [authorizationList, userGroupList] = await Promise.all([
        authorizationItem ? getAuthorizationsList(authorizationItem) : Promise.resolve(null),
        userGroupItem ? getGroupList(userGroupItem) : Promise.resolve(null)
      ]);

      if (currentRequestId === requestId) {
        detailSourceList.value.forEach(item => {
          if (authorizationList && item === authorizationItem) {
            item.tableData = authorizationList.records;
            item.activeFlag = true;
          }
          if (userGroupList && item === userGroupItem) {
            item.tableData = userGroupList.records;
            item.activeFlag = true;
          }
          item.pagination.count = item.count!;
        });
      }
    } catch (error) {
      console.log(error);
    } finally {
      isLoading.value = false;
    }
  }

  async function handlePaginationChange(fetchFunction: (item: SourceType) => Promise<any>, item: SourceType) {
    try {
      item.tableLoading = true;
      const res = await fetchFunction(item);
      item.tableData = res.records;
    } catch (error) {
      console.log(error);
    } finally {
      item.tableLoading = false;
    }
  }
  /**
   * 折叠面板调用接口获取表格数据
   */
  async function detailCollapseClick(resourceType: string, flag: HandoverType) {
    const item = detailSourceList.value.find(item => item.resourceType === resourceType && item.type === flag);
    console.log(item, 'item')
    if (!item || !item.count || item.tableData.length) return;

    item.pagination.current = 1;
    await handlePaginationChange(flag === HandoverType.GROUP ? getGroupList : getAuthorizationsList, item);
  }
  /**
   * 切换表格每页显示条数时
   */
  async function detailPageLimitChange(limit: number, resourceType: string, flag: HandoverType) {
    const item = detailSourceList.value.find(item => item.resourceType === resourceType && item.type === flag);
    if (item) {
      item.pagination.limit = limit;
      await handlePaginationChange(flag === HandoverType.GROUP ? getGroupList : getAuthorizationsList, item);
    }
  }
  /**
   * 切换表格分页时
   */
  async function detailPageValueChange(value: number, resourceType: string, flag: HandoverType) {
    const item = detailSourceList.value.find(item => item.resourceType === resourceType && item.type === flag);
    if (item) {
      item.pagination.current = value;
      await handlePaginationChange(flag === HandoverType.GROUP ? getGroupList : getAuthorizationsList, item);
    }
  }

  function getServiceIcon (type: string) {
    const iconMap = {
      'pipeline': pipelineIcon,
      'pipeline_group': pipelineIcon,
      'repertory': codelibIcon,
      'credential': ticketIcon,
      'cert': ticketIcon,
      'environment': environmentIcon,
      'env_node': pipelineIcon,
      'codecc_task': codeccIcon,
      'codecc_rule_set': codeccIcon,
      'codecc_ignore_type': codeccIcon,
      'experience_task': experienceIcon,
      'experience_group': experienceIcon,
      'rule': qualityIcon,
      'quality_group': qualityIcon,
      'pipeline_template': pipelineIcon,
    }
    return iconMap[type]
  }

  return {
    isLoading,
    detailSourceList,
    collapseList,
    fetchDetailList,
    detailCollapseClick,
    detailPageLimitChange,
    detailPageValueChange,
    getServiceIcon,
  };
});
