<template>
  <section class="permission-manage">
    <group-aside
      :show-create-group="showCreateGroup"
      :resource-type="resourceType"
      :resource-code="resourceCode"
      :resource-name="resourceName"
      :project-code="projectCode"
      :ajax-prefix="ajaxPrefix"
      @choose-group="handleChooseGroup"
      @create-group="handleCreateGroup"
      @close-manage="handleCloseManage"
      @change-group-detail-tab="handleChangeGroupDetailTab"
    />
    <iam-iframe
      v-if="path"
      :path="path"
    />
  </section>
</template>

<script>
import GroupAside from './group-aside.vue';
import IamIframe from './iam-Iframe.vue';

export default {
  name: 'permission-manage',

  components: {
    GroupAside,
    IamIframe,
  },

  props: {
    resourceType: {
      type: String,
      default: '',
    },
    resourceCode: {
      type: String,
      default: '',
    },
    projectCode: {
      type: String,
      default: '',
    },
    showCreateGroup: {
      type: Boolean,
      default: true,
    },
    resourceName: {
      type: String,
      default: '',
    },
    ajaxPrefix: {
      type: String,
      default: '',
    },
  },

  emits: ['close-manage'],

  data() {
    return {
      path: '',
      activeIndex: '',
    };
  },

  methods: {
    handleChangeGroupDetailTab(payload) {
      this.tabName = payload;
    },
    handleChooseGroup(payload) {
      this.path = `user-group-detail/${payload.groupId}?role_id=${payload.managerId}&tab=${this.tabName}`;
    },
    handleCreateGroup() {
      this.activeIndex = '';
      this.path = 'create-user-group';
    },
    handleCloseManage() {
      this.$emit('close-manage');
    },
  },
};
</script>

<style lang="scss" scoped>
.permission-manage {
    display: flex;
    height: 100%;
    box-shadow: 0 2px 2px 0 #00000026;
}
</style>
