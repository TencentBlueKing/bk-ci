import http from '@/http/api';
import { defineStore } from 'pinia';
import { useRoute } from 'vue-router';
import { ref, reactive, computed, watch } from 'vue';
import { useI18n } from 'vue-i18n';

export interface GroupTableType {
  groupId: string | number;
  groupName: string;
  groupDesc: string;
  expiredAtDisplay: string;
  joinedTime: string;
  operateSource: string;
  operator: string;
  removeMemberButtonControl: 'OTHER' | 'TEMPLATE' | 'UNIQUE_MANAGER';
};
interface Pagination {
  limit: string | number;
  current: string | number;
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
interface AsideItem {
  id: string,
  name: string,
  type: string
}

export default defineStore('userGroupTable', () => {
  const { t } = useI18n();
  const route = useRoute();
  const isLoading = ref(true);

  const projectId = computed(() => route.params?.projectCode as string);
  const paginations = ref({
    'project': [1, 10],
    'pipeline': [1, 10]
  })

  const sourceList = ref<SourceType[]>([]);
  const collapseList = ref<CollapseListType[]>([]);
  const memberId = ref();

  const isShowRenewal = ref(false);
  const isShowHandover = ref(false);
  const isShowRemove = ref(false);

  const unableMoveLength = ref();
  const selectedData = reactive<SelectedDataType>({});
  const selectSourceList = ref<SourceType[]>([]);
  const selectedRow = ref<GroupTableType | null>(null);
  const rowIndex = ref<number>();
  const selectedTableGroupType = ref('');
  const selectedLength = computed(() => Object.keys(selectedData).length);

  watch(sourceList, () => {
    sourceList.value.forEach(item => {
      if((item.count! > item.tableData.length)) {
        item.hasNext = true;
        item.remainingCount = item.count! - item.tableData.length
      } else {
        item.hasNext = false;
      }
    })
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
    Object.keys(selectedData).forEach(key => {
      delete selectedData[key];
    });
  }
  /**
   * 获取项目成员有权限的用户组数量
   */
  async function getCollapseList(memberId: string) {
    try {
      const res = await http.getMemberGroups(projectId.value, memberId);
      collapseList.value = res;
      sourceList.value = collapseList.value.map((item) => ({
        ...item,
        tableLoading: false,
        tableData: [],
      }))
    } catch (error) {
      console.log(error);
    }
  }
  /**
   * 获取项目成员有权限的用户组
   * @param resourceType 资源类型
   */
  async function getGroupList(resourceType: string) {
    try {
      const params = {
        projectId: projectId.value,
        resourceType,
        memberId: memberId.value,
        start: paginations.value[resourceType][0],
        limit: paginations.value[resourceType][1],
      }
      return await http.getMemberGroupsDetails(params);
    } catch (error) {
      console.log(error);
    }
  }
  /**
   * 获取项目成员页面数据
   */
  async function fetchUserGroupList(asideItem: AsideItem) {
    initData();
    getCollapseList(asideItem.id);
    memberId.value = asideItem.id;
    try {
      isLoading.value = true;
      const resourceTypes = ['project', 'pipeline'];
      const results = await Promise.all(
        resourceTypes.map(resourceType => getGroupList(resourceType))
      );
      const [projectResult, pipelineGroupResult] = results;

      sourceList.value.forEach(item => {
        if(item.resourceType === "project") {
          item.tableData = projectResult.records;
        }
        if(item.resourceType === "pipeline") {
          item.tableData = pipelineGroupResult.records;
          item.count && (item.activeFlag = true);
        }
      })
      isLoading.value = false;
    } catch (error: any) {
      console.error(error);
    }
  }
  /**
   * 续期按钮点击
   * @param row 行数据
   */
  function handleRenewal(row: GroupTableType, resourceType: string) {
    selectedRow.value = row;
    selectedTableGroupType.value = resourceType;
    isShowRenewal.value = true;
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
  function handleUpDateRow(expiredAt) {
    const activeTable = sourceList.value.find(group => group.resourceType === selectedTableGroupType.value);
    const activeTableRow = activeTable?.tableData.find(item => item.groupId === selectedRow.value?.groupId);
    if (activeTableRow) {
      const currentDisplay = activeTableRow.expiredAtDisplay;
      if (currentDisplay !== t('已过期')) {
        const days = Number(currentDisplay.split('天')[0]);
        activeTableRow.expiredAtDisplay = `${days + expiredAt}天`;
      } else {
        activeTableRow.expiredAtDisplay = `${expiredAt}天`;
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
    }
  }
  /**
   * 获取表格选择的数据
   */
  function getSelectList(selections, resourceType: string) {
    selectedData[resourceType] = selections
    if (!selectedData[resourceType].length) {
      delete selectedData[resourceType]
    }
    console.log('表格选择的数据', selectedData);
    unableMoveLength.value = countNonOtherObjects(selectedData);
  }
  /**
   * 找出无法移出用户数据
   */
  function countNonOtherObjects(data: SelectedDataType) {
    return Object.values(data)
      .flat()
      .filter((item: GroupTableType) => item.removeMemberButtonControl !== 'OTHER')
      .length;
  }
  /**
   * 获取选中的用户组数据
   */
  function getSourceList() {
    selectSourceList.value = Object.entries(selectedData)
      .map(([key, tableData]: [string, GroupTableType[]]) => {
        const sourceItem = sourceList.value.find((item: SourceType) => item.resourceType == key) as SourceType;
        return {
          pagination: {
            limit: 10,
            current: 1,
            count: sourceItem.isAll ? sourceItem.count! : tableData.length 
          },
          ...sourceItem,
          tableData,
        };
      });
  }
  /**
   * 加载更多
   */
  async function handleLoadMore(resourceType: string) {
    const pagination = paginations.value[resourceType];
    const currentOffset = pagination[0];
    const nextOffsetAdjustment = pagination[2] || 0;

    const newOffset = currentOffset + 10 - nextOffsetAdjustment;
    pagination[0] = newOffset;

    let item = sourceList.value.find((item: SourceType) => item.resourceType == resourceType);
    if(item){
      item.tableLoading = true;
      const res = await getGroupList(resourceType);
      item.tableLoading = false;
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
        item.activeFlag = true;
        try {
          item.tableLoading = true;
          paginations.value[resourceType] = [1, 10]
          const res = await getGroupList(resourceType);
          item.tableLoading = false;
          item.tableData = res.records;
        } catch (e) {
          console.error(e)
        }
      }
    }
  }
  async function pageLimitChange(limit: number, resourceType: string) {
    paginations.value[resourceType][1] = limit;
    try {
      let item = selectSourceList.value.find((item: SourceType) => item.resourceType == resourceType);
      if(item){
        item.tableLoading = true;
        const res = await getGroupList(resourceType)
        item.tableLoading = false;
        item.tableData = res.records;
      }
    } catch (error) {
      
    }
  }
  async function pageValueChange(value: number, resourceType: string) {
    paginations.value[resourceType][0] = (value - 1) * 10 + 1;
    try {
      let item = selectSourceList.value.find((item: SourceType) => item.resourceType == resourceType);
      if(item){
        item.tableLoading = true;
        const res = await getGroupList(resourceType)
        item.tableLoading = false;
        item.tableData = res.records;
      }
    } catch (error) {
      
    }
  }

  return {
    isLoading,
    sourceList,
    collapseList,
    isShowRenewal,
    isShowHandover,
    isShowRemove,
    selectedData,
    unableMoveLength,
    selectedLength,
    selectSourceList,
    selectedRow,
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
  };
});