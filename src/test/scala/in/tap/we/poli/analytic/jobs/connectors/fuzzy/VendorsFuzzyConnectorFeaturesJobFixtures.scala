package in.tap.we.poli.analytic.jobs.connectors.fuzzy

import in.tap.we.poli.analytic.jobs.graph.edges.CommitteeToVendorEdgeJob.ExpenditureEdge
import in.tap.we.poli.analytic.jobs.mergers.VendorsMergerJob.UniqueVendor
import in.tap.we.poli.analytic.jobs.transformers.VendorsTransformerJob.Vendor

trait VendorsFuzzyConnectorFeaturesJobFixtures {

  val vendor1: Vendor = {
    emptyVendor.copy(
      uid = 1L,
      name = "Vendor1",
      city = Some("Los Angeles"),
      state = Some("CA"),
      zip_code = Some("90026"),
      memo = None,
      edge = emptyEdge.copy(src_id = -111L)
    )
  }

  val uniqueVendor1: UniqueVendor = {
    UniqueVendor(
      uid = 1L,
      uids = Nil,
      name = "Vendor1",
      names = Set.empty,
      city = Some("Los Angeles"),
      state = Some("CA"),
      zip_code = Some("90026"),
      memos = Set.empty,
      edges = edgesInCommon,
      num_merged = 0
    )
  }

  val vendor2: Vendor = {
    emptyVendor.copy(
      uid = 2L,
      name = "Vendor2",
      edge = emptyEdge.copy(src_id = 22L)
    )
  }

  val uniqueVendor2: UniqueVendor = {
    emptyUniqueVendor.copy(
      uid = 2L,
      name = "Vendor2",
      edges = edgesInCommon
    )
  }

  val uniqueVendor3: UniqueVendor = {
    emptyUniqueVendor.copy(
      uid = 3L,
      name = "Vendor3"
    )
  }

  val vendor3: Vendor = {
    emptyVendor.copy(
      uid = 3L,
      name = "Vendor3",
      edge = emptyEdge.copy(src_id = 33L)
    )
  }

  lazy val emptyVendor: Vendor = {
    Vendor(
      uid = -1L,
      name = "",
      city = None,
      state = None,
      zip_code = None,
      memo = None,
      edge = emptyEdge.copy(src_id = -111L)
    )
  }

  lazy val emptyUniqueVendor: UniqueVendor = {
    UniqueVendor(
      uid = -1L,
      uids = Nil,
      name = "",
      names = Set.empty,
      city = None,
      state = None,
      zip_code = None,
      memos = Set.empty,
      edges = Set.empty,
      num_merged = 0
    )
  }

  lazy val emptyEdge: ExpenditureEdge = {
    ExpenditureEdge(
      src_id = -111L,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None
    )
  }

  lazy val edgesInCommon: Set[ExpenditureEdge] = {
    Set(emptyEdge.copy(src_id = 11L), emptyEdge.copy(src_id = 22L), emptyEdge.copy(src_id = 33L))
  }

}
