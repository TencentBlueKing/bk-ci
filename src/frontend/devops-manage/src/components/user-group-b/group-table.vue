<template>
  <article class="group-table">
    <bk-table
      :data="memberGroupList"
      :border="['outer']">
      <bk-table-column :label="$t('用户组')"></bk-table-column>
      <bk-table-column :label="$t('添加时间')"></bk-table-column>
      <bk-table-column :label="$t('有效期至')"></bk-table-column>
      <bk-table-column :label="$t('状态')"> </bk-table-column>
      <bk-table-column :label="$t('操作')">
        <template>
          <bk-button class="btn" theme="primary" text>{{ $t('权限详情') }}</bk-button>
          <bk-button class="btn" theme="primary" text>{{ $t('申请加入') }}</bk-button>
          <bk-button class="btn" theme="primary" text>{{ $t('续期') }}</bk-button>
          <bk-button class="btn" theme="primary" text>{{ $t('退出') }}</bk-button>
        </template>
      </bk-table-column>
    </bk-table>
    <bk-sideslider
      :is-show="showDetail"
      :width="640"
      quick-close
      @hidden="handleHidden"
    >
      <template #header>
        <div class="detail-title">
          {{ $t('权限详情') }}
          <span class="group-name">{{ 'CI-App2.0迭代-查看者' }}</span>
        </div>
      </template>
      <template #default>
        <div class="detail-content">
          todo..
        </div>
      </template>
    </bk-sideslider>
  </article>
</template>

<script>
export default {
  name: 'UserTable',
  props: {
    memberGroupList: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    return {
      showDetail: false,
    };
  },
  mounted() {

  },
  methods: {
    handleHidden() {
      this.showDetail = false;
    },
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
  .group-name {
    font-size: 12px;
    color: #979BA5;
    margin-left: 10px;
  }
</style>
