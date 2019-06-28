<template>
    <article class="store-home">
        <h3 class="market-home-title ">
            <icon class="title-icon" name="color-logo-store" size="25" />
            <p class="title-name">
                <router-link :to="{ name: 'atomHome', query: { pipeType: filterData.pipeType } }" class="back-home">研发商店</router-link>
                <i class="right-arrow banner-arrow"></i>
                <span class="banner-des">{{filterData.pipeType|pipeTypeFilter}}</span>
            </p>
            <router-link :to="{ name: 'atomList', params: { type: 'atom' } }" class="title-work">工作台</router-link>
        </h3>

        <main class="store-main" @scroll.passive="mainScroll">
            <section class="home-main">
                <nav class="home-nav">
                    <section :class="[{ 'control-active': isInputFocus }, 'g-input-search']">
                        <input class="g-input-border" type="text" placeholder="请输入名称" v-model="inputValue" @focus="isInputFocus = true" @blur="isInputFocus = false" @keyup.enter="filterData.searchStr = inputValue" />
                        <i class="bk-icon icon-search" v-if="!inputValue"></i>
                        <i class="bk-icon icon-close-circle-shape clear-icon" v-else @click="(inputValue = '', filterData.searchStr = '')"></i>
                    </section>

                    <section class="nav-pipetype">
                        <p class="pipe-type" :class="{ 'active-tab': filterData.pipeType === 'atom' }" @click="changePipeType('atom')">
                            <i class="bk-icon icon-atom"></i>
                            <span>流水线插件</span>
                        </p>
                        <p class="pipe-type" :class="{ 'active-tab': filterData.pipeType === 'template' }" @click="changePipeType('template')">
                            <i class="bk-icon icon-template"></i>
                            <span>流水线模板</span>
                        </p>
                    </section>

                    <section class="nav-fliter">
                        <h3>分类</h3>
                        <bk-select :value="`${filterData.classifyValue}${filterData.classifyKey || ''}`"
                            class="filter-select"
                            :scroll-height="500"
                            :clearable="false"
                        >
                            <bk-option-group
                                v-for="(group, index) in categories"
                                :name="group.name"
                                :key="index">
                                <bk-option v-for="(option, key) in group.children"
                                    :key="key"
                                    :id="option.id"
                                    :name="option.name"
                                    @click.native="selectClassifyCode(option)"
                                >
                                </bk-option>
                            </bk-option-group>
                        </bk-select>

                        <h3>特性<span @click="clearFliterData('features')" v-show="showFeatureClear">清除</span></h3>
                        <ul class="market-check-group">
                            <li v-for="(feature, index) in features" :key="index" class="market-checkbox-li" @click="chooseFeature(feature)">
                                <span :class="[feature.checked ? 'checked' : '', 'market-checkbox']"></span>
                                <span>{{ feature.name }}</span>
                            </li>
                        </ul>

                        <h3>评分<span @click="clearFliterData('rates')" v-show="showRateClear">清除</span></h3>
                        <ul class="rate-ul">
                            <li v-for="(rate, index) in rates" :key="rate.value" class="rate-li" @click="chooseRate(rate)">
                                <span :class="[{ checked: rate.checked }, 'rate-radio']"></span>
                                <comment-rate :rate="rate.value" class="rate-star"></comment-rate>
                                <span class="rate-above" v-if="index !== 0">及以上</span>
                            </li>
                        </ul>
                    </section>
                </nav>
                <transition name="atom-fade">
                    <router-view class="home-list"></router-view>
                </transition>
            </section>
        </main>

        <transition name="atom-fade">
            <icon v-if="showToTop" class="list-top" name="toTop" style="fill:#C3CDD7" @click.native="scrollToTop" />
        </transition>
    </article>
</template>

