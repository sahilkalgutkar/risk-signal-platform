# risk-signal-platform on Kubernetes

Manifests here target either a local `kind` cluster (using the in-cluster
`mysql`/`kafka` Deployments in this directory) or EKS (where you'd delete
`20-mysql.yaml` / `21-kafka.yaml` and point `DB_HOST` at RDS for MySQL and
`KAFKA_BOOTSTRAP_SERVERS` at MSK instead — see the root
[README](../../README.md#aws-deployment) for the full path).

## Local: kind

```bash
# 1. Create the cluster
kind create cluster --name risk-signal

# 2. Install an ingress controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod --selector=app.kubernetes.io/component=controller \
  --timeout=120s

# 3. Build and load the app images into kind (kind can't pull from your
#    local Docker daemon directly)
docker build -t risk-signal-transaction-api:latest -f transaction-api/Dockerfile .
docker build -t risk-signal-risk-scoring-service:latest -f risk-scoring-service/Dockerfile .
docker build -t risk-signal-alert-service:latest -f alert-service/Dockerfile .
kind load docker-image risk-signal-transaction-api:latest --name risk-signal
kind load docker-image risk-signal-risk-scoring-service:latest --name risk-signal
kind load docker-image risk-signal-alert-service:latest --name risk-signal

# 4. Create the namespace + secret (see 11-secret.example.yaml for the
#    literals it expects)
kubectl apply -f infra/k8s/00-namespace.yaml
kubectl create secret generic risk-signal-secrets -n risk-signal \
  --from-literal=DB_PASSWORD=risksignal \
  --from-literal=MYSQL_ROOT_PASSWORD=root

# 5. Apply everything else
kubectl apply -k infra/k8s/

# 6. Point risk-signal.local at the kind ingress and open it
echo "127.0.0.1 risk-signal.local" | sudo tee -a /etc/hosts
kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 8080:80 &
curl -X POST http://risk-signal.local:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"accountId":"acct-1","amount":5000.00,"currency":"USD","merchantCountry":"GB","accountCountry":"US"}'
```

## Known simplifications (called out deliberately, not accidental)

- **`mysql` and `kafka` here are single-replica Deployments with
  `strategy: Recreate`**, not StatefulSets or an operator-managed cluster —
  fine for a local demo, but neither survives a pod restart with zero
  downtime. Production would use RDS (MySQL) and MSK or the Strimzi operator
  (Kafka) instead — see `20-mysql.yaml` / `21-kafka.yaml` header comments.
- **No Flyway migration Job** — each app pod runs its own Flyway migration
  on boot (`spring.flyway.enabled=true` in `application.yml`). With 2
  replicas per service this means concurrent migration attempts on rollout;
  Flyway's own locking handles that safely for this schema size, but a
  larger schema would want a dedicated migration Job gating the rollout
  instead, the way `PipelineOps` calls out the same simplification for
  Django migrations.
- **No `HorizontalPodAutoscaler`** — all three services are fixed at 2
  replicas. `transaction-api` is the one on the synchronous request path
  and the most likely candidate for one if traffic became bursty.
- **No `ServiceMonitor`** — the Services expose the actuator/Prometheus port
  for a Prometheus Operator install to scrape, but wiring that up is left
  as a follow-on; `docker-compose.yml`'s Prometheus + Grafana pair is the
  working example of the metrics path end-to-end.
