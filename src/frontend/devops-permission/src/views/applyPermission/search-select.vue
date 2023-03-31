<template>
  <section
    ref="wrap"
    class="search-select"
    :class="{ 'is-focus': input.focus }"
    @click="handleWrapClick">
    <div
      class="search-input"
      :style="{ maxHeight: (shrink ? (input.focus ? maxHeight : minHeight) : maxHeight) + 'px' }">
      <template v-if="searchSelectValue.length">
        <!-- 已经选中的tag列表 -->
        <select-tag
          v-for="tagInfo in searchSelectValue"
          :key="tagInfo.id"
          :list="list"
          :tag-info="tagInfo"
          :is-disabled="isDisabled"
          @handleTagClear="handleTagClear">
        </select-tag>
      </template>
      <div class="search-input-input">
        <bk-popover
          trigger='manual'
          theme='light group-select-popover'
          :disableOutsideClick="true"
          :arrow="false"
          :isShow="showMenuPopover"
          placement='bottom-start'>
          <div
            ref="input"
            class="div-input"
            :class="{ 'input-before': isShowPlaceholder }"
            contenteditable="plaintext-only"
            :data-placeholder="placeholder"
            @focus="handleInputFocus"
            @input="handleInputChange"
            @keydown="handleInputKeyup"
            v-clickoutside="handleInputClickOutSide">
          </div>
          <template #content>
            <template v-if="!selectInfo.id">
              <!-- 搜索匹配列表 -->
              <template v-if="input.value && !selectInfo.id">
                <ul class="search-list-menu">
                  <template v-if="optionList.length">
                    <li
                      v-for="(option, index) in optionList"
                      :key="index"
                      class="menu-item search-menu-item"
                      :class="{ 'default-menu-item': option.isDefaultOption, 'is-hover': hoverId === option.id }"
                      @click="handleResultOptionSelect(option)">
                      <span class="search-menu-label">{{ option.name + '：' }}</span>
                      <span class="value-text">{{ input.value }}</span>
                    </li>
                  </template>
                  <li v-else class="menu-item no-search-data">{{ '查询无数据' }}</li>
                </ul>
              </template>
              <!-- 菜单默认列表 -->
              <template v-else-if="optionList.length">
                <ul class="search-list-menu">
                  <div v-for="option in optionList" :key="option.id">
                    <div v-bk-tooltips="{ content: $t('尚未加入项目。项目成员才可以根据资源实例查询'), placement: 'right', disabled: !option.disabled }">
                      <li
                        :key="option.id"
                        class="menu-item"
                        :class="{ 'is-hover': hoverId === option.id, 'is-disabled': option.disabled }"
                        @click="handleTitleSelect(option)">
                        {{ option.name }}
                      </li>
                    </div>
                  </div>
                </ul>
              </template>
            </template>
            <template v-else>
              <!-- 条件已选中操作或资源实例场景 -->
              <template v-if="hasResourceCode || hasActionId">
                <scroll-load-list
                  ref="loadList"
                  :list="resourceActionsList"
                  :hasLoadEnd="hasLoadEnd"
                  :getDataMethod="getDataMethod"
                  :resourceType="resourceType"
                  :showMenu="showMenu"
                  :titleType="titleType"
                  @change="changeKeyWords"
                >
                  <template v-slot="{ data }">
                    <div
                      :class="{ 'is-hover': hoverId === data.id }"
                      :key="data.id"
                      @click="handleResourceSelect(data)">
                      {{ data.name }}
                    </div>
                  </template>
                </scroll-load-list>
              </template>
              <template v-else>
                <section class="cascader-menu">
                  <ul class="search-list-menu">
                    <template v-if="optionList.length">
                      <div v-for="option in optionList" :key="option.id">
                        <li
                          :key="option.id"
                          class="menu-item"
                          :class="{ 'is-hover': hoverId === option.resourceType }"
                          @click="handleOptionSelect(option)">
                          {{ option.name }}
                          <a class="permission-icon permission-icon-angle-right angle-right-icon"></a>
                        </li>
                      </div>
                    </template>
                    <p v-else class="no-search-data">{{ '查询无数据' }}</p>
                  </ul>
                  <div>
                    <div v-if="showMenu" class="cascader-panel">
                      <scroll-load-list
                        ref="loadList"
                        :list="resourceActionsList"
                        :hasLoadEnd="hasLoadEnd"
                        :getDataMethod="getDataMethod"
                        :resourceType="resourceType"
                        :showMenu="showMenu"
                        :titleType="titleType"
                        @change="changeKeyWords"
                      >
                        <template v-slot="{ data }">
                          <div
                            :class="{ 'is-hover': hoverId === data.id }"
                            @click="handleResourceSelect(data)"
                          >
                            {{ data.name }}
                          </div>
                        </template>
                      </scroll-load-list>
                    </div>
                  </div>
                </section>
              </template>
            </template>
          </template>
        </bk-popover>
      </div>
    </div>
    <span v-if="input.value || searchSelectValue.length" @click.stop="handleClear" class="permission-icon permission-icon-close-circle-shape close-icon"></span>
    <span v-else @click.stop="handleKeyEnter" :class="['permission-icon permission-icon-search search-icon', { 'is-focus': input.focus }]"></span>
  </section>
