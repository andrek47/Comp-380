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

}

android {
    namespace = "com.example.keeppace"  // <-- use your app's package
    compileSdk = 34                     // 34 is widely supported; you can raise later

    defaultConfig {
        applicationId = "com.example.keeppace"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}



