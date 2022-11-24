<template>
  <article class="group-manage">
    <!-- 管理员 -->
    <div v-if="hasPermission">
      <group-aside></group-aside>
    </div>
    <!-- 为开启权限管理 -->
    <not-open-manage v-else-if="hasPermission && true"></not-open-manage>
    <!-- 普通成员 -->
    <div v-else-if="!hasPermission">
      <group-table></group-table>
    </div>
  </article>
</template>

<script lang="ts">
import groupAside from './group-aside.vue';
import groupTable from './group-table.vue';
import notOpenManage from './notOpen-manage.vue';
export default {
  name: 'UserGroup',
  components: {
    groupAside,
    groupTable,
    notOpenManage,
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
  }
</style>
