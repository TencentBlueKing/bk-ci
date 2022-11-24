<template>
  <article class="group-table">
    <bk-table
      :data="data">
      <bk-table-column :label="$t('用户组')"></bk-table-column>
      <bk-table-column :label="$t('添加时间')"></bk-table-column>
      <bk-table-column :label="$t('有效期至')"></bk-table-column>
      <bk-table-column :label="$t('状态')"> </bk-table-column>
      <bk-table-column :label="$t('操作')">
        <template #default="{ row }">
          <bk-button class="btn" theme="primary" text>{{ $t('权限详情') }}</bk-button>
          <bk-button class="btn" theme="primary" text>{{ $t('申请加入') }}</bk-button>
          <bk-button class="btn" theme="primary" text>{{ $t('续期') }}</bk-button>
          <bk-button class="btn" theme="primary" text>{{ $t('退出') }}</bk-button>
        </template>
      </bk-table-column>
    </bk-table>
  </article>
</template>

<script lang="ts">
export default {
  name: 'UserTable',
  components: {
  },
  props: {

  },
  data() {
    return {
      data: [
        {}, {}, {},
      ],
    };
  },
  mounted() {

  },
  methods: {

  },
};
</script>

<style lang="postcss" scoped>
  .group-table {
    padding: 25px;
  }
  .btn {
    margin-right: 5px;
  }
</style>
