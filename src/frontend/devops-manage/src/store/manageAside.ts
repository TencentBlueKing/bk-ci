import http from '@/http/api';
import { defineStore } from 'pinia';
import { ref } from 'vue';
import { Message } from 'bkui-vue';
import userGroupTable, { SearchParamsType, AsideItem } from "@/store/userGroupTable";
import dayjs from 'dayjs';
import { useI18n } from 'vue-i18n';

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
  relatedResourceType?: string,
  relatedResourceCode?: string,
  action?: string,
}

export default defineStore('manageAside', () => {
  const { t } = useI18n();
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
  const searchObj = ref<SearchParamsType>({});
  const checkedMemberList = ref();

  /**
   * 人员组织侧边栏点击事件
   */
  function handleAsideClick(item: AsideItem) {
    asideItem.value = item;
    activeTab.value = item.id;
    groupTableStore.fetchUserGroupList(item, searchObj.value);
  }
  /**
   * 人员组织侧边栏页码切换
   */
  async function handleAsidePageChange(current: number, projectId: string, selected: AsideItem[], searchGroup?: any) {
    checkedMemberList.value = selected
    asideItem.value = undefined;
    if (memberPagination.value.current !== current) {
      memberPagination.value.current = current;
      getProjectMembers(projectId, true, searchGroup);
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
  async function handleAsideRemoveConfirm(isBatchOperate: boolean, removeUsers: any, handOverMember: AsideItem, projectId: string, manageAsideRef: any) {
    showDeptListPermissionDialog.value = false
    const params = {
      targetMembers: removeUsers,
      ...(Object.keys(handOverMember).length && {handoverTo: handOverMember})
    }
    try {
      btnLoading.value = true;
      const { users,departments } = await http.removeMemberFromProject(projectId, params);
      
      asideItem.value = undefined;
      if (!users.length) {
        const allAreGroups = removeUsers.every(member => member.type === 'department');
        let message: string;
        if (isBatchOperate) {
          message = allAreGroups ? t('X个组织已成功移出本项目', [removeUsers.length]) : t('X个组织/用户已成功移出本项目', [removeUsers.length])
        } else {
          message = t('X(X)已成功移出本项目。', [removeUsers[0].id, removeUsers[0].name])
        }
        
        Message({
          theme: 'success',
          message
        });
      } else {
        removeUserDeptListMap.value = {
          list: departments,
          removeUsers: users
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

    if (searchGroup?.relatedResourceType) {
      params.relatedResourceType = searchGroup.relatedResourceType
    }
    if (searchGroup?.relatedResourceCode) {
      params.relatedResourceCode = searchGroup.relatedResourceCode
    }
    if (searchGroup?.action) {
      params.action = searchGroup.action
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

      searchObj.value = {
        ...['groupName', 'minExpiredAt', 'maxExpiredAt', 'relatedResourceType', 'relatedResourceCode', 'action']
          .reduce((acc, key) => {
            if (params[key]) {
              acc[key] = params[key];
            }
            return acc;
          }, {})
      }

      const res = await http.getProjectMembersByCondition(projectId, params);
      isLoading.value = false;
      memberList.value = res.records.map(item => ({
        ...item,
        checked: checkedMemberList.value?.some(checkedItem => checkedItem.id === item.id)
      }));

      const itemToClick = asideItem.value || res.records[0];
      handleAsideClick(itemToClick);

      memberPagination.value.count = res.count;
    } catch (error) {
      isLoading.value = false;
    }
  }

  function asideSelectAll(status: boolean) {
    memberList.value.forEach(item => {
      item.checked = status
    })
  }

  function updateMemberList(item: AsideItem) {
    const index = memberList.value.findIndex(member => member.id === item.id);
    if (index !== -1) {
      memberList.value[index] = item;
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
    asideSelectAll,
    updateMemberList
  };
});