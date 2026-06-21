@echo off
cd /d F:\sentinel-dashboard
java -Dserver.port=8888 -Dcsp.sentinel.dashboard.server=localhost:8888 -jar sentinel-dashboard-1.8.10.jar
