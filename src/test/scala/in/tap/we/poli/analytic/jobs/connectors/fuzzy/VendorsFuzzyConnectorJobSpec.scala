package in.tap.we.poli.analytic.jobs.connectors.fuzzy

import in.tap.we.poli.analytic.jobs.BaseSpec
import in.tap.we.poli.analytic.jobs.connectors.fuzzy.VendorsFuzzyConnectorFeaturesJob.{
  Comparator, Features, UniqueVendorComparison
}
import in.tap.we.poli.analytic.jobs.connectors.fuzzy.VendorsFuzzyConnectorJob.Prediction

class VendorsFuzzyConnectorJobSpec extends BaseSpec with VendorsFuzzyConnectorJobFixtures {

  it should "produce monotonically increasing predictions" in {
    val prediction1: Double = {
      Prediction.predict(
        Features(
          numTokens = 1.0,
          numTokensInCommon = 1.0,
          numEdges = 0.0,
          numEdgesInCommon = 0.0,
          sameCity = 0.0,
          sameZip = 0.0,
          sameState = 0.0
        )
      )
    }
    val prediction2: Double = {
      Prediction.predict(
        Features(
          numTokens = 2.0,
          numTokensInCommon = 2.0,
          numEdges = 0.0,
          numEdgesInCommon = 0.0,
          sameCity = 0.0,
          sameZip = 0.0,
          sameState = 0.0
        )
      )
    }
    val prediction3: Double = {
      Prediction.predict(
        Features(
          numTokens = 2.0,
          numTokensInCommon = 2.0,
          numEdges = 1.0,
          numEdgesInCommon = 1.0,
          sameCity = 0.0,
          sameZip = 0.0,
          sameState = 0.0
        )
      )
    }
    val prediction4: Double = {
      Prediction.predict(
        Features(
          numTokens = 2.0,
          numTokensInCommon = 2.0,
          numEdges = 2.0,
          numEdgesInCommon = 2.0,
          sameCity = 0.0,
          sameZip = 0.0,
          sameState = 0.0
        )
      )
    }
    val prediction5: Double = {
      Prediction.predict(
        Features(
          numTokens = 2.0,
          numTokensInCommon = 2.0,
          numEdges = 2.0,
          numEdgesInCommon = 2.0,
          sameCity = 1.0,
          sameZip = 0.0,
          sameState = 0.0
        )
      )
    }
    val prediction6: Double = {
      Prediction.predict(
        Features(
          numTokens = 2.0,
          numTokensInCommon = 2.0,
          numEdges = 2.0,
          numEdgesInCommon = 2.0,
          sameCity = 1.0,
          sameZip = 1.0,
          sameState = 0.0
        )
      )
    }
    val prediction7: Double = {
      Prediction.predict(
        Features(
          numTokens = 2.0,
          numTokensInCommon = 2.0,
          numEdges = 2.0,
          numEdgesInCommon = 2.0,
          sameCity = 1.0,
          sameZip = 1.0,
          sameState = 1.0
        )
      )
    }
    println("Monotonically Increasing")
    Seq(
      prediction1,
      prediction2,
      prediction3,
      prediction4,
      prediction5,
      prediction6,
      prediction7
    ).foreach(println)
    assert(prediction1 < prediction2)
    assert(prediction2 < prediction3)
    assert(prediction3 < prediction4)
    assert(prediction4 < prediction5)
    assert(prediction5 < prediction6)
    assert(prediction6 < prediction7)
  }

  it should "also product non-monotonically increasing predictions" in {
    val prediction1: Double = {
      Prediction.predict(
        Features(
          numTokens = 1.0,
          numTokensInCommon = 1.0,
          numEdges = 0.0,
          numEdgesInCommon = 0.0,
          sameCity = 0.0,
          sameZip = 0.0,
          sameState = 0.0
        )
      )
    }
    val prediction2: Double = {
      Prediction.predict(
        Features(
          numTokens = 2.0,
          numTokensInCommon = 1.0,
          numEdges = 0.0,
          numEdgesInCommon = 0.0,
          sameCity = 0.0,
          sameZip = 0.0,
          sameState = 0.0
        )
      )
    }
    val prediction3: Double = {
      Prediction.predict(
        Features(
          numTokens = 1.0,
          numTokensInCommon = 1.0,
          numEdges = 1.0,
          numEdgesInCommon = 1.0,
          sameCity = 0.0,
          sameZip = 0.0,
          sameState = 0.0
        )
      )
    }
    val prediction4: Double = {
      Prediction.predict(
        Features(
          numTokens = 1.0,
          numTokensInCommon = 1.0,
          numEdges = 2.0,
          numEdgesInCommon = 1.0,
          sameCity = 0.0,
          sameZip = 0.0,
          sameState = 0.0
        )
      )
    }
    println("Non-Monotonically Increasing")
    Seq(
      prediction1,
      prediction2,
      prediction3,
      prediction4
    ).foreach(println)
    assert(prediction1 > prediction2)
    assert(prediction3 > prediction4)
  }

  it should "build predictions from unique vendor comparisons" in {
    // identity
    Prediction(
      UniqueVendorComparison(
        Comparator(uniqueVendor1),
        Comparator(uniqueVendor1)
      )
    ) shouldBe {
      0.9999999745535614
    }
    // normalized as identity
    Prediction(
      UniqueVendorComparison(
        Comparator(uniqueVendor1),
        Comparator(uniqueVendor2)
      )
    ) shouldBe {
      0.9999999745535614
    }
    // nothing in common
    Prediction(
      UniqueVendorComparison(
        Comparator(uniqueVendor1),
        Comparator(uniqueVendor3)
      )
    ) shouldBe {
      3.7423476579841565e-108
    }
  }

}