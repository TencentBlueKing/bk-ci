const { src, dest, parallel, series, task } = require('gulp')
const fetch = require('node-fetch')
const chalk = require('chalk')
const svgSprite = require('gulp-svg-sprite')
const rename = require('gulp-rename')
const Ora = require('ora')
const yargs = require('yargs')
const fs = require('fs')
const path = require('path')
const argv = yargs.alias({
    dist: 'd',
    env: 'e',
    lsVersion: 'l',
    type: 't',
    scope: 's'
}).default({
    dist: 'frontend',
    env: 'master',
    lsVersion: 'v2',
    type: 'tencent'
}).describe({
    dist: 'build output dist directory',
    env: 'environment [dev, test, master, external]',
    lsVersion: 'localStorage version',
    type: 'bkdevops version 【ee | tencent】'
}).argv
const { dist, env, lsVersion, type, scope } = argv

const svgSpriteConfig = {
    mode: {
        symbol: true
    }
}
const isGray = env === 'gray'
const envPrefix = isGray || env === 'master' ? '' : `${env}.`
const BUNDLE_NAME = 'assets_bundle.json'
const ASSETS_JSON_URL = `http://${envPrefix}devnet.devops.oa.com/${BUNDLE_NAME}`

async function getAssetsJSON (jsonUrl) {
    try {
        const res = await fetch(jsonUrl, {
            headers: isGray ? { 'X-DEVOPS-PROJECT-ID': 'grayproject' } : {}
        })
        const assets = await res.json()

        console.log(chalk.blue.bold(`Successfully get assets json from ${jsonUrl}!`))
        console.table(assets)
        return assets
    } catch (error) {
        console.log(chalk.yellow.bgRed.bold(`Failed get assets json from ${jsonUrl}!`))
        process.exit(1)
    }
}

function taskGenerator (type) {
    return () => {
        return src(`./svg-sprites/${type}/*.svg`)
            .pipe(svgSprite(svgSpriteConfig))
            .pipe(dest(`./svg-sprites/dist/${type}`))
    }
}

function renameSvg (type) {
    return () => {
        return src(`./svg-sprites/dist/${type}/symbol/svg/sprite.symbol.svg`)
            .pipe(rename(`${type}_sprite.svg`))
            .pipe(dest(`${dist}/svg-sprites/`))
    }
}

function getScopeStr (scope) {
    try {
        if (!scope) return ''
        let scopeArray
        switch (true) {
            case typeof scope === 'string':
                scopeArray = scope.split(',')
                break
            default:
                scopeArray = scope
        }
        const isMultiple = scopeArray.length > 1
        return `--scope=devops-${isMultiple ? `{${scopeArray.join(',')}}` : scopeArray.join(',')}`
    } catch (e) {
        console.error(e)
        return ''
    }
}

task('devops', series([taskGenerator('devops'), renameSvg('devops')]))
task('pipeline', series([taskGenerator('pipeline'), renameSvg('pipeline')]))
task('copy', () => src(['common-lib/**'], { base: '.' }).pipe(dest(`${dist}/`)))

task('build', async cb => {
    const assetJson = await getAssetsJSON(ASSETS_JSON_URL)
    fs.writeFileSync(path.join(__dirname, dist, BUNDLE_NAME), JSON.stringify(assetJson))
    const spinner = new Ora('building bk-ci frontend project').start()
    const scopeStr = getScopeStr(scope)
    const envConfMap = {
        dist,
        version: type,
        lsVersion
    }
    Object.keys(envConfMap).forEach((key) => {
        process.env[key] = envConfMap[key]
    })

    require('child_process').exec(`lerna run public:master ${scopeStr}`, {
        maxBuffer: 5000 * 1024
    }, (err, res) => {
        if (err) {
            console.log(err)
            process.exit(1)
        }
        spinner.succeed('Finished building bk-ci frontend project')
        cb()
    })
})
  
exports.default = parallel('devops', 'pipeline', 'copy', 'build')
