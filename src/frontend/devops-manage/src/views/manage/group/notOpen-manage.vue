<template>
  <article class="group-manage">
    <div class="content-wrapper">
      <div class="title">{{ title }}</div>
      <bk-button class="btn" theme="primary" @click="handleOpenManage">{{ t('开启权限管理') }}</bk-button>
    </div>
  </article>
</template>

<script lang="ts">
import { useGroup } from '@/store/group.ts'
import { useI18n } from 'vue-i18n';
export default {
  name: 'NotOpenManage',
  components: {
  },
  data() {
    const { t } = useI18n();
    return {
      t,
      type: '',
    };
  },
  created() {
    const store = useGroup();
    this.type = store.$state.resourceType;
  },
  computed: {
    title() {
      const titleMap = {
        pipeline: this.t('尚未开启此流水线权限管理功能'),
      }
      return titleMap[this.type]
    }
  },
  methods: {
    handleOpenManage() {
      this.$router.push({
        name: 'group',
      });
    },
  },
};
</script>

<style lang="postcss" scoped>
.group-manage {
  padding: 20px;
  flex: 1;
  .content-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    width: 100%;
    height: 100%;
    background-color: #fff;
    box-shadow: 0 2px 2px 0 rgba(0,0,0,0.15);
    text-align: center;
    font-size: 14px;
  }
  .btn {
    margin-top: 32px;
  }
}
</style>
