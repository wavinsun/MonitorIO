@echo off&title Keystore Information
keytool -v -list -alias keystore -keystore keystore -storepass keystore -keypass keystore
echo. & pause