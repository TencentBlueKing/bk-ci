import http from '@/http/api';
import { defineStore } from 'pinia';
import { useRoute } from 'vue-router';
import { ref, reactive, computed, watch } from 'vue';
import { useI18n } from 'vue-i18n';

interface GroupTableType {
  resourceCode: string,
  resourceName: string,
  resourceType: string,
  groupId: number;
  groupName: string;
  groupDesc: string;
  expiredAtDisplay: string;
  joinedTime: number;
  expiredAt: number,
  operateSource: string;
  operator: string;
  removeMemberButtonControl: 'OTHER' | 'TEMPLATE' | 'UNIQUE_MANAGER' | 'UNIQUE_OWNER';
  memberType: string;
};
interface Pagination {
  limit: number;
  current: number;
  count: number;
}
interface SourceType {
  pagination?: Pagination;
  count?: number;
  isAll?: boolean;
  remainingCount?: number;
  resourceTypeName?: string;
  resourceType: string,
  hasNext?: boolean,
  activeFlag?: boolean;
  tableLoading?: boolean;
  scrollLoading?: boolean;
  isRemotePagination?: boolean;
  tableData: GroupTableType[];
}
interface SelectedDataType {
  [key: string]: GroupTableType[];
}
interface CollapseListType {
  resourceType: string;
  resourceTypeName: string;
  count: number;
}
export interface AsideItem {
  id: string,
  name: string,
  checked: boolean,
  type: "department" | "user"
}
export interface SearchParamsType {
  groupName?: string,
  minExpiredAt?: number,
  maxExpiredAt?: number,
}

