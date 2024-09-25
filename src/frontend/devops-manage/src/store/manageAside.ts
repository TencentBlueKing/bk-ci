import http from '@/http/api';
import { defineStore } from 'pinia';
import { ref } from 'vue';
import { Message } from 'bkui-vue';
import userGroupTable, { SearchParamsType, AsideItem } from "@/store/userGroupTable";
import dayjs from 'dayjs';

interface Pagination {
  limit: number;
  current: number;
  count: number;
}

interface MemberListParamsType {
  page: number;
  pageSize: number;
  userName?: string;
  deptName?: string;
  memberType?: string;
  departedFlag? : boolean;
  projectCode: string,
  groupName?: string,
  minExpiredAt?: number,
  maxExpiredAt?: number,
}

export default defineStore('manageAside', () => {
  const groupTableStore = userGroupTable();

  const isLoading = ref(false);
  const asideItem = ref<AsideItem>();
  const memberList = ref<AsideItem[]>([]);
  const personList = ref([]);
  const tableLoading = ref(false);
  const userName = ref('');
  const memberPagination = ref<Pagination>({ limit: 20, current: 1, count: 0 });
  const activeTab = ref();
  const btnLoading = ref(false);
  const removeUserDeptListMap = ref({});
  const showDeptListPermissionDialog = ref(false);
  const seacrhObj = ref<SearchParamsType>({});
  /**
   * 人员组织侧边栏点击事件
   */
  function handleAsideClick(item: AsideItem) {
    asideItem.value = item;
    activeTab.value = item.id;
    groupTableStore.fetchUserGroupList(item, seacrhObj.value);
  }
  /**
   * 人员组织侧边栏页码切换
   */
  async function handleAsidePageChange(current: number, projectId: string) {
    asideItem.value = undefined;
    if (memberPagination.value.current !== current) {
      memberPagination.value.current = current;
      getProjectMembers(projectId, true);
    }
  }
  /**
   * 人员列表数据获取
   */
  async function handleShowPerson(asideItem: AsideItem, projectId: string) {
    tableLoading.value = true;
    const res = await http.deptUsers(projectId);
    personList.value = res.map(item => ({ person: item.name }));
    tableLoading.value = false;
  }
  /**
   * 组织移出项目
   */
  async function handleAsideRemoveConfirm(removeUser: AsideItem, handOverMember: AsideItem, projectId: string, manageAsideRef: any) {
    showDeptListPermissionDialog.value = false
    console.log(handOverMember, 'handOverMember')
    const params = {
      targetMember: removeUser,
      ...(Object.keys(handOverMember).length && {handoverTo: handOverMember})
    }
    try {
      btnLoading.value = true;
      const res = await http.removeMemberFromProject(projectId, params);
      
      asideItem.value = undefined;
      if (!res.length) {
        Message({
          theme: 'success',
          message: `${removeUser.id}(${removeUser.name}) 已成功移出本项目。`,
        });
      } else {
        removeUserDeptListMap.value = {
          list: res,
          removeUser
        }
        showDeptListPermissionDialog.value = true
      }
      btnLoading.value = false;
      manageAsideRef.handOverClose();
      getProjectMembers(projectId, true);
    } catch (error) {
      btnLoading.value = false;
    }
  }
  function getTimestamp (dateString: string) {
    return dayjs(dateString).valueOf();
  }
  function getParams (projectId: string, departedFlag?: boolean, searchGroup?: any) {
    const params: MemberListParamsType = {
      page: memberPagination.value.current,
      pageSize: memberPagination.value.limit,
      projectCode: projectId,
    };

    if (departedFlag) {
      params.departedFlag = departedFlag;
    }

    if (searchGroup?.expiredAt && Object.keys(searchGroup?.expiredAt).length) {
      params.minExpiredAt = getTimestamp(searchGroup.expiredAt[0]?.formatText);
      params.maxExpiredAt = getTimestamp(searchGroup.expiredAt[1]?.formatText);
    }

    searchGroup?.searchValue?.forEach((item) => {
      switch (item.id) {
        case 'user':
          params.userName = item.values[0].id;
          params.memberType = 'user';
          break;
        case 'department':
          params.deptName = item.values[0].name;
          params.memberType = 'department';
          break;
        case 'groupName':
          params.groupName = item.values[0].name;
          break;
      }
    })
    return params;
  }
  /**
   * 获取项目下全体成员
   */
  async function getProjectMembers(projectId: string, departedFlag?: boolean, searchGroup?: any) {
    try {
      isLoading.value = true;
      const params = getParams(projectId, departedFlag, searchGroup);
      
      seacrhObj.value = {
        ...(params.groupName && {groupName: params.groupName}),
        ...(params.minExpiredAt && {minExpiredAt: params.minExpiredAt}),
        ...(params.maxExpiredAt && {maxExpiredAt: params.maxExpiredAt}),
      }

      const res = await http.getProjectMembersByCondition(projectId, params);
      isLoading.value = false;
      memberList.value = res.records;

      const itemToClick = asideItem.value || res.records[0];
      handleAsideClick(itemToClick);

      memberPagination.value.count = res.count;
    } catch (error) {
      isLoading.value = false;
    }
  }

  return {
    isLoading,
    asideItem,
    memberList,
    personList,
    tableLoading,
    userName,
    memberPagination,
    activeTab,
    btnLoading,
    removeUserDeptListMap,
    showDeptListPermissionDialog,
    handleAsideClick,
    handleAsidePageChange,
    handleShowPerson,
    handleAsideRemoveConfirm,
    getProjectMembers,
  };
});