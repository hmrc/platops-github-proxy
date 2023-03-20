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

package uk.gov.hmrc.platopsgithubproxy.config

import com.typesafe.config.ConfigFactory
import org.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration

class GitHubConfigSpec extends AnyWordSpec with Matchers with MockitoSugar {

  "GitHubConfig" should {
    "parse config correctly" in {
      val githubConfig = new GitHubConfig(Configuration(
          "github.rest.api.url"    -> "https://api.github.com"
        , "github.open.api.rawurl" -> "http://localhost:8461/github/raw"
        , "github.open.api.token"  -> "token1"

        , "ratemetrics.githubtokens.1.user " -> "user1"
        , "ratemetrics.githubtokens.1.token" -> "token1"
        , "ratemetrics.githubtokens.2.user"  -> "user2"
        , "ratemetrics.githubtokens.2.token" -> "token2"
      ))

      githubConfig.restUrl shouldBe "https://api.github.com"
      githubConfig.rawUrl shouldBe "http://localhost:8461/github/raw"
      githubConfig.tokens shouldBe List("user1" -> "token1", "user2" -> "token2")
    }

    "infer token config from open api credentials" in {
      val config =
        ConfigFactory.parseString(
          f"""|
              |github.rest.api.url     = "https://api.github.com"
              |github.open.api.rawurl  = "http://localhost:8461/github/raw"
              |github.open.api.user    = user1
              |github.open.api.token   = token1
              |ratemetrics.githubtokens.1.user  = $${?github.open.api.user}
              |ratemetrics.githubtokens.1.token = $${?github.open.api.token}
            """.stripMargin
        ).resolve
      val githubConfig = new GitHubConfig(new Configuration(config))

      githubConfig.tokens shouldBe List("user1" -> "token1")
    }
  }
}

