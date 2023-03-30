<template>
  <article class="group-table">
    <bk-table
      v-bkloading="{ isLoading }"
      :data="memberList"
    >
      <bk-table-column :label="$t('用户组')" prop="groupName"></bk-table-column>
      <bk-table-column :label="$t('添加时间')" prop="createdTime">
        <template #default="{ row }">
          <span>{{ row.createdTime ? row.createdTime : '--' }} </span>
        </template>
      </bk-table-column>
      <bk-table-column :label="$t('有效期')" prop="expiredDisplay">
        <template #default="{ row }">
          <span>{{ row.expiredDisplay ? row.expiredDisplay + $t('天') : '--' }} </span>
        </template>
      </bk-table-column>
      <bk-table-column :label="$t('状态')" prop="status">
        <template #default="{ row }">
          <div class="status-content">
            <img :src="statusIcon(row.status)" class="status-icon">
            {{ statusFormatter(row.status) }}
          </div>
        </template>
      </bk-table-column>
      <bk-table-column :label="$t('操作')">
        <template #default="{ row }">
          <bk-button
            class="btn"
            theme="primary"
            text
            @click="handleViewDetail(row)"
          >{{ $t('权限详情') }}</bk-button>
          <bk-button
            class="btn"
            theme="primary"
            text
            v-if="row.status === 'NOT_JOINED'"
            @click="handleApply(row)"
          >{{ $t('申请加入') }}</bk-button>
          <bk-button
            class="btn"
            theme="primary"
            text
            v-if="['EXPIRED', 'NORMAL'].includes(row.status)"
            @click="handleRenewal(row)"
          >{{ $t('续期') }}</bk-button>
          <bk-button
            class="btn"
            theme="primary"
            text
            v-if="['EXPIRED', 'NORMAL'].includes(row.status)"
            @click="handleShowLogout(row)"
          >{{ $t('退出') }}</bk-button>
        </template>
      </bk-table-column>
    </bk-table>
    <side-slider
      :is-show="showDetail"
      :width="640"
      @hidden="handleHidden"
    >
      <template v-slot:header>
        <div class="detail-title">
          {{ $t('权限详情') }}
          <span class="group-name">{{ groupName }}</span>
        </div>
      </template>
      <template v-slot:content>
        <div class="detail-content" v-bkloading="{ isLoading: isDetailLoading }">
          <div class="title">{{ $t('流水线管理') }}</div>
          <div class="content">
            <bk-checkbox
              v-for="(item, index) in groupPolicies"
              :key="index"
              v-model="item.permission"
              disabled
              class="permission-item"
            >
              {{ item.actionName }}
            </bk-checkbox>
          </div>
        </div>
      </template>
    </side-slider>
    <permission-dialog
      :is-show="logout.isShow"
      :title="$t('确认退出用户组')"
      :loading="logout.loading"
      @confirm="handleLogout"
      @cancel="handleCancelLogout"
    >
      {{ $t('退出后，将无法再使用所赋予的权限。', [logout.name]) }}
    </permission-dialog>
    <apply-dialog
      :is-show="apply.isShow"
      v-bind="apply"
      :resource-type="resourceType"
      @cancel="() => apply.isShow = false"
    />
  </article>
</template>

<script>
import ajax from '../../../ajax/index';
import ApplyDialog from './apply-dialog.vue';
import syncDefault from '../../../svg/sync-default.svg?inline';
import syncSuccess from '../../../svg/sync-success.svg?inline';
import syncFailed from '../../../svg/sync-failed.svg?inline';
import SideSlider from '../../widget-components/side-slider.jsx';
import PermissionDialog from '../../widget-components/dialog.jsx';

