<template>
  <article class="group-wrapper">
    <!-- 管理员 -->
    <template v-if="hasPermission">
      <div
        v-if="isEnablePermission"
        class="group-manage"
      >
        <group-aside
          v-bind="$props"
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
      <!-- 普通成员 -->
      <group-table v-else v-bind="$props" />
    </template>
    <!-- 未开启权限管理 -->
    <not-open-manage v-else v-bind="$props" />
  </article>
</template>

<script>
import GroupAside from './group-aside.vue';
import GroupTable from './group-table.vue';
import NotOpenManage from './not-open-manage.vue';
import IamIframe from '../IAM-Iframe';

export default {
  components: {
    GroupAside,
    GroupTable,
    NotOpenManage,
    IamIframe,
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
  },

  emits: ['delete-group'],

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
      this.path = `user-group-detail/${payload.id}`;
    },

    handleCreateGroup() {
      this.path = 'create-user-group';
    },

    handleMessage(event) {
      const { data } = event;
      if (data.type === 'IAM') {
        switch (data.code) {
          case 'cancel':
            this.handleChooseGroup();
            break;
          case 'success':
            this.handleChooseGroup();
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
