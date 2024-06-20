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
  const pagination = ref({ limit: 10, current: 1 });

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
  const selectProjectlist = ref<GroupTableType[]>([]);
  const selectSourceList = ref<SourceType[]>([]);
  const selectedRow = ref<GroupTableType | null>(null);
  const selectedTableGroupId = ref('');
  const selectedLength = computed(() => Object.keys(selectedData).length);

  /**
   * 获取sourceList（需处理数据），collapseList
   */
  async function fetchUserGroupList(groupId: string) {
    sourceList.value = []
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
    isShowRenewal.value = true;
    selectedTableGroupId.value = groupId;
  }
  /**
   * 移交按钮点击
   * @param row 行数据
   */
  function handleHandOver(row: GroupTableType, groupId: string, index) {
    selectedRow.value = row;
    isShowHandover.value = true;
    selectedTableGroupId.value = groupId;
  }
  /**
   * 移出按钮点击
   * @param row 行数据
   */
  function handleRemove(row: GroupTableType, groupId: string) {
    selectedRow.value = row;
    isShowRemove.value = true;
    selectedTableGroupId.value = groupId;
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
    // 调用接口 获取selectedData[groupId]数据
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
    selectProjectlist,
    selectSourceList,
    selectedRow,
    selectedTableGroupId,
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
  };
});