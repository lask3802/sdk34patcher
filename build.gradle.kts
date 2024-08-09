plugins {
    id("java")
}

group = "com.noxygames"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.ow2.asm:asm:9.7")
    implementation("org.ow2.asm:asm-tree:9.7")
    // https://mvnrepository.com/artifact/org.smali/dexlib2
    implementation("org.smali:dexlib2:2.5.2")
    // https://mvnrepository.com/artifact/com.google.common/google-collect
    implementation("com.google.guava:guava:33.2.1-jre")
    // https://mvnrepository.com/artifact/com.github.lanchon.dexpatcher/multidexlib2
    implementation("com.github.lanchon.dexpatcher:multidexlib2:2.3.4.r2")



}

tasks.test {
    useJUnitPlatform()
}