const initFormData = () => ({
  isShow: false,
  groupName: '',
  groupId: '',
  expiredDisplay: '',
  title: '',
  type: '',
});
export default {
  components: {
    ApplyDialog,
    SideSlider,
    PermissionDialog,
  },

  props: {
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
    ajaxPrefix: {
      type: String,
      default: '',
    },
  },

  data() {
    return {
      showDetail: false,
      logout: {
        loading: false,
        isShow: false,
        groupId: '',
        name: '',
      },
      apply: initFormData(),
      memberList: [],
      isLoading: false,
      isDetailLoading: false,
      groupPolicies: [],
      groupName: '',
    };
  },

  mounted() {
    this.getMemberList();
  },

  methods: {
    handleHidden() {
      this.showDetail = false;
    },

    getMemberList() {
      this.isLoading = true;
      return ajax
        .get(`${this.ajaxPrefix}/auth/api/user/auth/resource/${this.projectCode}/${this.resourceType}/${this.resourceCode}/groupMember`)
        .then((res) => {
          this.memberList = res.data;
        })
        .catch((err) => {
          this.$bkMessage({
            theme: 'error',
            message: err.message || err,
          });
        })
        .finally(() => {
          this.isLoading = false;
        });
    },

    handleViewDetail(row) {
      const { groupId, groupName } = row;
      this.groupName = groupName;
      this.showDetail = true;
      this.isDetailLoading = true;
      ajax
        .get(`${this.ajaxPrefix}/auth/api/user/auth/resource/group/${this.projectCode}/${this.resourceType}/${groupId}/groupPolicies`)
        .then(({ data }) => {
          this.groupPolicies = data;
        })
        .catch((err) => {
          this.$bkMessage({
            theme: 'error',
            message: err.message || err,
          });
        })
        .finally(() => {
          this.isDetailLoading = false;
        });
    },

    statusFormatter(status) {
      const map = {
        NOT_JOINED: this.$t('未加入'),
        NORMAL: this.$t('正常'),
        EXPIRED: this.$t('已过期'),
      };
      return map[status];
    },

    statusIcon(status) {
      const map = {
        NOT_JOINED: syncDefault,
        NORMAL: syncSuccess,
        EXPIRED: syncFailed,
      };
      return map[status];
    },

    handleRenewal(row) {
      this.apply.isShow = true;
      this.apply.groupName = row.groupName;
      this.apply.groupId = row.groupId;
      this.apply.expiredDisplay = row.expiredDisplay;
      this.apply.title = this.$t('续期');
      this.apply.type = 'renewal';
    },

    handleApply(row) {
      this.apply.isShow = true;
      this.apply.groupName = row.groupName;
      this.apply.groupId = row.groupId;
      this.apply.title = this.$t('申请加入');
      this.apply.type = 'apply';
    },

    handleShowLogout(row) {
      this.logout.isShow = true;
      this.logout.groupId = row.groupId;
      this.logout.name = row.groupName;
    },

    handleCancelLogout() {
      this.logout.isShow = false;
    },

    handleLogout() {
      this.logout.loading = true;
      return ajax
        .delete(`${this.ajaxPrefix}/auth/api/user/auth/resource/group/${this.projectCode}/${this.resourceType}/${this.logout.groupId}/member`)
        .then(() => {
          this.handleCancelLogout();
          this.getMemberList();
        })
        .catch((err) => {
          this.$bkMessage({
            theme: 'error',
            message: err.message || err,
          });
        })
        .finally(() => {
          this.logout.loading = false;
        });
    },
  },
};
</script>

<style lang="scss" scoped>
    .btn {
        margin-right: 5px;
    }
    .group-name {
        font-size: 12px;
        color: #979BA5;
        margin-left: 10px;
    }
    .status-content {
        display: flex;
        align-items: center;
    }
    .status-icon {
        height: 16px;
        width: 16px;
        margin-right: 5px;
    }
    .detail-content {
        padding: 20px;
        .title {
            font-size: 14px;
            color: #313238;
            margin-left: 14px;
            &::before {
                content: '';
                position: absolute;
                left: 20px;
                top: 22px;
                width: 4px;
                height: 16px;
                background: #699DF4;
                border-radius: 1px;
            }
        }
        .content {
            margin-top: 15px;
        }
        .permission-item {
            min-width: 150px;
            margin-bottom: 10px;
            cursor: default !important;
        }
         .is-disabled {
            .bk-checkbox-text {
                color:#d6d7d7 !important;
            }
        }
        .is-checked {
            .bk-checkbox {
                background-color: #c2daff !important;
                border-color: #c2daff !important;
            }
            .bk-checkbox-text {
                color: #63656e !important;
            }
        }
    }
</style>
