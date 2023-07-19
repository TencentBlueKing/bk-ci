<template>
    <section class="trigger-event">
        <header class="header-warpper">
            <bk-date-picker
                class="date-picker mr15"
                :placeholder="$t('codelib.选择日期范围')"
                type="daterange"
            >
            </bk-date-picker>
            <bk-search-select
                class="search-select"
                clearable
                :placeholder="$t('codelib.触发器类型/事件类型/触发人/流水线名称')"
                :data="searchList"
            >
            </bk-search-select>
        </header>
    </section>
</template>
<script>
    export default {
        name: 'basicSetting',
        components: {
        },
        props: {
           
        },
        data () {
            return {
                searchList: [
                    {
                        name: this.$t('codelib.触发器类型'),
                        id: 'triggerType',
                        children: []
                    },
                    {
                        name: this.$t('codelib.事件类型'),
                        id: 'eventType',
                        children: []
                    },
                    {
                        name: this.$t('codelib.触发人'),
                        id: 'triggerUser'
                    },
                    {
                        name: this.$t('codelib.流水线名称'),
                        id: 'pipelineName'
                    }
                ]
            }
        }
    }
</script>
<style lang='scss'>
    .trigger-event {
        .header-warpper {
            display: flex;
        }
        .date-picker {
            max-width: 300px;
            min-width: 200px;
        }
        .search-select {
            width: 70%;
        }
    }
</style>
