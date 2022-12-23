<template>
  <article class="group-wrapper">
    <!-- 管理员 -->
    <template v-if="hasPermission">
      <div class="group-manage" v-if="!isEnablePermission">
        <group-aside
          v-bind="$props"
          @choose-group="handleChooseGroup"
          @create-group="handleCreateGroup"
          @update-enable="handelUpdateEnable"
        >
        </group-aside>
        <IAMIframe
          class="group-frame"
          :path="path"
        />
      </div>
      <!-- 未开启权限管理 -->
      <not-open-manage
        v-else
        v-bind="$props"
      >
      </not-open-manage>
    </template>
    <!-- 普通成员 -->
    <template v-else-if="!hasPermission">
      <group-table v-bind="$props"></group-table>
    </template>
  </article>
</template>

<script lang="ts">
// import http from '@/http/api';
import groupAside from './group-aside.vue';
import groupTable from './group-table.vue';
import notOpenManage from './notOpen-manage.vue';
import IAMIframe from '../../devops-manage/src/components/IAM-Iframe';
export default {
  components: {
    groupAside,
    groupTable,
    notOpenManage,
    IAMIframe,
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
    IAMIframePath: {
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
    }
  },
  data() {
    return {
      path: ''
    };
  },
  computed: {

  },
  watch: {
    
  },
  async created() {
    this.path = this.IAMIframePath;
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
    handleMessage(event: any) {
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

    handelUpdateEnable(payload) {
      this.isEnablePermission = payload;
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
