import http from '@/http/api';
import { defineStore } from 'pinia';
import { ref } from 'vue';
import { Message } from 'bkui-vue';
import userGroupTable from "@/store/userGroupTable";

export interface GroupTableType {
  groupId: number;
  groupName: string;
  groupDesc: string;
  expiredAtDisplay: string;
  joinedTime: string;
  operateSource: string;
  operator: string;
  removeMemberButtonControl: 'OTHER' | 'TEMPLATE' | 'UNIQUE_MANAGER';
};

export default defineStore('manageAside', () => {
  const groupTableStore = userGroupTable();

  const isLoading = ref(false);
  const asideItem = ref(null);
  const memberList = ref([]);
  const personList = ref([]);
  const overTable = ref([]);
  const userName = ref('');
  const memberPagination = ref({ limit: 10, current: 1 });

  /**
   * 人员组织侧边栏点击事件
   */
  function handleAsideClick(item) {
    // 调用接口，获取侧边表格数据和折叠面板数
    asideItem.value = item;
    groupTableStore.fetchUserGroupList(item);
  }
  /**
   * 人员组织侧边栏页码切换
   */
  async function handleAsidePageChange(current, projectId) {
    if (memberPagination.value.current !== current) {
      memberPagination.value.current = current;
      getProjectMembers(projectId);
    }
  }
  /**
   * 人员列表数据获取
   */
  async function handleShowPerson(value) {
    const res = await http.deptUsers(value.id);
    personList.value = res.map(item => ({ person: item }));
  }
  /**
   * 组织移出项目
   */
  const flag = ref(true);
  async function handleAsideRemoveConfirm(value, manageAsideRef) {
    const res = await http.removeMemberFromProject(value.id, {
      type: value.type,
      member: value.name,
    });
    // 这里根据返回判断移出成功和失败的情况而不用flag
    if (value.type == 'DEPARTMENT') {
      Message({
        theme: 'success',
        message: `${value.name} 已成功移出本项目。`,
      });
    } else {
      // 这里根据返回判断移出成功和失败的情况
      if (flag.value) {
        manageAsideRef.handOverfail(true);
        overTable.value = [
          {
            id: 1,
            code: 'bkdevops-plugins-test/fayenodejstesa',
            reason: '指定用户未操作过 OAuth',
            percent: '',
          },
          {
            id: 2,
            code: 'bkdevops-plugins-test/fayenodejstesa',
            reason: '指定用户没有此代码库权限',
            percent: '',
          },
        ];
        flag.value = false;
      } else {
        console.log(overTable.value, '移交失败表格数据');
        Message({
          theme: 'success',
          message: `${value.name} 已成功移出本项目。`,
        });
        manageAsideRef.handOverClose();
      }
    }
  }
  /**
   * 获取项目下全体成员
   */
  async function getProjectMembers(projectId: string) {
    try {
      isLoading.value = true;
      const params = {
        page: memberPagination.value.current,
        pageSize: memberPagination.value.limit,
        ...(userName.value && { userName: userName.value }),
      };
      const res = await http.getProjectMembers(projectId, params);
      isLoading.value = false;
      memberList.value = res.records
    } catch (error) {
      isLoading.value = false;
    }
  }

  return {
    isLoading,
    asideItem,
    memberList,
    personList,
    overTable,
    userName,
    memberPagination,
    handleAsideClick,
    handleAsidePageChange,
    handleShowPerson,
    handleAsideRemoveConfirm,
    getProjectMembers,
  };
});