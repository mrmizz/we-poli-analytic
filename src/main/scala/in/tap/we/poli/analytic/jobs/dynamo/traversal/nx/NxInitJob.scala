package in.tap.we.poli.analytic.jobs.dynamo.traversal.nx

import in.tap.base.spark.jobs.composite.TwoInOneOutJob
import in.tap.base.spark.main.InArgs.TwoInArgs
import in.tap.base.spark.main.OutArgs.OneOutArgs
import in.tap.we.poli.analytic.jobs.dynamo.traversal.nx.InitJob.DstId
import in.tap.we.poli.analytic.jobs.graph.edges.CommitteeToVendorEdgeJob.Analytics
import org.apache.spark.graphx.VertexId
import org.apache.spark.sql.{Dataset, Encoder, Encoders, SparkSession}

import scala.reflect.runtime.universe

class NxInitJob[A <: NxKey, B <: NxKey](
  val inArgs: TwoInArgs,
  val outArgs: OneOutArgs,
  val f: (A, VertexId) => Option[B]
)(
  implicit
  val spark: SparkSession,
  val readTypeTagA: universe.TypeTag[(VertexId, DstId)],
  val readTypeTagB: universe.TypeTag[(A, DstId.WithCount)],
  val readTypeTagC: universe.TypeTag[(VertexId, (Analytics, A))],
  val writeTypeTagA: universe.TypeTag[(B, DstId.WithCount)]
) extends TwoInOneOutJob[(VertexId, DstId), (A, DstId.WithCount), (B, DstId.WithCount)](inArgs, outArgs) {

  override def transform(
    input: (Dataset[(VertexId, DstId)], Dataset[(A, DstId.WithCount)])
  ): Dataset[(B, DstId.WithCount)] = {
    import spark.implicits._
    val fBroadcast = {
      spark.sparkContext.broadcast(f)
    }
    val (init, nxInit) = {
      input
    }
    val encoder: Encoder[(VertexId, (Analytics, A))] = {
      Encoders.product(readTypeTagC)
    }
    init
      .rdd
      .join {
        nxInit.flatMap { tup: (A, DstId.WithCount) =>
          NxInitJob.apply[A](tup)
        }(encoder).rdd
      }
      .flatMap {
        case (srcId: VertexId, (dstId: DstId, (analytics: Analytics, a))) =>
          NxInitJob.apply[A, B](fBroadcast.value)(srcId)(dstId, analytics, a)
      }
      .reduceByKey {
        NxInitJob.reduce[B]
      }
      .map {
        _._2
      }
      .toDS
  }

}

object NxInitJob {

  private val N: Int = {
    100
  }

  def apply[A <: NxKey](tup: (A, DstId.WithCount)): Seq[(VertexId, (Analytics, A))] = {
    if (tup._2.count <= N) {
      tup._2.dst_ids.map { dstId: DstId =>
        (dstId.dst_id, (dstId.analytics, tup._1))
      }
    } else {
      Seq.empty
    }
  }

  def apply[A <: NxKey, B <: NxKey](
    f: (A, VertexId) => Option[B]
  )(
    srcId: VertexId
  )(dstId: DstId, analytics: Analytics, a: A): Option[(String, (B, DstId.WithCount))] = {
    val maybeB: Option[B] = {
      f(a, dstId.dst_id)
    }
    maybeB.map { b =>
      b.key -> {
        (
          b,
          DstId.WithCount(
            Seq(
              DstId(
                srcId,
                reduce(analytics, dstId.analytics)
              )
            ),
            1L
          )
        )
      }
    }
  }

  def reduce[A <: NxKey](left: (A, DstId.WithCount), right: (A, DstId.WithCount)): (A, DstId.WithCount) = {
    (left._1, DstId.WithCount.reduce(left._2, right._2))
  }

  private def reduce(left: Analytics, right: Analytics): Analytics = {
    Analytics(
      num_edges = left.num_edges + right.num_edges,
      total_spend = reduce(left.total_spend, right.total_spend),
      avg_spend = reduce(left.avg_spend, right.avg_spend),
      min_spend = reduce(left.min_spend, right.min_spend),
      max_spend = reduce(left.max_spend, right.max_spend)
    )
  }

  private def reduce(left: Option[Double], right: Option[Double]): Option[Double] = {
    (left, right) match {
      case (Some(l), Some(r)) => Some(l + r)
      case _                  => None
    }
  }

}
