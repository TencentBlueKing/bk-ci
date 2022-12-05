plugins {
    id("com.tencent.devops.boot")
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-data-redis") {
        exclude(group = "io.lettuce", module = "lettuce-core")
    }
    api("redis.clients:jedis")
    api("org.apache.commons:commons-pool2")
}
