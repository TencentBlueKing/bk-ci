<script setup lang="ts">
import {
  ref,
  onMounted,
  watch,
  h,
} from 'vue';
import {
  sharedProps,
} from '../common/props-type';
import http from '@/http/api';
import { useI18n } from "vue-i18n";
const { t } = useI18n();
import {
  InfoLine,
} from 'bkui-vue/lib/icon';
import dayjs from 'dayjs';
import duration from 'dayjs/plugin/duration';
dayjs.extend(duration);

interface IShowTime {
  h?: number,
  m?: number,
  s: number
}

interface IData {
  successExecuteCount: number,
  totalExecuteCount: number,
  totalCostTime: IShowTime,
  totalSuccessRate: number,
  totalAvgCostTime: IShowTime,
  resolvedDefectNum: number,
  repoCodeccAvgScore: number,
  qualityInterceptionRate: number,
  interceptionCount: number,
  totalQualityExecuteCount: number,
  turboSaveTime: IShowTime
}

// 状态
const props = defineProps(sharedProps);
const isLoading = ref(false);
const data = ref<IData>({
  successExecuteCount: 0,
  totalExecuteCount: 0,
  totalCostTime: {
    s: 0,
  },
  totalSuccessRate: 0,
  totalAvgCostTime: {
    s: 0,
  },
  resolvedDefectNum: 0,
  repoCodeccAvgScore: 0,
  qualityInterceptionRate: 0,
  interceptionCount: 0,
  totalQualityExecuteCount: 0,
  turboSaveTime: {
    s: 0,
  },
});

// 方法
const timeFormatter = (val) => {
  const time = dayjs.duration(val);
  const h = time.hours();
  const m = time.minutes();
  const s = time.seconds();
  const showTime: IShowTime = {
    s,
  };
  if (m) showTime.m = m;
  if (h) showTime.h = h;
  return showTime;
};

const init = () => {
  isLoading.value = true;
  Promise
    .all([
      http.getPipelineSummaryData(props.status),
      http.getThirdpartySummaryData(props.status),
    ])
    .then(([
      {
        pipelineSumInfoDO,
      },
      {
        codeCheckInfo,
        qualityInfo,
        turboInfo,
      },
    ]) => {
      data.value.successExecuteCount = pipelineSumInfoDO?.successExecuteCount;
      data.value.totalExecuteCount = pipelineSumInfoDO?.totalExecuteCount;
      data.value.totalCostTime = timeFormatter(pipelineSumInfoDO?.totalCostTime);
      data.value.totalSuccessRate = pipelineSumInfoDO?.totalSuccessRate;
      data.value.totalAvgCostTime = timeFormatter(pipelineSumInfoDO?.totalAvgCostTime);
      data.value.resolvedDefectNum = codeCheckInfo?.resolvedDefectNum;
      data.value.repoCodeccAvgScore = codeCheckInfo?.repoCodeccAvgScore;
      data.value.qualityInterceptionRate = qualityInfo?.qualityInterceptionRate;
      data.value.turboSaveTime = timeFormatter(turboInfo?.turboSaveTime || 0 * 1000);
      data.value.interceptionCount = qualityInfo?.interceptionCount
      data.value.totalQualityExecuteCount = qualityInfo?.totalExecuteCount
    })
    .finally(() => {
      isLoading.value = false;
    });
};

const RenderEmptyNodeIfNone = ({ data }, { slots }) => {
  if ([0, undefined, null].includes(data)) {
    return h('span', { class: 'card-num' }, '--');
  } else {
    return slots.default();
  }
}

// 触发
watch(
  () => props.status,
  init,
);
onMounted(init);
</script>

