<template>
  <article class="group-aside" v-bkloading="{ isLoading: !groupList.length }">
    <span class="group-title">{{ $t('权限角色') }}</span>
    <scroll-load-list
      class="group-list"
      ref="loadList"
      :list="groupList"
      :has-load-end="hasLoadEnd"
      :page="page"
      :get-data-method="handleGetData"
    >
      <template v-slot:default="{ data: group }">
        <div
          :class="{ 'group-item': true, 'group-active': activeTab === group.groupId }"
          @click="handleChooseGroup(group)"
        >
          <span class="group-name" :title="group.name">{{ group.name }}</span>
          <span class="user-num">
            <img src="../../../svg/user.svg?inline" class="group-icon">
            {{ group.userCount }}
          </span>
          <span class="group-num">
            <img src="../../../svg/organization.svg?inline" class="group-icon">
            {{ group.departmentCount }}
          </span>
          <bk-popover
            v-if="resourceType === 'project'"
            class="group-more-option"
            placement="bottom"
            theme="dot-menu light"
            :arrow="false"
            offset="15"
            :distance="0">
            <img src="../../../svg/more.svg?inline" class="more-icon">
            <template #content>
              <bk-button
                class="btn"
                :disabled="[1, 2].includes(group.id)"
                text
                @click="handleShowDeleteGroup(group)">
                {{ $t('删除') }}
              </bk-button>
            </template>
          </bk-popover>
        </div>
      </template>
    </scroll-load-list>
    <div class="line-split" />
    <div
      v-if="showCreateGroup"
      :class="{ 'group-item': true, 'group-active': activeTab === '' }"
      @click="handleCreateGroup">
      <span class="add-group-btn">
        <i class="bk-icon bk-icon-add-fill add-icon"></i>
        {{ $t('新建用户组') }}
      </span>
    </div>
    <div class="close-btn">
      <bk-button @click="handleCloseManage" :loading="isClosing">{{ $t('关闭权限管理') }}</bk-button>
    </div>
    <bk-dialog
      header-align="center"
      theme="danger"
      quick-close
      :value="deleteObj.isShow"
      :title="$t('删除')"
      :is-loading="deleteObj.isLoading"
      @cancel="handleHiddenDeleteGroup"
      @confirm="handleDeleteGroup"
    >
      {{ $t('是否删除用户组', [deleteObj.group.name]) }}
    </bk-dialog>
  </article>
</template>

<script>
import ScrollLoadList from '../../widget-components/scroll-load-list';
import ajax from '../../../ajax/index';

export default {
  components: {
    ScrollLoadList,
  },
  props: {
    activeIndex: {
      type: Boolean,
      default: 0,
    },
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
    ajaxPrefix: {
      type: String,
      default: '',
    },
  },
  emits: ['choose-group', 'create-group', 'close-manage'],
  data() {
    return {
      page: 1,
      activeTab: '',
      deleteObj: {
        group: {},
        isShow: false,
        isLoading: false,
      },
      groupList: [],
      hasLoadEnd: false,
      isClosing: false,
    };
  },
  watch: {
    activeIndex(newVal) {
      this.activeTab = this.groupList[newVal]?.groupId || '';
    },
  },
  async created() {
    window.addEventListener('message', this.handleMessage);
  },

  beforeUnmount() {
    window.removeEventListener('message', this.handleMessage);
  },
  methods: {
    handleGetData(pageSize) {
      return ajax
        .get(`${this.ajaxPrefix}/auth/api/user/auth/resource/${this.projectCode}/${this.resourceType}/${this.resourceCode}/listGroup?page=${this.page}&pageSize=${pageSize}`)
        .then(({ data }) => {
          this.hasLoadEnd = !data.hasNext;
          this.groupList.push(...data.records);
          // 首页需要加载
          if (this.page === 1) {
            this.handleChooseGroup(this.groupList[0]);
          }
          this.page += 1
        });
    },
    refreshList() {
      this.groupList = [];
      this.hasLoadEnd = false;
      this.page = 1;
      return this.handleGetData(100)
    },
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
      return ajax
        .delete(`${this.ajaxPrefix}/auth/api/user/auth/resource/group/${this.projectCode}/${this.resourceType}/${this.deleteObj.group.groupId}`)
        .then(() => {
          this.handleHiddenDeleteGroup();
          this.refreshList();
        })
        .finally(() => {
          this.deleteObj.isLoading = false;
        });
    },
    handleChooseGroup(group) {
      this.activeTab = group.groupId;
      this.$emit('choose-group', group);
    },
    handleCreateGroup() {
      this.activeTab = '';
      this.$emit('create-group');
    },
    handleCloseManage() {
      this.isClosing = true;
      return ajax
        .put(`${this.ajaxPrefix}/auth/api/user/auth/resource/${this.projectCode}/${this.resourceType}/${this.resourceCode}/disable`)
        .then(() => {
          this.$emit('close-manage');
        })
        .finally(() => {
          this.isClosing = false;
        });
    },
    handleMessage(event) {
      const { data } = event;
      if (data.type === 'IAM') {
        switch (data.code) {
          case 'create_user_group_submit':
            this
              .refreshList()
              .then(() => {
                const group = this.groupList.find(group => group.groupId === data?.data?.id) || this.groupList[0]
                this.handleChooseGroup(group);
              })
            break;
          case 'create_user_group_cancel':
            this.handleChooseGroup(this.groupList[0]);
            break;
          case 'add_user_confirm':
            this.refreshList()
            break;
        }
      }
    },
  },
};
</script>

<style lang="scss" scoped>
.group-aside {
  width: 240px;
  height: 100%;
  background-color: #fff;
  border-right: 1px solid #dde0e6;
}
.group-list {
  max-height: calc(100% - 62px);
  height: auto;
  overflow-y: auto;
}
.group-title {
  display: inline-block;
  line-height: 50px;
  padding-left: 24px;
  width: 100%;
  font-size: 14px;
  margin-bottom: 8px;
  font-weight: bold;
}
.group-item {
  display: flex;
  align-items: center;
  width: 100%;
  height: 40px;
  line-height: 40px;
  font-size: 14px;
  padding: 0 12px;
  color: #63656E;
  cursor: pointer;
  &:hover {
    color: #3A84FF;
    background-color: #E1ECFF;
    .group-icon {
      filter: invert(100%) sepia(0%) saturate(1%) hue-rotate(151deg) brightness(104%) contrast(101%);
    }
  }
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
  .group-icon {
    filter: invert(100%) sepia(0%) saturate(1%) hue-rotate(151deg) brightness(104%) contrast(101%);
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
.group-icon {
  height: 12px;
  width: 12px;
  filter: invert(89%) sepia(8%) saturate(136%) hue-rotate(187deg) brightness(91%) contrast(86%);
}
.more-icon {
  height: 18px;
  filter: invert(89%) sepia(8%) saturate(136%) hue-rotate(187deg) brightness(91%) contrast(86%);
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

.group-more-option {
  height: 18px;
  display: flex;
  align-items: center;
}
.close-btn {
  margin-bottom: 20px;
  text-align: center;
}
.small-size {
  scale: 0.9;
}
</style>
<style lang="scss">
.group-more-option .bk-tooltip-ref {
  height: 18px;
  display: flex;
  align-items: center;
}
</style>
