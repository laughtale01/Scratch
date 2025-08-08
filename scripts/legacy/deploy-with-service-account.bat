@echo off
echo Setting up Firebase deployment with service account...

REM Set the service account credentials
set GOOGLE_APPLICATION_CREDENTIALS=service-account-key.json

echo Deploying to laughtale-scratch-ca803...
firebase deploy --only hosting --project laughtale-scratch-ca803

echo Deployment complete!
pause