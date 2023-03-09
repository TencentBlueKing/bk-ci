<template>
  <section class="group-wrapper">
    <template v-if="!isLoading">
      <!-- 管理员 -->
      <template v-if="isEnablePermission">
        <div
          v-if="hasPermission"
          class="group-manage"
        >
          <group-aside
            v-bind="$props"
            :active-index="activeIndex"
            :delete-group="handleDeleteGroup"
            @choose-group="handleChooseGroup"
            @create-group="handleCreateGroup"
            @update-enable="handelUpdateEnable"
          />
          <iam-iframe
            class="group-frame"
            :path="path"
          />
        </div>
        <!-- 项目维度 -> 无权限 -->
        <no-permission
          v-else-if="!hasPermission && resourceType === 'project'"
          :title="$t('无该项目用户组管理权限')"
          v-bind="$props"
          :resource-action="resourceAction"
        >
        </no-permission>
        <!-- 普通成员 -->
        <!-- <group-table v-else-if="!hasPermission && resourceType !== 'project'" v-bind="$props" /> -->
      </template>
      <!-- 未开启权限管理 -->
      <not-open-manage
        v-else-if="!isEnablePermission && resourceType !== 'project'"
        v-bind="$props"
      />
    </template>
  </section>
</template>

<script>
import GroupAside from './group-aside.vue';
import GroupTable from './group-table.vue';
import NotOpenManage from './not-open-manage.vue';
import NoPermission from './no-permission.vue';
import IamIframe from '../IAM-Iframe';
import {
  RESOURCE_ACTION,
} from '@/utils/permission.js'

export default {
  components: {
    GroupAside,
    GroupTable,
    NotOpenManage,
    IamIframe,
    NoPermission,
  },

  props: {
    // 资源类型
    resourceType: {
      type: String,
      default: '',
    },
    // 资源ID
    resourceCode: {
      type: String,
      default: '',
    },
    // 项目id => englishName
    projectCode: {
      type: String,
      default: '',
    },
    groupList: {
      type: Array,
      default: () => [],
    },
    memberGroupList: {
      type: Array,
      default: () => [],
    },
    hasPermission: {
      type: Boolean,
      default: true,
    },
    isEnablePermission: {
      type: Boolean,
      default: false,
    },
    iamIframePath: {
      type: String,
      default: '',
    },
    openManage: {
      type: Function,
      default: () => {},
    },
    closeManage: {
      type: Function,
      default: () => {},
    },
    deleteGroup: {
      type: Function,
      default: () => {},
    },
    isLoading: {
      type: Boolean,
      default: false,
    },
    fetchGroupList: {
      type: Function,
      default: () => {},
    },
    renameGroupName: {
      type: Function,
      default: () => {},
    },
  },

  emits: ['delete-group'],

  computed: {
    resourceAction() {
      return RESOURCE_ACTION.MANAGE;
    },
  },

  data() {
    return {
      path: '',
    };
  },

  watch: {
    iamIframePath: {
      handler() {
        this.path = this.iamIframePath;
      },
      immediate: true,
    },
  },

  async created() {
    window.addEventListener('message', this.handleMessage);
  },

  beforeUnmount() {
    window.removeEventListener('message', this.handleMessage);
  },

  methods: {
    handleChooseGroup(payload) {
      this.path = `user-group-detail/${payload.groupId}?role_id=${payload.managerId}`;
    },

    handleCreateGroup() {
      this.activeIndex = '';
      this.path = 'create-user-group';
    },

    async handleComfigCreate() {
      await this.fetchGroupList();
      setTimeout(() => {
        this.handleCancelCreate(this.groupList.length - 1);
        this.activeIndex = this.groupList.length - 1;
      });
    },

    handleCancelCreate(index = 0) {
      this.activeIndex = 0;
      this.path = `user-group-detail/${this.groupList[index].groupId}?role_id=${this.groupList[index].managerId}`;
    },

    handleMessage(event) {
      const { data } = event;
      if (data.type === 'IAM') {
        switch (data.code) {
          case 'cancel':
            this.handleCancelCreate();
            break;
          case 'success':
            this.handleComfigCreate();
            break;
        }
      }
    },

    handleDeleteGroup(group) {
      return this.deleteGroup(group);
    },

    handelUpdateEnable() {
      // this.isEnablePermission = payload;
    },
  },
};
</script>

<style lang="postcss" scoped>
  .group-wrapper {
    display: flex;
    flex: 1;
    width: 100%;
  }
  .group-manage {
    display: flex;
    flex: 1;
    overflow: hidden;
  }
  .group-frame {
    flex: 1;
  }
</style>
