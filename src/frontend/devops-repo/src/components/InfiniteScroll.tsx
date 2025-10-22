/*
* Tencent is pleased to support the open source community by making
* 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition) available.
*
* Copyright (C) 2021 Tencent.  All rights reserved.
*
* 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition) is licensed under the MIT License.
*
* License for 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition):
*
* ---------------------------------------------------
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
* documentation files (the "Software"), to deal in the Software without restriction, including without limitation
* the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
* to permit persons to whom the Software is furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all copies or substantial portions of
* the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
* THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
* CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
* IN THE SOFTWARE.
*/
import { throttle } from '@/utils';
import { Loading } from 'bkui-vue';
import { defineComponent, nextTick, ref } from 'vue';
import { useI18n } from 'vue-i18n';

export default defineComponent({
  props: {
    threshold: {
      type: Number,
      default: 200,
    },
    hasNext: Boolean,
  },
  emits: ['srollEnd'],
  setup(props, ctx) {
    const { t } = useI18n();
    const scrollComponent = ref();
    const isLoadingMore = ref(false);
    function handleScroll(e: UIEvent) {
      const target = e.target as HTMLElement;
      const offset = target?.scrollHeight - (target?.offsetHeight + target?.scrollTop);
      console.log(offset, target?.scrollHeight, target?.offsetHeight, target?.scrollTop);
      if (offset <= props.threshold && props.hasNext && !isLoadingMore.value) {
        isLoadingMore.value = true;
        ctx.emit('srollEnd', done);
      }
    }

    async function done() {
      await nextTick();
      isLoadingMore.value = false;
    }
    const onScroll = throttle(handleScroll);

    return () => (
      <div class="infinite-scroll-container" ref={scrollComponent} onScroll={onScroll}>
        {ctx.slots.default?.()}
          <p class="infinite-scroll-load-more-indicate">
            {
              isLoadingMore.value && (
                <>
                <Loading loading size='small' mode='spin' />
                <span class="load-more-label">{t('loadMore')}</span>
                </>
              )
            }
          </p>
      </div>
    );
  },
});
