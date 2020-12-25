<template>
    <ul>
        <li
            :class="picked && picked.includes(week.id) ? 'active' : ''"
            @click="selectedWeek(week.id)"
            class="settings-trigger-week"
            v-for="week in weekList"
            :key="week.label">
            {{week.name}}
        </li>
    </ul>
</template>

<script>
    export default {
        name: 'week-selector',
        props: {
            picked: {
                type: Array,
                default: () => ([])
            }
        },
        data () {
            return {
                weekList: [
                    {
                        id: 1,
                        name: this.$t('一'),
                        label: 'Mon'
                    },
                    {
                        id: 2,
                        name: this.$t('二'),
                        label: 'Tues'
                    },
                    {
                        id: 3,
                        name: this.$t('三'),
                        label: 'Wed'
                    },
                    {
                        id: 4,
                        name: this.$t('四'),
                        label: 'Thur'
                    },
                    {
                        id: 5,
                        name: this.$t('五'),
                        label: 'Fri'
                    },
                    {
                        id: 6,
                        name: this.$t('六'),
                        label: 'Sat'
                    },
                    {
                        id: 7,
                        name: this.$t('日'),
                        label: 'Sun'
                    }
                ]
            }
        },
        methods: {
            selectedWeek (id) {
                this.$emit('select', id)
            }
        }
    }
</script>

<style lang="postcss" scoped>
    @import '../../css/variable.css';

    .settings-trigger-week {
        margin-right: 8px;
        display: inline-block;
        width: 32px;
        height: 32px;
        border-radius: 2px;
        border: 1px solid $itemBorderColor;
        cursor: pointer;
        line-height: 32px;
        text-align: center;
    }
    .active {
        border: 1px solid $goingColor;
        color: $goingColor;
    }
</style>
