# Preserve Heimdall's JNI symbols, Shizuku-reflected service entrypoints, and
# diagnostic method names. Optimize/shrink the dependency graph without changing
# application names or persistence contracts in this first size pass.
-keep class com.mastercook777.heimdall.** { *; }
