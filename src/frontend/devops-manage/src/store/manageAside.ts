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
}

export default defineStore('manageAside', () => {
  const groupTableStore = userGroupTable();

  const isLoading = ref(false);
  const asideItem = ref<ManageAsideType>();
  const memberList = ref<ManageAsideType[]>([]);
  const personList = ref([]);
  const tableLoading = ref(false);
  const overTable = ref([]);
  const userName = ref('');
  const memberPagination = ref<Pagination>({ limit: 20, current: 1, count: 0 });
  const activeTab = ref();
  const btnLoading = ref(false);

  /**
   * 人员组织侧边栏点击事件
   */
  function handleAsideClick(item: ManageAsideType) {
    // 调用接口，获取侧边表格数据和折叠面板数
    asideItem.value = item;
    activeTab.value = item.id;
    groupTableStore.fetchUserGroupList(item);
  }
  /**
   * 人员组织侧边栏页码切换
   */
  async function handleAsidePageChange(current: number, projectId: string) {
    if (memberPagination.value.current !== current) {
      memberPagination.value.current = current;
      getProjectMembers(projectId);
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
  async function handleAsideRemoveConfirm(removeUser, handOverMember, projectId: string) {
    const params = {
      targetMember: removeUser,
      ...(handOverMember && {handoverTo: handOverMember})
    }
    try {
      btnLoading.value = true;
      await http.removeMemberFromProject(projectId, params);
      asideItem.value = undefined;
      Message({
        theme: 'success',
        message: `${removeUser!.name} 已成功移出本项目。`,
      });
      btnLoading.value = false;
      getProjectMembers(projectId);
    } catch (error) {

    }
  }
  /**
   * 获取项目下全体成员
   */
  async function getProjectMembers(projectId: string, searchValue?) {
    try {
      isLoading.value = true;
      const params: MemberListParamsType = {
        page: memberPagination.value.current,
        pageSize: memberPagination.value.limit,
      };
      searchValue?.forEach(item => {
        if (item.id === 'user') {
          params.userName = item.values[0].id;
          params.memberType = item.id;
        } else if (item.id === 'department') {
          params.deptName = item.values[0].id;
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
    overTable,
    userName,
    memberPagination,
    activeTab,
    btnLoading,
    handleAsideClick,
    handleAsidePageChange,
    handleShowPerson,
    handleAsideRemoveConfirm,
    getProjectMembers,
  };
});