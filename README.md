# Learningpath API
 [![Build Status](https://travis-ci.org/NDLANO/learningpath-api.svg?branch=master)](https://travis-ci.org/NDLANO/learningpath-api)

## Usage
Creates, updates, deletes and returns a Learningpath. Implements Elasticsearch for search within the learningpath database.

#### Licenses
Returns a list of licenses with the possibility of filtering on the license key.

#### Api access
To interact with the api, you need valid security credentials; see [Access Tokens usage](https://github.com/NDLANO/auth/blob/master/README.md).

To write data to the api, you need write role access. This is only accessible in [learningpath-frontend](https://learningpath-frontend.staging.api.ndla.no) today.

For a more detailed documentation of the API, please refer to the [API documentation](https://api.ndla.no) (Staging: [API documentation](https://staging.api.ndla.no)).

## Developer documentation


**Compile:** sbt compile

**Run tests:** sbt test

**Run integration tests:** sbt it:test

**Create Docker Image:** ./build.sh


## Create Docker Image
    ./build.sh
