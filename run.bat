@echo off
cd /d %~dp0
java -cp "target/classes;target/HytaleMCProxy-1.0.0.jar;target/*" com.proxy.network.ProxyServer
pause