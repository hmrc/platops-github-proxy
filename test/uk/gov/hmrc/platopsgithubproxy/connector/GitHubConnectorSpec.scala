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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.platopsgithubproxy.config.GitHubConfig

import scala.concurrent.ExecutionContext.Implicits.global

class GitHubConnectorSpec extends AnyWordSpec with Matchers with WireMockSupport with HttpClientV2Support {

  private val testToken = "test-token"

  private lazy val githubConnector =
    new GitHubConnector(
      httpClientV2 = httpClientV2,
      githubConfig = new GitHubConfig(Configuration(
        "github.rest.api.url"    -> wireMockUrl,
        "github.open.api.rawurl" -> wireMockUrl,
        "github.open.api.token"  -> testToken
      ))
    )

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "getGithubRawContent" should {

    "return 200 with response body" in {
      stubFor(
        get(urlEqualTo("/hmrc/service-one/test?query=test"))
          .willReturn(aResponse().withStatus(200).withBody("Hello World"))
      )

      val response = githubConnector
        .getGithubRawContent("service-one", "test", Map("query" -> Seq("test")))
        .futureValue

      response.isRight shouldBe true
      response.map(_.body) shouldBe Right("Hello World")
      response.map(_.status) shouldBe Right(200)
    }

    "return 404 when url not found in GitHub" in {
      stubFor(
        get(urlEqualTo("/hmrc/service-one/test?query=test"))
          .willReturn(aResponse().withStatus(404))
      )

      val response = githubConnector
        .getGithubRawContent("service-one", "test", Map("query" -> Seq("test")))
        .futureValue

      response.isLeft shouldBe true
    }
  }

  "getGithubRestContent" should {

    "return 200 with response body" in {
      stubFor(
        get(urlEqualTo("/repos/hmrc/service-one/test?query=test"))
          .willReturn(aResponse().withStatus(200).withBody("Hello World"))
      )

      val response = githubConnector
        .getGithubRestContent("service-one", "test", Map("query" -> Seq("test")))
        .futureValue

      response.isRight shouldBe true
      response.map(_.body) shouldBe Right("Hello World")
      response.map(_.status) shouldBe Right(200)
    }

    "return 404 when url not found in GitHub" in {
      stubFor(
        get(urlEqualTo("/repos/hmrc/service-one/test?query=test"))
          .willReturn(aResponse().withStatus(404))
      )

      val response = githubConnector
        .getGithubRestContent("service-one", "test", Map("query" -> Seq("test")))
        .futureValue

      response.isLeft shouldBe true
    }

    "return 401 when unauthenticated" in {
      stubFor(
        get(urlEqualTo("/repos/hmrc/service-one/test?query=test"))
          .willReturn(aResponse().withStatus(401))
      )

      val response = githubConnector
        .getGithubRestContent("service-one", "test", Map("query" -> Seq("test")))
        .futureValue

      response.isLeft shouldBe true
    }
  }

  "extractQueryParams" should {

    "extract params with single key and value" in {
      val queryParams = Map("pulls" -> Seq("open"))
      GitHubConnector.extractQueryParams(queryParams) shouldBe Map("pulls" -> "open")
    }

    "extract params with single key and multiple values" in {
      val queryParams = Map("pulls" -> Seq("open", "closed"))
      GitHubConnector.extractQueryParams(queryParams) shouldBe Map("pulls" -> "open,closed")
    }

    "extract params with multiple keys and multiple values" in {
      val queryParams = Map("pulls" -> Seq("open", "closed"), "test" -> Seq("open"))
      GitHubConnector.extractQueryParams(queryParams) shouldBe Map("pulls" -> "open,closed", "test" -> "open")
    }

    "extract no params when map is empty" in {
      val queryParams = Map.empty[String, Seq[String]]
      GitHubConnector.extractQueryParams(queryParams) shouldBe Map.empty
    }

    "comma separated query params aren't encoded" in {
      val queryParams = GitHubConnector.extractQueryParams(Map("pulls" -> Seq("open", "closed")))
      url"http://localhost/x?$queryParams".toString shouldBe "http://localhost/x?pulls=open,closed"

    }
  }

}