export default defineStore('userGroupTable', () => {
  const { t } = useI18n();
  const route = useRoute();
  const isLoading = ref(true);

  const projectId = computed(() => (route.params?.projectCode || route.query?.projectCode) as string) ;
  const paginations = ref({})

  const sourceList = ref<SourceType[]>([]);
  const collapseList = ref<CollapseListType[]>([]);
  const memberItem = ref<AsideItem>();

  const isShowRenewal = ref(false);
  const isShowHandover = ref(false);
  const isShowRemove = ref(false);
  const selectedData = reactive<SelectedDataType>({});
  const selectSourceList = ref<SourceType[]>([]);
  const selectedRow = ref<GroupTableType | null>(null);
  const rowIndex = ref<number>();
  const selectedTableGroupType = ref('');
  const selectedLength = ref(0);
  const isPermission = ref(true);
  let currentRequestId = 0;
  const seacrhObj = ref<SearchParamsType>();

  watch(selectedData, ()=>{
    getSourceList()
  })

  /**
   * 初始化数据
   */
  function initData() {
    selectedRow.value = null;
    rowIndex.value = undefined;
    selectedTableGroupType.value = '';
    sourceList.value = [];
    selectSourceList.value = [];
    selectedLength.value = 0;
    Object.keys(selectedData).forEach(key => {
      delete selectedData[key];
    });
  }
  /**
   * 获取项目成员有权限的用户组数量
   */
  async function getCollapseList(memberId: string, seacrhObj: SearchParamsType) {
    paginations.value = []
    try {
      const query = {
        memberId,
        ...seacrhObj,
      }
      const res = await http.getMemberGroups(projectId.value, query);
      collapseList.value = res;
      if(res.length){
        isPermission.value = true;
      } else {
        isPermission.value = false;
      }
      sourceList.value = collapseList.value.map((item) => ({
        ...item,
        tableLoading: false,
        scrollLoading: false,
        isRemotePagination: true,
        tableData: [],
      }))

      collapseList.value.map(i => i.resourceType).forEach(i => {
        paginations.value[i] = [0, 10]
      })
    } catch (error) {
      console.log(error);
    }
  }
  /**
   * 获取项目成员有权限的用户组
   * @param resourceType 资源类型
   */
  async function getGroupList(resourceType: string, seacrhObj?) {
    if (!collapseList.value.some(item => item.resourceType === resourceType)) {
      return {};
    }
    try {
      const params = {
        memberId: memberItem.value!.id,
        start: paginations.value[resourceType][0],
        limit: paginations.value[resourceType][1],
        ...seacrhObj,
      }
      return await http.getMemberGroupsDetails(projectId.value, resourceType, params);
    } catch (error) {
      console.log(error);
    }
  }
  /**
   * 获取项目成员页面数据
   */
  async function fetchUserGroupList(asideItem: AsideItem, seacrhParams: SearchParamsType) {
    const requestId = ++currentRequestId;
    initData();
    seacrhObj.value = seacrhParams
    asideItem && await getCollapseList(asideItem.id, seacrhParams);
    memberItem.value = asideItem;
    try {
      isLoading.value = true;
      const resourceTypes = sourceList.value.map(i => i.resourceType).slice(0, 2);
      const results = await Promise.all(
        resourceTypes.map(resourceType => getGroupList(resourceType, seacrhParams))
      );
      if(currentRequestId === requestId) {
        sourceList.value.forEach((item, index) => {
          if((index === 0 || index === 1) && results[index]) {
            item.tableData = results[index].records;
            item.activeFlag = true;
          }
        })
        isLoading.value = false;
      }
    } catch (error: any) {
      isLoading.value = false;
      console.error(error);
    }
  }
  const currentTableRef= ref();
  /**
   * 续期按钮点击
   * @param row 行数据
   */
  function handleRenewal(row: GroupTableType, resourceType: string, tableRef) {
    selectedRow.value = row;
    selectedTableGroupType.value = resourceType;
    isShowRenewal.value = true;
    currentTableRef.value = tableRef
  }
  /**
   * 移交按钮点击
   * @param row 行数据
   */
  function handleHandOver(row: GroupTableType, resourceType: string, index: number) {
    selectedRow.value = row;
    rowIndex.value = index;
    selectedTableGroupType.value = resourceType;
    isShowHandover.value = true;
  }
  /**
   * 移出按钮点击
   * @param row 行数据
   */
  function handleRemove(row: GroupTableType, resourceType: string, index: number) {
    selectedRow.value = row;
    rowIndex.value = index;
    selectedTableGroupType.value = resourceType;
    isShowRemove.value = true;
  }
  /**
   * 更新表格行数据
   * @param expiredAt 续期时间
   */
  async function handleUpDateRow(expiredAt: number) {
    const activeTable = sourceList.value.find(group => group.resourceType === selectedTableGroupType.value);
    if (activeTable) {
      try {
        const params = {
          groupId: selectedRow.value!.groupId,
          targetMember: memberItem.value,
          renewalDuration: expiredAt
        };
        const res = await http.renewal(projectId.value, params)
        activeTable.tableData = activeTable.tableData.map(item => item.groupId === selectedRow.value!.groupId ? res : item)
        delete selectedData[selectedTableGroupType.value];
        currentTableRef.value.clearSelection();
      } catch (error) {
        console.log(error);
      }
    }
  }
  /**
   * 删除行数据
   */
  function handleRemoveRow() {
    const current = paginations.value[selectedTableGroupType.value];
    current[2] = (current[2] ?? 0) + 1;

    const activeTableData = sourceList.value.find(group => group.resourceType === selectedTableGroupType.value);
    if (activeTableData) {
      activeTableData.tableData?.splice(rowIndex.value as number, 1);
      activeTableData.tableData = [...activeTableData.tableData];
      activeTableData.count = activeTableData.count! - 1;
    }
    isPermission.value = sourceList.value.every(item => item.count !== 0)
    handleClear(selectedTableGroupType.value);
  }
  /**
   * 获取表格选择的数据
   */
  function getSelectList(selections: GroupTableType[], resourceType: string) {
    let item = sourceList.value.find((item: SourceType) => item.resourceType == resourceType);
    item && (item.isAll = false);
    selectedData[resourceType] = selections
    if (!selectedData[resourceType].length) {
      delete selectedData[resourceType]
    }
  }
  /**
   * 获取选中的用户组数据
   */
  function getSourceList() {
    selectedLength.value = 0;
    selectSourceList.value = Object.entries(selectedData)
      .map(([key, tableData]: [string, GroupTableType[]]) => {
        const sourceItem = sourceList.value.find((item: SourceType) => item.resourceType == key) as SourceType;
        selectedLength.value += sourceItem.isAll ? sourceItem.count! : tableData.length 
        return {
          pagination: {
            limit: 10,
            current: 1,
            count: sourceItem.isAll ? sourceItem.count! : tableData.length 
          },
          ...sourceItem,
          tableData: tableData,
          count: sourceItem.isAll ? sourceItem.count! : tableData.length,
          ...(!sourceItem.isAll && { groupIds: tableData.map(item => ({ id: item.groupId, memberType: item.memberType })) }),
          ...(!sourceItem.isAll && { isRemotePagination: false }),
        };
      });
  }
  /**
   * 加载更多
   */
  async function handleLoadMore(resourceType: string) {
    let item = sourceList.value.find((item: SourceType) => item.resourceType == resourceType);
    if (item?.scrollLoading) return;
    const pagination = paginations.value[resourceType];
    const currentOffset = pagination[0];
    const nextOffsetAdjustment = pagination[2] || 0;

    const newOffset = currentOffset + 10 - nextOffsetAdjustment;
    pagination[0] = newOffset;

    if (item){
      item.scrollLoading = true;
      const res = await getGroupList(resourceType, seacrhObj.value);
      item.scrollLoading = false;
      item.tableData = [...item.tableData, ...res.records];
      if(pagination[2]){
        pagination.pop();
      }
    }
  }
  /**
   * 全量数据选择
   */
  function handleSelectAllData(resourceType: string) {
    let item = sourceList.value.find((item: SourceType) => item.resourceType == resourceType);
    if(item){
      item.isAll = true;
      selectedData[resourceType] = item.tableData
    }
  }
  /**
   * 清除选择
   */
  function handleClear(resourceType: string) {
    let item = sourceList.value.find((item: SourceType) => item.resourceType == resourceType);
    if(item){
      item.isAll = false;
    }
    delete selectedData[resourceType]
  }
  /**
   * 折叠面板调用接口获取表格数据
   */
  async function collapseClick(resourceType: string) {
    let item = sourceList.value.find((item: SourceType) => item.resourceType == resourceType);
    if(item){
      if (!item.count || item.tableData.length) {
        return;
      } else {
        try {
          item.tableLoading = true;
          paginations.value[resourceType] = [0, 10]
          const res = await getGroupList(resourceType, seacrhObj.value);
          item.tableLoading = false;
          item.tableData = res.records;
        } catch (e) {
          item.tableLoading = false;
        }
      }
    }
  }
  async function pageLimitChange(limit: number, resourceType: string) {
    try {
      let item = selectSourceList.value.find((item: SourceType) => item.resourceType == resourceType);
      if (item) {
        paginations.value[resourceType][1] = limit;
        item.tableLoading = true;
        if (item.isRemotePagination) {
          const res = await getGroupList(resourceType, seacrhObj.value)
          item.tableData = res.records;
        }
        item.tableLoading = false;
      }
    } catch (error) {
      
    }
  }
  async function pageValueChange(value: number, resourceType: string) {
    try {
      let item = selectSourceList.value.find((item: SourceType) => item.resourceType == resourceType);
      if (item) {
        paginations.value[resourceType][0] = (value - 1) * 10 + 1;
        item.tableLoading = true;
        if (item.isRemotePagination) {
          const res = await getGroupList(resourceType, seacrhObj.value)
          item.tableData = res.records;
        }
        item.tableLoading = false;
      }
    } catch (error) {
      
    }
  }

  function clearPaginations() {
    Object.keys(paginations.value).forEach(key => {
      paginations.value[key] = [0, 10];
    });
  }

  return {
    isLoading,
    sourceList,
    collapseList,
    isShowRenewal,
    isShowHandover,
    isShowRemove,
    selectedData,
    selectedLength,
    selectSourceList,
    selectedRow,
    isPermission,
    fetchUserGroupList,
    handleRenewal,
    handleHandOver,
    handleRemove,
    getSelectList,
    getSourceList,
    handleLoadMore,
    handleSelectAllData,
    handleClear,
    collapseClick,
    handleRemoveRow,
    handleUpDateRow,
    pageLimitChange,
    pageValueChange,
    clearPaginations,
  };
});