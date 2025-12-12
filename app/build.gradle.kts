plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.28")
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")


}

// 定义清除缓存的 Task
tasks.register("cleanCacheDir") {
    group = "cache"
    description = "Clears specific build cache directories."
    
    doLast {
        // 指定要清除的缓存目录
        val cacheDirs = listOf(
            file("${project.buildDir}/intermediates/transforms"),
            file("${project.buildDir}/tmp"),
            // 添加更多需要清除的目录
        )
        
        cacheDirs.forEach { dir ->
            if (dir.exists()) {
                println("Clearing cache directory: ${dir.absolutePath}")
                dir.deleteRecursively()
                println("Cache directory cleared: ${dir.absolutePath}")
            } else {
                println("Cache directory does not exist, skipping: ${dir.absolutePath}")
            }
        }
    }
}

// 让 preBuild 任务依赖于 cleanCacheDir，确保编译前执行清除
tasks.named("preBuild") {
    dependsOn("cleanCacheDir")
}
