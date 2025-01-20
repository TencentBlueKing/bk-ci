import { useI18n } from 'vue-i18n';
import { defineComponent, ref, onMounted, nextTick } from 'vue';
import { TIME_FILTERS } from '@/common/constants';

export default defineComponent({
  emits: ['changeTime'],
  setup(_, { emit, expose }) {
    const { t } = useI18n();
    const customTime = ref(1);
    const currentActive = ref<string | number>(30);

    const initTime = () => {
      currentActive.value = 30;
      customTime.value = 1;
    };

    expose({
      initTime,
    });

    onMounted(() => {
      emit('changeTime', currentActive.value);
    });

    const handleChangeTime = (value: string | number) => {
      currentActive.value = value;
      emit('changeTime', value === 'custom' ? customTime.value : Number(value))
    };

    const handleChangeCustomTime = (value: string | number) => {
      let newValue = Number(value);
      if (!/^[0-9]*$/.test(value.toString())) {
        newValue = 1;
      } else if (newValue > 3650) {
        newValue = 3650;
      }
      nextTick(() => {
        emit('changeTime', newValue);
      });
    };

    return () => (
      <div>
        <div class="flex">
          {Object.entries(TIME_FILTERS).map(([key, item], index) => (
            <bk-button
              key={index}
              onClick={() => handleChangeTime(key)}
              class={`text-[12px] h-[26px] ${currentActive.value == key ? '!border !border-[#3A84FF] !bg-[#E1ECFF] !text-[#3A84FF]' : ''}`}
            >
              {t(item)}
            </bk-button>
          ))}
        </div>
        <bk-input
          v-model={customTime.value}
          v-show={currentActive.value === 'custom'}
          class="!w-[146px] h-[26px] relative -left-px mt-[12px]"
          type="number"
          suffix={t('天')}
          placeholder="1-3650"
          min={1}
          max={3650}
          onInput={handleChangeCustomTime}
        >
        </bk-input>
      </div>
    );
  },
});
