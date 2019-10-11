import scala.sys.process._

pactBrokerAddress := sys.env.getOrElse("PACT_BROKER_URL", "")
pactBrokerCredentials := (
  sys.env.getOrElse("PACT_BROKER_USERNAME", ""),
  sys.env.getOrElse("PACT_BROKER_PASSWORD", "")
)
pactContractTags := Seq(
  sys.env.getOrElse(
    "TRAVIS_BRANCH",
    git.gitCurrentBranch.value
  ) + sys.env
    .get("TRAVIS_PULL_REQUEST_BRANCH")
    .filter(_.nonEmpty)
    .map(prBranch => s"-from-$prBranch")
    .getOrElse("")
)
pactContractVersion := ("git rev-parse --short=7 HEAD" !!).trim
