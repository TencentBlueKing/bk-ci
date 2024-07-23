const { src, dest, parallel, series, task } = require('gulp')
const fs = require('fs')
const path = require('path')
const htmlmin = require('gulp-htmlmin')
const svgSprite = require('gulp-svg-sprite')
const inject = require('gulp-inject')
const rename = require('gulp-rename')
const hash = require('gulp-hash')
const replace = require('gulp-replace')
const Ora = require('ora')
const yargs = require('yargs')
const del = require('del')

const argv = yargs.alias({
    dist: 'd',
    env: 'e',
    lsVersion: 'l',
    scope: 's'
}).default({
    dist: 'frontend',
    env: 'master',
    lsVersion: 'dev'
}).describe({
    dist: 'build output dist directory',
    env: 'environment [dev, test, master, external]',
    lsVersion: 'localStorage version'
}).argv
const { dist, env, lsVersion, scope } = argv
const svgSpriteConfig = {
    mode: {
        symbol: true
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

function generatorSvgJs (type) {
    return () => {
        const svgCode = fs.readFileSync(`${dist}/svg-sprites/${type}_sprite.svg`, 'utf8')
        return src('./svg-sprites/svgjs-template.js')
            .pipe(replace('__SVG_SPRITES_SYMBOLS__', svgCode))
            .pipe(rename(`${type}_sprite.js`))
            .pipe(hash())
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
        return `-p ${scopeArray.map(item => `devops-${item}`).join(' ')}`
    } catch (e) {
        console.error(e)
        return ''
    }
}

task('devops', series([taskGenerator('devops'), renameSvg('devops'), generatorSvgJs('devops')]))
task('pipeline', series([taskGenerator('pipeline'), renameSvg('pipeline'), generatorSvgJs('pipeline')]))
task('copy', () => src(['common-lib/**'], { base: '.' }).pipe(dest(`${dist}/`)))

task('build', series([cb => {
    const spinner = new Ora('building bk-ci frontend project').start()
    const scopeStr = getScopeStr(scope)
    const envConfMap = {
        dist,
        // version: type,
        lsVersion
    }
    
    const cmd = scopeStr ? `pnpx nx run-many -t public:master ${scopeStr}`: `pnpx nx affected -t public:master --base=${process.env.NX_BASE} --head=${process.env.NX_HEAD}`
    console.log('gulp cmd: ', cmd);
    const spawnCmd = require('node:child_process').spawn(cmd, {
        stdio: 'inherit',
        env: {
            ...process.env,
            dist,
            lsVersion
        }
    })
    spawnCmd.stdout.on('data', (data) => {
        console.log(`stdout: ${data}`);
      });
      
      spawnCmd.stderr.on('data', (data) => {
        console.error(`stderr: ${data}`);
            process.exit(1)
      });
      
      spawnCmd.on('close', (code) => {
        console.log(`child process exited with code ${code}`);
        spinner.succeed('Finished building bk-ci frontend project')
        cb()
      }); 
}], () => {
    try {
        const fileContent = `window.SERVICE_ASSETS = ${fs.readFileSync(`${dist}/assets_bundle.json`, 'utf8')}`
        fs.writeFileSync(`${dist}/assetsBundles.js`, fileContent)
        return src(`${dist}/assetsBundles.js`)
            .pipe(hash())
            .pipe(dest(`${dist}/`))
    } catch (error) {
        console.error(error)
    }
}, (cb) => {
    ['console', 'pipeline'].map(prefix => {
        const dir = path.join(dist, prefix)
        const spriteNameGlob = `${prefix === 'console' ? 'devops' : 'pipeline'}_sprite-*.js`
        const fileName = `frontend#${prefix}#index.html`
        return src(path.join(dir, fileName), {
            allowEmpty: true
        })
            .pipe(inject(src([
                ...(prefix === 'console' ? [`${dist}/assetsBundles-*.js`] : []),
                `${dist}/svg-sprites/${spriteNameGlob}`
            ], {
                read: false
            }), {
                ignorePath: dist,
                addRootSlash: false,
                addPrefix: '__BK_CI_PUBLIC_PATH__'
            }))
            .pipe(htmlmin({
                collapseWhitespace: true,
                removeComments: true,
                minifyJS: true
            }))
            .pipe(dest(dir))
    })
    cb()
}))

task('clean', () => {
    return del(dist)
})
  
exports.default = series('clean', parallel('devops', 'pipeline', 'copy', 'build'))
