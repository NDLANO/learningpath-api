# Learningpath API 
API for Learningpaths from NDLA

# Building and distribution

## Compile
    sbt compile

## Run tests
    sbt test

## Package and run locally
    ndla deploy local ndla/learningpath-api

## Publish to nexus
    sbt publish

## Create Docker Image
    ./build.sh

## Deploy Docker Image
    ndla release ndla/learningpath-api
    ndla deploy <environment> ndla/learningpath-api
    
# Setup local environment
## Database
    ndla deploy local postgres
    psql --host $(DOCKER_ADDR) --port 5432 --username "postgres" --password -d postgres -f src/main/db/local_testdata.sql
    
## Search-engine
    ndla deploy local ndla/search-engine
    
## Index the metadata to search-engine
    ndla index local ndla/learningpath-api
    
## Test
    curl http://$DOCKER_ADDR:30008/learningpaths/
    

