<template>
  <user-group
    :resourceType="resourceType"
    :resourceCode="resourceCode"
    :projectCode="projectCode"
    :group-list="groupList"
    :member-group-list="memberGroupList"
    :IAMIframePath="IAMIframePath"
    :hasPermission="hasPermission"
    :isEnablePermission="isEnablePermission"
    :open-manage="handleOpenManage"
    :close-manage="handleCloseManage"
  ></user-group>
</template>

<script lang="ts">
import http from '@/http/api';
import userGroup from '../../../../../common-lib/user-group/index.vue'
import { Message } from 'bkui-vue';
export default {
  components: {
    userGroup,
  },
  props: {
    // 资源类型
    resourceType: {
      type: String,
      default: 'pipeline',
    },
    // 资源ID
    resourceCode: {
      type: String,
      default: 'p-39dca34a469d4691a70f4a6fdb90eb96',
    },
    // 项目id => englishName
    projectCode: {
      type: String,
      default: 'hw-test-6',
    },
  },
  data() {
    return {
      hasPermission: false,
      isEnablePermission: false,
      IAMIframePath: 'user-group-detail/29912',
      groupList: [
        {
          name: '管理员',
          id: 1,
          user: 10,
          group: 10,
        },
        {
          name: '查看项目权限组',
          id: 2,
          user: 10,
          group: 10,
        },
        {
          name: '开发人员',
          user: 10,
          group: 10,
          id: 3,
        },
        {
          name: '产品人员',
          user: 10,
          group: 10,
          id: 4,
        },
        {
          name: '测试人员',
          user: 10,
          group: 10,
          id: 5,
        },
        {
          name: '运维人员',
          user: 10,
          group: 10,
          id: 6,
        },
        {
          name: '质管人员',
          user: 10,
          group: 10,
          id: 7,
        },
      ],
      memberGroupList: [{}, {}, {}, {}]
    };
  },
  watch: {
    // isEnablePermission (val) {
    //   if (val) this.fetchGroupList();
    // }
  },
  async created() {
    await this.fetchHasManagerPermission();
    await this.fetchEnablePermission();
    await this.fetchGroupList();
  },
  methods: {
    /**
     * 是否为资源的管理员
     */
    fetchHasManagerPermission() {
      const { projectCode, resourceType, resourceCode } = this;
      http.fetchHasManagerPermission({
        projectCode,
        resourceType,
        resourceCode,
      }).then(res => {
        this.hasPermission = res;
      });
    },
    /**
     * 是否开启了权限管理
     */
    fetchEnablePermission() {
      const { projectCode, resourceType, resourceCode } = this;
      http.fetchEnablePermission({
        projectCode,
        resourceType,
        resourceCode,
      }).then(res => {
        this.isEnablePermission = res;
      });
    },
    /**
     * 开启权限管理
     */
    handleOpenManage() {
      const { resourceType, resourceCode, projectCode } = this;
      http.enableGroupPermission({
        resourceType,
        resourceCode,
        projectCode,
      }).then(res => {
        if (res) {
          Message({
            theme: 'success',
            message: this.t('开启成功'),
          });
          this.isEnablePermission = res;
        }
      });
    },
    /**
     * 关闭权限管理
     */
    handleCloseManage() {
      const { resourceType, resourceCode, projectCode } = this;
      http.disableGroupPermission({
        resourceType,
        resourceCode,
        projectCode,
      }).then(res => {
        if (res) {
          Message({
            theme: 'success',
            message: this.t('关闭成功'),
          });
        }
        this.isEnablePermission = res;
      });
    },

    /**
     * 获取用户组列表 (管理员、创建者)
     */
    fetchGroupList() {
      const { resourceType, resourceCode, projectCode } = this;
      http.fetchUserGroupList({
        resourceType,
        resourceCode,
        projectCode,
      }).then(res => {
        this.groupList = res.data;
      })
    },


    /**
     * 获取用户所属组 (普通成员)
     */
    fetchMemberGroupList() {
      const { resourceType, resourceCode, projectCode } = this;
      http.fetchGroupMember({
        resourceType,
        resourceCode,
        projectCode,
      }).then(res => {
        this.memberGroupList = res.data;
      })
    },
  },
};
</script>

<style lang="postcss" scoped>

</style>
