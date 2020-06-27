package in.tap.we.poli.analytic.jobs.connectors

import in.tap.base.spark.io.{Formats, In, Out}
import in.tap.base.spark.main.InArgs.OneInArgs
import in.tap.base.spark.main.OutArgs.OneOutArgs
import in.tap.we.poli.analytic.jobs.BaseSparkJobSpec
import org.apache.spark.graphx.VertexId

class VendorsConnectorJobSpec extends BaseSparkJobSpec with VendorsConnectorJobFixtures {

  val resourcePath: String = {
    "/Users/alex/Documents/GitHub/Alex/tap-in/we-poli/we-poli-analytic/src/test/resources/connectors/vendors/"
  }

  val inPath: String = {
    s"$resourcePath/in"
  }

  val outPath: String = {
    s"$resourcePath/out"
  }

  val _: Unit = {
    import spark.implicits._
    import org.apache.spark.sql.SaveMode
    Seq(vendor1, vendor2, vendor3).toDS.write.mode(SaveMode.Overwrite).json(inPath)
  }

  new VendorsConnectorJob(
    OneInArgs(In(path = inPath, format = Formats.JSON)),
    OneOutArgs(Out(path = outPath, format = Formats.JSON))
  ).execute()

  it should "connect vendors" in {
    import spark.implicits._
    spark.read.json(outPath).as[(VertexId, VertexId)].collect.toSeq.sortBy(_._1) shouldBe {
      Seq(
        1L -> 1L,
        2L -> 2L,
        3L -> 1L
      )
    }
  }

}
