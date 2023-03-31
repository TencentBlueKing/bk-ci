<template>
  <permission-dialog
    :isShow="isShow"
    :width="700"
    :title="title"
    @cancel="handleCancel"
  >
    <bk-form
      ref="applyFrom"
      :model="formData"
      class="apply-form"
      :rules="rules"
      label-width="100"
    >
      <bk-form-item
        :label="t('用户组名')"
      >
        <span>{{ groupName }}</span>
      </bk-form-item>
      <bk-form-item
        :label="t('授权期限')"
        property="expireTime"
        required
        error-display-type="normal"
      >
        <div class="bk-button-group deadline-wrapper">
          <bk-button
            v-for="(item, key, index) in timeFilters"
            :key="index"
            @click="handleChangeTime(key)"
            :class="{
              'is-selected': currentActive === Number(key),
              'deadline-btn': true
            }">
            {{ item }}
          </bk-button>
          <bk-button
            class="deadline-btn"
            v-show="currentActive !== 'custom'"
            @click="handleChangCustom"
          >
            {{ t('自定义') }}
          </bk-button>
          <bk-input
            v-model="customTime"
            v-show="currentActive === 'custom'"
            class="custom-time-select"
            type="number"
            :show-controls="false"
            placeholder="1-365"
            :min="1"
            :max="365"
            @change="handleChangeCustomTime"
          >
            <template v-slot:append>
              <div class="group-text">
                {{ t('天') }}
              </div>
            </template>
          </bk-input>
        </div>
      </bk-form-item>
      <bk-form-item
        v-if="type === 'renewal'"
        :label="t('到期时间')"
      >
        <span class="expired">{{ expiredDisplay }}{{ t('天')}}</span>
        <img class="arrows-icon" src="../../../svg/arrows-right.svg?inline">
        <span class="new-expired">{{ newExpiredDisplay }}{{ t('天')}}</span>
      </bk-form-item>
      <bk-form-item
        v-else
        :label="t('理由')"
        property="reason"
        required
        error-display-type="normal"
      >
        <bk-input
          v-model="formData.reason"
          type="textarea"
          :rows="3"
          :maxlength="100"
        >
        </bk-input>
      </bk-form-item>
    </bk-form>
    <template v-slot:footer>
      <bk-button
        class="mr10"
        theme="primary"
        :loading="isLoading"
        @click="handleConfirm"
      >
        {{ t('确定') }}
      </bk-button>
      <bk-button
        :loading="isLoading"
        @click="handleCancel"
      >
        {{ t('取消') }}
      </bk-button>
    </template>
  </permission-dialog>
</template>
<script>
import ajax from '../../../ajax/index';
import PermissionDialog from '../../widget-components/dialog'
import { localeMixins } from '../../../utils/locale'

