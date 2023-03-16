/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.platopsgithubproxy.scheduler

import akka.actor.ActorSystem
import cats.implicits._
import com.kenshoo.play.metrics.Metrics
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}
import uk.gov.hmrc.mongo.metrix.{MetricOrchestrator, MetricSource, MongoMetricRepository}
import uk.gov.hmrc.platopsgithubproxy.config.{GitHubConfig, SchedulerConfigs}
import uk.gov.hmrc.platopsgithubproxy.connector.GitHubConnector
import uk.gov.hmrc.platopsgithubproxy.helpers.SchedulerUtils

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}



@Singleton
class GitHubRateLimitMetricsScheduler @Inject()(
  gitHubConnector: GitHubConnector,
  schedulerConfig: SchedulerConfigs,
  gitHubConfig   : GitHubConfig,
  mongoLock      : MongoLockRepository,
  metrics        : Metrics,
  mongoComponent : MongoComponent
)(implicit
  actorSystem         : ActorSystem,
  applicationLifecycle: ApplicationLifecycle,
  ec                  : ExecutionContext
) extends SchedulerUtils {

  final val metrixLock: LockService = LockService(mongoLock, "metrix-lock" , 20.minutes)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val metricsDefinitions: Map[String, () => Future[Int]] = {
    import uk.gov.hmrc.platopsgithubproxy.connector.RateLimitMetrics.Resource._

    gitHubConfig.tokens
      .flatMap { case (username, token) =>
        List(
          s"github.token.$username.api.rate.remaining" -> { () =>
            gitHubConnector.getRateLimitMetrics(token, Core).map(_.remaining)
          },
          s"github.token.$username.graphql.rate.remaining" -> { () =>
            gitHubConnector.getRateLimitMetrics(token, GraphQl).map(_.remaining)
          },
        )
      }.toMap
  }

  val source: MetricSource =
    new MetricSource {
      def metrics(implicit ec: ExecutionContext): Future[Map[String, Int]] =
        metricsDefinitions.toList.traverse { case (k, f) => f().map(i => (k, i)) }.map(_.toMap)
    }

  val metricOrchestrator = new MetricOrchestrator(
    metricSources    = List(source),
    lockService      = metrixLock,
    metricRepository = new MongoMetricRepository(mongoComponent),
    metricRegistry   = metrics.defaultRegistry
  )

  schedule("Github Ratelimit metrics", schedulerConfig.metrixScheduler) {
    metricOrchestrator
      .attemptMetricRefresh()
      .map(_ => ())
  }
}
