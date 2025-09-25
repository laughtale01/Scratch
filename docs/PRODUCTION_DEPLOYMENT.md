# 🚀 Production Deployment Guide

Complete guide for deploying the Minecraft Collaboration System to production.

## 📋 Deployment Overview

The system consists of three main components:
1. **Minecraft Mod** - Deployed to Minecraft clients
2. **Scratch GUI** - Deployed to Firebase Hosting
3. **Documentation** - Deployed to GitHub Pages

## 🎯 Prerequisites

### Required Software
- Java 17 (not Java 21)
- Node.js 18+
- Git
- Minecraft with Forge 1.20.1
- Firebase CLI (for web deployment)

### Access Requirements
- GitHub repository write access
- Firebase project access
- Minecraft Forge 1.20.1 installed

## 📦 Component 1: Minecraft Mod Deployment

### Automated Deployment (Recommended)

```bash
# Run the deployment script
./deploy-to-minecraft.bat
```

### Manual Deployment

1. **Build the mod**:
   ```bash
   cd minecraft-mod
   export JAVA_HOME="C:/Program Files/Java/jdk-17"
   ./gradlew clean jarJar -x test
   ```

2. **Locate the JAR**:
   ```
   minecraft-mod/build/libs/minecraft-collaboration-mod-1.0.0-all.jar
   ```

3. **Install to Minecraft**:
   - Copy JAR to `%APPDATA%\.minecraft\mods\`
   - Or use the Minecraft launcher to manage mods

### Verification

1. Start Minecraft with Forge 1.20.1
2. Check the Mods menu for "Minecraft Collaboration Mod"
3. Load a world and check logs for "WebSocket server started on port 14711"

## 🌐 Component 2: Scratch GUI Deployment

### Automatic Deployment (CI/CD)

Deployment happens automatically when pushing to main:

```bash
git push origin main
# GitHub Actions will deploy to Firebase
```

### Manual Deployment

1. **Build the application**:
   ```bash
   cd scratch-gui
   npm install
   npm run build
   ```

2. **Deploy to Firebase**:
   ```bash
   firebase deploy --only hosting --project laughtale-scratch-ca803
   ```

3. **Access the deployed site**:
   ```
   https://laughtale-scratch-ca803.web.app
   ```

### Local Testing

```bash
cd scratch-gui
npm start
# Access at http://localhost:8601
```

## 📚 Component 3: Documentation Deployment

Documentation is automatically deployed to GitHub Pages on push to main.

### Manual Update

```bash
# Documentation is in the docs/ directory
# Edit markdown files as needed

git add docs/*
git commit -m "Update documentation"
git push origin main
```

## ✅ Production Checklist

### Pre-Deployment

- [ ] All tests passing (85%+ success rate)
- [ ] Java 17 environment configured
- [ ] Build successful without errors
- [ ] Checkstyle warnings reviewed
- [ ] Version number updated
- [ ] CHANGELOG.md updated
- [ ] Documentation up to date

### Deployment Steps

1. **Prepare Release**:
   - [ ] Create release branch
   - [ ] Update version in build.gradle
   - [ ] Update README.md version

2. **Build & Test**:
   - [ ] Run full test suite
   - [ ] Build mod with jarJar
   - [ ] Build Scratch GUI
   - [ ] Test locally

3. **Deploy Components**:
   - [ ] Deploy mod to test environment
   - [ ] Deploy Scratch GUI to staging
   - [ ] Verify WebSocket connection
   - [ ] Test all features

4. **Production Release**:
   - [ ] Tag release in Git
   - [ ] Deploy to production Firebase
   - [ ] Release mod on CurseForge/Modrinth
   - [ ] Update documentation

5. **Post-Deployment**:
   - [ ] Monitor error logs
   - [ ] Check performance metrics
   - [ ] Gather user feedback
   - [ ] Document known issues

## 🔍 Monitoring

### Health Checks

1. **WebSocket Server**:
   ```bash
   curl http://localhost:14711/health
   ```

2. **Firebase Hosting**:
   - Check Firebase Console for hosting status
   - Monitor bandwidth and request metrics

3. **GitHub Actions**:
   - Review workflow runs
   - Check for deployment failures

### Logs

- **Minecraft**: `.minecraft/logs/latest.log`
- **WebSocket**: Check console output
- **Firebase**: Firebase Console → Functions → Logs

## 🔧 Troubleshooting

### Common Issues

#### Build Fails
```bash
# Clean and rebuild
cd minecraft-mod
./gradlew clean
./gradlew build --refresh-dependencies
```

#### WebSocket Connection Failed
1. Check firewall settings
2. Verify port 14711 is not in use
3. Restart Minecraft client
4. Check logs for error messages

#### Firebase Deployment Failed
1. Verify FIREBASE_TOKEN is set
2. Check project permissions
3. Review GitHub Actions logs
4. Try manual deployment

## 🔄 Rollback Procedures

### Mod Rollback
1. Restore backup: `*.jar.backup`
2. Or download previous version from releases

### Web Rollback
```bash
# List Firebase hosting releases
firebase hosting:releases:list

# Rollback to previous
firebase hosting:rollback
```

## 📈 Performance Optimization

### Recommended Settings

1. **JVM Arguments** (Minecraft):
   ```
   -Xmx4G -Xms2G -XX:+UseG1GC
   ```

2. **Node.js** (Scratch GUI):
   ```bash
   export NODE_OPTIONS="--max-old-space-size=4096"
   ```

## 🔐 Security Considerations

1. **Never commit secrets** to the repository
2. **Rotate tokens** every 90 days
3. **Use HTTPS** for all web traffic
4. **Validate** all WebSocket messages
5. **Rate limit** API requests
6. **Monitor** for suspicious activity

## 📞 Support Channels

- **GitHub Issues**: Report bugs and feature requests
- **Discord**: Community support (if available)
- **Email**: project-maintainer@example.com

## 📝 Release Notes Template

```markdown
## Version X.Y.Z (YYYY-MM-DD)

### Features
- New feature descriptions

### Improvements
- Performance enhancements
- UI/UX improvements

### Bug Fixes
- Fixed issues list

### Known Issues
- Current limitations

### Breaking Changes
- API changes requiring attention
```

## 🎉 Post-Deployment Celebration

After successful deployment:
1. Announce release on social media
2. Update project website
3. Notify community members
4. Celebrate team achievement! 🎊

---

*Last updated: 2025-09-26*
*Version: 1.5.1*