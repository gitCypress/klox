import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// 插件：管理项目类型和 Kotlin
plugins {
    kotlin("jvm") version "2.2.20"
    application
}

// 项目基本信息
group = "exp.compiler.klox"
version = "1.0-SNAPSHOT"

// 仓库
repositories {
    mavenCentral()
}

// Java 工具链配置
kotlin {
    jvmToolchain(23)
}

// 依赖
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.20")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.2.20")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

// Kotlin 编译器配置
tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_23)

        // 启用实验性功能
        optIn.add("kotlin.contracts.ExperimentalContracts")

        // 启用 context parameters 功能
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

// 运行配置
application {
    mainClass.set("exp.compiler.klox.LoxKt")
}

// 配置所有 JavaExec 任务
tasks.withType<JavaExec> {
    systemProperty("file.encoding", "UTF-8")
    systemProperty("sun.stdout.encoding", "UTF-8")
    systemProperty("sun.stderr.encoding", "UTF-8")
}

// 测试配置
tasks.test {
    useJUnitPlatform()
}