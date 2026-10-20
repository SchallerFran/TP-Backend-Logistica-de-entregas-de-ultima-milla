-- Se ejecuta UNA sola vez, como superusuario: psql -U postgres -f infra/init-db.sql
-- Las contrasenas de abajo son valores de DESARROLLO y coinciden con los valores
-- por defecto de los application.yml (un script .sql no lee variables de entorno).
-- Para un entorno real: cambiar ambos lados.

-- ---------- pedidos ----------
CREATE USER pedidos_user WITH PASSWORD 'pedidos_pass';
CREATE DATABASE pedidos_db OWNER pedidos_user;
GRANT ALL PRIVILEGES ON DATABASE pedidos_db TO pedidos_user;
\connect pedidos_db
GRANT ALL ON SCHEMA public TO pedidos_user;

-- ---------- despacho ----------
\connect postgres
CREATE USER despacho_user WITH PASSWORD 'despacho_pass';
CREATE DATABASE despacho_db OWNER despacho_user;
GRANT ALL PRIVILEGES ON DATABASE despacho_db TO despacho_user;
\connect despacho_db
GRANT ALL ON SCHEMA public TO despacho_user;

-- ---------- keycloak ----------
\connect postgres
CREATE USER keycloak_user WITH PASSWORD 'keycloak_pass';
CREATE DATABASE keycloak_db OWNER keycloak_user;
GRANT ALL PRIVILEGES ON DATABASE keycloak_db TO keycloak_user;
\connect keycloak_db
GRANT ALL ON SCHEMA public TO keycloak_user;
