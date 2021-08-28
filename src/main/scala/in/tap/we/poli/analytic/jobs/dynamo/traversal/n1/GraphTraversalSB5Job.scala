package in.tap.we.poli.analytic.jobs.dynamo.traversal.n1

import in.tap.base.spark.main.InArgs.OneInArgs
import in.tap.base.spark.main.OutArgs.OneOutArgs
import in.tap.we.poli.analytic.jobs.dynamo.traversal.nx.NxTraversalBuilder.N1TraversalBuilder
import org.apache.spark.sql.SparkSession

/**
 * Paginated Graph Traversals
 * Sorted By => Maximum Spend.
 */
class GraphTraversalSB5Job(
  override val inArgs: OneInArgs,
  override val outArgs: OneOutArgs
)(
  implicit
  override val spark: SparkSession
) extends GraphTraversalJob(
      inArgs,
      outArgs,
      _.max_spend,
      N1TraversalBuilder
    )
