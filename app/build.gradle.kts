plugins {

    id("com.android.application")

    // Add the Google services Gradle plugin

    id("com.google.gms.google-services")



}


dependencies {

    // Import the Firebase BoM

    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))


    // TODO: Add the dependencies for Firebase products you want to use

    // When using the BoM, don't specify versions in Firebase dependencies

    implementation("com.google.firebase:firebase-analytics")


    // Add the dependencies for any other desired Firebase products

    // https://firebase.google.com/docs/android/setup#available-libraries

    implementation("com.google.android.material:material:1.12.0")

    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))

    // Add the dependency for the Firebase Authentication library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-auth")



    implementation("com.firebaseui:firebase-ui-auth:8.0.2")

    implementation("androidx.appcompat:appcompat:1.7.0")

    // Also add the dependencies for the Credential Manager libraries and specify their versions
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

}

android {
    namespace = "com.comp380.keeppace"  // <-- use your app's package
    compileSdk = 34                     // 34 is widely supported; you can raise later

    defaultConfig {
        applicationId = "com.comp380.keeppace"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_16
        targetCompatibility = JavaVersion.VERSION_16
    }
}



