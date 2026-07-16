-- Default bootstrap admin so a fresh environment has at least one login
-- without a manual SQL step (see CLAUDE.md's "no seed data" note - this
-- migration replaces that manual step for the admin user specifically).
-- Password hash is bcrypt for "Admin@1234" - change it after first login.
INSERT INTO teams (id, team_name)
VALUES (gen_random_uuid(), 'Default Team')
ON CONFLICT DO NOTHING;

INSERT INTO users (id, username, email, password, enabled, role, team_id)
SELECT gen_random_uuid(), 'admin', 'admin@sir.com',
       '$2y$05$VWFbt5fpSshI3Zy5SChhDu2im/NK8GK56duQGzJ06oGXeaVP24LDK',
       true, 'ADMIN', t.id
FROM teams t
WHERE t.team_name = 'Default Team'
ON CONFLICT (email) DO NOTHING;
