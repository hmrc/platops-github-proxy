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

package uk.gov.hmrc.platopsgithubproxy.connector

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{Reads, __}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.platopsgithubproxy.config.GitHubConfig

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubConnector @Inject()(
  httpClientV2: HttpClientV2,
  githubConfig: GitHubConfig
)(using ExecutionContext):

  def getGithubRawContent(
    repoName: String,
    path    : String,
    queryMap: Map[String, Seq[String]]
  )(using HeaderCarrier
  ): Future[Either[UpstreamErrorResponse, HttpResponse]] =
    getFromGithub(s"${githubConfig.rawUrl}/hmrc", repoName, path, queryMap)

  def getGithubRestContent(
    repoName: String,
    path    : String,
    queryMap: Map[String, Seq[String]]
  )(using HeaderCarrier
  ): Future[Either[UpstreamErrorResponse, HttpResponse]] =
    getFromGithub(s"${githubConfig.restUrl}/repos/hmrc", repoName, path, queryMap)

  private def getFromGithub(
    baseUrl : String,
    repoName: String,
    path    : String,
    queryMap: Map[String, Seq[String]]
  )(using HeaderCarrier
  ): Future[Either[UpstreamErrorResponse, HttpResponse]] =
    httpClientV2
      .get(URL(s"$baseUrl/$repoName/$path${extractQueryParams(queryMap)}")) // Not using url interpolator since it doesn't escape the query params correctly
      .setHeader("Authorization" -> s"token ${githubConfig.githubToken}")
      .withProxy
      .execute[Either[UpstreamErrorResponse, HttpResponse]]

  private def extractQueryParams(queryMap: Map[String, Seq[String]]): String =
    if queryMap.isEmpty then ""
    else
      queryMap
        .map(entry => entry._1 + "=" + entry._2.mkString(","))
        .mkString("?", "&", "")

  def getRateLimitMetrics(token: String, resource: RateLimitMetrics.Resource)(using HeaderCarrier): Future[RateLimitMetrics] =
    given Reads[RateLimitMetrics] = RateLimitMetrics.reads(resource)
    httpClientV2
      .get(url"${githubConfig.restUrl}/rate_limit")
      .setHeader("Authorization" -> s"token $token")
      .withProxy
      .execute[RateLimitMetrics]

case class RateLimitMetrics(
  limit    : Int,
  remaining: Int,
  reset    : Int
)

object RateLimitMetrics:

  enum Resource:
    case Core, GraphQl

    def asString: String =
      this match
        case Core    => "core"
        case GraphQl => "graphql"
    
  def reads(resource: Resource): Reads[RateLimitMetrics] =
    Reads.at(__ \ "resources" \ resource.asString)(
      ( (__ \ "limit"    ).read[Int]
      ~ (__ \ "remaining").read[Int]
      ~ (__ \ "reset"    ).read[Int]
      )(RateLimitMetrics.apply _)
    )
