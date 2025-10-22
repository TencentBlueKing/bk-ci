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
import { classes } from '@/utils';
import { CommonOption } from '@/utils/vue-ts';
import { defineComponent, PropType, computed } from 'vue';
import Icon from './Icon';

export default defineComponent({
  props: {
    options: {
      type: Array as PropType<CommonOption[]>,
      required: true,
    },
    showCornerCheck: {
      type: Boolean,
    },
    modelValue: {
      type: String,
    },
  },
  emits: ['change', 'update:modelValue', 'input'],
  setup(props, ctx) {
    function handleClick(option: CommonOption) {
      ctx.emit('change', option.id, option);
      ctx.emit('update:modelValue', option.id, option);
      ctx.emit('input', option.id, option);
    }
    const optionList = computed(() => props.options.map((option: CommonOption) => {
      const isActive = option.id === props.modelValue;
      return {
        ...option,
        isActive,
        optionCls: classes({
          active: isActive,
          'corn-check': isActive && props.showCornerCheck,
        }, 'block-enum-option'),
      };
    }));
    return () => (
        <div class="block-enum-select">
          {
            optionList.value.map(option => (
              <div
                onClick={() => handleClick(option)}
                class={option.optionCls}>
                {ctx.slots?.default?.(option) ?? option.name }
                {option.isActive && props.showCornerCheck && <Icon size={20} class="corn-check-icon" name="check" />}
              </div>
            ))
          }
      </div>
    );
  },
});
