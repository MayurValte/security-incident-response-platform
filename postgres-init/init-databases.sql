-- Runs automatically on first container boot (docker-entrypoint-initdb.d),
-- against the default POSTGRES_DB ("sirp") connection. docker-compose.yml's
-- postgres service only creates that one database - every service's actual
-- database (see CLAUDE.md "Build and run") is missing on a fresh volume and
-- must exist before that service's Flyway migration can run. workflow_db is
-- deliberately named without the sirp_ prefix, matching every other service.
CREATE DATABASE sirp_auth_db;
CREATE DATABASE sirp_user_db;
CREATE DATABASE sirp_incident_db;
CREATE DATABASE sirp_audit_db;
CREATE DATABASE sirp_notification_db;
CREATE DATABASE sirp_analytics_db;
CREATE DATABASE workflow_db;
