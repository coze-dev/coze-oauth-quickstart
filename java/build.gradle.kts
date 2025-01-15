plugins {
    id("org.springframework.boot") version "3.2.3" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
}

allprojects {
    group = "org.example"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/coze-dev/coze-api")
        }
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    dependencies {
        // Javalin 和 Jetty 依赖
        "implementation"("io.javalin:javalin:4.6.8")
        "implementation"("org.eclipse.jetty:jetty-server:9.4.51.v20230217")
        "implementation"("org.eclipse.jetty:jetty-webapp:9.4.51.v20230217")
        "implementation"("org.eclipse.jetty:jetty-util:9.4.51.v20230217")
        "implementation"("org.eclipse.jetty:jetty-servlet:9.4.51.v20230217")
        "implementation"("org.eclipse.jetty:jetty-security:9.4.51.v20230217")
        "implementation"("org.eclipse.jetty:jetty-http:9.4.51.v20230217")
        "implementation"("org.eclipse.jetty:jetty-io:9.4.51.v20230217")
        "implementation"("org.slf4j:slf4j-simple:2.0.7")

        // Lombok 支持
        "compileOnly"("org.projectlombok:lombok")
        "annotationProcessor"("org.projectlombok:lombok")
        
        // YAML 支持
        "implementation"("org.yaml:snakeyaml")
        
        // JSON 处理
        "implementation"("com.fasterxml.jackson.core:jackson-databind")
        "implementation"("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")

        // coze api
        "implementation"("com.coze:coze-api:0.2.0")
        
        // 测试依赖
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"(platform("org.junit:junit-bom:5.10.0"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
    }
}