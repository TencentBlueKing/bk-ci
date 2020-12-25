<template>
    <div class="repo-selector">
        <bk-select v-model="value" style="width: 250px;" searchable>
            <bk-option v-for="(option, index) in list"
                :key="index"
                :id="option.id"
                :name="option.name">
            </bk-option>
        </bk-select>
        <a href="">{{$t('新增')}}<i class="bk-icon icon-link"></i></a>
    </div>
</template>

<script>
    import http from '@/api'

    export default {
        name: 'repo-selector',
        props: {
        },
        data () {
            return {
                list: [
                    {
                        id: 123,
                        name: 'test'
                    }
                ]
            }
        },
        computed: {
        },
        created () {
            this.fetchData()
        },
        methods: {
            fetchData () {
                http.get('/repo/index?invoke=list').then(res => {
                    const list = res.data || []
                }).catch(e => e)
            },
            handleClick (e) {
                this.picked = !this.picked
                this.$emit('click', this.picked, this.tool.name, e)
            }
        }
    }
</script>

<style lang="postcss">
    @import '../../css/mixins.css';

    .repo-selector {

    }
</style>
