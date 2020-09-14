@echo off
where mvn >nul 2>nul || goto maven_not_found
if "%~1"=="" (
    mvn versions:set
) else (
    mvn versions:set versions:update-child-modules -DnewVersion=%1
)

:maven_not_found
echo Could not find the mvn.cmd script in PATH
exit /b 1