<template>
  <bk-loading class="manage" :loading="isLoading">
    <div class="manage-search">
      <select-group
        :value="serveValue"
        :options="serveList"
        prefix="所属服务"
        @handle-change="value => handleChange(value, 'serve')"
      />
      <select-group
        :value="resourceValue"
        :options="resourceList"
        prefix="资源"
        @handle-change="value => handleChange(value, 'resource')"
      />
      <select-group
        :value="operateValue"
        :options="operateList"
        prefix="操作"
        @handle-change="value => handleChange(value, 'operate')"
      />
      <bk-search-select
        v-model="searchValue"
        :data="searchData"
        unique-select
        class="multi-search"
        placeholder="用户/组织架构/用户组名/用户组 ID/用户组描述"
      />
    </div>
    <div class="manage-article">
      <div class="manage-aside">
        <manage-aside :member-list="memberList" @handle-click="handleClick" />
      </div>
      <div class="manage-content">
        <div class="manage-content-btn">
          <bk-button @click="handleReset">{{ t('批量续期') }}</bk-button>
          <bk-button @click="handleReset">{{ t('批量移交') }}</bk-button>
          <bk-button @click="handleReset">{{ t('批量移出') }}</bk-button>
        </div>
        <div class="manage-content-project">4</div>
        <div class="manage-content-resource">5</div>
      </div>
    </div>
  </bk-loading>
</template>

<script setup name="manageAll">
import { useI18n } from 'vue-i18n';
import { ref, watch  } from 'vue';
import SelectGroup from './select-group.vue';
import ManageAside from './manage-aside.vue';

const { t } = useI18n();
const isLoading = ref(false);

const serveValue = ref([]);
const serveList = ref([
  {
    value: 'climbing',
    label: '爬山',
  },
  {
    value: 'running',
    label: '跑步',
  },
]);

const resourceValue = ref([]);
const resourceList = ref([
  {
    value: 'unknow',
    label: '未知',
  },
  {
    value: 'fitness',
    label: '健身',
  },
]);

const operateValue = ref([]);
const operateList = ref([
  {
    value: 'bike',
    label: '骑车',
  },
  {
    value: 'dancing',
    label: '跳舞',
  },
]);

const searchValue = ref([]);
const searchData = ref([
  {
    name: '实例业务',
    id: '2',
    onlyRecommendChildren: true,
    children: [
      {
        name: '王者荣耀',
        id: '2-1',
      },
      {
        name: '刺激战场',
        id: '2-2',
      },
      {
        name: '绝地求生',
        id: '2-3',
      },
    ],
  },
  {
    name: 'IP地址',
    id: '3',
  },
  {
    name: 'testestset',
    id: '4',
  },
]);

const stateRefs = {
  serve: serveValue,
  resource: resourceValue,
  operate: operateValue,
};

const memberList = ref([
  {
    projectId: 1,
    bgName: 'IEG互动娱乐事业群',
    centerName: 'SRE平台研发中心',
    deptName: '技术运营部',
    approver: 'v_hejieehe',
    subjectScopes: [
      {
        name: '余姣姣',
        full_name: 'v_yjjiaoyu',
        expiredId: 1743602525,
      },
      {
        name: '何福寿',
        full_name: 'terlinhe',
        expiredId: 4102444800,
      },
    ],
  },
]);

watch([serveValue, resourceValue, operateValue, searchValue], () => {
  // 侦听值的变化，调用接口获取筛选数据
  console.log(serveValue.value, resourceValue.value, operateValue.value, searchValue.value, '搜索的数据');
});

function handleChange(value, target) {
  stateRefs[target].value = value;
}

function handleClick(id) {
  console.log(id, '这里根据id展示表格数据');
}

</script>

<style lang="scss" scoped>
.manage{
  width: 100%;

  .manage-search{
    display: flex;
    width: 100%;
    height: 64px;
    background: #FFFFFF;
    padding: 16px 24px;
    box-shadow: 0 2px 4px 0 #1919290d;

    .multi-search{
      flex: 1;
    }
  }

  .manage-article{
    display: flex;
    margin: 16px 0 24px 24px;

    .manage-aside{
      width: 230px;
      height: 686px;
      padding: 8px 0 0;
      background: #FFFFFF;
      box-shadow: 0 2px 4px 0 #1919290d;
    }

    .manage-content{
      flex: 1;
      margin-left: 16px;
      overflow-y: auto;

      .manage-content-btn{
        margin-bottom: 10px;

        .bk-button{
          margin-right: 8px
        }
      }

      .manage-content-project{
        height: 280px;
        margin-bottom: 15px;
        background: #FFFFFF;
        box-shadow: 0 2px 4px 0 #1919290d;
      }

      .manage-content-resource{
        margin-bottom: 24px;
        background: #FFFFFF;
        box-shadow: 0 2px 4px 0 #1919290d;
      }
    }
  }
}
</style>
