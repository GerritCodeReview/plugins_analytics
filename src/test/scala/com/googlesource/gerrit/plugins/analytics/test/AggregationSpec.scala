package com.googlesource.gerrit.plugins.analytics.test

import java.security.InvalidParameterException
import java.util.Date

import com.googlesource.gerrit.plugins.analytics.common.AggregationStrategy
import org.eclipse.jgit.lib.PersonIdent
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

class AggregationFunctionsSpec extends FlatSpec with Matchers {

  "AggregationStrategy" should
    "have valid constructors" in {
    val p = new PersonIdent("name", "email")
    val d = new Date


    AggregationStrategy("EMAIL").aggregationStrategyMapping(p, d)
      .split("/") should have size 1
    AggregationStrategy("YEAR").aggregationStrategyMapping(p, d)
      .split("/") should have size 2
    AggregationStrategy("MONTH").aggregationStrategyMapping(p, d)
      .split("/") should have size 3
    AggregationStrategy("DAY").aggregationStrategyMapping(p, d)
      .split("/") should have size 4
    AggregationStrategy("HOUR").aggregationStrategyMapping(p, d)
      .split("/") should have size 5

    AggregationStrategy("hour") == AggregationStrategy.HOUR

    intercept[InvalidParameterException] {
      AggregationStrategy("INVALID")
    }


  }

}
