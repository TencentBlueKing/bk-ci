/**
 * ApplyDialog Component
 * Dialog for applying to join a user group or renewing membership
 */

import { Button, Dialog, Form, Input, Message } from 'bkui-vue';
import {
    computed,
    defineComponent,
    type PropType,
    reactive,
    ref,
    watch,
} from 'vue';
import { useI18n } from 'vue-i18n';
import { applyToJoinGroup, renewGroupMembership } from '../api';
import {
    APPLY_DIALOG_TYPES,
    createTimeFilterOptions,
    CUSTOM_TIME_RANGE,
    MEMBER_STATUS,
    TIME_DURATIONS,
    TIME_TO_DAYS,
} from '../constants';
import type { ApplyDialogType, MemberStatus, ResourceType } from '../types';
import styles from './index.module.css';

const { FormItem } = Form;

export default defineComponent({
  name: 'ApplyDialog',

  props: {
    /**
     * Whether the dialog is visible
     */
    isShow: {
      type: Boolean,
      default: false,
    },
    /**
     * User group name
     */
    groupName: {
      type: String,
      default: '',
    },
    /**
     * User group ID
     */
    groupId: {
      type: String,
      default: '',
    },
    /**
     * Current expired display (days remaining or 'expired')
     */
    expiredDisplay: {
      type: String,
      default: '',
    },
    /**
     * Dialog title
     */
    title: {
      type: String,
      default: '',
    },
    /**
     * Dialog type: 'apply' or 'renewal'
     */
    type: {
      type: String as PropType<ApplyDialogType>,
      default: APPLY_DIALOG_TYPES.APPLY,
    },
    /**
     * Member status
     */
    status: {
      type: String as PropType<MemberStatus>,
      default: '',
    },
    /**
     * Resource type
     */
    resourceType: {
      type: String as PropType<ResourceType>,
      default: 'pipeline',
    },
    /**
     * Project code
     */
    projectCode: {
      type: String,
      default: '',
    },
    /**
     * API prefix
     */
    ajaxPrefix: {
      type: String,
      default: '',
    },
  },

  emits: ['cancel', 'success', 'update:isShow'],

  setup(props, { emit }) {
    const { t } = useI18n();

    const formRef = ref<InstanceType<typeof Form> | null>(null);
    const isLoading = ref(false);
    const currentActive = ref<number | 'custom'>(TIME_DURATIONS.ONE_MONTH);
    const customTime = ref(1);

    const formData = reactive({
      expireTime: 0,
      reason: '',
    });

    /**
     * Time filter options for selection
     */
    const timeFilters = computed(() =>
      createTimeFilterOptions((key) => t(`flow.permission.${key}`)),
    );

    /**
     * Form validation rules
     */
    const rules = computed(() => ({
      expireTime: [
        {
          validator: () => {
            if (currentActive.value === 'custom' && customTime.value) {
              return true;
            }
            return currentActive.value !== 'custom';
          },
          message: t('flow.permission.pleaseSelectApplicationPeriod'),
          trigger: 'blur',
        },
      ],
      reason: [
        {
          required: true,
          message: t('flow.permission.pleaseFillInReason'),
          trigger: 'blur',
        },
      ],
    }));

    /**
     * Calculate new expiration days
     */
    const newExpiredDisplay = computed(() => {
      const currentExpired =
        props.status === MEMBER_STATUS.EXPIRED
          ? 0
          : Number(props.expiredDisplay) || 0;

      if (currentActive.value === 'custom') {
        return currentExpired + Number(customTime.value);
      }

      const days = TIME_TO_DAYS[currentActive.value as number] || 30;
      return currentExpired + days;
    });

    /**
     * Format timestamp (add seconds to current time)
     */
    const formatTimes = (seconds: number): number => {
      const nowTimestamp = Math.floor(Date.now() / 1000);
      return Number(seconds) + nowTimestamp;
    };

    /**
     * Handle time filter button click
     */
    const handleChangeTime = (value: number) => {
      formRef.value?.clearValidate();
      currentActive.value = value;
      formData.expireTime = formatTimes(value);
    };

    /**
     * Handle custom time selection
     */
    const handleChangeCustom = () => {
      currentActive.value = 'custom';
    };

    /**
     * Handle custom time input change
     */
    const handleChangeCustomTime = (value: string | number) => {
      const numValue = Number(value);
      if (Number.isNaN(numValue) || numValue < CUSTOM_TIME_RANGE.MIN) {
        customTime.value = CUSTOM_TIME_RANGE.MIN;
      } else if (numValue > CUSTOM_TIME_RANGE.MAX) {
        customTime.value = CUSTOM_TIME_RANGE.MAX;
      } else {
        customTime.value = numValue;
      }
    };

    /**
     * Get current username from window object
     */
    const getUserName = (): string => {
      const userInfo = (window as unknown as { $userInfo?: { username?: string } })
        .$userInfo;
      return userInfo?.username || '';
    };

    /**
     * Handle apply to join group
     */
    const handleApplyGroup = async () => {
      try {
        await formRef.value?.validate();
        isLoading.value = true;

        await applyToJoinGroup(
          {
            groupIds: [props.groupId],
            expiredAt: formData.expireTime,
            reason: formData.reason,
            applicant: getUserName(),
            projectCode: props.projectCode,
          },
          props.ajaxPrefix,
        );

        Message({
          theme: 'success',
          message: t('flow.permission.applicationSuccessWaitApproval'),
        });

        emit('success');
        handleCancel();
      } catch (err) {
        const error = err as { message?: string };
        Message({
          theme: 'error',
          message: error.message || String(err),
        });
      } finally {
        isLoading.value = false;
      }
    };

    /**
     * Handle renewal of group membership
     */
    const handleRenewalGroup = async () => {
      isLoading.value = true;

      try {
        // Calculate expiration time based on new expired display
        const timestamp = newExpiredDisplay.value * 24 * 3600;
        const expiredDisplayTime = formatTimes(timestamp);

        await renewGroupMembership(
          props.projectCode,
          props.resourceType,
          props.groupId,
          {
            expiredAt: expiredDisplayTime,
            resourceType: props.resourceType,
          },
          props.ajaxPrefix,
        );

        Message({
          theme: 'success',
          message: t('flow.permission.applicationSuccessWaitApproval'),
        });

        emit('success');
        handleCancel();
      } catch (err) {
        const error = err as { message?: string };
        Message({
          theme: 'error',
          message: error.message || String(err),
        });
      } finally {
        isLoading.value = false;
      }
    };

    /**
     * Handle confirm button click
     */
    const handleConfirm = () => {
      // Calculate expiration time for custom selection
      if (currentActive.value === 'custom') {
        const timestamp = customTime.value * 24 * 3600;
        formData.expireTime = formatTimes(timestamp);
      }

      if (props.type === APPLY_DIALOG_TYPES.RENEWAL) {
        handleRenewalGroup();
      } else {
        handleApplyGroup();
      }
    };

    /**
     * Handle cancel/close
     */
    const handleCancel = () => {
      // Reset form state
      customTime.value = 1;
      formData.expireTime = formatTimes(TIME_DURATIONS.ONE_MONTH);
      formData.reason = '';
      currentActive.value = TIME_DURATIONS.ONE_MONTH;

      setTimeout(() => {
        formRef.value?.clearValidate();
      }, 500);

      emit('cancel');
      emit('update:isShow', false);
    };

    // Initialize expireTime on mount
    watch(
      () => props.isShow,
      (visible) => {
        if (visible) {
          formData.expireTime = formatTimes(TIME_DURATIONS.ONE_MONTH);
        }
      },
      { immediate: true },
    );

    return () => (
      <Dialog
        isShow={props.isShow}
        width={700}
        title={props.title}
        onClosed={handleCancel}
        onConfirm={handleConfirm}
        v-slots={{
          footer: () => (
            <div class={styles.dialogFooter}>
              <Button
                theme="primary"
                loading={isLoading.value}
                onClick={handleConfirm}
              >
                {t('flow.permission.confirm')}
              </Button>
              <Button loading={isLoading.value} onClick={handleCancel}>
                {t('flow.permission.cancel')}
              </Button>
            </div>
          ),
        }}
      >
        <Form
          ref={formRef}
          model={formData}
          class={styles.applyForm}
          rules={rules.value}
          labelWidth={100}
        >
          <FormItem label={t('flow.permission.userGroupName')}>
            <span>{props.groupName}</span>
          </FormItem>

          <FormItem
            label={t('flow.permission.authorizationTerm')}
            property="expireTime"
            required
          >
            <div class={styles.deadlineWrapper}>
              {Object.entries(timeFilters.value).map(([key, label]) => (
                <Button
                  key={key}
                  class={[
                    styles.deadlineBtn,
                    currentActive.value === Number(key) && styles.isSelected,
                  ]}
                  onClick={() => handleChangeTime(Number(key))}
                >
                  {label}
                </Button>
              ))}

              {currentActive.value !== 'custom' ? (
                <Button
                  class={styles.deadlineBtn}
                  onClick={handleChangeCustom}
                >
                  {t('flow.permission.custom')}
                </Button>
              ) : (
                <Input
                  v-model={customTime.value}
                  class={styles.customTimeSelect}
                  type="number"
                  placeholder="1-365"
                  min={CUSTOM_TIME_RANGE.MIN}
                  max={CUSTOM_TIME_RANGE.MAX}
                  onChange={handleChangeCustomTime}
                  v-slots={{
                    suffix: () => (
                      <span class={styles.groupText}>
                        {t('flow.permission.days')}
                      </span>
                    ),
                  }}
                />
              )}
            </div>
          </FormItem>

          {props.type === APPLY_DIALOG_TYPES.RENEWAL ? (
            <FormItem label={t('flow.permission.expireAt')}>
              <span class={styles.expired}>
                {props.expiredDisplay}
                {props.status !== MEMBER_STATUS.EXPIRED &&
                  t('flow.permission.days')}
              </span>
              <span class={styles.arrowIcon}>→</span>
              <span class={styles.newExpired}>
                {newExpiredDisplay.value}
                {t('flow.permission.days')}
              </span>
            </FormItem>
          ) : (
            <FormItem
              label={t('flow.permission.reason')}
              property="reason"
              required
            >
              <Input
                v-model={formData.reason}
                type="textarea"
                rows={3}
                maxlength={100}
              />
            </FormItem>
          )}
        </Form>
      </Dialog>
    );
  },
});
