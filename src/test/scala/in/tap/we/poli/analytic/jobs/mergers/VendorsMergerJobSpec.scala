package in.tap.we.poli.analytic.jobs.mergers

import in.tap.base.spark.io.{Formats, In, Out}
import in.tap.base.spark.main.InArgs.TwoInArgs
import in.tap.base.spark.main.OutArgs.OneOutArgs
import in.tap.we.poli.analytic.jobs.BaseSparkJobSpec
import in.tap.we.poli.analytic.jobs.connectors.auto.VendorsAutoConnectorJobFixtures
import in.tap.we.poli.analytic.jobs.mergers.VendorsMergerJob.UniqueVendor
import in.tap.we.poli.analytic.jobs.mergers.VendorsMergerJob.UniqueVendor._
import org.scalatest.DoNotDiscover

@DoNotDiscover
class VendorsMergerJobSpec extends BaseSparkJobSpec with VendorsMergerJobFixtures {

  val resourcePath: String = {
    getClass.getResource("../../../../../../../mergers/vendors").toString
  }

  val in1Path: String = {
    s"$resourcePath/in1/"
  }

  val in2Path: String = {
    s"$resourcePath/in2/"
  }

  val outPath: String = {
    s"$resourcePath/mergers/vendors/out"
  }

  val _: Unit = {
    new VendorsMergerJob(
      TwoInArgs(In(path = in1Path, Formats.PARQUET), In(path = in2Path)),
      OneOutArgs(Out(path = outPath, Formats.PARQUET))
    ).execute()
  }

  it should "merge vendors from connector" in {
    object AutoConnectorMockEdges extends VendorsAutoConnectorJobFixtures
    import spark.implicits._
    spark.read.parquet(outPath).as[UniqueVendor].collect.toSeq.sortBy(_.uid) shouldBe {
      Seq(
        UniqueVendor(
          uid = 1L,
          uids = Seq(3L, 1L),
          name = "Vendor",
          names = Set("Vendor", "Vendor, Inc. # 1"),
          name_freq = Map(
            "Vendor" -> 1L,
            "Vendor, Inc. # 1" -> 1L
          ),
          address = address1,
          addresses = Set(address1),
          address_freq = Map(address1 -> 2L),
          num_merged = 2,
          memos = Set("memo1"),
          edges = Set(AutoConnectorMockEdges.edge3, AutoConnectorMockEdges.edge1)
        ),
        UniqueVendor(
          uid = 2L,
          uids = Seq(2L),
          name = "Vendor Two",
          names = Set("Vendor Two"),
          name_freq = Map("Vendor Two" -> 1L),
          address = address2,
          addresses = Set(address2),
          address_freq = Map(address2 -> 1L),
          num_merged = 1,
          memos = Set(),
          edges = Set(AutoConnectorMockEdges.edge2)
        )
      )
    }
  }

  it should "reduce two vendors found to be the same entity" in {
    reduce(fromVendor(vendor1), fromVendor(vendor2)) shouldBe {
      UniqueVendor(
        uid = 1L,
        uids = Seq(1L, 2L),
        name = "Vendor1",
        names = Set("Vendor1", "Vendor2"),
        name_freq = Map(
          "Vendor1" -> 1L,
          "Vendor2" -> 1L
        ),
        address = address1,
        addresses = Set(address1, address2),
        address_freq = Map(
          address2 -> 2L
        ),
        num_merged = 2,
        memos = Set("memo1"),
        edges = Set(edge1, edge2)
      )
    }
  }

}
