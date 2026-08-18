-- Database-per-service: each service owns its own schema and Flyway history,
-- even though they share one MySQL instance for local dev. Sharing a single
-- database would collide on Flyway's schema_history table — every service's
-- migration starts at V1 with different SQL, so the second service to boot
-- would fail Flyway's checksum validation against the first service's V1.
CREATE DATABASE IF NOT EXISTS transactions;
CREATE DATABASE IF NOT EXISTS risk_scoring;
CREATE DATABASE IF NOT EXISTS alerts;

GRANT ALL PRIVILEGES ON transactions.* TO 'risksignal'@'%';
GRANT ALL PRIVILEGES ON risk_scoring.* TO 'risksignal'@'%';
GRANT ALL PRIVILEGES ON alerts.* TO 'risksignal'@'%';
FLUSH PRIVILEGES;
