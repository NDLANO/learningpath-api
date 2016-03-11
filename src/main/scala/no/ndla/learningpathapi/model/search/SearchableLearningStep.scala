package no.ndla.learningpathapi.model.search

import no.ndla.learningpathapi.model.domain.{Description, Title}

case class SearchableLearningStep (title:List[Title], description:List[Description])
