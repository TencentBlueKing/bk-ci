<template>
    <article class="store-home">
        <bread-crumbs :bread-crumbs="navList" :type="filterData.pipeType">
            <router-link :to="{ name: `${filterData.pipeType || 'atom'}Work` }" class="g-title-work"> {{ $t('store.工作台') }} </router-link>
        </bread-crumbs>

        <main class="store-main" @scroll.passive="mainScroll">
            <section class="home-main">
                <nav class="home-nav">
                    <bk-input class="nav-input"
                        :placeholder="$t('store.请输入关键字')"
                        :clearable="true"
                        right-icon="bk-icon icon-search"
                        :value="filterData.searchStr"
                        @change="changeSearchStr"
                    ></bk-input>
                    <section class="nav-pipetype">
                        <p v-for="storeType in storeTypes"
                            :key="storeType.type"
                            class="pipe-type"
                            :class="{ 'active-tab': filterData.pipeType === storeType.type }"
                            @click="changePipeType(storeType.type)"
                        >
                            <icon class="title-icon" :name="`store-${storeType.type}`" size="18" />
                            <span>{{storeType.des}}</span>
                        </p>
                    </section>

                    <section class="nav-fliter">
                        <h3> {{ $t('store.分类') }} </h3>
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

                        <h3> {{ $t('store.特性') }} <span @click="clearFliterData('features')" v-show="filterData.features.length"> {{ $t('store.清除') }} </span></h3>
                        <ul class="market-check-group">
                            <li v-for="(feature, index) in features.filter(x => !x.hidden)" :key="index" class="market-checkbox-li" @click="chooseFeature(feature)">
                                <span :class="[filterData.features.some((x) => (x.key === feature.key && String(x.value) === String(feature.value))) ? 'checked' : '', 'market-checkbox']"></span>
                                <span>{{ feature.name }}</span>
                            </li>
                        </ul>

                        <h3> {{ $t('store.评分') }} <span @click="clearFliterData('rates')" v-show="showRateClear"> {{ $t('store.清除') }} </span></h3>
                        <ul class="rate-ul">
                            <li v-for="(rate, index) in rates" :key="rate.value" class="rate-li" @click="chooseRate(rate)">
                                <span :class="[{ checked: rate.checked }, 'rate-radio']"></span>
                                <comment-rate :rate="rate.value" class="rate-star"></comment-rate>
                                <span class="rate-above" v-if="index !== 0"> {{ $t('store.及以上') }} </span>
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
    import { debounce } from '@/utils/index'
    import eventBus from '@/utils/eventBus'
    import { mapActions } from 'vuex'
    import commentRate from '@/components/common/comment-rate'
    import breadCrumbs from '@/components/bread-crumbs.vue'

    export default {
        components: {
            commentRate,
            breadCrumbs
        },

        data () {
            return {
                filterData: {
                    searchStr: undefined,
                    classifyKey: undefined,
                    classifyValue: 'all',
                    features: [],
                    score: undefined,
                    sortType: undefined,
                    pipeType: undefined
                },
                isInputFocus: false,
                categories: [{ name: this.$t('store.所有'), children: [{ name: this.$t('store.所有'), id: 'all', classifyValue: 'all' }] }],
                rates: [
                    { value: 5, checked: false },
                    { value: 4, checked: false },
                    { value: 3, checked: false },
                    { value: 2, checked: false },
                    { value: 1, checked: false }
                ],
                showToTop: false,
                storeTypes: [
                    { type: 'atom', des: this.$t('store.流水线插件') },
                    { type: 'template', des: this.$t('store.流水线模板') },
                    { type: 'image', des: this.$t('store.容器镜像') }
                ]
            }
        },

        computed: {
            showRateClear () {
                const rates = this.rates || []
                const index = rates.findIndex((rate) => (rate.checked === true))
                return index > -1
            },

            features () {
                return [
                    { name: this.$t('store.蓝鲸官方'), key: 'rdType', value: 'SELF_DEVELOPED' },
                    { name: this.$t('store.质量红线指标'), key: 'qualityFlag', value: true, hidden: this.filterData.pipeType !== 'atom' },
                    { name: this.$t('store.推荐使用'), key: 'recommendFlag', value: true }
                ]
            },

            navList () {
                let name
                switch (this.filterData.pipeType) {
                    case 'template':
                        name = this.$t('store.流水线模板')
                        break
                    case 'image':
                        name = this.$t('store.容器镜像')
                        break
                    default:
                        name = this.$t('store.流水线插件')
                        break
                }
                return [
                    { name }
                ]
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
                handler (val) {
                    this.getClassifys(val)
                },
                immediate: true
            }
        },

        created () {
            eventBus.$off('clear')
            eventBus.$on('clear', () => {
                this.filterData.classifyValue = 'all'
                this.filterData.searchStr = ''
                this.clearFliterData('rates')
                this.clearFliterData('features')
            })
        },

        methods: {
            ...mapActions('store', [
                'requestAtomClassifys',
                'requestAtomLables',
                'requestTemplateList',
                'requestTplCategorys',
                'requestTplLabel',
                'requestTplClassify',
                'requestImageClassifys',
                'requestImageCategorys',
                'requestImageLabel',
                'setMarketQuery'
            ]),

            initData () {
                const { searchStr, classifyKey, classifyValue = 'all', features, score, sortType, pipeType = 'atom' } = this.$route.query
                Object.assign(this.filterData, { searchStr, classifyKey, classifyValue, score, sortType, pipeType })
                this.filterData.features = []
                if (features) {
                    const featuresArray = features.split(',')
                    featuresArray.forEach((feature) => {
                        feature = feature.split('-')
                        this.filterData.features.push({ key: feature[0], value: feature[1] })
                    })
                }

                this.rates.forEach((item) => {
                    if (+item.value === +this.filterData.score) item.checked = true
                    else item.checked = false
                })
            },

            clearFliterData (...keys) {
                keys.forEach((key) => {
                    switch (key) {
                        case 'features':
                            this.filterData.features = []
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

            changeSearchStr (val) {
                debounce(() => {
                    this.filterData.searchStr = val
                })
            },

            chooseFeature (feature) {
                const index = this.filterData.features.findIndex((item) => (feature.key === item.key && String(feature.value) === String(item.value)))
                if (index > -1) this.filterData.features.splice(index, 1)
                else this.filterData.features.push(feature)
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
                let categories = this.categories[2] || {}
                if (this.filterData.pipeType === 'atom') categories = this.categories[1] || {}
                const selected = (categories.children || []).find((category) => category.classifyCode === key) || {}
                this.filterData.classifyValue = selected.classifyValue
                this.filterData.classifyKey = 'classifyCode'
            },

            changeRoute () {
                const { searchStr, classifyValue, features, score, sortType, pipeType } = this.filterData
                const hasFilter = searchStr || classifyValue !== 'all' || features.length || score || sortType

                const query = JSON.parse(JSON.stringify(this.filterData), (key, value) => {
                    const validate = ((key === '' || key !== 'classifyValue') && key !== 'classifyKey') || classifyValue !== 'all'
                    if (key === 'features') value = value.map(x => `${x.key}-${x.value}`).join(';')
                    if (validate) return value
                })

                this.setMarketQuery(query)

                if (hasFilter) this.$router.push({ name: 'list', query })
                else this.$router.push({ name: 'atomHome', query: { pipeType } })
            },

            getClassifys (val) {
                const fun = {
                    atom: () => this.getAtomClassifys(),
                    template: () => this.getTemplateClassifys(),
                    image: () => this.getImageClassifys()
                }
                const type = val || 'atom'
                const method = fun[type]
                method().then((arr) => {
                    const query = this.$route.query || {}
                    this.filterData.classifyValue = query.classifyValue || 'all'
                    this.filterData.classifyKey = query.classifyKey
                    this.categories = [{ name: this.$t('store.所有'), children: [{ name: this.$t('store.所有'), id: 'all', classifyValue: 'all' }] }]

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
                    if (classifys.length > 0) res.push({ name: 'classifyName', key: 'classifyCode', groupName: this.$t('store.按分类'), data: classifys })
                    if (lables.length > 0) res.push({ name: 'labelName', key: 'labelCode', groupName: this.$t('store.按功能'), data: lables })
                    return res
                })
            },

            getTemplateClassifys () {
                return Promise.all([this.requestTplCategorys(), this.requestTplLabel(), this.requestTplClassify()]).then(([categorys, lables, classify]) => {
                    const res = []
                    if (categorys.length > 0) res.push({ name: 'categoryName', key: 'categoryCode', groupName: this.$t('store.按应用范畴'), data: categorys })
                    if (classify.length > 0) res.push({ name: 'classifyName', key: 'classifyCode', groupName: this.$t('store.按分类'), data: classify })
                    if (lables.length > 0) res.push({ name: 'labelName', key: 'labelCode', groupName: this.$t('store.按功能'), data: lables })
                    return res
                })
            },

            getImageClassifys () {
                return Promise.all([this.requestImageCategorys(), this.requestImageLabel(), this.requestImageClassifys()]).then(([categorys, lables, classify]) => {
                    const res = []
                    if (categorys.length > 0) res.push({ name: 'categoryName', key: 'categoryCode', groupName: this.$t('store.按应用范畴'), data: categorys })
                    if (classify.length > 0) res.push({ name: 'classifyName', key: 'classifyCode', groupName: this.$t('store.按分类'), data: classify })
                    if (lables.length > 0) res.push({ name: 'labelName', key: 'labelCode', groupName: this.$t('store.按功能'), data: lables })
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
        overflow-y: scroll;
    }

    .home-main {
        height: calc(100vh - 5.6vh - 70px);
        min-height: 710px;
        width: 1200px;
        margin: 20px auto 0;
        display: flex;
        flex-direction: row;
        .home-nav {
            width: 240px;
            background: $white;
            border: 1px solid $borderWeightColor;
            .nav-input {
                margin: 20px 15px 0;
                width: 210px;
            }
            .nav-pipetype {
                margin: 20px 0 15px;
                .pipe-type {
                    cursor: pointer;
                    height: 40px;
                    line-height: 18px;
                    display: flex;
                    align-items: center;
                    .title-icon {
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
