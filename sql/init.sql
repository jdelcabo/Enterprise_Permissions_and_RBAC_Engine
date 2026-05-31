-- RBAC Engine Schema

-- Stores all roles in the system
CREATE TABLE IF NOT EXISTS roles (
                                     id      SERIAL PRIMARY KEY,
                                     name    VARCHAR(100) NOT NULL UNIQUE
    );

-- Stores all permissions in the system
CREATE TABLE IF NOT EXISTS permissions (
                                           id      SERIAL PRIMARY KEY,
                                           name    VARCHAR(100) NOT NULL UNIQUE
    );

-- Many-to-many: which permissions belong to which roles
CREATE TABLE IF NOT EXISTS role_permissions (
                                                role_id         INT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id   INT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
    );

-- Models the inheritance graph: child_role inherits from parent_role
CREATE TABLE IF NOT EXISTS role_inheritance (
                                                child_role_id   INT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    parent_role_id  INT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (child_role_id, parent_role_id)
    );

-- Seed Data
INSERT INTO roles (name) VALUES ('intern'), ('employee'), ('manager'), ('ceo');

INSERT INTO permissions (name) VALUES
                                   ('read_wiki'),
                                   ('submit_expenses'),
                                   ('approve_budget'),
                                   ('fire_anybody');

-- Assign direct permissions to roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE (r.name = 'intern'   AND p.name = 'read_wiki')
   OR (r.name = 'employee' AND p.name = 'submit_expenses')
   OR (r.name = 'manager'  AND p.name = 'approve_budget')
   OR (r.name = 'ceo'      AND p.name = 'fire_anybody');

-- Set up inheritance chain
INSERT INTO role_inheritance (child_role_id, parent_role_id)
SELECT child.id, parent.id FROM roles child, roles parent
WHERE (child.name = 'employee' AND parent.name = 'intern')
   OR (child.name = 'manager'  AND parent.name = 'employee')
   OR (child.name = 'ceo'      AND parent.name = 'manager');