plugins {
    id("java")
    id("application")
}

group = "com.coze"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/coze-dev/coze-api")
    }
}

dependencies {
    // Javalin 和 Jetty 依赖
    implementation("io.javalin:javalin:4.6.8")
    implementation("org.eclipse.jetty:jetty-server:9.4.51.v20230217")
    implementation("org.eclipse.jetty:jetty-webapp:9.4.51.v20230217")
    implementation("org.eclipse.jetty:jetty-util:9.4.51.v20230217")
    implementation("org.eclipse.jetty:jetty-servlet:9.4.51.v20230217")
    implementation("org.eclipse.jetty:jetty-security:9.4.51.v20230217")
    implementation("org.eclipse.jetty:jetty-http:9.4.51.v20230217")
    implementation("org.eclipse.jetty:jetty-io:9.4.51.v20230217")
    implementation("org.slf4j:slf4j-simple:2.0.7")

    // Lombok 支持 - 添加版本号
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // YAML 支持
    implementation("org.yaml:snakeyaml")

    // JSON 处理
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")

    // coze api
    implementation("com.coze:coze-api:0.2.0")

    // 测试依赖
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("com.coze.pkce.Main")
}

tasks.register<Jar>("uberJar") {
    archiveClassifier.set("uber")
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
