package in.tap.we.poli.analytic.jobs.connectors.fuzzy.predictor

import in.tap.we.poli.analytic.jobs.BaseSpec
import in.tap.we.poli.analytic.jobs.connectors.fuzzy.features.VendorsFuzzyConnectorFeaturesJob.{Comparison, Features}
import in.tap.we.poli.analytic.jobs.connectors.fuzzy.predictor.VendorsFuzzyPredictorJob.Prediction

class VendorsFuzzyPredictorJobSpec extends BaseSpec with VendorsFuzzyPredictorJobFixtures {

  it should "produce monotonically increasing predictions" in {
    val prediction1: Double = {
      Prediction.predict(
        Features(
          numTokens = 1.0,
          numTokensInCommon = 1.0,
          sameSrcId = 0.0,
          sameZip = 0.0,
          sameCity = 0.0,
          sameState = 0.0
        )
      )
    }
    val prediction2: Double = {
      Prediction.predict(
        Features(
          numTokens = 2.0,
          numTokensInCommon = 2.0,
          sameSrcId = 0.0,
          sameZip = 0.0,
          sameCity = 0.0,
          sameState = 0.0
        )
      )
    }
    val prediction3: Double = {
      Prediction.predict(
        Features(
          numTokens = 2.0,
          numTokensInCommon = 2.0,
          sameSrcId = 1.0,
          sameZip = 0.0,
          sameCity = 0.0,
          sameState = 0.0
        )
      )
    }
    val prediction4: Double = {
      Prediction.predict(
        Features(
          numTokens = 2.0,
          numTokensInCommon = 2.0,
          sameSrcId = 1.0,
          sameZip = 1.0,
          sameCity = 0.0,
          sameState = 0.0
        )
      )
    }
    val prediction5: Double = {
      Prediction.predict(
        Features(
          numTokens = 2.0,
          numTokensInCommon = 2.0,
          sameSrcId = 1.0,
          sameZip = 1.0,
          sameCity = 1.0,
          sameState = 0.0
        )
      )
    }
    val prediction6: Double = {
      Prediction.predict(
        Features(
          numTokens = 2.0,
          numTokensInCommon = 2.0,
          sameSrcId = 1.0,
          sameZip = 1.0,
          sameCity = 1.0,
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
      prediction6
    ).foreach(println)
    assert(prediction1 < prediction2)
    assert(prediction2 < prediction3)
    assert(prediction3 < prediction4)
    assert(prediction4 < prediction5)
    assert(prediction5 < prediction6)
  }

  it should "also product non-monotonically increasing predictions" in {
    val prediction1: Double = {
      Prediction.predict(
        Features(
          numTokens = 1.0,
          numTokensInCommon = 1.0,
          sameSrcId = 0.0,
          sameZip = 0.0,
          sameCity = 0.0,
          sameState = 0.0
        )
      )
    }
    val prediction2: Double = {
      Prediction.predict(
        Features(
          numTokens = 2.0,
          numTokensInCommon = 1.0,
          sameSrcId = 0.0,
          sameZip = 0.0,
          sameCity = 0.0,
          sameState = 0.0
        )
      )
    }
    println("Non-Monotonically Increasing")
    Seq(
      prediction1,
      prediction2
    ).foreach(println)
    assert(prediction1 > prediction2)
  }

  it should "build predictions from vendor comparisons" in {
    // identity
    Prediction(
      Comparison(
        vendor1,
        vendor1
      )
    ) shouldBe {
      0.9769401785040871
    }
    // normalized as identity
    Prediction(
      Comparison(
        vendor1,
        vendor2
      )
    ) shouldBe {
      0.9769401785040871
    }
    // some in common
    Prediction(
      Comparison(
        vendor1,
        vendor3
      )
    ) shouldBe {
      0.4400597163839571
    }
    // nothing in common
    Prediction(
      Comparison(
        vendor3,
        vendor4
      )
    ) shouldBe {
      8.366057410404834E-6
    }
  }

}
