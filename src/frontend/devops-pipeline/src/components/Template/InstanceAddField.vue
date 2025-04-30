<template>
    <div>
        <bk-popover
            ref="dotMenuRef"
            :width="400"
            placement="bottom-start"
            theme="dot-menu light"
            trigger="click"
            :transfer="true"
            ext-cls="template-ext-menu"
            :on-show="handleShowMenu"
            :on-hide="handleHideMenu"
        >
            <bk-button
                type="primary"
                size="small"
            >
                {{ $t('template.addField') }}
            </bk-button>
            <div
                slot="content"
                class="popover-container"
            >
                <bk-input
                    clearable
                    v-model="searchKey"
                    :right-icon="'bk-icon icon-search'"
                    :placeholder="$t('template.keyword')"
                    class="search-input"
                    @right-icon-click="handlerSearch"
                />
                <div class="container-groups">
                    <div
                        v-for="(group, groupIndex) in filteredGroups"
                        :key="groupIndex"
                        class="group"
                    >
                        <div class="group-title">
                            <p>
                                {{ group.title }}
                                <span
                                    v-if="group.itemCount"
                                    class="item-count"
                                >
                                    （{{ group.itemCount }}）
                                </span>
                            </p>
                            <bk-checkbox
                                :value="group.isAll"
                                ext-cls="item-check"
                                @change="value => handleGroupSelect(value, group)"
                            >
                                {{ $t('template.selectAll') }}
                            </bk-checkbox>
                        </div>
                        <div class="group-content">
                            <div
                                v-for="(item, itemIndex) in group.items"
                                :key="itemIndex"
                                @click="toggleItemSelection(group, item)"
                                :class="{ 'item-selected': item.selected }"
                                v-if="item.name.includes(searchKey)"
                            >
                                {{ item.name }}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </bk-popover>
    </div>
</template>

<script setup>
    import { computed, ref, defineProps, defineEmits } from 'vue'
    const props = defineProps({
        searchKey: {
            type: String,
            default: ''
        },
        groups: {
            type: Array,
            default: () => []
        }
    })
    const emits = defineEmits(['selected-data-changed'])
    const hasShow = ref(false)
    // 选中的数据
    const selectedData = computed(() => props.groups.flatMap(group =>
        group.items.filter(item => item.selected)
    ))

    const filteredGroups = computed(() => props.groups.map(group => ({
        ...group,
        items: group.items.filter(item => item.name.includes(props.searchKey))
    })))
    function handleShowMenu () {
        hasShow.value = true
    }
    function handleHideMenu () {
        hasShow.value = false
    }
    function toggleItemSelection (group, item) {
        item.selected = !item.selected
        group.isAllSelected = group.items.every(item => item.selected)
        emits('selected-data-changed', selectedData.value)
    }
    function handleGroupSelect (value, group) {
        group.isAllSelected = value
        group.items.forEach(item => {
            item.selected = value
        })
        emits('selected-data-changed', selectedData.value)
    }
    function handlerSearch (value) {
        // TODO
    }
</script>

<style lang="scss">
.template-ext-menu {
  
  .popover-container {
      margin: 9px 0;

      .search-input {
          margin-bottom: 12px;
      }
      .container-groups{
          max-height: 500px;
          overflow: auto;
      }
      
      .group {
          margin-bottom: 12px;
  
          .group-title {
              display: flex;
              justify-content: space-between;
              align-items: center;
              font-size: 12px;
              color: #313238;
              line-height: 22px;

              p {
                  font-weight: 700;

                  .item-count {
                      font-weight: 500;
                      color: #63656E;
                  }
              }
          }
          
          .group-content {
              display: grid;
              flex-wrap: wrap;
              gap: 8px;
              grid-template-columns: repeat(2, 1fr);
              color: #63656E;
              margin-top: 4px;
          }
          
          .group-content div {
              box-sizing: border-box;
              height: 32px;
              padding: 6px;
              font-size: 12px;
              cursor: pointer;
              border-radius: 2px;
              &:hover {
                  background: #F5F7FA;
              }
          }
          
          .item-selected {
              background: #F5F7FA;
              color: #3A84FF;
              border-radius: 2px;
          }
      }
  }
}
</style>
