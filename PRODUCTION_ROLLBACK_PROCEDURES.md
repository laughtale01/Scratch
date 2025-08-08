# 🔄 Production Rollback Procedures

**Version**: 1.0.0  
**Last Updated**: 2025-08-08  
**Environment**: Production (https://laughtale-scratch-ca803.web.app)

## 🚨 When to Initiate Rollback

### Immediate Rollback Triggers (Execute within 5 minutes)
- **Critical Security Vulnerability**: Discovered during testing
- **Complete Service Failure**: Site inaccessible or non-functional
- **Data Corruption Risk**: User data at risk
- **Mass User Impact**: >50% of users affected

### Standard Rollback Triggers (Execute within 15 minutes)  
- **High Error Rate**: >10% command failure rate
- **Performance Degradation**: >5x slower than baseline
- **Feature Regression**: Core functionality broken
- **Browser Incompatibility**: Major browsers affected

### Planned Rollback Triggers (Execute within 30 minutes)
- **Failed Test Scenarios**: Multiple critical scenarios fail
- **Monitoring Alerts**: Multiple warning thresholds exceeded
- **User Complaints**: Significant negative feedback
- **Stakeholder Decision**: Business decision to rollback

## 🎯 Rollback Strategy

### Current Production State
- **URL**: https://laughtale-scratch-ca803.web.app
- **CDN**: Firebase Hosting
- **Current Version**: 1.5.0
- **Last Known Good Version**: 1.4.0 (commit: b94ec83)
- **Deployment Method**: Firebase CLI

### Rollback Targets
1. **Level 1 - Configuration Rollback**: Revert config changes only
2. **Level 2 - Asset Rollback**: Revert static files and extensions  
3. **Level 3 - Full Rollback**: Complete revert to previous version
4. **Level 4 - Emergency Maintenance**: Take site offline

## 📋 Rollback Procedures

### Level 1: Configuration Rollback ⏱️ 5 minutes

**When to use**: Minor configuration issues, feature flags

**Steps**:
1. **Identify Issue**:
   ```bash
   # Check current config
   firebase hosting:sites:get laughtale-scratch-ca803
   ```

2. **Revert Configuration**:
   ```bash
   # Revert firebase.json if needed
   git checkout HEAD~1 firebase.json
   firebase deploy --only hosting:config
   ```

3. **Verify Fix**:
   ```bash
   curl -I https://laughtale-scratch-ca803.web.app
   ```

**Success Criteria**: Configuration restored, site accessible

---

### Level 2: Asset Rollback ⏱️ 10 minutes

**When to use**: Extension problems, static file issues

**Steps**:
1. **Backup Current State**:
   ```bash
   firebase hosting:releases:list --project laughtale-scratch-ca803
   ```

2. **Identify Last Good Assets**:
   ```bash
   git log --oneline -10
   # Find last working commit (likely b94ec83)
   ```

3. **Revert Assets**:
   ```bash
   # Revert specific files
   git checkout b94ec83 -- public/static/extensions/
   git checkout b94ec83 -- public/
   ```

4. **Redeploy**:
   ```bash
   firebase deploy --only hosting --project laughtale-scratch-ca803
   ```

5. **Verify**:
   ```bash
   curl -s https://laughtale-scratch-ca803.web.app/static/extensions/minecraft-unified.js | head -5
   ```

**Success Criteria**: Assets reverted, extension loads correctly

---

### Level 3: Full Rollback ⏱️ 15 minutes

**When to use**: Major issues, multiple component failures

**Steps**:
1. **Document Current Issue**:
   ```bash
   # Capture logs and state
   firebase hosting:releases:view --project laughtale-scratch-ca803
   ```

2. **Checkout Previous Version**:
   ```bash
   git checkout b94ec83
   # Or specific known good commit
   ```

3. **Verify Rollback Target**:
   ```bash
   # Check that this version was working
   git show --stat b94ec83
   ```

4. **Full Redeploy**:
   ```bash
   # Build if needed
   cd scratch-gui
   npm run build
   cp -r build/* ../public/
   cd ..
   
   # Deploy
   firebase deploy --only hosting --project laughtale-scratch-ca803
   ```

5. **Comprehensive Verification**:
   ```bash
   # Test all critical endpoints
   curl -I https://laughtale-scratch-ca803.web.app
   curl -I https://laughtale-scratch-ca803.web.app/static/extensions/minecraft-unified.js
   ```

**Success Criteria**: Full site restored to working state

---

### Level 4: Emergency Maintenance ⏱️ 2 minutes

**When to use**: Severe security issue, data breach risk

**Steps**:
1. **Immediate Takedown**:
   ```bash
   # Create maintenance page
   echo "<html><body><h1>Site Under Maintenance</h1><p>We'll be back shortly.</p></body></html>" > public/maintenance.html
   
   # Quick deploy maintenance page
   firebase deploy --only hosting --project laughtale-scratch-ca803
   ```

2. **Notify Stakeholders**:
   ```bash
   # Send alerts to team
   echo "URGENT: Production site taken offline due to critical issue at $(date)" 
   ```

3. **Investigate and Plan**:
   - Identify root cause
   - Plan proper fix or rollback
   - Estimate restoration time

**Success Criteria**: Site offline, team notified, investigation started

## 🔧 Rollback Commands Reference

### Quick Command Set
```bash
# Level 1: Config only
firebase deploy --only hosting:config

# Level 2: Assets only  
git checkout b94ec83 -- public/
firebase deploy --only hosting

# Level 3: Full rollback
git checkout b94ec83
firebase deploy --only hosting

# Level 4: Maintenance mode
echo "<html><body><h1>Maintenance</h1></body></html>" > public/index.html
firebase deploy --only hosting
```

### Verification Commands
```bash
# Check site status
curl -I https://laughtale-scratch-ca803.web.app

# Check extension
curl -s https://laughtale-scratch-ca803.web.app/static/extensions/minecraft-unified.js | head -5

# Check deployment history
firebase hosting:releases:list

# Check current git state
git log --oneline -5
git status
```

## 📞 Communication Plan

### Internal Communication
1. **Immediate Notification** (within 2 minutes):
   - Team chat/Slack: "🚨 ROLLBACK INITIATED - [Issue Description]"
   - Include: Issue, Rollback level, ETA

2. **Status Updates** (every 5 minutes until resolved):
   - Progress on rollback
   - Any complications
   - Revised ETA

3. **Completion Notification**:
   - Rollback completed
   - Verification results
   - Next steps

### External Communication (if needed)
1. **User Notification**:
   - Social media/Discord if available
   - Website banner if partial functionality
   - Email if user database available

2. **Stakeholder Notification**:
   - Management briefing
   - Customer support awareness
   - Documentation updates

## 🔍 Post-Rollback Procedures

### Immediate Actions (within 30 minutes)
1. **Verify Complete Recovery**:
   - Run abbreviated test suite
   - Check monitoring dashboards
   - Confirm user functionality

2. **Root Cause Analysis**:
   - Identify what went wrong
   - Determine why it wasn't caught earlier
   - Document timeline of events

3. **Communication**:
   - Notify stakeholders of resolution
   - Document lessons learned
   - Update rollback procedures if needed

### Follow-up Actions (within 24 hours)
1. **Complete Testing**:
   - Run full test suite on rolled-back version
   - Verify no side effects from rollback
   - Check data integrity

2. **Fix Planning**:
   - Identify fix for original issue
   - Plan proper testing for fix
   - Schedule redeployment

3. **Process Improvement**:
   - Update deployment checklist
   - Improve monitoring/alerting
   - Enhance testing procedures

## 📊 Rollback Decision Matrix

| Severity | User Impact | Response Time | Rollback Level |
|----------|-------------|---------------|----------------|
| Critical | >50% | <5 min | Level 3-4 |
| High | 20-50% | <15 min | Level 2-3 |
| Medium | 5-20% | <30 min | Level 1-2 |
| Low | <5% | <60 min | Level 1 |

## ⚠️ Rollback Risks and Mitigation

### Potential Risks
1. **Data Loss**: User progress/settings lost
   - **Mitigation**: No user data stored currently
   
2. **Version Mismatch**: Minecraft mod incompatible  
   - **Mitigation**: Versioning strategy, compatibility testing

3. **Cache Issues**: CDN serving old content
   - **Mitigation**: Cache invalidation commands

4. **Partial Rollback**: Some components not reverted
   - **Mitigation**: Comprehensive verification checklist

### Cache Invalidation Commands
```bash
# Firebase hosting cache clear (if available)
firebase hosting:cache:clear --project laughtale-scratch-ca803

# Manual cache busting verification
curl -H "Cache-Control: no-cache" https://laughtale-scratch-ca803.web.app
```

## 🧪 Rollback Testing

### Pre-Production Testing
Before each deployment, verify rollback procedures:
1. **Test Level 1 Rollback**: Config changes
2. **Test Level 2 Rollback**: Asset reversion  
3. **Test Level 3 Rollback**: Full version revert
4. **Verify Restoration**: All functionality works

### Rollback Drill Schedule
- **Monthly**: Practice Level 1-2 rollbacks
- **Quarterly**: Practice Level 3 full rollback
- **Annually**: Practice Level 4 emergency procedures

## 📋 Rollback Checklist Template

```
ROLLBACK EXECUTION CHECKLIST

Date: _________ Time: _________ 
Issue: _________________________
Rollback Level: ________________
Executed by: __________________

Pre-Rollback:
□ Issue severity confirmed
□ Rollback level determined  
□ Team notified
□ Current state documented

During Rollback:
□ Commands executed
□ No errors encountered
□ Verification successful
□ Stakeholders updated

Post-Rollback:
□ Full functionality verified
□ Monitoring shows normal state
□ Users notified (if needed)
□ Post-mortem scheduled

Sign-off: _________________ Time: _________
```

---

**Emergency Contact**: Claude Code Team  
**Last Tested**: 2025-08-08  
**Next Review**: 2025-09-08