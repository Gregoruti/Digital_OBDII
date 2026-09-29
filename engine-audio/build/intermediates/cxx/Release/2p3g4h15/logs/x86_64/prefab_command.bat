@echo off
"C:\\Program Files\\Eclipse Adoptium\\jdk-17.0.16.8-hotspot\\bin\\java" ^
  --class-path ^
  "C:\\Users\\grego\\.gradle\\caches\\modules-2\\files-2.1\\com.google.prefab\\cli\\2.1.0\\aa32fec809c44fa531f01dcfb739b5b3304d3050\\cli-2.1.0-all.jar" ^
  com.google.prefab.cli.AppKt ^
  --build-system ^
  cmake ^
  --platform ^
  android ^
  --abi ^
  x86_64 ^
  --os-version ^
  26 ^
  --stl ^
  c++_shared ^
  --ndk-version ^
  27 ^
  --output ^
  "C:\\Users\\grego\\AppData\\Local\\Temp\\agp-prefab-staging4543182402807841520\\staged-cli-output" ^
  "C:\\Users\\grego\\.gradle\\caches\\8.11.1\\transforms\\261e7d2cb4997819ffa98b68b8aa8c07\\transformed\\oboe-1.10.0\\prefab"
