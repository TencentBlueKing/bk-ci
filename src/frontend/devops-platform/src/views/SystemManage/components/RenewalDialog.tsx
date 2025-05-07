
import { useI18n } from 'vue-i18n';
import { defineComponent, ref } from 'vue';
import TimeLimit from '@/components/time-limit'
import ArrowsRight from '@/css/svg/arrows-right.svg'

export default defineComponent({
  props: {
    isShow: {
      type: Boolean,
      default: false
    },
    formData: {
      type: Object
    },
  },
  emits: ['confirm', 'closed', 'changeTime'],
  setup(props, { emit }) {
    const { t } = useI18n();
    const renewalRef = ref();

    function handleConfirm() {
      emit('confirm')
      if (renewalRef.value) {
        renewalRef.value.initTime();
      }
    };
    function handleClosed() {
      emit('closed')
      if (renewalRef.value) {
        renewalRef.value.initTime();
      }
    };
    function handleChangeTime(value) {
      emit('changeTime', value, 'renewal')
    };

    return () => (
      <bk-dialog
        width={580}
        is-show={props.isShow}
        title={t('续期')}
        onConfirm={handleConfirm}
        onClosed={handleClosed}
      >
        <p class="flex mt-[24px] ml-[24px]">
          <span class="inline-block text-right w-[80px] text-[#4D4F56] text-[14px]">{t("用户名")}：</span> {'xxx'}
        </p>
        <p class="flex mt-[24px] ml-[24px]">
          <span class="relative inline-block text-right w-[80px] text-[#4D4F56] text-[14px] pr-[16px] required">{t("续期期限")}</span>
          <TimeLimit ref={renewalRef} onChangeTime={handleChangeTime} />
        </p>
        <p class="flex mt-[24px] ml-[24px]">
          <span class="inline-block text-right w-[80px] text-[#4D4F56] text-[14px]">{t("到期时间")}：</span>
          {
            true ? (
              <>
                <span class="text-[#C4C6CC]">{t("已过期")}</span>
                <span class="text-[#F59500] flex items-center">
                  <img src={ArrowsRight} alt="" class="w-[13px] mx-[12px] align-middle" />
                  {'xxx'} {t("天")}
                </span>
              </>
            ) : (
              <>
                <span class="text-[#C4C6CC]">{'xxx'}</span>
                <span class="text-[#F59500] flex items-center">
                  <img src={ArrowsRight} alt="" class="w-[13px] mx-[12px] align-middle" />
                  {/* { Number(selectedRow?.expiredAtDisplay.replace(/\D/g, '')) + expiredAt } { t("天") } */}
                </span>
              </>
            )
          }
        </p>
      </bk-dialog>
    );
  },
});