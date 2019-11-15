<template>
    <div class="record-list" v-if="records.length">
        <div class="search-area">
            <input v-bk-focus="1" v-model.trim="searchValue" />
            <i class="bk-icon icon-search"></i>
        </div>
        <ul>
            <li v-for="item in filterRecords" :title="item[paramName]" :key="item[paramId]" :class="{ 'active': activeId === item[paramId] }" @click.stop="handleRecordClick(item)">
                {{ item[paramName] }}
            </li>
        </ul>
    </div>
</template>

<script>
    export default {
        name: 'crumb-records',
        props: {
            records: {
                type: Array,
                default: []
            },
            handleRecordClick: {
                type: Function
            },
            activeId: {
                type: String,
                default: ''
            },
            paramId: {
                type: String,
                default: 'id'
            },
            paramName: {
                type: String,
                default: 'name'
            }
        },
        data () {
            return {
                searchValue: ''
            }
        },
        computed: {
            filterRecords () {
                return this.records.filter(item => {
                    const lcName = item[this.paramName].toLowerCase()
                    const lcSearchValue = this.searchValue.toLowerCase()
                    return lcName.indexOf(lcSearchValue) > -1
                })
            }
        }
    }
</script>

<style lang="scss">
    @import '../../scss/mixins/ellipsis';
    
    .record-list {
        position: absolute;
        background: white;
        width: 222px;
        border-radius: 2px;
        box-shadow: 0 0 8px 1px rgba(0,0,0,0.1);
        z-index: 100;
        top: 52px;
        .search-area {
            position: relative;
            border-bottom: 1px solid #e5e5e5;
            cursor: default;
            padding: 5px;
            line-height: 32px;
            input {
                width: 100%;
                height: 32px;
                line-height: 32px;
                padding: 10px;
                font-size: 14px;
                -webkit-box-shadow: none;
                box-shadow: none;
                outline: none;
                background-color: #fafbfd;
                border: 1px solid #dde4eb;
                border-radius: 2px;
                color: #63656E;
            }
            > i.icon-search {
                position: absolute;
                right: 14px;
                top: 5px;
                height: 30px;
                line-height: 30px;
                color: #ccc;
            }
        }
        ul {
            overflow: auto;
            max-height: 360px;
            > li {
                height: 42px;
                line-height: 42px;
                width: 100%;
                border-left: #c3cdd7;
                border-right: #c3cdd7;
                background-color: #fff;
                cursor: pointer;
                text-align: left;
                padding: 0 10px;
                font-size: 14px;
                @include ellipsis();

                &:hover,
                &.active {
                    background-color: #eef6fe;
                    color: #3c96ff;
                }
            }
        }
    }
</style>
