package in.tap.we.poli.analytic.jobs.connectors.fuzzy.features

import in.tap.base.spark.jobs.composite.TwoInOneOutJob
import in.tap.base.spark.main.InArgs.TwoInArgs
import in.tap.base.spark.main.OutArgs.OneOutArgs
import in.tap.we.poli.analytic.jobs.connectors.cleanedNameTokens
import in.tap.we.poli.analytic.jobs.connectors.fuzzy.VendorsFuzzyConnectorJob.CandidateGenerator
import in.tap.we.poli.analytic.jobs.connectors.fuzzy.features.VendorsFuzzyConnectorFeaturesJob.{
  buildSamplingRatio, CandidateReducer, Comparator, Comparison, Features
}
import in.tap.we.poli.analytic.jobs.connectors.fuzzy.transfomer.IdResVendorTransformerJob.IdResVendor
import org.apache.spark.graphx.VertexId
import org.apache.spark.rdd.{PairRDDFunctions, RDD}
import org.apache.spark.sql.{Dataset, SparkSession}

import scala.reflect.ClassTag
import scala.reflect.runtime.universe

// TODO: fix typo in parent class
class VendorsFuzzyConnectorFeaturesJob(val inArgs: TwoInArgs, val outArgs: OneOutArgs)(
  implicit
  val spark: SparkSession,
  val readTypeTagA: universe.TypeTag[IdResVendor],
  val readTypeTagB: universe.TypeTag[(VertexId, VertexId)],
  val writeTypeTagA: universe.TypeTag[(Long, Features)]
) extends TwoInOneOutJob[IdResVendor, (VertexId, VertexId), (Long, Features)](inArgs, outArgs) {

  override def transform(input: (Dataset[IdResVendor], Dataset[(VertexId, VertexId)])): Dataset[(Long, Features)] = {
    import spark.implicits._
    val (vendors, connector) = {
      input
    }
    val positives: RDD[Comparison] = {
      val join: RDD[(VertexId, IdResVendor)] = {
        vendors
          .map { vendor: IdResVendor =>
            vendor.uid -> vendor
          }
          .rdd
          .join {
            connector.rdd
          }
          .map {
            case (_, (vendor: IdResVendor, connectedId: VertexId)) =>
              (connectedId, vendor)
          }
      }
      CandidateReducer(join).flatMap { maybe =>
        Comparison(maybe.toList.flatten.map(Comparator))
      }
    }
    // TODO: CC ?
    val negatives: RDD[Comparison] = {
      CandidateGenerator(vendors)
    }
    val numPositives: Double = {
      positives.count.toDouble
    }
    val numNegatives: Double = {
      negatives.count.toDouble
    }
    positives
      .map { comparison: Comparison =>
        1L -> comparison.features
      }
      .sample(
        withReplacement = false,
        fraction = buildSamplingRatio(numPositives, numNegatives)
      )
      .union(
        negatives.map { comparison: Comparison =>
          0L -> comparison.features
        }
      )
      .toDS
  }

}

object VendorsFuzzyConnectorFeaturesJob {

  val POS_TO_NEG_RATIO: Double = {
    1.0
  }

  val MAX_COMPARISON_SIZE: Int = {
    100
  }

  def buildSamplingRatio(numPositives: Double, numNegatives: Double): Double = {
    (numNegatives * POS_TO_NEG_RATIO) / numPositives
  }

  object CandidateReducer {

    def apply[A: ClassTag, B](rdd: RDD[(A, B)])(
      implicit spark: SparkSession
    ): RDD[Option[List[B]]] = {
      val pair: PairRDDFunctions[A, Option[List[B]]] = {
        new PairRDDFunctions[A, Option[List[B]]](
          rdd
            .map {
              case (key, value) =>
                key -> Option(List(value))
            }
        )
      }
      pair
        .reduceByKey(reduce)
        .map {
          case (_, candidates) =>
            candidates
        }
    }

    private def reduce[A](left: Option[List[A]], right: Option[List[A]]): Option[List[A]] = {
      (left, right) match {
        case (Some(l), Some(r)) =>
          if ((l.size + r.size) <= MAX_COMPARISON_SIZE) {
            Some(l ++ r)
          } else {
            None
          }
        case _ => None
      }
    }

  }

  // TODO: scale numEdgesInCommon
  // TODO: 0, 1, more than 1 -> categorical
  final case class Features(
    numTokens: Double,
    numTokensInCommon: Double,
    sameSrcId: Double,
    sameCity: Double,
    sameState: Double
  ) {

    def toArray: Array[Double] = {
      Array(
        numTokens,
        numTokensInCommon,
        sameSrcId,
        sameCity,
        sameState
      )
    }

  }

  object Features {

    // TODO: delete ?
    def scale(raw: Double): Double = {
      raw match {
        case 0 => 0
        case 1 => 1
        case _ => 2
      }
    }

  }

  final case class Comparator(
    vendor: IdResVendor
  ) {

    val nameTokens: Set[String] = {
      cleanedNameTokens(vendor.name).toSet
    }

    val addressTokens: Set[String] = {
      Set(vendor.address.city, vendor.address.zip_code).flatten
    }

    val cgTokens: Set[String] = {
      nameTokens ++ addressTokens
    }

  }

  final case class Comparison(
    left_side: Comparator,
    right_side: Comparator
  ) {

    lazy val features: Features = {
      Features(
        numTokens = numTokens,
        numTokensInCommon = numTokensInCommon,
        sameSrcId = toDouble(sameSrcId),
        sameCity = toDouble(sameCity),
        sameState = toDouble(sameState)
      )
    }

    private lazy val numTokens: Double = {
      Seq(left_side.nameTokens.size, right_side.nameTokens.size).max.toDouble
    }

    // TODO: Validate for city/state
    private lazy val numTokensInCommon: Double = {
      left_side.nameTokens.intersect(right_side.nameTokens).size.toDouble
    }

    private lazy val sameSrcId: Boolean = {
      left_side.vendor.src_id.equals(right_side.vendor.src_id)
    }

    private lazy val sameCity: Boolean = {
      same(_.address.city)
    }

    private lazy val sameState: Boolean = {
      same(_.address.state)
    }

    private def toDouble(bool: Boolean): Double = {
      bool.compare(false)
    }

    private def same(f: IdResVendor => Option[String]): Boolean = {
      (f(left_side.vendor), f(right_side.vendor)) match {
        case (Some(left), Some(right)) =>
          left.equals(right)
        case _ =>
          false
      }
    }

  }

  object Comparison {

    def apply(list: List[Comparator]): List[Comparison] = {
      combinations(list).map {
        case (left, right) =>
          Comparison(
            left_side = left,
            right_side = right
          )
      }
    }

    private def combinations[A](list: List[A]): List[(A, A)] = {
      list.combinations(n = 2).toList.flatMap { combination: Seq[A] =>
        combination match {
          case left :: right :: Nil =>
            Some((left, right))
          case _ =>
            None
        }
      }
    }
  }

}
