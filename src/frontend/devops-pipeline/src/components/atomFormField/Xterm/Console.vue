<template>
    <div class="console" id="terminal" style="height: 100%; width: 100%; margin: 0; padding: 0;"></div>
</template>

<script>
    import Terminal from './Xterm'
    export default {
        name: 'Console',
        props: {
            url: {
                type: String,
                default: ''
            },
            resizeUrl: {
                type: String,
                default: ''
            },
            execId: {
                type: String,
                default: ''
            }
        },
        data () {
            return {
                term: null,
                terminalSocket: null,
                wsUrl: '',
                heartTimer: null
            }
        },
        mounted () {
            window.onresize = this.triggerTermResize
            this.term = new Terminal()
            const terminalContainer = document.getElementById('terminal')
            this.term.open(terminalContainer)

            if (this.url) {
                this.wsUrl = this.url
                this.terminalSocket = new WebSocket(this.wsUrl)
                this.terminalSocket.onopen = this.runTerminal
                this.terminalSocket.onmessage = this.receiveFromTerminal
                this.terminalSocket.onclose = this.closeTerminal
                this.terminalSocket.onerror = this.errorTerminal
                this.term.attach(this.terminalSocket)
                this.term.on('resize', this.handleResize)
                setTimeout(() => {
                    this.term.fit()
                    this.term._initialized = true
                }, 1000)

                this.term.write('#######################################################################\r\n#                    Welcome To BKDevOps Console                      #\r\n#######################################################################\r\n')
            } else {
                this.term.write('url is null')
            }
        },
        beforeDestroy () {
            this.terminalSocket.close()
            this.term.destroy()
        },
        methods: {
            runTerminal (e) {
                // this.heartCheck()
                this.terminalSocket.send('source /etc/profile\n')
            },
            receiveFromTerminal (e) {
                // this.heartCheck()
                // console.log(e.data, 'receive msg')
            },
            errorTerminal (e) {
                console.log(e, 'error')
            // this.term.write('发生异常 ')
            },
            closeTerminal (e) {
                console.log(e, 'close')
                // this.term.write('连接中断')
            },
            triggerTermResize (e) {
                this.term.fit()
                // this.term.on('resize', this.handleResize)
            },
            handleResize (size) {
                this.$nextTick(() => {
                    this.$store.dispatch('soda/resizeTerm', {
                        resizeUrl: this.resizeUrl,
                        params: {
                            exec_id: this.execId,
                            height: size.rows,
                            width: size.cols
                        }
                    })
                })
            }
            // heartCheck () {
            //     clearTimeout(this.heartTimer)
            //     this.heartTimer = setTimeout(() => {
            //         this.terminalSocket.send('\n')
            //         console.log('sendping')
            //     }, 10000)
            // }
        }
    }
</script>

<style lang="scss">
    .terminal {
        height: 100%;
    }
</style>
