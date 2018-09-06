/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.caching

class Memoize[T, R](f: T => R, maxAgeMs: Long) extends (T => R) {
  case class CacheValue(value: R, lastUpdated: Long) {

    def isExpired: Boolean =
      lastUpdated + maxAgeMs <= System.currentTimeMillis()
  }

  private[this] var cache: Map[T, CacheValue] = Map.empty

  override def apply(value: T): R = {
    cache.get(value) match {
      case Some(cachedValue) if !cachedValue.isExpired => cachedValue.value
      case _ =>
        cache = cache.updated(value, CacheValue(f(value), System.currentTimeMillis()))
        cache(value).value
    }
  }
}

object Memoize {

  // default to 30 minutes cache time
  def apply[T, R](f: T => R, maxAgeMs: Long = 1000 * 60 * 30) =
    new Memoize[T, R](f, maxAgeMs)
}
