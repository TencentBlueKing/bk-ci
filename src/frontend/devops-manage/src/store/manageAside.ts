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

  /**
   * 人员组织侧边栏点击事件
   */
  function handleAsideClick(item: ManageAsideType) {
    // 调用接口，获取侧边表格数据和折叠面板数
    asideItem.value = item;
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
  // const flag = ref(true);
  async function handleAsideRemoveConfirm(handOverMember: ManageAsideType, projectId: string, manageAsideRef) {
    const params = {
      targetMember: asideItem.value,
      // ...(handOverMember && {handoverTo: handOverMember})
      ...(handOverMember && {handoverTo: {
        id: 'greysonfang',
        name: '方灿',
        type: 'user'
      }})
    }
    const res = await http.removeMemberFromProject(projectId, params);
    // 这里根据返回判断移出成功和失败的情况而不用flag
    // if (value.type == 'DEPARTMENT') {
    //   Message({
    //     theme: 'success',
    //     message: `${value.name} 已成功移出本项目。`,
    //   });
    // } else {
    //   // 这里根据返回判断移出成功和失败的情况
    //   if (flag.value) {
    //     manageAsideRef.handOverfail(true);
    //     overTable.value = [
    //       {
    //         id: 1,
    //         code: 'bkdevops-plugins-test/fayenodejstesa',
    //         reason: '指定用户未操作过 OAuth',
    //         percent: '',
    //       },
    //       {
    //         id: 2,
    //         code: 'bkdevops-plugins-test/fayenodejstesa',
    //         reason: '指定用户没有此代码库权限',
    //         percent: '',
    //       },
    //     ];
    //     flag.value = false;
    //   } else {
    //     console.log(overTable.value, '移交失败表格数据');
    //     Message({
    //       theme: 'success',
    //       message: `${value.name} 已成功移出本项目。`,
    //     });
    //     manageAsideRef.handOverClose();
    //   }
    // }
    Message({
      theme: 'success',
      message: `${asideItem.value!.name} 已成功移出本项目。`,
    });
    getProjectMembers(projectId);
  }
  /**
   * 获取项目下全体成员
   */
  async function getProjectMembers(projectId: string, searchValue?) {
    try {
      isLoading.value = true;
      const params = {
        page: memberPagination.value.current,
        pageSize: memberPagination.value.limit,
      };
      searchValue?.forEach(item => {
        if (item.id === 'user') {
          params.userName = item.values[0].name;
          params.memberType = item.id;
        } else if (item.id === 'department') {
          params.deptName = item.values[0].name;
          params.memberType = item.id;
        }
      })
      const res = await http.getProjectMembers(projectId, params);
      isLoading.value = false;
      memberList.value = res.records
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
    handleAsideClick,
    handleAsidePageChange,
    handleShowPerson,
    handleAsideRemoveConfirm,
    getProjectMembers,
  };
});