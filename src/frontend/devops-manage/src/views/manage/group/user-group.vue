<template>
  <article class="group-manage">
    <!-- 管理员 -->
    <template v-if="hasPermission">
      <group-aside></group-aside>
      <IAMIframe
        class="group-frame"
        path="user-group-detail/10137"
        :query="{
          role_id: 1
        }"
      />
    </template>
    <!-- 为开启权限管理 -->
    <not-open-manage v-else-if="hasPermission && true"></not-open-manage>
    <!-- 普通成员 -->
    <template v-else-if="!hasPermission">
      <group-table></group-table>
    </template>
  </article>
</template>

<script lang="ts">
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

  },
  data() {
    return {
      hasPermission: true,
    };
  },
  mounted() {
  },
  methods: {

  },
};
</script>

<style lang="postcss" scoped>
  .group-manage {
    display: flex;
    flex: 1;
    overflow: hidden;
  }
  .group-frame {
    flex: 1;
  }
</style>
