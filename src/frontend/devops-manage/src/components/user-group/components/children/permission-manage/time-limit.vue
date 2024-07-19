<template>
  <div class="bk-button-group">
    <bk-button
      v-for="(item, key, index) in TIME_FILTERS"
      :key="index"
      @click="handleChangeTime(key)"
      :class="{
        'is-selected': currentActive === Number(key),
        'deadline-btn': true
      }">
      {{ t(item) }}
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
      :showControl="false"
      placeholder="1-365"
      :min="1"
      :max="365"
      @input="handleChangeCustomTime"
    >
      <template #suffix>
        <div class="suffix-slot">
            {{ t('天') }}
        </div>
      </template>
    </bk-input>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n';
import { ref, defineExpose, defineEmits, onMounted, nextTick } from 'vue';
import { TIME_FILTERS } from "@/utils/constants";

const { t } = useI18n();
const emit = defineEmits(['changeTime']);

const customTime = ref(1);
const currentActive = ref(30);
defineExpose({
  initTime,
});

onMounted(()=>{
  emit('changeTime', currentActive.value)
})

function initTime(){
  currentActive.value = 30;
  customTime.value = 1;
}
// /**
//  * 传入的值与当前时间戳秒数相加
//  * @param value 传入的值
//  */
// function formatTimes(value) {
//   const nowSecond = Math.floor(Date.now() / 1000);
//   return Number(value) + nowSecond;
// }
/**
 * 授权期限选择
 */
const handleChangeTime = (value) => {
  currentActive.value = Number(value)
  emit('changeTime', currentActive.value)
};
/**
 * 自定义期限点击
 */
const handleChangCustom = () => {
  currentActive.value = 'custom'
  emit('changeTime', customTime.value)
};
/**
 * 自定义期限输入事件
 * @param value 输入值
 */
const handleChangeCustomTime = (value) => {
  let newValue = value;
  if (!/^[0-9]*$/.test(value)) {
    newValue = 1;
  } else if (value > 365) {
    newValue = 365;
  }
  nextTick(() => {
    emit('changeTime', newValue);
  });
};
</script>

<style lang="scss" scoped>
  .bk-button-group {
    display: flex;

    .bk-button {
      font-size: 12px;
      height: 26px;
    }
  }

  .deadline-btn {
    min-width: 60px;
  }

  .custom-time-select {
    width: 110px;
    height: 26px;
    position: relative;
    left: -1px;
  }

  .suffix-slot {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 35px;
    background: #fff;
  }
</style>
