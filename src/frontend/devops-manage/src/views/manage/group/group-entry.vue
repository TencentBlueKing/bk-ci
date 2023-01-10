<template>
  <user-group
    :resource-type="resourceType"
    :resource-code="projectCode"
    :project-code="projectCode"
    :group-list="groupList"
    :member-group-list="memberGroupList"
    :iam-iframe-path="iamIframePath"
    :has-permission="hasPermission"
    :is-enable-permission="isEnablePermission"
    :open-manage="handleOpenManage"
    :close-manage="handleCloseManage"
  />
</template>

<script lang="ts">
import http from '@/http/api';
import UserGroup from '@/components/user-group/group-entry.vue';
import { Message } from 'bkui-vue';

export default {
  components: {
    UserGroup,
  },

  data() {
    return {
      hasPermission: false,
      isEnablePermission: false,
      iamIframePath: 'user-group-detail/29912',
      resourceType: 'project',
      groupList: [],
      memberGroupList: [],
    };
  },

  computed: {
    projectCode() {
      return this.$route.params.projectCode;
    },
  },

  async created() {
    await this.fetchHasManagerPermission();
    await this.fetchEnablePermission();
    // 管理员获取用户组数据
    if (this.isEnablePermission && this.hasPermission) {
      await this.fetchGroupList();
    }
    // 普通成员获取成员数据
    if (this.isEnablePermission && !this.hasPermission) {
      await this.fetchMemberGroupList();
    }
  },

  methods: {
    /**
     * 是否为资源的管理员
     */
    fetchHasManagerPermission() {
      const {
        projectCode,
        resourceType,
        projectCode: resourceCode,
      } = this;

      return http.fetchHasManagerPermission({
        projectCode,
        resourceType,
        resourceCode,
      }).then((res) => {
        this.hasPermission = res;
      });
    },
    /**
     * 是否开启了权限管理
     */
    fetchEnablePermission() {
      const {
        projectCode,
        resourceType,
        projectCode: resourceCode,
      } = this;

      return http
        .fetchEnablePermission({
          projectCode,
          resourceType,
          resourceCode,
        })
        .then((res) => {
          this.isEnablePermission = res;
        });
    },
    /**
     * 开启权限管理
     */
    handleOpenManage() {
      const {
        resourceType,
        projectCode: resourceCode,
        projectCode,
      } = this;

      return http
        .enableGroupPermission({
          resourceType,
          resourceCode,
          projectCode,
        })
        .then((res) => {
          if (res) {
            Message({
              theme: 'success',
              message: this.$t('开启成功'),
            });
            this.isEnablePermission = res;
          }
        });
    },
    /**
     * 关闭权限管理
     */
    handleCloseManage() {
      const {
        resourceType,
        projectCode: resourceCode,
        projectCode,
      } = this;

      return http
        .disableGroupPermission({
          resourceType,
          resourceCode,
          projectCode,
        })
        .then((res) => {
          if (res) {
            Message({
              theme: 'success',
              message: this.$t('关闭成功'),
            });
          }
          this.isEnablePermission = res;
        });
    },

    /**
     * 获取用户组列表 (管理员、创建者)
     */
    fetchGroupList() {
      const {
        resourceType,
        projectCode: resourceCode,
        projectCode,
      } = this;

      return http
        .fetchUserGroupList({
          resourceType,
          resourceCode,
          projectCode,
        })
        .then((res) => {
          this.groupList = res.data;
        });
    },

    /**
     * 获取用户所属组 (普通成员)
     */
    fetchMemberGroupList() {
      const {
        resourceType,
        projectCode: resourceCode,
        projectCode,
      } = this;

      return http
        .fetchGroupMember({
          resourceType,
          resourceCode,
          projectCode,
        })
        .then((res) => {
          this.memberGroupList = res.data;
        });
    },
  },
};
</script>

<style lang="postcss" scoped>

</style>
