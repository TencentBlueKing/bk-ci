import http from '@/http/api';
import { defineStore } from 'pinia';
import { useRoute } from 'vue-router';
import { ref, reactive, computed, nextTick } from 'vue';

export interface GroupTableType {
  groupId: string | number;
  groupName: string;
  groupDesc: string;
  validityPeriod: string;
  joinedTime: string;
  operateSource: string;
  operator: string;
  removeMemberButtonControl: 'OTHER' | 'TEMPLATE' | 'UNIQUE_MANAGER';
};
interface SourceType {
  count: string | number;
  resourceTypeName: string;
  resourceType: string,
  hasNext?: boolean,
  activeFlag?: boolean;
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

export default defineStore('userGroupTable', () => {
  const route = useRoute();

  const isLoading = ref(true);
  const pagination = ref({ limit: 10, current: 1, count: 0 });

  const projectId = computed(() => route.params?.projectCode as string);
  const sourceGroup = ref({
    'project': [1, 10],
    'pipelineGroup': [1, 5]
  })

  const sourceList = ref<SourceType[]>([]);
  const collapseList = ref<CollapseListType[]>([]);

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
   * 获取折叠数据
   */
  async function getCollapseList({ type, name }) {
    const res = await http.getMemberGroups(projectId.value, { type, member: name });
    // collapseList.value = res;
    collapseList.value = [
      {
        resourceType: 'project',
        resourceTypeName: '项目（project）',
        count: 10,
      },
      {
        resourceType: 'pipilineGroup',
        resourceTypeName: '流水线 (Pipiline) - 流水线组',
        count: 2,
      },
      {
        resourceType: 'pipiline',
        resourceTypeName: '流水线 (Pipiline)',
        count: 3,
      },
      {
        resourceType: 'ticket',
        resourceTypeName: '凭证管理(Ticket)-凭据',
        count: 0,
      },
    ];
  }
  async function getGroupList(resourceType, asideItem) {
    const pathParams = {
      projectId: projectId.value,
      resourceType,
      start: sourceGroup.value[resourceType][0],
      end: sourceGroup.value[resourceType][1],
    }
    return await http.getMemberGroupsWithPermissions(pathParams, asideItem.name);
  }
  /**
   * 获取sourceList（需处理数据），collapseList
   */
  async function fetchUserGroupList(asideItem) {
    initData();
    getCollapseList(asideItem);
    try {
      isLoading.value = true;
      setTimeout(async () => {
        const resourceTypes = ['project', 'pipelineGroup'];
        const results = await Promise.all(
          resourceTypes.map(resourceType => getGroupList(resourceType, asideItem))
        );
        const [projectResult, pipelineGroupResult] = results;

        sourceList.value = collapseList.value.map((item) => ({
          ...item,
          tableData: [],
        }))
        // sourceList.value[0].tableData = projectResult.records;
        // sourceList.value[0].hasNext = projectResult.hasNext;
        // sourceList.value[1].tableData = pipelineGroupResult.records;
        // sourceList.value[1].hasNext = pipelineGroupResult.hasNext;
        sourceList.value[1].activeFlag = true;

        // 模拟数据
        sourceList.value[0].hasNext = true;
        sourceList.value[0].tableData = [{
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
        }];
        sourceList.value[1].tableData = [{
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
        }];

        isLoading.value = false;
      }, 1000)
    } catch (error: any) {
      console.error(error);
    }
  }
  /**
   * 续期按钮点击
   * @param row 行数据
   */
  function handleRenewal(row: GroupTableType, groupType: string) {
    selectedRow.value = row;
    selectedTableGroupType.value = groupType;
    isShowRenewal.value = true;
  }
  /**
   * 移交按钮点击
   * @param row 行数据
   */
  function handleHandOver(row: GroupTableType, groupType: string, index) {
    selectedRow.value = row;
    rowIndex.value = index;
    selectedTableGroupType.value = groupType;
    isShowHandover.value = true;
  }
  /**
   * 移出按钮点击
   * @param row 行数据
   */
  function handleRemove(row: GroupTableType, groupType: string, index) {
    selectedRow.value = row;
    rowIndex.value = index;
    selectedTableGroupType.value = groupType;
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
      activeTableRow.joinedTime = expiredAt
    }
  }
  /**
   * 删除行数据
   */
  function handleRemoveRow() {
    const activeTableData = sourceList.value.find(group => group.resourceType === selectedTableGroupType.value);
    if (activeTableData) {
      activeTableData.tableData?.splice(rowIndex.value, 1);
      activeTableData.tableData = [...activeTableData.tableData];
    }
  }
  /**
   * 获取表格选择的数据
   */
  function getSelectList(selections, groupType: string) {
    selectedData[groupType] = selections
    if (!selectedData[groupType].length) {
      delete selectedData[groupType]
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
        count: tableData.length,
        resourceTypeName: sourceList.value.find((item: SourceType) => item.resourceType == key)?.resourceTypeName,
        resourceType: sourceList.value.find((item: SourceType) => item.resourceType == key)?.resourceType,
        activeFlag: true,
        tableData,
      }));
  }
  /**
   * 加载更多
   */
  function handleLoadMore(groupType: string) {
    console.log('加载更多', groupType);
  }
  /**
   * 全量数据选择
   */
  function handleSelectAllData(groupType: string) {
    console.log('全量数据选择', groupType);
    // 不需调用接口获取selectedData[groupType]数据
    pagination.value.count = 20;

  }
  /**
   * 清除选择
   */
  function handleClear(groupType: string) {
    delete selectedData[groupType]
  }
  /**
   * 折叠面板调用接口获取表格数据
   */
  async function collapseClick(resourceType, asideItem) {
    let item = sourceList.value.find((item: SourceType) => item.resourceType == resourceType);

    if (!item || item.tableData.length) return;
    try {
      sourceGroup.value[resourceType] = [1, 10]
      const res = await getGroupList(resourceType, asideItem);
      // item.tableData = res.records;
      item.tableData = [{
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
      }]
      item.activeFlag = true;
      item.hasNext = res.hasNext;
    } catch (e) {
      console.error(e)
    }
  }

  function pageLimitChange(limit, groupType) {
    pagination.value.limit = limit;
    // 调用获取表格数据的接口
  }
  function pageValueChange(value, groupType) {
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