export default {
  components: {
    PermissionDialog
  },
  mixins: [localeMixins],
  props: {
    isShow: {
      type: Boolean,
    },
    groupName: {
      type: String,
    },
    groupId: {
      type: String,
    },
    expiredDisplay: {
      type: String,
    },
    title: {
      type: String,
    },
    type: {
      type: String,
      default: 'apply',
    },
    resourceType: {
      type: String,
    },
    ajaxPrefix: {
      type: String,
      default: '',
    },
  },
  emits: ['update:show'],
  data() {
    return {
      isLoading: false,
      pagination: {
        page: 1,
        pageSize: 20,
        projectName: '',
      },
      customTime: 1,
      formData: {
        expireTime: 0,
        reason: '',
      },
      currentActive: 2592000,
      timeFilters: {
        2592000: t('1个月'),
        7776000: t('3个月'),
        15552000: t('6个月'),
        31104000: t('12个月'),
      },
      rules: {
        expireTime: [
          {
            validator: () => {
              if (this.currentActive === 'custom' && this.customTime) {
                return true;
              }
              return this.currentActive !== 'custom';
            },
            message: t('请选择申请期限'),
            trigger: 'blur',
          },
        ],
        reason: [
          {
            required: true,
            message: t('请填写申请理由'),
            trigger: 'blur',
          },
        ],
      },
    };
  },
  computed: {
    userName() {
      return this.$userInfo && this.$userInfo.username ? this.$userInfo.username : '';
    },
    projectId() {
      return this.$route.params.projectId;
    },
    newExpiredDisplay() {
      const timeMap = {
        2592000: 30,
        7776000: 90,
        15552000: 180,
        31104000: 360,
      };
      if (this.currentActive === 'custom') {
        return Number(this.expiredDisplay) + Number(this.customTime);
      }
      return Number(this.expiredDisplay) + timeMap[this.currentActive];
    },
  },
  created() {
    this.formData.expireTime = this.formatTimes(2592000);
    if (this.projectCode) {
      this.formData.englishName = this.projectCode;
    }
    if (this.type === 'apply') {
      this.formData.reason = '';
    }
  },
  methods: {
    handleConfirm() {
      if (this.currentActive === 'custom') {
        const timestamp = this.customTime * 24 * 3600;
        this.formData.expireTime = this.formatTimes(timestamp);
      }
      if (this.type === 'renewal') {
        const timestamp = this.newExpiredDisplay * 24 * 3600;
        const expiredDisplayTime = this.formatTimes(timestamp);
        this.formData.expireTime = expiredDisplayTime;
        this.handleRenewalGroup();
      } else {
        this.handleApplyGroup();
      }
    },
    handleCancel() {
      this.$emit('cancel');
      this.customTime = 1;
      this.formData.expireTime = this.formatTimes(2592000);
      this.formData.reason = '';
      this.currentActive = 2592000;
      setTimeout(() => {
        this.$refs.applyFrom.clearError();
      }, 500);
    },
    handleChangeCustomTime(value) {
      if (!/^[0-9]*$/.test(value)) {
        this.$nextTick(() => {
          this.customTime = 1;
        });
      } else if (this.customTime > 365) {
        this.$nextTick(() => {
          this.customTime = 365;
        });
      }
    },
    handleChangeTime(value) {
      this.$refs.applyFrom.clearError();
      this.currentActive = Number(value);
      this.formData.expireTime = this.formatTimes(value);
    },
    handleChangCustom() {
      this.currentActive = 'custom';
    },
    formatTimes(value) {
      const nowTimestamp = +new Date() / 1000;
      const tempArr = String(nowTimestamp).split('');
      const dotIndex = tempArr.findIndex(i => i === '.');
      const nowSecond = parseInt(tempArr.splice(0, dotIndex).join(''), 10);
      return Number(value) + nowSecond;
    },
    handleApplyGroup() {
      this.$refs.applyFrom.validate().then(() => {
        this.isLoading = true;
        ajax
          .post(`${this.ajaxPrefix}/auth/api/user/auth/apply/applyToJoinGroup`, {
            groupIds: [this.groupId],
            expiredAt: this.formData.expireTime,
            reason: this.formData.reason,
            applicant: this.userName,
          })
          .then(() => {
            this.$bkMessage({
              theme: 'success',
              message: t('申请成功，请等待审批'),
            });
          })
          .catch((err) => {
            this.$bkMessage({
              theme: 'error',
              message: err.message,
            });
          })
          .finally(() => {
            this.isLoading = false;
            this.handleCancel();
          });
      });
    },
    handleRenewalGroup() {
      this.isLoading = true;
      ajax
        .put(`${this.ajaxPrefix}/auth/api/user/auth/resource/group/${this.projectCode}/${this.resourceType}/${this.groupId}/member/renewal`, {
          expiredAt: this.formData.expireTime,
          projectId: this.projectId,
          resourceType: this.resourceType,
        })
        .then(() => {
          this.$bkMessage({
            theme: 'success',
            message: t('申请成功，请等待审批'),
          });
        })
        .catch((err) => {
          this.$bkMessage({
            theme: 'error',
            message: err.message,
          });
        })
        .finally(() => {
          this.isLoading = false;
          this.handleCancel();
        });
    },
  },
};
</script>
<style lang="scss" scoped>
    .apply-form {
        width: 98%;
    }
    ::v-deep .bk-dialog-header {
        text-align: left !important;
    }
    .deadline-wrapper {
        display: flex;
    }
    .deadline-btn {
        min-width: 100px;
    }
    .custom-time-select {
        width: 110px;
    }
    .expired {
        padding-right: 10px;
    }
    .new-expired {
        padding-left: 10px;
    }
    .arrows-icon {
        width: 12px;
        height: 12px;
    }
</style>