</template>

<script>
import http from '@/http/api';
import tools from '../../utils/tools.js'
import { clickoutside, Message } from 'bkui-vue';
import selectTag from './selectTag'
import { Search } from 'bkui-vue/lib/icon'
import scrollLoadList from '@/components/scroll-load-list.vue'

export default {
  name: 'searchSelect',
  directives: {
    clickoutside,
  },
  components: {
    selectTag,
    Search,
    scrollLoadList,
  },
  model: {
    prop: 'values',
    event: 'change',
  },
  props: {
    placeholder: {
      type: String,
      default: '资源实例/用户组/描述/操作/ID',
    },
    searchList: {
      type: Array,
      default: () => [],
    },
    values: {
      type: Array,
      default: () => [],
    },
    maxHeight: {
      type: [String, Number],
      default: 120,
    },
    minHeight: {
      type: [String, Number],
      default: 32,
    },
    shrink: {
      type: Boolean,
      default: true,
    },
    isSearch: {
      type: Boolean,
      default: true,
    },
    id: {
      type: String,
      default: '',
    },
    projectCode: {
      type: String,
      default: '',
    },
    isDisabled: {
      type: Boolean,
    }
  },
  computed: {
    isShowPlaceholder() {
      return !this.searchSelectValue.length
        && !this.input.value.length
        && !Object.keys(this.selectInfo).length;
    },
    optionList() {
      let curList = this.list.filter(option => {
        return !this.searchSelectValue.some(item => item.id === option.id);
      });
      const { id, children } = this.selectInfo;
      if (id) {
        if (children) {
          const text = this.input.value;
          if (text) {
            curList = children.filter(item => item.name.indexOf(text) > -1);
          } else {
            curList = children;
          }
        }
      } else if (this.input.value) {
        const inputOptions = this.list.filter(item => !item.children) || [];
    
        curList = inputOptions.filter(option => {
          const isMatch = this.searchSelectValue.some(item => item.id === option.id);
          return !isMatch;
        }) || []
      }

      curList.forEach(option => {
        if (['actionId', 'resourceCode'].includes(option.id)) {
          option.children = this.resourcesTypeList;
        }
      });

      return curList;
    },
    boundary() {
      return document.body;
    },
    resourceActionsList() {
      if (this.resourceList.length) return this.resourceList;
      return this.actionsList;
    },
    getDataMethod() {
      if (this.selectInfo.id === 'actionId') {
        return this.getActionsList;
      }
      return this.getResourceList;
    }
  },
  watch: {
    searchList: {
      handler (val) {
        this.list = tools.deepClone(val);
      },
      deep: true,
      immediate: true,
    },
    searchSelectValue: {
      handler (val) {
        this.$emit('change', val)
      },
      deep: true
    },
    projectCode: {
      handler (val) {
        this.curProjectCode = val
        this.searchSelectValue = []
      },
      deep: true
    }
  },
  data() {
    return {
      list: [],
      selectInfo: {},
      input: {
        value: '',
        focus: false,
      },
      searchSelectValue: [],
      selectTagList: [],
      showMenuPopover: false,
      showMenu: false,
      hoverId: '',
      actionsList: [],
      resourceList: [],
      resourcesTypeList: [],
      isLoading: false,
      resourceType: '',
      resourceTypeName: '',
      hasResourceCode: false,
      hasActionId: false,
      hasLoadEnd: false,
      titleType: '',
      curProjectCode: ''
    }
  },
  async created() {
    await this.getResourceTypesList();
    await this.initApplyQuery();
  },
  methods: {
    async initApplyQuery() {
      const cacheQuery =  JSON.parse(sessionStorage.getItem('group-apply-query'));
      
      if (!cacheQuery || (cacheQuery && this.$route.query.project_code !== cacheQuery?.project_code)) {
        sessionStorage.setItem('group-apply-query', JSON.stringify(this.$route.query))
      }
      const query = cacheQuery || this.$route.query
      const { resourceType, action, iamResourceCode, groupId, groupName } = query;
      if (resourceType && iamResourceCode && action) {
        this.resourceType = resourceType;
        if (groupId) {
          await this.getResourceList();
          await this.getActionsList();
          const resourceTypeName = this.resourcesTypeList.find(i => i.resourceType === resourceType).name
          const resourceValue = this.resourceList.find(i => i.iamResourceCode === iamResourceCode);
          resourceValue.name = `${resourceTypeName}/${resourceValue.resourceName}`
          const resourceCodeParams = {
            id: 'resourceCode',
            name: this.$t('资源实例'),
            values: [resourceValue],
          };
          this.searchSelectValue.push(resourceCodeParams);
          const actionValue = this.actionsList.find(i => i.action === action)
          const actionParams = {
            id: 'actionId',
            name: this.$t('操作'),
            values: [actionValue],
          }
          this.searchSelectValue.push(actionParams);
        } else {
          await this.getActionsList();
          const resourceTypeName = this.resourcesTypeList.find(i => i.resourceType === resourceType).name
          const actionValue = this.actionsList.find(i => i.action === action)
          actionValue.name = `${resourceTypeName}/${actionValue.actionName}`
          const actionParams = {
            id: 'actionId',
            name: this.$t('操作'),
            values: [actionValue],
          }
          this.searchSelectValue.push(actionParams);
        }

        if (groupName) {
          const nameParams = {
            id: 'name',
            name: this.$t('用户组名'),
            values: [groupName]
          }
          this.searchSelectValue.push(nameParams);
        }
      } else if (resourceType && iamResourceCode) {
        this.resourceType = resourceType;
        await this.getResourceList();
        await this.getActionsList();
        const resourceTypeName = this.resourcesTypeList.find(i => i.resourceType === resourceType).name
        const resourceValue = this.resourceList.find(i => i.iamResourceCode === iamResourceCode);
        resourceValue.name = `${resourceTypeName}/${resourceValue?.resourceName}`
        const resourceCodeParams = {
          id: 'resourceCode',
          name: this.$t('资源实例'),
          values: [resourceValue],
        };
        this.searchSelectValue.push(resourceCodeParams);
      }
    },
    changeKeyWords() {
      this.resourceList = [];
    },
    async getActionsList() {
      this.isLoading = true;
      return await http.getActionsList(this.resourceType).then(res => {
        this.actionsList = res.map(item => {
          return {
            ...item,
            id: item.actionId,
            name: item.actionName,
          }
        });
        this.hasLoadEnd = true;
      }).catch(() => []);
    },
    
    async getResourceList(page, pageSize, keyWords) {
      if (!this.curProjectCode) {
        Message({
          theme: 'error',
          message: this.$t('请选择项目'),
        });
        return;
      };
      this.isLoading = true
      return await http.getResourceList(
        {
          resourceType: this.resourceType,
          projectId: this.projectCode,
          resourceName: keyWords
        },
        {
          page,
          pageSize
        }
      )
      .then(res => {
        this.hasLoadEnd = !res.hasNext;
        const list = res.records.map(item => {
          return {
            ...item,
            name: item.resourceName,
          }
        });
        this.resourceList = [...this.resourceList, ...list]
        this.isLoading = false
      });
    },
    // 获取资源类型列表
    async getResourceTypesList() {
      await http.getResourceTypesList().then(res => {
        this.resourcesTypeList = res.map(item => {
          return {
            ...item,
            id: item.resourceType,
          }
        });
      });
    },
    setInputFocus() {
      setTimeout(() => {
        this.$refs.input.focus();
      }, 100);
    },
    // searchSelect点击事件
    handleWrapClick() {
      if (this.shrink) {
        this.setInputFocus();
      }
      if (!this.selectInfo.id || this.selectInfo.children) {
        this.showMenuPopover = true;
      }
    },
    // 点击到searchSelect外面
    handleInputClickOutSide(e) {
      const parent = e.target.offsetParent;
      const classList = parent ? parent.classList : null;
      
      const unFocus = !parent || (classList && !Array.from(classList.values()).some(key => {
        return ['search-select', 'bk-popover', 'menu-item'].includes(key);
      }));
      if (unFocus) {
        this.showMenuPopover = false;
        this.input.focus = false;
        this.hoverId = '';
        this.showMenu = false;
      };
    },
    // 文本框获取焦点
    handleInputFocus() {
      this.input.focus = true
      const input = this.$refs.input;
      // 设置文本框焦点显示位置
      let selection = null;
      if (window.getSelection) {
        selection = window.getSelection();
        selection.selectAllChildren(input);
        selection.collapseToEnd();
      } else if (document.onselectionchange) {
        selection = document.onselectionchange.createRange();
        selection.moveToElementText(input);
        selection.collapse(false);
        selection.select();
      };
    },
    // 选中标题类型
    handleTitleSelect(val) {
      if (val.disabled) return;
      this.actionsList = [];
      this.resourceList = [];
      this.hasResourceCode = this.searchSelectValue.some(item => item.id === 'resourceCode');
      this.hasActionId = this.searchSelectValue.some(item => item.id === 'actionId');
      this.titleType = val.id;

      this.selectInfo = tools.deepClone(val);
      const inputDom = this.$refs.input;
      inputDom.innerText = val.name + '：';
      this.input.value = this.selectInfo.id ? '' : val.name + '：';
      // 输入框获取焦点
      inputDom.focus();
      if (val.children) {
        this.selectTagList = [];
      } else {
        // 收起popover
        this.showMenuPopover = false;
      };
    },
    
    async handleOptionSelect(val) {
      if (this.showMenu && (this.resourceType === val.resourceType)) return
      this.showMenu = true;
      this.hasLoadEnd = false;
      this.actionsList = [];
      this.resourceList = [];
      const { resourceType, name } = val
      this.resourceType = resourceType;
      this.resourceTypeName = name;
      this.hoverId = resourceType;
      this.isLoading = true;
      this.isLoading = false;
    },

    handleResourceSelect(val) {
      this.showMenuPopover = false;
      const valIndex = this.selectTagList.findIndex(item => item.id === val.id);
      if (valIndex > -1) {
        this.selectTagList.splice(valIndex, 1);
      } else {
        this.selectTagList.push(tools.deepClone({
          ...val,
          name: (this.hasResourceCode || this.hasActionId) ? val.name : this.resourceTypeName + '/' +  val.name,
        }));
      };
      const values = [...this.selectTagList];
      this.searchSelectValue.push({
        id: this.selectInfo.id,
        name: this.selectInfo.name,
        values,
      });

      this.resetPopover();
      setTimeout(() => {
        this.showMenuPopover = true;
        this.hoverId = '';
        this.showMenu = false;
      }, 100);
    },
    // 取消按钮
    selectOptionCancel() {
      this.showMenuPopover = false;
      this.selectTagList = [];
      this.input.value = '';
      this.$refs.input.innerText = this.selectInfo.name + '：';
      this.setInputFocus();
    },
    // 重置popover数据
    resetPopover() {
      this.selectTagList = [];
      this.$refs.input.innerText = '';
      this.input.value = '';
      this.selectInfo = {};
      this.setInputFocus();
    },

    // 快速清空
    handleClear() {
      this.setInputFocus();
      this.input.value = '';
      this.selectInfo = {};
      this.searchSelectValue = [];
      this.isVisible = true;
    },
    handleInputChange(e) {
      const text = e.target.innerText;
      this.input.value = this.selectInfo.id ? text.slice(text.indexOf('：') + 1) : text.trim();
      this.hoverId = '';
      if (this.selectInfo.id) {
        // 不包含：标识符默认为自定义搜索条件
        if (text.indexOf('：') === -1) {
          this.selectInfo = {};
        }
      } else {
        this.selectInfo = {};
      }
      if (!text && !this.searchSelectValue.length) { // 没有自定义搜索条件和已选中条件，重置searchPopover
        this.isVisible = true;
      }
    },

    // 文本框按键事件
    handleInputKeyup (e) {
      switch (e.code) {
        case 'Enter':
        case 'NumpadEnter':
          this.handleKeyEnter(e);
          break;
        case 'Backspace':
          this.handleKeyBackspace(e);
          break;
        case 'ArrowDown':
        case 'ArrowUp':
          e.preventDefault();
          this.handleDocumentKeydown(e);
          break;
        default:
          if (this.selectTagList.length) {
            e.preventDefault();
            return false;
          }
          return false;
      }
    },

    judgeOptionShow (option) {
      return !this.searchSelectValue.some(item => item.id === option.id)
    },
    handleDocumentKeydown (e) {
      const len = this.optionList.filter(option => {
        return this.judgeOptionShow(option)
      }).length;
      if (len) {
        e.preventDefault();
        e.stopPropagation();
        this.setInputFocus();
        let curIndex = this.optionList.findIndex(set => set.id === this.hoverId);
        curIndex = e.code === 'ArrowDown' ? curIndex + 1 : curIndex - 1;
        curIndex = curIndex > len - 1 ? 0 : (curIndex < 0 ? len - 1 : curIndex);
        const option = this.optionList[curIndex];
        if (option) {
          this.hoverId = this.selectInfo.id ? option.resourceType : option.id;
          setTimeout(() => {
            const dom = document.getElementsByClassName('is-hover')[0];
            const searchListDom = document.getElementsByClassName('search-list-menu')[0];
            const scrollTop = searchListDom.scrollTop;
            const searchListDomHeight = searchListDom.clientHeight;
            if ((searchListDomHeight + scrollTop) < (curIndex + 1) * 32) {
              searchListDom.scrollTop = ((curIndex + 1) * 32) + 12 - searchListDomHeight;
            } else if (scrollTop > (curIndex * 32) - 6) {
              searchListDom.scrollTop = (curIndex * 32) + 6
            }
          });
        };
      }
    },

    // 回车按键
    handleKeyEnter (e) {
      e.preventDefault()
      setTimeout(() => {
        if (this.hoverId) {
          const option = this.optionList.find(item => item.id === this.hoverId)
          this.hoverId = '';
          if (option) {
            if (this.selectInfo.id) {
              this.handleOptionSelect(option)
            } else {
              this.handleTitleSelect(option)
            }
          }
          this.handleInputFocus()
          return
        }
        if (!this.input.value) {
          return
        }
        let info = {}
        const { id, children } = this.selectInfo
        if (id) {
          const index = this.searchSelectValue.findIndex(item => item.id === id)
          if (index === -1) {
            if (children && !this.optionList.length) return // 当包含子项时，如果输入匹配列表为空时，回车禁止选中
            info = {
              id: this.selectInfo.id,
              name: this.selectInfo.name,
              values: [this.input.value]
            }
            this.searchSelectValue.push(info)
          }
        } else {
          const isMatch = this.searchSelectValue.some(item => item.isDefaultOption)
          if (!isMatch) {
            const defaultOption = this.list.find(item => item.isDefaultOption) || {}
            info = {
              id: defaultOption.id,
              name: defaultOption.name,
              values: [this.input.value]
            }
            this.searchSelectValue.push(info)
          } else {
            return
          }
        }

        this.hoverId = ''
        this.input.value = ''
        this.$refs.input.innerText = ''
        this.selectInfo = {}
        this.showMenuPopover = true
      }, 100)
    },
    // 选择搜索结果匹配下拉项
    handleResultOptionSelect(option) {
      const selectInfo = {
        id: option.id,
        name: option.name,
        values: [this.input.value]
      };
      this.searchSelectValue.push(selectInfo);
      this.resetPopover();
    },

    // 清空按键
    handleKeyBackspace(e) {
      if (!this.input.value && !this.selectInfo.id) {
        this.searchSelectValue.pop();
      }
    },

    // 修改已生成的tag
    handleTagClear(id) {
      const index = this.searchSelectValue.findIndex(item => item.id === id);
      this.searchSelectValue.splice(index, 1);
      this.setInputFocus();
      setTimeout(() => {
        this.showMenuPopover = true;
      }, 100);
    },
  }
}
</script>

