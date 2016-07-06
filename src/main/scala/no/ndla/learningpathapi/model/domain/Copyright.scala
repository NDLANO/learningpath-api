package no.ndla.learningpathapi.model.domain

case class Copyright(license: License, origin: String, contributors: Seq[Author])
