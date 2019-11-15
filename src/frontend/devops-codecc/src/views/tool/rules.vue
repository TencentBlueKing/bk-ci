<template>
    <div class="rules">
        <div class="rules-header">
            <div class="rules-header-left">
                <bk-select @clear="contentClose" @change="search" v-model="searchData" style="width: 501px;background-color:#fff;" searchable>
                    <bk-option v-for="(option, index) in optionData"
                        :key="index"
                        :id="option"
                        :name="option">
                    </bk-option>
                </bk-select>
            </div>
            <div class="rules-header-right">
                <span class="is-using">{{using}}</span><p>{{$t('checkers.已启用')}}</p>
                <span class="not-using">{{optionData.length - using}}</span><p>{{$t('checkers.未启用')}}</p>
            </div>
            <div class="rules-header-tips" v-if="tipsVisible">
                <i class="bk-icon icon-info-circle-shape"></i>
                {{$t('checkers.建议先了解规则，根据项目实际情况渐进式打开规则，一次性打开全部规则有可能造成项目告警数大量增加')}}
                <a @click="tipsVisibleChange">{{$t('checkers.不再提示')}}</a>
            </div>
        </div>
        <div id="card-position" ref="parents" class="rules-body">
            <p>{{$t('checkers.默认')}}</p>
            <div class="rules-body-default" v-for="(value, index) in rulesData" :key="index">
                <Card :ref="index" v-if="index === 0" :data="rulesData[0]" :index="index" @change="contentVisibaleChange" />
            </div>
            <p>{{$t('checkers.推荐')}}</p>
            <div class="rules-body-recommend" v-for="(value, index) in rulesData" :key="index" :style="index !== 0 ? '' : 'padding: 0px;'">
                <Card :ref="index" v-if="index !== 0" :data="value" :index="index" @change="contentVisibaleChange" @close="contentClose" />
            </div>
        </div>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import Card from '@/components/rules-card/index'
    export default {
        components: {
            Card
        },
        data () {
            return {
                searchData: '',
                tipsVisible: true
            }
        },
        computed: {
            ...mapState('tool', {
                toolRules: 'rules'
            }),
            rulesData () {
                let rulesData = []
                if (this.toolRules) {
                    rulesData = Object.assign([], this.toolRules)
                }
                rulesData.map(item => {
                    item.open = false
                })
                return rulesData
            },
            optionData () {
                const optionData = []
                for (const item in this.rulesData) {
                    for (const i in this.rulesData[item].checkerList) {
                        optionData.push(this.rulesData[item].checkerList[i].checkerKey)
                    }
                }
                return optionData
            },
            using () {
                let using = 0
                for (const item in this.rulesData) {
                    using = using + this.rulesData[item].openCheckerNum
                }
                return using
            }
        },
        mounted () {
            const tipsVisible = JSON.parse(window.localStorage.getItem('tipsVisible'))
            tipsVisible === null ? this.tipsVisible = 'true' : this.tipsVisible = tipsVisible
        },
        created () {
            this.init()
        },
        methods: {
            init () {
                this.$store.dispatch('tool/rules', this.$route.params.toolId)
            },
            tipsVisibleChange () {
                window.localStorage.setItem('tipsVisible', JSON.stringify(false))
                this.tipsVisible = false
            },
            contentClose (index) {
                this.rulesData.map(item => {
                    item.open = false
                })
                for (let i = 0; i < this.rulesData.length; i++) {
                    this.rulesData[i] = Object.assign({}, this.rulesData[i])
                }
                this.$forceUpdate()
            },
            contentVisibaleChange (index) {
                if (this.rulesData[index].open === false) {
                    this.rulesData.map(item => {
                        item.open = false
                    })
                    this.rulesData[index].open = true
                    if (this.$refs[index] && this.$refs[index][0]) {
                        const top = 18 + this.$refs[index][0].$el.offsetTop
                        setTimeout(() => {
                            document.getElementsByClassName('main-container')[0].scrollTo({
                                left: 0,
                                top: top,
                                behavior: 'smooth'
                            })
                        }, 0)
                    }
                } else {
                    this.rulesData.map(item => {
                        item.open = false
                    })
                }
                for (let i = 0; i < this.rulesData.length; i++) {
                    this.rulesData[i] = Object.assign({}, this.rulesData[i])
                }
                this.$forceUpdate()
            },
            search (val) {
                for (const item in this.rulesData) {
                    for (const i in this.rulesData[item].checkerList) {
                        if (this.rulesData[item].checkerList[i].checkerKey === val) {
                            this.rulesData.map(item => {
                                item.open = false
                            })
                            this.rulesData[item].open = true
                            if (this.$refs[item] && this.$refs[item][0]) {
                                const top = 18 + this.$refs[item][0].$el.offsetTop
                                setTimeout(() => {
                                    document.getElementsByClassName('main-container')[0].scrollTo({
                                        top: top,
                                        left: 0,
                                        behavior: 'smooth'
                                    })
                                }, 0)
                            }
                            for (let i = 0; i < this.rulesData.length; i++) {
                                this.rulesData[i] = Object.assign({}, this.rulesData[i])
                            }
                            this.$forceUpdate()
                            this.$refs[item][0].position(i)
                        }
                    }
                }
            }
        }
    }
</script>

<style lang="postcss" scoped>
    @import '../../css/variable.css';
    .rules {
        .rules-header {
            display: flow-root;
            .rules-header-left {
                color: #ffffff;
                float: left;
            }
            .rules-header-right {
                color: $fontLightColor;
                font-size: 14px;
                display: flex;
                float: right;
                p {
                    padding: 10px;
                }
                .is-using {
                    color: $successColor;
                    font-size: 18px;
                    line-height: 2;
                }
                .not-using {
                    color: #63656e;
                    font-size: 18px;
                    line-height: 2;
                    padding-left: 18px;
                }
            }
            .rules-header-tips {
                border: 1px solid #ffe8c3;
                background-color: #fff8ee;
                display: inline-block;
                font-size: 12px;
                line-height: 46px;
                padding: 0 18px;
                width: 100%;
                a {
                    float: right;
                    cursor: pointer;
                }
                .icon-info-circle-shape {
                    color: #ffb848;
                    height: 14px;
                    width: 14px;
                    padding-right: 2px;
                }
            }
        }
        .rules-body {
            min-width: 1085px;
            font-size:14px;
            color: #63656e;
            padding-top: 10px;
            p {
                padding: 10px 0;
                font-weight:bold;
            }
            .rules-body-default, .rules-body-recommend {
                display: inline-table;
                height:19px;
                line-height:19px;
                padding: 5px;
            }
        }
    }
    
</style>