<script>
    import { mapActions } from 'vuex'
    import commentRate from '@/components/common/comment-rate'

    export default {
        components: {
            commentRate
        },
        
        filters: {
            pipeTypeFilter (val) {
                let res = ''
                switch (val) {
                    case 'template':
                        res = '流水线模板'
                        break
                    default:
                        res = '流水线插件'
                        break
                }
                return res
            }
        },

        data () {
            return {
                filterData: {
                    searchStr: undefined,
                    classifyKey: undefined,
                    classifyValue: 'all',
                    rdType: undefined,
                    score: undefined,
                    sortType: undefined,
                    pipeType: undefined
                },
                inputValue: '',
                isInputFocus: false,
                categories: [{ name: '所有', children: [{ name: '所有', id: 'all', classifyValue: 'all' }] }],
                features: [
                    { name: '蓝鲸官方', value: 'SELF_DEVELOPED', checked: false }
                ],
                rates: [
                    { value: 5, checked: false },
                    { value: 4, checked: false },
                    { value: 3, checked: false },
                    { value: 2, checked: false },
                    { value: 1, checked: false }
                ],
                showToTop: false
            }
        },

        computed: {
            showFeatureClear () {
                const features = this.features || []
                const index = features.findIndex((feature) => (feature.checked === true))
                return index > -1
            },

            showRateClear () {
                const rates = this.rates || []
                const index = rates.findIndex((rate) => (rate.checked === true))
                return index > -1
            }
        },

        watch: {
            '$route.path': {
                handler () {
                    this.initData()
                },
                immediate: true
            },

            filterData: {
                handler () {
                    this.changeRoute()
                },
                deep: true,
                immediate: true
            },

            'filterData.pipeType': {
                handler () {
                    this.getClassifys()
                },
                immediate: true
            }
        },

        methods: {
            ...mapActions('store', ['requestAtomClassifys', 'requestAtomLables', 'requestTemplateList', 'requestTplCategorys', 'requestTplLabel', 'setMarketQuery']),

            initData () {
                const { searchStr, classifyKey, classifyValue = 'all', rdType, score, sortType, pipeType = 'atom' } = this.$route.query
                Object.assign(this.filterData, { searchStr, classifyKey, classifyValue, rdType, score, sortType, pipeType })

                this.features.forEach((item) => {
                    if (item.value === this.filterData.rdType) item.checked = true
                    else item.checked = false
                })

                this.rates.forEach((item) => {
                    if (+item.value === +this.filterData.score) item.checked = true
                    else item.checked = false
                })
            },

            clearFliterData (...keys) {
                keys.forEach((key) => {
                    switch (key) {
                        case 'features':
                            (this[key] || []).forEach((data) => (data.checked = false))
                            this.filterData.rdType = undefined
                            break
                        case 'rates':
                            (this[key] || []).forEach((data) => (data.checked = false))
                            this.filterData.score = undefined
                            break
                        case 'classifyValue':
                            this.filterData[key] = 'all'
                            break
                        case 'searchStr':
                            this.filterData[key] = undefined
                            this.inputValue = undefined
                            break
                        default:
                            this.filterData[key] = undefined
                            break
                    }
                })
            },

            changePipeType (type) {
                this.filterData.pipeType = type
                this.clearFliterData('features', 'rates', 'classifyValue', 'classifyKey', 'searchStr', 'sortType')
            },

            chooseFeature (feature) {
                feature.checked = !feature.checked
                this.filterData.rdType = feature.checked ? feature.value : undefined
            },

            chooseRate (rate) {
                const rates = this.rates || []
                rates.forEach((item) => {
                    if (item !== rate) {
                        item.checked = false
                    } else {
                        item.checked = true
                        this.filterData.score = item.value
                    }
                })
            },

            selectClassifyCode (item) {
                this.filterData.classifyKey = item.classifyKey
                this.filterData.classifyValue = item.classifyValue
            },

            setClassifyValue (key) {
                const categories = this.categories[1] || {}
                const selected = (categories.children || []).find((category) => category.classifyCode === key) || {}
                this.filterData.classifyValue = selected.classifyValue
                this.filterData.classifyKey = 'classifyCode'
            },

            changeRoute () {
                const { searchStr, classifyValue, rdType, score, sortType, pipeType } = this.filterData
                const hasFilter = searchStr || classifyValue !== 'all' || rdType || score || sortType

                const query = JSON.parse(JSON.stringify(this.filterData), (key, value) => {
                    const validate = ((key === '' || key !== 'classifyValue') && key !== 'classifyKey') || classifyValue !== 'all'
                    if (validate) return value
                })
                this.setMarketQuery(query)

                if (hasFilter) this.$router.push({ name: 'list', query })
                else this.$router.push({ name: 'atomHome', query: { pipeType } })
            },

            getClassifys () {
                const fun = { atom: () => this.getAtomClassifys(), template: () => this.getTemplateClassifys() }
                const type = this.$route.query.pipeType || 'atom'
                const method = fun[type]
                method().then((arr) => {
                    const query = this.$route.query || {}
                    this.filterData.classifyValue = query.classifyValue || 'all'
                    this.filterData.classifyKey = query.classifyKey
                    this.categories = [{ name: '所有', children: [{ name: '所有', id: 'all', classifyValue: 'all' }] }]

                    arr.forEach((item) => {
                        const key = item.key
                        const name = item.name
                        const children = item.data
                        children.forEach((x) => {
                            x.name = x[name]
                            x.id = x[key] + key
                            x.classifyValue = x[key]
                            x.classifyKey = key
                        })
                        this.categories.push({ name: item.groupName, children })
                    })
                }).catch((err) => {
                    this.$bkMessage({ message: (err.message || err), theme: 'error' })
                })
            },

            getAtomClassifys () {
                return Promise.all([this.requestAtomClassifys(), this.requestAtomLables()]).then(([classifys, lables]) => {
                    const res = []
                    if (classifys.length > 0) res.push({ name: 'classifyName', key: 'classifyCode', groupName: '按分类', data: classifys })
                    if (lables.length > 0) res.push({ name: 'labelName', key: 'labelCode', groupName: '按功能', data: lables })
                    return res
                })
            },

            getTemplateClassifys () {
                return Promise.all([this.requestTplCategorys(), this.requestTplLabel()]).then(([categorys, lables]) => {
                    const res = []

                    if (categorys.length > 0) res.push({ name: 'categoryName', key: 'categoryCode', groupName: '按应用范畴', data: categorys })
                    if (lables.length > 0) res.push({ name: 'labelName', key: 'labelCode', groupName: '按功能', data: lables })
                    return res
                })
            },

            mainScroll (event) {
                const target = event.target
                this.showToTop = target.scrollTop > 0
            },

            scrollToTop () {
                const mainBody = document.querySelector('.store-main')
                const dis = mainBody.scrollTop
                const ticDis = dis / 15

                const animateScroll = () => {
                    if (mainBody.scrollTop <= 0) {
                        this.showToTop = false
                        return
                    }

                    mainBody.scrollTop -= ticDis
                    requestAnimationFrame(animateScroll)
                }

                requestAnimationFrame(animateScroll)
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';
    .store-home {
        overflow: hidden;
    }

    .store-main {
        margin-left: calc(100vw - 100%);
        overflow-y: scroll;
    }

    .home-main {
        height: calc(100vh - 114px);
        width: 1200px;
        margin: 21px auto 0;
        display: flex;
        flex-direction: row;
        .home-nav {
            width: 240px;
            height: 886px;
            background: $white;
            border: 1px solid $borderWeightColor;
            .nav-pipetype {
                margin: 20px 0 15px;
                .pipe-type {
                    cursor: pointer;
                    height: 40px;
                    line-height: 18px;
                    display: flex;
                    align-items: center;
                    i {
                        margin: 0 15px;
                        font-size: 18px;
                    }
                }
                .active-tab {
                    background: $lightBlue;
                    color: $primaryColor;
                    i {
                        color: $primaryColor;
                    }
                    span {
                        flex: 1;
                    }
                    &:after {
                        content: '';
                        height: 40px;
                        width: 4px;
                        background: $primaryColor
                    }
                }
            }

            .nav-fliter {
                padding-top: 17px;
                margin: 0 15px;
                border-top: 1px solid $borderWeightColor;
                h3 {
                    font-weight: normal;
                    height: 19px;
                    font-size: 14px;
                    line-height: 19px;
                    color: $fontBlack;
                    span {
                        float: right;
                        height: 16px;
                        font-size: 12px;
                        color: $primaryColor;
                        line-height: 19px;
                        cursor: pointer;
                    }
                }
                .filter-select {
                    margin: 8px 0 23px;
                    height: 32px;
                    line-height: 32px;
                }
            }
        }
        .home-list {
            width: 940px;
            margin-left: 20px;
        }
    }
    .rate-ul {
        margin-top: 8px;
        .rate-li {
            box-sizing: content-box;
            padding: 4px 0;
            height: 18px;
            display: flex;
            align-items: center;
            cursor: pointer;
            .rate-radio {
                position: relative;
                width: 18px;
                height: 18px;
                display: block;
                border-radius: 100%;
                border: 1px solid $fontLigtherColor;
                margin-right: 10px;
                &.checked:after {
                    position: absolute;
                    right: 4px;
                    top: 4px;
                    content: '';
                    width: 8px;
                    height: 8px;
                    display: block;
                    border-radius: 100%;
                    background: $primaryColor;
                }
            }
            .rate-star {
                margin-right: 6px;
            }
            .rate-above {
                height: 16px;
                line-height: 16px;
                font-size: 12px;
                color: $darkWhite;
            }
        }
    }

    .list-top {
        position: fixed;
        right: calc(50vw - 660px);
        bottom: 100px;
        height: 50px;
        width: 50px;
        cursor: pointer;
        &:hover {
            fill: $fontWeightColor !important;
        }
    }
</style>
