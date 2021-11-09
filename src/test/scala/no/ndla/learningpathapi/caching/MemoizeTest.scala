/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.caching

import no.ndla.learningpathapi.UnitSuite
import org.mockito.Mockito._

class MemoizeTest extends UnitSuite {

  class Target {
    def targetMethod(value: String): String = s"Hei, $value"
  }

  test("That an uncached value will do an actual call") {
    val targetMock = mock[Target]
    val name = "Rune Rudberg"
    val memoizedTarget = Memoize[String, String](targetMock.targetMethod)

    when(targetMock.targetMethod(name)).thenReturn("Hello from mock")
    memoizedTarget(name) should equal("Hello from mock")
    verify(targetMock, times(1)).targetMethod(name)
  }

  test("That a cached value will not forward the call to the target") {
    val targetMock = mock[Target]
    val name = "Rune Rudberg"
    val memoizedTarget = Memoize[String, String](targetMock.targetMethod)

    when(targetMock.targetMethod(name)).thenReturn("Hello from mock")
    Seq(1 to 10).foreach(i => {
      memoizedTarget(name) should equal("Hello from mock")
    })
    verify(targetMock, times(1)).targetMethod(name)
  }

  test("That the cache is invalidated after cacheMaxAge") {
    val cacheMaxAgeInMs = 20
    val name = "Rune Rudberg"
    val targetMock = mock[Target]
    val memoizedTarget =
      Memoize[String, String](targetMock.targetMethod, cacheMaxAgeInMs)

    when(targetMock.targetMethod(name)).thenReturn("Hello from mock")

    memoizedTarget(name) should equal("Hello from mock")
    memoizedTarget(name) should equal("Hello from mock")
    Thread.sleep(cacheMaxAgeInMs)
    memoizedTarget(name) should equal("Hello from mock")
    memoizedTarget(name) should equal("Hello from mock")

    verify(targetMock, times(2)).targetMethod(name)
  }
}
