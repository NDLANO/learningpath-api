-- Schema
CREATE SCHEMA learningpathapi;

-- READONLY
CREATE USER learningpathapi_read with PASSWORD '<passord>';
ALTER DEFAULT PRIVILEGES IN SCHEMA learningpathapi GRANT SELECT ON TABLES TO learningpathapi_read;

GRANT CONNECT ON DATABASE data_prod to learningpathapi_read;
GRANT USAGE ON SCHEMA learningpathapi to learningpathapi_read;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA learningpathapi TO learningpathapi_read;
GRANT SELECT ON ALL TABLES IN SCHEMA learningpathapi TO learningpathapi_read;

-- WRITE
CREATE USER learningpathapi_write with PASSWORD '<passord>';

GRANT CONNECT ON DATABASE data_prod to learningpathapi_write;
GRANT USAGE ON SCHEMA learningpathapi to learningpathapi_write;
GRANT CREATE ON SCHEMA learningpathapi to learningpathapi_write;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA learningpathapi TO learningpathapi_write;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA learningpathapi TO learningpathapi_write;