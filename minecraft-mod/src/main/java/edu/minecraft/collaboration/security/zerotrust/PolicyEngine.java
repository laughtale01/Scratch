package edu.minecraft.collaboration.security.zerotrust;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Policy Engine for Zero-Trust Access Control
 * Evaluates access policies and makes authorization decisions
 */
public class PolicyEngine {
    
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    
    private final Map<String, Policy> policies = new ConcurrentHashMap<>();
    private final Map<String, Integer> policyPriorities = new ConcurrentHashMap<>();
    
    /**
     * Add a policy to the engine
     */
    public void addPolicy(Policy policy) {
        policies.put(policy.getName(), policy);
        policyPriorities.put(policy.getName(), policy.getPriority());
        LOGGER.debug("Added policy: {} with priority: {}", policy.getName(), policy.getPriority());
    }
    
    /**
     * Remove a policy from the engine
     */
    public void removePolicy(String policyName) {
        policies.remove(policyName);
        policyPriorities.remove(policyName);
        LOGGER.debug("Removed policy: {}", policyName);
    }
    
    /**
     * Evaluate all applicable policies for the given context
     */
    public PolicyDecision evaluate(AccessContext context, RiskAssessment riskAssessment) {
        List<PolicyDecision> decisions = new ArrayList<>();
        
        // Get policies sorted by priority (higher priority first)
        List<Policy> sortedPolicies = policies.values().stream()
            .sorted((p1, p2) -> Integer.compare(p2.getPriority(), p1.getPriority()))
            .toList();
        
        // Evaluate each policy
        for (Policy policy : sortedPolicies) {
            try {
                if (policy.applies(context)) {
                    PolicyDecision decision = policy.evaluate(context, riskAssessment);
                    decisions.add(decision);
                    
                    // If this is a DENY decision with high priority, return immediately
                    if (decision.getDecision() == PolicyDecision.Decision.DENY && policy.getPriority() >= 1000) {
                        LOGGER.debug("High priority DENY policy {} triggered", policy.getName());
                        return decision;
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error evaluating policy: {}", policy.getName(), e);
                // Continue with other policies on error
            }
        }
        
        // Combine decisions using decision logic
        return combineDecisions(decisions, context);
    }
    
    /**
     * Combine multiple policy decisions into a final decision
     */
    private PolicyDecision combineDecisions(List<PolicyDecision> decisions, AccessContext context) {
        if (decisions.isEmpty()) {
            return PolicyDecision.deny("No applicable policies found");
        }
        
        // If any decision is DENY, return DENY (fail-safe approach)
        for (PolicyDecision decision : decisions) {
            if (decision.getDecision() == PolicyDecision.Decision.DENY) {
                return decision;
            }
        }
        
        // If any decision is ALLOW, return ALLOW
        for (PolicyDecision decision : decisions) {
            if (decision.getDecision() == PolicyDecision.Decision.ALLOW) {
                return decision;
            }
        }
        
        // Default to DENY
        return PolicyDecision.deny("No explicit ALLOW decision found");
    }
    
    /**
     * Get all policies
     */
    public Collection<Policy> getAllPolicies() {
        return new ArrayList<>(policies.values());
    }
    
    /**
     * Get policy count
     */
    public int getPolicyCount() {
        return policies.size();
    }
    
    /**
     * Check if a policy exists
     */
    public boolean hasPolicy(String policyName) {
        return policies.containsKey(policyName);
    }
    
    /**
     * Get a specific policy
     */
    public Policy getPolicy(String policyName) {
        return policies.get(policyName);
    }
    
    /**
     * Clear all policies
     */
    public void clear() {
        policies.clear();
        policyPriorities.clear();
        LOGGER.info("Cleared all policies");
    }
}

/**
 * Represents a security policy
 */
class Policy {
    private final String name;
    private final Predicate<AccessContext> condition;
    private final PolicyDecision.Decision decision;
    private final String reason;
    private final int priority;
    private final Map<String, Object> attributes;
    
    private Policy(Builder builder) {
        this.name = builder.name;
        this.condition = builder.condition;
        this.decision = builder.decision;
        this.reason = builder.reason;
        this.priority = builder.priority;
        this.attributes = new HashMap<>(builder.attributes);
    }
    
    public String getName() { return name; }
    public int getPriority() { return priority; }
    public Map<String, Object> getAttributes() { return attributes; }
    
    /**
     * Check if this policy applies to the given context
     */
    public boolean applies(AccessContext context) {
        try {
            return condition.test(context);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Evaluate the policy for the given context
     */
    public PolicyDecision evaluate(AccessContext context, RiskAssessment riskAssessment) {
        if (!applies(context)) {
            return PolicyDecision.notApplicable("Policy does not apply");
        }
        
        String effectiveReason = reason != null ? reason : ("Policy " + name);
        
        return switch (decision) {
            case ALLOW -> PolicyDecision.allow(effectiveReason);
            case DENY -> PolicyDecision.deny(effectiveReason);
            case NOT_APPLICABLE -> PolicyDecision.notApplicable(effectiveReason);
        };
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String name;
        private Predicate<AccessContext> condition;
        private PolicyDecision.Decision decision = PolicyDecision.Decision.DENY;
        private String reason;
        private int priority = 500; // Default priority
        private Map<String, Object> attributes = new HashMap<>();
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder condition(Predicate<AccessContext> condition) {
            this.condition = condition;
            return this;
        }
        
        public Builder decision(PolicyDecision.Decision decision) {
            this.decision = decision;
            return this;
        }
        
        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }
        
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }
        
        public Builder attribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }
        
        public Policy build() {
            if (name == null || condition == null) {
                throw new IllegalArgumentException("Policy name and condition are required");
            }
            return new Policy(this);
        }
    }
}

/**
 * Represents a policy decision
 */
class PolicyDecision {
    public enum Decision {
        ALLOW,
        DENY,
        NOT_APPLICABLE
    }
    
    private final Decision decision;
    private final String reason;
    private final Map<String, Object> context;
    
    private PolicyDecision(Decision decision, String reason, Map<String, Object> context) {
        this.decision = decision;
        this.reason = reason;
        this.context = context != null ? context : Collections.emptyMap();
    }
    
    public static PolicyDecision allow(String reason) {
        return new PolicyDecision(Decision.ALLOW, reason, null);
    }
    
    public static PolicyDecision deny(String reason) {
        return new PolicyDecision(Decision.DENY, reason, null);
    }
    
    public static PolicyDecision notApplicable(String reason) {
        return new PolicyDecision(Decision.NOT_APPLICABLE, reason, null);
    }
    
    public static PolicyDecision allow(String reason, Map<String, Object> context) {
        return new PolicyDecision(Decision.ALLOW, reason, context);
    }
    
    public static PolicyDecision deny(String reason, Map<String, Object> context) {
        return new PolicyDecision(Decision.DENY, reason, context);
    }
    
    public Decision getDecision() { return decision; }
    public String getReason() { return reason; }
    public Map<String, Object> getContext() { return context; }
}