<template>
  <article class="group-aside">
    <section class="group-list">
      <bk-loading :style="{ 'min-height': '100px' }" :loading="!groupList.length">
        <span class="group-title">{{ $t('权限角色') }}</span>
        <div
          :class="{ 'group-item': true, 'group-active': activeTab === group.groupId }"
          v-for="(group, index) in groupList"
          :key="index"
          @click="handleChangeTab(group)">
          <span class="group-name" :title="group.name">{{ group.name }}</span>
          <span class="user-num">
            <i class="manage-icon small-size manage-icon-user-shape"></i>
            {{ group.userCount }}
          </span>
          <span class="group-num">
            <i class="manage-icon small-size manage-icon-organization"></i>
            {{ group.departmentCount }}
          </span>
          <bk-popover
            v-if="resourceType === 'project'"
            class="group-more-option"
            placement="bottom"
            trigger="click"
            theme="dot-menu light"
            :arrow="false"
            offset="15"
            :distance="0">
            <i class="more-icon manage-icon manage-icon-more-fill"></i>
            <template #content>
              <bk-button
                class="btn"
                :disabled="group.defaultGroup"
                text
                @click="handleShowDeleteGroup(group)">
                {{ $t('删除') }}
              </bk-button>
            </template>
          </bk-popover>
        </div>
      </bk-loading>
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
    <bk-dialog
      dialogType="show"
      header-align="center"
      theme="danger"
      quick-close
      :is-show="deleteObj.isShow"
      :title="$t('确认删除用户组？')"
      :is-loading="deleteObj.isLoading"
      @closed="handleHiddenDeleteGroup"
      @confirm="handleDeleteGroup"
    >
      <div>{{ $t('删除【】，将产生以下影响：', [deleteObj.group.name]) }}</div>
      <div class="delete-tips">
        <p>{{ $t('组内用户和组织将被全部移除') }}</p>
        <p>{{ $t('组权限将全部移除') }}</p>
        <p>{{ $t('组内用户继承该组的权限将失效') }}</p>
      </div>
    </bk-dialog>
  </article>
</template>

<script>
export default {
  name: 'GroupAside',
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
    closeManage: {
      type: Function,
      default: () => {},
    },
    deleteGroup: {
      type: Function,
      default: () => {},
    },
    activeIndex: {
      type: Boolean,
      default: 0,
    }
  },
  emits: ['create-group', 'choose-group', 'delete-group'],
  data() {
    return {
      activeTab: '',
      deleteObj: {
        group: {},
        isShow: false,
        isLoading: false,
      },
    };
  },
  watch: {
    groupList: {
      handler() {
        if (this.groupList.length) {
          this.handleChangeTab(this.groupList[0]);
        }
      },
      immediate: true,
    },
    activeIndex(newVal) {
      this.activeTab = this.groupList[newVal]?.groupId || '';
    },
  },
  methods: {
    handleShowDeleteGroup(group) {
      this.deleteObj.group = group;
      this.deleteObj.isShow = true;
    },
    handleHiddenDeleteGroup() {
      this.deleteObj.isShow = false;
      this.deleteObj.group = {};
    },
    handleDeleteGroup() {
      this.deleteObj.isLoading = true;
      return this
        .deleteGroup(this.deleteObj.group)
        .then(() => {
          this.deleteObj.isLoading = false;
          this.handleHiddenDeleteGroup();
        });
    },
    handleChangeTab(group) {
      this.activeTab = group.groupId;
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
  .group-title {
    display: inline-block;
    line-height: 50px;
    padding-left: 24px;
    width: 100%;
    background-image: linear-gradient(transparent 49px, rgb(220, 222, 229) 1px);
    font-size: 14px;
    margin-bottom: 8px;
  }
  .group-aside {
    width: 240px;
    height: 100%;
    background-color: #fff;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    border-right: 1px solid #dde0e6;
  }
  .group-list {
    max-height: calc(100% - 62px);
    overflow-y: auto;
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
      .small-size {
        color: #fff
      }
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
    .user-num, .group-num {
      background-color: #A3C5FD;
      color: #fff;
    }
    .small-size {
      color: #fff
    }
  }
  .user-num,
  .group-num {
    background-color: #A3C5FD;
    color: #fff;
  }
  .group-name {
    display: inline-block;
    width: 100px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
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
    color: #C4C6CC;
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
    color: #979BA5;
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
  .small-size {
    scale: 0.9;
    color: #C4C6CC;
  }
  .delete-tips {
    padding: 10px 0 25px;
  }
</style>