<style lang="postcss" scoped>
  :deep(.bk-popover-content) {
    z-index: 999 !important;
  }
  .search-select {
    position: relative;
    z-index: 666;
    background: #fff;
    display: flex;
    flex-direction: row;
    align-items: center;
    font-size: 12px;
    min-height: 32px;
    box-sizing: border-box;
    border: 1px solid #c4c6cc;
    border-radius: 2px;
    outline: none;
    resize: none;
    transition: border 0.2s linear;
    overflow: auto;
    color: #63656e;
    flex-wrap: wrap;
    padding-right: 25px;
    &::-webkit-scrollbar {
      width: 6px;
      height: 6px;
      &-thumb {
        border-radius: 20px;
        background: #a5a5a5;
        box-shadow: inset 0 0 6px hsla(0, 0%, 80%, .3);
      }
    }
    &.is-focus {
      border-color: #3a84ff;
    }
    transition: all .5s;
  }
  .search-input {
    flex: 1;
    position: relative;
    text-align: left;
    overflow: visible;
    display: flex;
    flex-wrap: wrap;
    min-height: 30px;
    transition: max-height .3s cubic-bezier(0.4, 0, 0.2, 1);

    .search-input-input {
      position: relative;
      padding: 0 8px;
      color: #63656e;
      flex: 1 1 auto;
      border: none;
      min-width: 40px;
      display: flex;
      align-items: center;
      .div-input {
        width: 100%;
        line-height: 30px;
        word-break: break-all;
        position: relative;
        border: none;
        padding-right: 5px;
        &:focus {
          outline: none;
        }
      }
      .input-before {
        &:before {
          content: attr(data-placeholder);
          color: #c4c6cc;
          padding-left: 2px;
        }
      }
      .div-input-disabled {
        &:before {
          content: attr(data-placeholder);
          color: #c4c6cc;
          padding-left: 2px;
        }
      }
      :deep(.bk-tooltip) {
          width: 100%;
          .bk-tooltip-ref {
              width: 100%;
          }
      }
    }
  }
  .search-icon, .close-icon {
    position: absolute;
    top: 8px;
    right: 5px;
    font-size: 18px;
    color: #c4c6cc;
    display: inline-block;
    cursor: pointer;
    &.is-focus {
        color: #3a84ff;
    }
  }
  .close-icon {
    font-size: 14px;
    &:hover {
      color: #979ba5;
    }
  }

