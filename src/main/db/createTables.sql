CREATE TABLE learningpathapi.learningpaths (
  id BIGSERIAL PRIMARY KEY,
  external_id TEXT,
  document JSONB
);

CREATE TABLE learningpathapi.learningsteps (
  id BIGSERIAL PRIMARY KEY,
  learning_path_id BIGSERIAL REFERENCES learningpathapi.learningpaths(id) ON DELETE CASCADE,
  external_id TEXT,
  document JSONB
);
