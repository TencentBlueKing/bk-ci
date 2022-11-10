<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import {
  useRouter,
} from 'vue-router';
const { t } = useI18n();
const router = useRouter();


const handleEdit = () => {
  router.push({
    path: 'edit',
  });
};
</script>

<template>
  <section class="content-wrapper">
    <article class="project-info-content">
      <bk-alert type="success" title="新建项目申请目前正在审批中，可前往查看" closable></bk-alert>
      <section class="content-main">
        <bk-form class="detail-content-form" :label-width="160">
          <bk-form-item :label="t('项目名称')" :property="'name'">
            <span class="item-value">王者荣耀</span>
          </bk-form-item>
          <bk-form-item :label="t('项目ID')" :property="'name'">
            <span class="item-value">wzry</span>
          </bk-form-item>
          <bk-form-item :label="t('项目描述')" :property="'name'">
            <span class="item-value">《王者荣耀》是腾讯第一5V5团队公平竞技手游，国民MOBA手游大作！5V5王者峡谷、公平对战、还原MOBA经典体验；契约之战、
              五军对决、边境突围等，带来花式作战乐趣！10秒实时跨区匹配，与好友开黑上分，向最强王者进击！多款英雄任凭选择，一血、五杀、超神，
              实力碾压，收割全场！敌军即将到达战场，王者召唤师快来集结好友，准备团战，就在《王者荣耀》！</span>
          </bk-form-item>
          <bk-form-item :label="t('项目所属组织')" :property="'name'">
            <span class="item-value">IEG互动娱乐事业群-技术运营部-蓝鲸产品中心-蓝鲸运营组；</span>
          </bk-form-item>
          <bk-form-item :label="t('项目性质')" :property="'name'">
            <span class="item-value">私有项目</span>
          </bk-form-item>
          <bk-form-item :label="t('项目最大可授权人员范围')" :property="'name'">
            <span class="item-value">腾讯公司</span>
          </bk-form-item>
          <bk-form-item>
            <bk-button class="btn mr10" theme="primary" @click="handleEdit">{{ t('编辑') }}</bk-button>
            <bk-button class="btn" theme="default">{{ t('取消创建') }}</bk-button>
          </bk-form-item>
        </bk-form>

      </section>
    </article>
  </section>
</template>

<style lang="postcss" scoped>
  .content-wrapper {
    height: 100%;
    overflow: auto;
    &::-webkit-scrollbar-thumb {
      background-color: #c4c6cc !important;
      border-radius: 5px !important;
      &:hover {
        background-color: #979ba5 !important;
      }
    }
    &::-webkit-scrollbar {
      width: 8px !important;
      height: 8px !important;
    }
  }
  .project-info-content {
    display: flex;
    flex-direction: column;
    padding: 24px;
    height: 100%;
    &::-webkit-scrollbar-thumb {
      background-color: #c4c6cc !important;
      border-radius: 5px !important;
      &:hover {
        background-color: #979ba5 !important;
      }
    }
    &::-webkit-scrollbar {
      width: 8px !important;
      height: 8px !important;
    }
  }
  .content-main {
    flex: 1;
    padding: 32px 48px;
    margin-top: 16px;
    background-color: #fff;
    box-shadow: 0 2px 2px 0 rgba(0,0,0,0.15);
  }

  .detail-content-form {
    :deep(.bk-form-label) {
      font-size: 12px;
      text-align: left;
      color: #979BA5;
    }
    .item-value {
      display: inline-block;
      max-width: 800px;
    }
    .mr10 {
      margin-right: 10px;
    }
    .btn {
      width: 88px;
    }
  }
</style>
