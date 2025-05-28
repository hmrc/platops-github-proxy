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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.util.ByteString
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, get, getRequestedFor, stubFor, urlEqualTo, urlPathEqualTo}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.platopsgithubproxy.config.GitHubConfig

import scala.concurrent.ExecutionContext.Implicits.global

class GitHubConnectorSpec
  extends AnyWordSpec
     with Matchers
     with WireMockSupport
     with HttpClientV2Support
     with ScalaFutures
     with IntegrationPatience:

  private given ActorSystem = ActorSystem()

  private val testToken = "test-token"

  private lazy val githubConnector: GitHubConnector =
    GitHubConnector(
      httpClientV2 = httpClientV2,
      githubConfig = GitHubConfig(Configuration(
        "github.rest.api.url"      -> wireMockUrl,
        "github.open.api.rawurl"   -> wireMockUrl,
        "github.open.api.token"    -> testToken,
        "ratemetrics.githubtokens" -> List(),
        "metrics.jvm"              -> false
      ))
    )

  given HeaderCarrier = HeaderCarrier()

  "getGithubRawContent" should:

    "return 200 with response body" in:
      stubFor(
        get(urlEqualTo("/hmrc/service-one/test?query=test"))
          .willReturn(aResponse().withStatus(200).withBody("Hello World"))
      )

      val response: Either[UpstreamErrorResponse, HttpResponse] =
        githubConnector
          .getGithubRawContent("service-one", "test", Map("query" -> Seq("test")))
          .futureValue

      response.isRight shouldBe true
      response.map(_.body) shouldBe Right("Hello World")
      response.map(_.status) shouldBe Right(200)

    "return 200 with response body when path is more than 1 level deep" in:
      stubFor(
        get(urlEqualTo("/hmrc/service-one/test/deeper/path?query=test"))
          .willReturn(aResponse().withStatus(200).withBody("Hello World"))
      )

      val response: Either[UpstreamErrorResponse, HttpResponse] =
        githubConnector
          .getGithubRawContent("service-one", "test/deeper/path", Map("query" -> Seq("test")))
          .futureValue

      response.isRight shouldBe true

    "return 200 when query param has multiple values" in:
      stubFor(
        get(urlEqualTo("/hmrc/service-one/test?pulls=open,closed"))
          .willReturn(aResponse().withStatus(200).withBody("Hello World"))
      )

      val response: Either[UpstreamErrorResponse, HttpResponse] =
        githubConnector
          .getGithubRawContent("service-one", "test", Map("pulls" -> Seq("open", "closed")))
          .futureValue

      response.isRight shouldBe true

    "return 200 when multiple query params" in:
      stubFor(
        get(urlEqualTo("/hmrc/service-one/test?pulls=open,closed&test=open"))
          .willReturn(aResponse().withStatus(200).withBody("Hello World"))
      )

      val response: Either[UpstreamErrorResponse, HttpResponse] =
        githubConnector
          .getGithubRawContent("service-one", "test", Map("pulls" -> Seq("open", "closed"), "test" -> Seq("open")))
          .futureValue

      response.isRight shouldBe true

    "return 404 when url not found in GitHub" in:
      stubFor(
        get(urlEqualTo("/hmrc/service-one/test?query=test"))
          .willReturn(aResponse().withStatus(404))
      )

      val response: Either[UpstreamErrorResponse, HttpResponse] =
        githubConnector
          .getGithubRawContent("service-one", "test", Map("query" -> Seq("test")))
          .futureValue

      response.isLeft shouldBe true

  "getGithubRestContent" should:

    "return 200 with response body" in:
      stubFor(
        get(urlEqualTo("/repos/hmrc/service-one/test?query=test"))
          .willReturn(aResponse().withStatus(200).withBody("Hello World"))
      )

      val response: Either[UpstreamErrorResponse, HttpResponse] =
        githubConnector
          .getGithubRestContent("service-one", "test", Map("query" -> Seq("test")))
          .futureValue

      response.isRight shouldBe true
      response.map(_.body) shouldBe Right("Hello World")
      response.map(_.status) shouldBe Right(200)

    "return 404 when url not found in GitHub" in:
      stubFor(
        get(urlEqualTo("/repos/hmrc/service-one/test?query=test"))
          .willReturn(aResponse().withStatus(404))
      )

      val response: Either[UpstreamErrorResponse, HttpResponse] =
        githubConnector
          .getGithubRestContent("service-one", "test", Map("query" -> Seq("test")))
          .futureValue

      response.isLeft shouldBe true

    "return 401 when unauthenticated" in:
      stubFor(
        get(urlEqualTo("/repos/hmrc/service-one/test?query=test"))
          .willReturn(aResponse().withStatus(401))
      )

      val response: Either[UpstreamErrorResponse, HttpResponse] =
        githubConnector
          .getGithubRestContent("service-one", "test", Map("query" -> Seq("test")))
          .futureValue

      response.isLeft shouldBe true

  "getGithubZip" should:

    "return 200 with streaming response body" in:
      val expectedBytes = "zip-content-bytes".getBytes("UTF-8")
      stubFor(
        get(urlEqualTo("/repos/hmrc/service-one/zipball/HEAD"))
          .willReturn(aResponse().withStatus(200).withBody(expectedBytes))
      )

      val response = githubConnector
        .getGithubZip("service-one")
        .futureValue

      response.isRight shouldBe true

      val actualBytes = response match
          case Right(source) => 
            source
              .runFold(ByteString.empty)(_ ++ _)
              .futureValue
              .toArray
          case Left(_) => fail("Expected Right but got Left")

      actualBytes shouldBe expectedBytes

    "return 404 when repository not found in GitHub" in:
      stubFor(
        get(urlEqualTo("/repos/hmrc/service-one/zipball/HEAD"))
          .willReturn(aResponse().withStatus(404))
      )

      val response = githubConnector
        .getGithubZip("service-one")
        .futureValue

      response.isLeft shouldBe true

    "return 401 when unauthenticated" in:
      stubFor(
        get(urlEqualTo("/repos/hmrc/service-one/zipball/HEAD"))
          .willReturn(aResponse().withStatus(401))
      )

      val response = githubConnector
        .getGithubZip("service-one")
        .futureValue

      response.isLeft shouldBe true

  "getRateLimitMetrics" should:

    "return rate limit metrics" in:
      stubFor(
        get(urlPathEqualTo("/rate_limit"))
          .willReturn(
            aResponse()
              .withBody(
                """
                  {
                    "resources": {
                      "core": {
                        "limit": 100,
                        "used": 0,
                        "remaining": 100,
                        "reset": 1644407813
                      },
                      "search": {
                        "limit": 30,
                        "used": 0,
                        "remaining": 30,
                        "reset": 1644404273
                      },
                      "graphql": {
                        "limit": 200,
                        "used": 0,
                        "remaining": 200,
                        "reset": 1644407813
                      },
                      "integration_manifest": {
                        "limit": 5000,
                        "used": 0,
                        "remaining": 5000,
                        "reset": 1644407813
                      },
                      "source_import": {
                        "limit": 100,
                        "used": 0,
                        "remaining": 100,
                        "reset": 1644404273
                      },
                      "code_scanning_upload": {
                        "limit": 500,
                        "used": 0,
                        "remaining": 500,
                        "reset": 1644407813
                      },
                      "actions_runner_registration": {
                        "limit": 10000,
                        "used": 0,
                        "remaining": 10000,
                        "reset": 1644407813
                      },
                      "scim": {
                        "limit": 15000,
                        "used": 0,
                        "remaining": 15000,
                        "reset": 1644407813
                      }
                    },
                    "rate": {
                      "limit": 5000,
                      "used": 0,
                      "remaining": 5000,
                      "reset": 1644407813
                    }
                  }
                """
              )
          )
      )

      import RateLimitMetrics.Resource._

      githubConnector.getRateLimitMetrics(testToken, Core).futureValue shouldBe RateLimitMetrics(
        limit = 100,
        remaining = 100,
        reset = 1644407813
      )

      githubConnector.getRateLimitMetrics(testToken, GraphQl).futureValue shouldBe RateLimitMetrics(
        limit = 200,
        remaining = 200,
        reset = 1644407813
      )

      wireMockServer.verify(
        2,
        getRequestedFor(urlPathEqualTo("/rate_limit"))
          .withHeader("Authorization", equalTo(s"token $testToken"))
      )
