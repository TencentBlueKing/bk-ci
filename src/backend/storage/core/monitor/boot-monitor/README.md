### Running Spring Boot Admin Server for development
To develop the ui on an running server the best to do is

1. Running the ui build in watch mode so the resources get updated:
```shell
npm run watch
```
2. Run a Spring Boot Admin Server instances with the template-location and resource-location pointing to the build output and disable caching:
```
spring.boot.admin.ui.cache.no-cache: true
spring.boot.admin.ui.extension-resource-locations: file:@project.basedir@/../spring-boot-admin-sample-custom-ui/target/dist/
spring.boot.admin.ui.cache-templates: false
```

3. disable npm build gradle task
```
// compileKotlin.dependsOn(npmBuild)
```



