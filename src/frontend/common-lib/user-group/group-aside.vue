<template>
  <article class="group-aside">
    <section class="group-list">
      <div
        :class="{ 'group-item': true, 'group-active': activeTab === group.name }"
        v-for="(group, index) in groupList"
        :key="index"
        @click="handleChangeTab(group)">
        <span class="group-name">{{ group.name }}</span>
        <span class="user-num">
          <i class="manage-icon manage-icon-user-shape"></i>
          {{ group.user }}
        </span>
        <span class="group-num">
          <i class="manage-icon manage-icon-user-shape"></i>
          {{ group.group }}
        </span>
        <bk-popover
          v-if="resourceType === 'project'"
          class="group-more-option"
          placement="bottom"
          theme="dot-menu light"
          :arrow="false"
          offset="15"
          :distance="0">
          <i class="more-icon manage-icon manage-icon-more-fill"></i>
          <template #content>
            <bk-button
              class="btn"
              :disabled="[1, 2].includes(group.id)"
              text
              @click="handleDeleteGroup(group)">
              {{ $t('删除') }}
            </bk-button>
          </template>
        </bk-popover>
      </div>
      <div class="line-split" />
      <div
        :class="{ 'group-item': true, 'group-active': activeTab === '' }"
        @click="handleCreateGroup">
        <span class="add-group-btn">
          <i class="manage-icon manage-icon-add-fill add-icon"></i>
          {{ $t('新建用户组') }}
        </span>
      </div>
    </section>
    <div class="close-btn">
      <bk-button @click="handleCloseManage">{{ $t('关闭权限管理') }}</bk-button>
    </div>
  </article>
</template>

<script lang="ts">
import { InfoBox, Message } from 'bkui-vue';
import { useI18n } from 'vue-i18n';

export default {
  name: 'GroupAside',
  emits: ['create-group', 'choose-group'],
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
    deleteGroup: {
      type: Function,
      default: () => {},
    },
    closeManage: {
      type: Function,
      default: () => {},
    },
  },
  data() {
    return {
      activeTab: '管理员',
    };
  },
  mounted() {
  },
  methods: {
    handleDeleteGroup(group: any) {
      InfoBox({
        title: this.$t('是否删除用户组', [group.name]),
        contentAlign: 'center',
        headerAlign: 'center',
        footerAlign: 'center',
        onConfirm() {
          this.deleteGroup();
        },
      });
    },
    handleChangeTab(group: any) {
      this.activeTab = group.name;
      this.$emit('choose-group', group);
    },
    handleCreateGroup() {
      this.activeTab = '';
      this.$emit('create-group');
    },
    handleCloseManage() {
      this.closeManage();
    },
  },
};
</script>

<style lang="postcss" scoped>
  .group-aside {
    width: 240px;
    height: 100%;
    background-color: #FAFBFD;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
  }
  .group-list {
    margin-top: 8px;
  }
  .group-item {
    display: flex;
    align-items: center;
    width: 100%;
    height: 40px;
    line-height: 40px;
    font-size: 14px;
    padding-left: 24px;
    color: #63656E;
    cursor: pointer;
    &:hover {
      color: #3A84FF;
      background-color: #E1ECFF;
      
    }
  }
  .group-item:hover {
    color: #3A84FF;
    background-color: #E1ECFF;
  }
  .group-item:hover .user-num,
  .group-item:hover .group-num {
    background-color: #A3C5FD;
    color: #fff;
  }
 
  .group-active {
    color: #3A84FF;
    background-color: #E1ECFF;
  }
  .user-num,
  .group-num {
    background-color: #A3C5FD;
    color: #fff;
  }
  .group-name {
    display: inline-block;
    min-width: 110px;
  }
  .user-num,
  .group-num {
    display: flex;
    align-items: center;
    justify-content: space-evenly;
    width: 40px;
    height: 16px;
    background: #F0F1F5;
    border-radius: 2px;
    font-size: 12px;
    line-height: 16px;
    margin-right: 3px;
    text-align: center;
    color: #979BA5;
  }
  .more-icon {
    border-radius: 50%;
    padding: 1px;
  }
  .more-icon:hover {
    background-color: #d5ddef;
    color: #3A84FF !important;
  }
  .line-split {
    width: 80%;
    height: 1px;
    background: #ccc;
    margin: 10px auto;
  }
  .add-group-btn {
    display: flex;
    align-items: center;
  }
  .add-icon {
    margin-right: 10px;
  }

  .group-more-option .btn {
    width: 60px;
    height: 32px;
    line-height: 32px;
    text-align: center;
    font-size: 12px;
    margin-top: 0;
  }
  .group-more-option .btn:hover {
    background-color: #F5F7FA;
  }
  :deep(.bk-popover-content) {
    padding: 0;
  }
  .close-btn {
    margin-bottom: 20px;
    text-align: center;
  }
</style>

