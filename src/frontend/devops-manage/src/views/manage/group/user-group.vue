<template>
  <article class="group-wrapper">
    <!-- 管理员 -->
    <template v-if="hasPermission">
      <div class="group-manage" v-if="true">
        <group-aside
          @choose-group="handleChooseGroup"
          @create-group="handleCreateGroup"
        ></group-aside>
        <IAMIframe
          class="group-frame"
          :path="path"
        />
      </div>
      <!-- 未开启权限管理 -->
      <not-open-manage v-else></not-open-manage>
    </template>
    <!-- 普通成员 -->
    <template v-else-if="!hasPermission">
      <group-table></group-table>
    </template>
  </article>
</template>

<script lang="ts">
import http from '@/http/api';
import { useGroup } from '@/store/group.ts'
import groupAside from './group-aside.vue';
import groupTable from './group-table.vue';
import notOpenManage from './notOpen-manage.vue';
import IAMIframe from '@/components/IAM-Iframe';
export default {
  name: 'UserGroup',
  components: {
    groupAside,
    groupTable,
    notOpenManage,
    IAMIframe,
  },
  props: {
    resourceType: {
      type: String,
      default: 'pipeline',
    },
    resourceCode: {
      type: String,
      default: '',
    },
    projectId: {
      type: String,
      default: '',
    },
  },
  data() {
    return {
      hasPermission: true,
      path: 'user-group-detail/29918',
    };
  },
  computed: {

  },
  created() {
    this.initStore();
    window.addEventListener('message', this.handleMessage);
    this.fetchResourceManager();
  },
  beforeUnmount() {
    window.removeEventListener('message', this.handleMessage);
  },
  methods: {
    handleChooseGroup() {
      this.path = 'user-group-detail/10137';
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
    initStore() {
      const store = useGroup(); 
      store.setResourceType(this.resourceType);
      store.setResourceCode(this.resourceCode);
    },
    /**
     * 是否为资源的管理员
     */
    fetchResourceManager() {
      http.fetchResourceManager({
        id: '322956612c1049d9a4b2c803cc473555',
        resourceType: 'pipeline',
        resourceCode: 'p-39dca34a469d4691a70f4a6fdb90eb96'
      }).then(res => {
        console.log(res, 12312)
      })
    }
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
