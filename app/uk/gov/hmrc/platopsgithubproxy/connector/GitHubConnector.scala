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
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.platopsgithubproxy.config.GitHubConfig

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubConnector @Inject()(
  httpClientV2: HttpClientV2,
  githubConfig: GitHubConfig
)(implicit ec: ExecutionContext) {

  def getGithubRawContent(repoName: String, path: String, queryMap: Map[String, Seq[String]])
                         (implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, HttpResponse]] = {
    val baseUrl = s"${githubConfig.rawUrl}/hmrc"
    getFromGithub(baseUrl, repoName, path, queryMap)
  }

  def getGithubRestContent(repoName: String, path: String, queryMap: Map[String, Seq[String]])
                          (implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, HttpResponse]] = {
    val baseUrl = s"${githubConfig.restUrl}/repos/hmrc"
    getFromGithub(baseUrl, repoName, path, queryMap)
  }

  private def getFromGithub(baseUrl: String, repoName: String, path: String, queryMap: Map[String, Seq[String]])(implicit hc: HeaderCarrier) = {
    val url = s"$baseUrl/$repoName/$path${extractQueryParams(queryMap)}"
    httpClientV2
      .get(new URL(url))
      .setHeader("Authorization" -> s"token ${githubConfig.githubToken}")
      .withProxy
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
  }

  private def extractQueryParams(queryMap: Map[String, Seq[String]]): String = {
    if (queryMap.isEmpty) "" else
      "?" +
        queryMap
          .map(entry => entry._1 + "=" + entry._2.mkString(","))
          .mkString("&")
  }
}
