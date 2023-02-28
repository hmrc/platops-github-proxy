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

import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.platopsgithubproxy.config.GitHubConfig
import uk.gov.hmrc.platopsgithubproxy.connector.GitHubConnector.extractQueryParams

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubConnector @Inject()(
  httpClientV2: HttpClientV2,
  githubConfig: GitHubConfig
)(implicit ec: ExecutionContext) {

  def getGithubRawContent(repoName: String, path: String, queryMap: Map[String, Seq[String]])
                         (implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, HttpResponse]] = {
    val url = url"${githubConfig.rawUrl}/hmrc/$repoName/$path?${extractQueryParams(queryMap)}"
    httpClientV2
      .get(url)
      .setHeader("Authorization" -> s"token ${githubConfig.githubToken}")
      .withProxy
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
  }

  def getGithubRestContent(repoName: String, path: String, queryMap: Map[String, Seq[String]])
                          (implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, HttpResponse]] = {
    val url = url"${githubConfig.restUrl}/repos/hmrc/$repoName/$path?${extractQueryParams(queryMap)}"
    httpClientV2
      .get(url)
      .setHeader("Authorization" -> s"token ${githubConfig.githubToken}")
      .withProxy
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
  }
}

object GitHubConnector {
  def extractQueryParams(queryMap: Map[String, Seq[String]]): Map[String, String] =
    queryMap.map(entry => (entry._1, entry._2.mkString(",")))
}