CREATE TABLE learningpaths (
  id BIGSERIAL PRIMARY KEY,
  external_id TEXT,
  document JSONB
);

CREATE TABLE learningsteps (
  id BIGSERIAL PRIMARY KEY,
  learning_path_id BIGSERIAL REFERENCES learningpathapi.learningpaths(id) ON DELETE CASCADE,
  external_id TEXT,
  document JSONB
);
