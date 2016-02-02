CREATE TABLE learningpathapi.learningpaths (
  id BIGSERIAL PRIMARY KEY,
  document JSONB
);

CREATE TABLE learningpathapi.learningsteps (
  id BIGSERIAL PRIMARY KEY,
  learning_path_id BIGSERIAL REFERENCES learningpathapi.learningpaths(id) ON DELETE CASCADE,
  document JSONB
);
