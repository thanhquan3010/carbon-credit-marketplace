-- Create databases for microservices
CREATE DATABASE carbon_marketplace_users;
CREATE DATABASE carbon_marketplace_vehicles;
CREATE DATABASE carbon_marketplace_trips;
CREATE DATABASE carbon_marketplace_credits;
CREATE DATABASE carbon_marketplace_marketplace;
CREATE DATABASE carbon_marketplace_transactions;
CREATE DATABASE carbon_marketplace_analytics;

-- Create user with privileges
CREATE USER carbon_user WITH PASSWORD 'carbon_password';
GRANT ALL PRIVILEGES ON DATABASE carbon_marketplace_users TO carbon_user;
GRANT ALL PRIVILEGES ON DATABASE carbon_marketplace_vehicles TO carbon_user;
GRANT ALL PRIVILEGES ON DATABASE carbon_marketplace_trips TO carbon_user;
GRANT ALL PRIVILEGES ON DATABASE carbon_marketplace_credits TO carbon_user;
GRANT ALL PRIVILEGES ON DATABASE carbon_marketplace_marketplace TO carbon_user;
GRANT ALL PRIVILEGES ON DATABASE carbon_marketplace_transactions TO carbon_user;
GRANT ALL PRIVILEGES ON DATABASE carbon_marketplace_analytics TO carbon_user;

-- Enable required extensions
\c carbon_marketplace_users;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c carbon_marketplace_vehicles;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c carbon_marketplace_trips;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis"; -- For geographic data

\c carbon_marketplace_credits;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c carbon_marketplace_marketplace;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c carbon_marketplace_transactions;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c carbon_marketplace_analytics;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "timescaledb"; -- For time-series data
