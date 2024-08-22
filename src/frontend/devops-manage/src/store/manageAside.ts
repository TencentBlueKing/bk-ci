import http from '@/http/api';
import { defineStore } from 'pinia';
import { ref } from 'vue';
import { Message } from 'bkui-vue';
import userGroupTable from "@/store/userGroupTable";

interface ManageAsideType {
  id: string,
  name: string,
  type: "department" | "user"
};

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
}

export default defineStore('manageAside', () => {
  const groupTableStore = userGroupTable();

  const isLoading = ref(false);
  const asideItem = ref<ManageAsideType>();
  const memberList = ref<ManageAsideType[]>([]);
  const personList = ref([]);
  const tableLoading = ref(false);
  const userName = ref('');
  const memberPagination = ref<Pagination>({ limit: 20, current: 1, count: 0 });
  const activeTab = ref();
  const btnLoading = ref(false);
  const removeUserDeptListMap = ref({});
  const showDeptListPermissionDialog = ref(false);
  /**
   * 人员组织侧边栏点击事件
   */
  function handleAsideClick(item: ManageAsideType) {
    asideItem.value = item;
    activeTab.value = item.id;
    groupTableStore.fetchUserGroupList(item);
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
  async function handleShowPerson(asideItem: ManageAsideType, projectId: string) {
    tableLoading.value = true;
    const res = await http.deptUsers(projectId);
    personList.value = res.map(item => ({ person: item.name }));
    tableLoading.value = false;
  }
  /**
   * 组织移出项目
   */
  async function handleAsideRemoveConfirm(removeUser: ManageAsideType, handOverMember: ManageAsideType, projectId: string, manageAsideRef: any) {
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
  /**
   * 获取项目下全体成员
   */
  async function getProjectMembers(projectId: string, departedFlag?: boolean, searchValue?: any) {
    try {
      isLoading.value = true;
      const params: MemberListParamsType = {
        page: memberPagination.value.current,
        pageSize: memberPagination.value.limit,
        ...(departedFlag && {departedFlag}),
      };
      searchValue?.forEach(item => {
        if (item.id === 'user') {
          params.userName = item.values[0].id;
          params.memberType = item.id;
        } else if (item.id === 'department') {
          params.deptName = item.values[0].name;
          params.memberType = item.id;
        }
      })
      const res = await http.getProjectMembers(projectId, params);
      isLoading.value = false;
      memberList.value = res.records;
      if (!asideItem.value) {
        handleAsideClick(res.records[0])
      } else{
        handleAsideClick(asideItem.value)
      }
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