<template>
  <bk-loading
    class="overview-nums overview-card mt20"
    :loading="isLoading"
  >
    <section class="w2 gap-line">
      <h3 class="g-card-title">{{ t('Pipeline runs') }}</h3>
      <section class="card-num-group">
        <section class="card-detail">
          <h5 class="card-num">
            <render-empty-node-if-none :data="data.totalSuccessRate">
              {{ data.totalSuccessRate }}<span class="card-num-sub">%</span>
            </render-empty-node-if-none>
          </h5>
          <span class="card-desc">
            {{ t('Success rate') }}
            <bk-popover placement="top">
              <info-line />
              <template #content>
                {{ t('Success') }}: {{ data.successExecuteCount || '--' }} /  {{ t('No. of total runs') }}: {{ data.totalExecuteCount || '--' }}
              </template>
            </bk-popover>
          </span>
        </section>
        <section class="card-detail">
          <h5 class="card-num">
            <render-empty-node-if-none :data="data.totalAvgCostTime.s">
              {{ data.totalAvgCostTime.h }}<!--
              --><span v-if="data.totalAvgCostTime.h" class="card-num-sub">h</span><!--
              -->{{ data.totalAvgCostTime.m }}<!--
              --><span v-if="data.totalAvgCostTime.m" class="card-num-sub">m</span><!--
              -->{{ data.totalAvgCostTime.s }}<!--
              --><span v-if="data.totalAvgCostTime.s" class="card-num-sub">s</span>
            </render-empty-node-if-none>
          </h5>
          <span class="card-desc">
            {{ t('Average time') }}
            <bk-popover placement="top">
              <info-line />
              <template #content>
                {{ t('Total time') }}: {{ data.totalCostTime.h }}<!--
                --><span v-if="data.totalCostTime.h" class="card-num-sub">h</span><!--
                -->{{ data.totalCostTime.m }}<!--
                --><span v-if="data.totalCostTime.m" class="card-num-sub">m</span><!--
                -->{{ data.totalCostTime.s || '--' }}<!--
                --><span v-if="data.totalCostTime.s" class="card-num-sub">s</span> / {{ t('No. of total runs') }} : {{ data.totalExecuteCount || '--' }}
              </template>
            </bk-popover>
          </span>
        </section>
        <span class="line-split"></span>
      </section>
    </section>
    <section class="w2 gap-line">
      <h3 class="g-card-title">{{ t('Code check') }}</h3>
      <section class="card-num-group">
        <section class="card-detail">
          <render-empty-node-if-none :data="data.repoCodeccAvgScore">
            <bk-rate
              :editable="false"
              :model-value="data.repoCodeccAvgScore / 20"
              size="large"
              class="card-num"
            />
          </render-empty-node-if-none>
          <span class="card-desc">
            {{ t('Code Quality') }}
            <bk-popover placement="top">
              <info-line />
              <template #content>
                {{ t('Code quality star based on Tencent Open Source Governance indicator system') }}
              </template>
            </bk-popover>
          </span>
        </section>
        <section class="card-detail">
          <h5 class="card-num">
            <render-empty-node-if-none :data="data.resolvedDefectNum">
              {{ data.resolvedDefectNum }}
            </render-empty-node-if-none>
          </h5>
          <span class="card-desc">
            {{ t('Resolved Code Defects') }}
            <bk-popover placement="top">
              <info-line />
              <template #content>
                {{ t('Resolved Code Defects') }}
              </template>
            </bk-popover>
          </span>
        </section>
        <span class="line-split"></span>
      </section>
    </section>
    <section class="w1 gap-line">
      <h3 class="g-card-title">{{ t('Quality Gate') }}</h3>
      <section class="card-num-group">
        <section class="card-detail">
          <h5 class="card-num">
            <render-empty-node-if-none :data="data.qualityInterceptionRate">
              {{ data.qualityInterceptionRate }}<span class="card-num-sub">%</span>
            </render-empty-node-if-none>
          </h5>
          <span class="card-desc">
            {{ t('Interception rate') }}
            <bk-popover placement="top">
              <info-line />
              <template #content>
                {{ t('Intercepted') }}: {{ data.interceptionCount || '--' }} / {{ t('No. of total runs') }}: {{ data.totalQualityExecuteCount || '--' }}
              </template>
            </bk-popover>
          </span>
        </section>
        <span class="line-split"></span>
      </section>
    </section>
    <section class="w1">
      <h3 class="g-card-title">{{ t('Turbo') }}</h3>
      <section class="card-num-group">
        <section class="card-detail">
          <h5 class="card-num">
            <render-empty-node-if-none :data="data.turboSaveTime">
              {{ data.turboSaveTime.h }}<!--
              --><span v-if="data.turboSaveTime.h" class="card-num-sub">h</span><!--
              -->{{ data.turboSaveTime.m }}<!--
              --><span v-if="data.turboSaveTime.m" class="card-num-sub">m</span><!--
              -->{{ data.turboSaveTime.s }}<!--
              --><span class="card-num-sub">s</span>
            </render-empty-node-if-none>
          </h5>
          <span class="card-desc">
            {{ t('Saving Time') }}
            <bk-popover placement="top">
              <info-line />
              <template #content>
                {{ t('Time saved after using Turbo') }}
              </template>
            </bk-popover>
          </span>
        </section>
      </section>
    </section>
  </bk-loading>
</template>

<style lang="scss" scoped>
.overview-nums {
  display: flex;
  width: 100%;
  height: 1.36rem;
}

.w2 {
  width: 33.33%;
}

.w1 {
  width: 16.67%;
}

.gap-line {
  position: relative;
}

.card-num-group {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: .24rem;
  position: relative;
  right: .4rem;
  .line-split {
    display: inline-block;
    width: 1px;
    height: .52rem;
    background: #dcdee5;
  }
}

.card-detail {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;

  .card-num {
    font-size: .24rem;
    line-height: .32rem;
    height: .32rem;
    color: #313238;

    .card-num-sub {
      font-size: .16rem;
      color: #63656e;
    }
    ::v-deep(.bk-rate-stars) {
      margin-top: 0.1rem
    }
  }

  .card-desc {
    color: #979ba5;
    margin-top: .06rem;
    line-height: .2rem;
  }
}
</style>
