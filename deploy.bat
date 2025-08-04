@echo off
echo ========================================
echo Firebase Hosting Deploy
echo Account: laughtale.education@gmail.com
echo Project: laughtale-scratch-ca803
echo ========================================
echo.

echo Checking current login status...
firebase login:list
echo.

echo If not logged in as laughtale.education@gmail.com, please run:
echo   firebase logout
echo   firebase login
echo.

echo Press any key to continue with deployment...
pause > nul

echo.
echo Setting project...
firebase use laughtale-scratch-ca803

echo.
echo Starting deployment...
firebase deploy --only hosting

echo.
echo ========================================
echo Deployment process completed!
echo ========================================
echo.
echo Check your site at:
echo - https://laughtale-scratch-ca803.web.app/
echo.
pause