import http from '@/http/api';
import { defineStore } from 'pinia';
import { ref, reactive, computed } from 'vue';

export interface GroupTableType {
  groupId: number;
  groupName: string;
  groupDesc: string;
  validityPeriod: string;
  joinedTime: string;
  operateSource: string;
  operator: string;
  removeMemberButtonControl: 'OTHER' | 'TEMPLATE' | 'UNIQUE_MANAGER';
};
interface SourceType {
  id: string | number;
  groupTotal: string | number;
  groupItem?: string;
  activeFlag?: boolean;
  tableData: GroupTableType[];
}
interface SelectRowType {
  row: GroupTableType;
  index: number;
  checked: boolean;
  data: GroupTableType[];
  isAll?: boolean;
}
interface SelectedDataType {
  [key: string]: GroupTableType[];
}
interface CollapseListType {
  id: string | number;
  groupItem: string;
  groupTotal: number;
  type: string;
}

export default defineStore('userGroupTable', () => {
  const isLoading = ref(true);
  const pagination = ref({ limit: 10, current: 1, count: 0 });

  const sourceList = ref<SourceType[]>([]);
  const collapseList = ref<CollapseListType[]>([
    {
      id: 1,
      groupItem: '项目（project）',
      groupTotal: 10,
      type: 'project',
    },
    {
      id: 2,
      groupItem: '流水线 (Pipiline) - 流水线组',
      groupTotal: 2,
      type: 'source',
    },
    {
      id: 3,
      groupItem: '流水线 (Pipiline)',
      groupTotal: 3,
      type: 'source',
    },
  ]);

  const isShowRenewal = ref(false);
  const isShowHandover = ref(false);
  const isShowRemove = ref(false);

  const unableMoveLength = ref();
  const selectedData = reactive<SelectedDataType>({});
  const selectSourceList = ref<SourceType[]>([]);
  const selectedRow = ref<GroupTableType | null>(null);
  const rowIndex = ref<number>();
  const selectedTableGroupId = ref('');
  const selectedLength = computed(() => Object.keys(selectedData).length);

  /**
   * 初始化数据
   */
  function initData() {
    selectedRow.value = null;
    rowIndex.value = undefined;
    selectedTableGroupId.value = '';
    sourceList.value = [];
    selectSourceList.value = [];
    Object.keys(selectedData).forEach(key => {
      delete selectedData[key];
    });
  }
  /**
   * 获取sourceList（需处理数据），collapseList
   */
  async function fetchUserGroupList(groupId: string) {
    initData();
    try {
      isLoading.value = true;
      setTimeout(() => {
        sourceList.value = [
          {
            id: 1,
            groupItem: '项目（project）',
            groupTotal: 10,
            activeFlag: true,
            tableData: [{
              groupId: 1,
              groupName: '11',
              groupDesc: 'kjkjkjk',
              validityPeriod: '0505',
              joinedTime: '08-18',
              operateSource: '加入组',
              operator: '张三',
              removeMemberButtonControl: 'UNIQUE_MANAGER',
            },
            {
              groupId: 2,
              groupName: '22',
              groupDesc: 'kjkjkjk',
              validityPeriod: '0505',
              joinedTime: '08-18',
              operateSource: '加入组',
              operator: '张三',
              removeMemberButtonControl: 'OTHER',
            }, {
              groupId: 3,
              groupName: '33',
              groupDesc: 'kjkjkjk',
              validityPeriod: '0505',
              joinedTime: '08-18',
              operateSource: '加入组',
              operator: '张三',
              removeMemberButtonControl: 'OTHER',
            },
            {
              groupId: 4,
              groupName: '44',
              groupDesc: 'kjkjkjk',
              validityPeriod: '0505',
              joinedTime: '08-18',
              operateSource: '加入组',
              operator: '张三',
              removeMemberButtonControl: 'OTHER',
            }, {
              groupId: 5,
              groupName: '55',
              groupDesc: 'kjkjkjk',
              validityPeriod: '0505',
              joinedTime: '08-18',
              operateSource: '加入组',
              operator: '张三',
              removeMemberButtonControl: 'OTHER',
            },
            {
              groupId: 6,
              groupName: '66',
              groupDesc: 'kjkjkjk',
              validityPeriod: '0505',
              joinedTime: '08-18',
              operateSource: '加入组',
              operator: '张三',
              removeMemberButtonControl: 'TEMPLATE',
            }, {
              groupId: 7,
              groupName: '77',
              groupDesc: 'kjkjkjk',
              validityPeriod: '0505',
              joinedTime: '08-18',
              operateSource: '加入组',
              operator: '张三',
              removeMemberButtonControl: 'OTHER',
            },
            {
              groupId: 8,
              groupName: '88',
              groupDesc: 'kjkjkjk',
              validityPeriod: '0505',
              joinedTime: '08-18',
              operateSource: '加入组',
              operator: '张三',
              removeMemberButtonControl: 'OTHER',
            }]
          },
          {
            id: 2,
            groupItem: '流水线 (Pipiline) - 流水线组',
            groupTotal: 3,
            activeFlag: true,
            tableData: [{
              groupId: 1,
              groupName: '11',
              groupDesc: 'kjkjkjk',
              validityPeriod: '0505',
              joinedTime: '08-18',
              operateSource: '加入组',
              operator: '张三',
              removeMemberButtonControl: 'TEMPLATE',
            },
            {
              groupId: 2,
              groupName: '12',
              groupDesc: 'kjkjkjk',
              validityPeriod: '0505',
              joinedTime: '08-18',
              operateSource: '加入组',
              operator: '张三',
              removeMemberButtonControl: 'OTHER',
            }],
          },
          {
            id: 3,
            groupItem: '流水线 (Pipiline)',
            groupTotal: 3,
            tableData: [{
              groupId: 1,
              groupName: '21',
              groupDesc: 'kjkjkjk',
              validityPeriod: '0505',
              joinedTime: '08-18',
              operateSource: '加入组',
              operator: '张三',
              removeMemberButtonControl: 'UNIQUE_MANAGER',
            },
            {
              groupId: 2,
              groupName: '22',
              groupDesc: 'kjkjkjk',
              validityPeriod: '0505',
              joinedTime: '08-18',
              operateSource: '加入组',
              operator: '张三',
              removeMemberButtonControl: 'OTHER',
            },
            {
              groupId: 2,
              groupName: '23',
              groupDesc: 'kjkjkjk',
              validityPeriod: '0505',
              joinedTime: '08-18',
              operateSource: '加入组',
              operator: '张三',
              removeMemberButtonControl: 'OTHER',
            }],
          },
        ]
        isLoading.value = false;
      }, 1000)
      // const res = await http.getMemberManageList(groupId);
      // memberManageList.value = res;
    } catch (error: any) {
      console.log(error);
    }
  }
  /**
   * 续期按钮点击
   * @param row 行数据
   */
  function handleRenewal(row: GroupTableType, groupId: string) {
    selectedRow.value = row;
    selectedTableGroupId.value = groupId;
    isShowRenewal.value = true;
  }
  /**
   * 移交按钮点击
   * @param row 行数据
   */
  function handleHandOver(row: GroupTableType, groupId: string, index) {
    selectedRow.value = row;
    rowIndex.value = index;
    selectedTableGroupId.value = groupId;
    isShowHandover.value = true;
  }
  /**
   * 移出按钮点击
   * @param row 行数据
   */
  function handleRemove(row: GroupTableType, groupId: string, index) {
    selectedRow.value = row;
    rowIndex.value = index;
    selectedTableGroupId.value = groupId;
    isShowRemove.value = true;
  }
  /**
   * 更新表格行数据
   * @param expiredAt 续期时间
   */
  function handleUpDateRow(expiredAt) {
    const activeTableRow = sourceList.value.find(group => group.id === selectedTableGroupId.value)?.tableData.find(item => item.groupId === selectedRow.value?.groupId)
    if (activeTableRow) {
      activeTableRow.joinedTime = expiredAt
    }
  }
  /**
   * 删除行数据
   */
  function handleRemoveRow() {
    const activeTableData = sourceList.value.find(group => group.id === selectedTableGroupId.value);
    if (activeTableData) {
      activeTableData.tableData.splice(rowIndex.value, 1);
      activeTableData.tableData = [...activeTableData.tableData];
    }
  }
  /**
   * 获取表格选择的数据
   */
  function getSelectList(rowData: SelectRowType, groupId: string) {
    if (!rowData.isAll) {
      if (rowData.checked) {
        const newSelectedData = !selectedData[groupId] ? [] : selectedData[groupId]
        selectedData[groupId] = newSelectedData.concat(rowData.row);
      } else {
        selectedData[groupId] = selectedData[groupId].filter(item => item !== rowData.row);
        !selectedData[groupId].length && handleClear(groupId);
      }
    } else {
      rowData.checked ? (selectedData[groupId] = rowData.data) : handleClear(groupId);
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
      .map(([key, tableData]: [string, GroupTableType[]]) => ({
        id: key,
        groupTotal: tableData.length,
        groupItem: collapseList.value.find((item: CollapseListType) => item.id == key)?.groupItem,
        activeFlag: true,
        tableData,
      }));
  }
  /**
   * 加载更多
   */
  function handleLoadMore(groupId: string) {
    console.log('加载更多', groupId);
  }
  /**
   * 全量数据选择
   */
  function handleSelectAllData(groupId: string) {
    console.log('全量数据选择', groupId);
    // 不需调用接口获取selectedData[groupId]数据
    pagination.value.count = 20;

  }
  /**
   * 清除选择
   */
  function handleClear(groupId: string) {
    delete selectedData[groupId];
  }
  /**
   * 折叠面板调用接口获取表格数据
   */
  function collapseClick(id) {
    // 折叠面板调用接口获取表格数据
    console.log('折叠面板', id)
  }

  function pageLimitChange(limit, groupId) {
    pagination.value.limit = limit;
    // 调用获取表格数据的接口
  }
  function pageValueChange(value, groupId) {
    pagination.value.current = value;
    // 调用获取表格数据的接口
  }

  return {
    isLoading,
    pagination,
    sourceList,
    collapseList,
    isShowRenewal,
    isShowHandover,
    isShowRemove,
    selectedData,
    unableMoveLength,
    selectedLength,
    selectSourceList,
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