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
import { Operation } from '@/utils/vue-ts';
import { Button, Dropdown } from 'bkui-vue';
import { defineComponent, PropType } from 'vue';
import { useI18n } from 'vue-i18n';
import Icon from './Icon';

const { DropdownMenu, DropdownItem } = Dropdown;
export default defineComponent({
  props: {
    operationList: {
      type: Array as PropType<Operation[]>,
      default: () => [],
    },
    trigger: {
      type: String,
      default: 'hover',
    },
    placement: {
      type: String,
      default: 'bottom-start',
    },
    handleOperation: {
      type: Function,
      default: () => () => {},
    },
  },
  setup(props) {
    const { t } = useI18n();
    function handleClick(e: MouseEvent, operation: Operation) {
      if (operation.disabled) return;
      props.handleOperation(e, operation);
    }
    return () => (
      <Dropdown class="operation-menu" trigger={props.trigger} placement={props.placement}>
      {{
        default: () => <span><Icon size={14} name="more" /></span>,
        content: () => (
          <DropdownMenu>
            {
              props.operationList.map(operation => (
                <DropdownItem
                  key={operation.id}
                  onClick={(e: MouseEvent) => handleClick(e, operation)}
                >
                    <Button class="operation-item-btn" text disabled={operation.disabled} >
                      {t(operation.name)}
                    </Button>
                </DropdownItem>
              ))
            }
          </DropdownMenu>
        ),
      }}
    </Dropdown>
    );
  },
});
