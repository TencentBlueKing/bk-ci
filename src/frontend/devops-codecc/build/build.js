/**
 * @file build
 * @author blueking
 */

import {join} from 'path'
import ora from 'ora'
import chalk from 'chalk'
import webpack from 'webpack'
import rm from 'rimraf'

import config from './config'
import checkVer from './check-versions'
import webpackConf from './webpack.prod.conf.babel'

checkVer()

const spinner = ora('building...')
spinner.start()

rm(config.build.assetsRoot, e => {
    if (e) {
        throw e
    }
    webpack(webpackConf, (err, stats) => {
        spinner.stop()
        if (err) {
            throw err
        }
        process.stdout.write(stats.toString({
            colors: true,
            modules: false,
            children: false,
            chunks: false,
            chunkModules: false
        }) + '\n\n')

        if (stats.hasErrors()) {
            console.log(chalk.red('  Build failed with errors.\n'))
            process.exit(1)
        }

        console.log(chalk.cyan('  Build complete.\n'))
        console.log(chalk.yellow(
            '  Tip: built files are meant to be served over an HTTP server.\n'
            + '  Opening index.html over file:// won\'t work.\n'
        ))
    })
})
