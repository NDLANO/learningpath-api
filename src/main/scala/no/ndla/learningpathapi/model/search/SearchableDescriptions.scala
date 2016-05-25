package no.ndla.learningpathapi.model.search


case class SearchableDescriptions(nb: Option[String],
                                  nn: Option[String],
                                  en: Option[String],
                                  fr: Option[String],
                                  de: Option[String],
                                  es: Option[String],
                                  se: Option[String],
                                  zh: Option[String],
                                  unknown: Option[String])
