# Rate Limiter Operations Runbook

## Service objectives

- Availability: 99.9% of valid rate-limit checks return a non-5xx response.
- Latency: P99 below 200 ms at the service boundary.
- Page when 5xx responses exceed 1% for five minutes, P99 exceeds 200 ms for ten minutes, Redis memory exceeds 80%, or the HPA remains at its maximum for ten minutes.
- Create a warning when rejected decisions depart materially from the established traffic baseline.

These are proposed objectives for the portfolio environment. A production team should set targets from measured traffic, downstream capacity, and error budgets.

## First response

1. Confirm scope with `kubectl -n rate-limiter get deploy,pods,hpa`.
2. Check `/actuator/health/readiness` and Redis health.
3. Compare `rate_limit_decisions_total` with HTTP request, error, and latency metrics.
4. Search structured logs by `rate_limit_backend_failure`, fingerprinted identity, path, and deployment revision. Never paste raw API keys into an incident ticket.
5. Check recent configuration and deployment changes.
6. Roll back with `kubectl -n rate-limiter rollout undo deployment/rate-limiter` if the incident began after a release.

## Redis unavailable

The service intentionally returns `503` for protected paths.

1. Confirm DNS resolution and the configured Redis host and port.
2. Check NetworkPolicy, Redis readiness, connection saturation, memory pressure, and recent failover events.
3. Restore Redis or switch the application to the approved managed endpoint.
4. Do not bypass the limiter during an active traffic surge without incident-commander approval.

## Elevated 429 responses

1. Determine whether one fingerprinted identity or the whole service is affected.
2. Confirm the configured capacity and window.
3. Inspect the TTL for a sampled sanitized Redis key.
4. Compare traffic volume and client behavior with the previous baseline.
5. Raise the quota only after confirming downstream capacity and recording the change.

## High latency

1. Split latency between the application and Redis.
2. Check CPU throttling, garbage collection, HPA status, Redis command latency, and connection counts.
3. If the application is saturated, scale within the tested ceiling.
4. If Redis is saturated, reduce hot-key concentration or scale the managed Redis tier.

## Suspected proxy misconfiguration

If many clients share one limit unexpectedly, confirm whether `TRUST_FORWARDED_FOR` matches the deployment topology. Enable it only when the trusted ingress overwrites client-supplied forwarding headers. Disable it immediately if untrusted clients can supply the accepted header.

## Recovery verification

1. Run the load script and confirm accepted and rejected counts match the configured policy.
2. Confirm `RateLimit-Reset` and `Retry-After` contain a short delay in seconds rather than an epoch timestamp.
3. Confirm P99 is below 200 ms and error rates have recovered.
4. Watch the dashboards for ten minutes.
5. Record the timeline, customer impact, root cause, corrective action, and follow-up owner.