</style>
<style lang="postcss">
  .group-select-popover {
    padding: 0 !important;
    padding-bottom: 0 !important;
    .loading-panel,
    .search-list-menu,
    .select-list-menu {
      padding: 6px 0;
      max-height: 300px;
      overflow: auto;
      width: 200px !important;
      &::-webkit-scrollbar {
        width: 4px;
        height: 4px;
        &-thumb {
          border-radius: 20px;
          background: #a5a5a5;
          box-shadow: inset 0 0 6px hsla(0, 0%, 80%, .3);
        }
      }
      .data-null {
        text-align: center;
        padding: 90px 0;
        color: #979ba5;
      }
    }
    .loading-panel {
      height: 100%;
      color: #64666f;
      display: flex;
      align-items: center;
      justify-content: space-around;
    }
    .menu-item {
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      color: #63656e;
      padding: 0 15px !important;
      cursor: pointer;
      font-size: 12px;
      .icon-check-1 {
        font-size: 22px;
        color: #3a84ff;
      }
      &.is-disabled {
        cursor: not-allowed;
        color: #c4c6cc;
      }
      &:hover,
      &.is-hover {
        background: #f5f7fa;
      }
    }
    .search-menu-item {
      .search-menu-label {
        min-width: 60px;
        text-align: right;
        padding-left: 12px !important;
        font-weight: 700;
        white-space: nowrap;
      }
      .value-text {
        flex: 1;
        text-align: left;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      &.default-menu-item {
        background: #FFF5F7FA;
      }
    }

    .cascader-menu {
      display: flex;
      .cascader-panel {
        width: 200px;
        height: 100%;
      }
    }
    .no-search-data {
      height: 46px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #63656e;
      cursor: default;
      &:hover {
        color: #63656e;
        background: none;
      }
    }
    .popper__arrow {
      display: none !important;
    }
  }

  .search-tippy-popover {
    .tippy-tooltip {
      top:  5px !important;
      transform: translateY(0px) !important;
      padding: 7px 0;
    }
  }
  .angle-right-icon {
    color: #c4c6cc;
  }
</style>
