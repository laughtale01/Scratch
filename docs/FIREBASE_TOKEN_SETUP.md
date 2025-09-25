# 🔐 Firebase Token Setup Guide

This guide explains how to set up the FIREBASE_TOKEN secret for automated deployments.

## 📋 Prerequisites

- Firebase CLI installed locally
- Access to the Firebase project (`laughtale-scratch-ca803`)
- GitHub repository admin access

## 🔧 Setup Steps

### 1. Generate Firebase CI Token

Run the following command in your terminal:

```bash
firebase login:ci
```

This will:
1. Open your browser for authentication
2. Ask you to log in with your Google account
3. Generate a CI token after successful authentication
4. Display the token in your terminal

⚠️ **Important**: Copy this token immediately and store it securely. You won't be able to retrieve it again.

### 2. Add Token to GitHub Secrets

1. Go to your GitHub repository
2. Navigate to **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add the following:
   - **Name**: `FIREBASE_TOKEN`
   - **Value**: Paste the token from step 1
5. Click **Add secret**

### 3. Verify the Setup

The token is automatically used by the GitHub Actions workflow:
- `.github/workflows/firebase-deploy.yml`

To test the deployment:

```bash
# Push to main branch to trigger deployment
git push origin main

# Or manually trigger the workflow
# Go to Actions → Deploy to Firebase Hosting → Run workflow
```

## 🔒 Security Best Practices

1. **Never commit the token** to your repository
2. **Rotate tokens regularly** (every 90 days recommended)
3. **Use repository secrets** instead of organization secrets for better isolation
4. **Limit access** to the Firebase project to authorized users only

## 🔄 Token Rotation

To rotate your token:

1. Revoke the old token:
   ```bash
   firebase logout --token OLD_TOKEN
   ```

2. Generate a new token:
   ```bash
   firebase login:ci
   ```

3. Update the GitHub secret with the new token

## 🚨 Troubleshooting

### Error: "Failed to authenticate"
- Token may have expired
- Generate a new token following the steps above

### Error: "Project not found"
- Verify the project ID in the workflow file
- Ensure your account has access to the project

### Error: "Insufficient permissions"
- Check that your Firebase account has deployment permissions
- Contact the project owner if needed

## 📝 Local Testing

To test Firebase deployment locally:

```bash
# Set the token as an environment variable
export FIREBASE_TOKEN="your-token-here"

# Run deployment
firebase deploy --only hosting --project laughtale-scratch-ca803
```

## 🔗 Related Documentation

- [Firebase CI Documentation](https://firebase.google.com/docs/cli#cli-ci-systems)
- [GitHub Encrypted Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Project Firebase Console](https://console.firebase.google.com/project/laughtale-scratch-ca803)

## 📞 Support

If you encounter issues:
1. Check the GitHub Actions logs
2. Verify token validity
3. Review Firebase project permissions
4. Contact the repository maintainer

---

*Last updated: 2025-09-26*