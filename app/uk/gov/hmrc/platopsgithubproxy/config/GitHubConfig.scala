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

import javax.inject.{Inject, Singleton}
import com.typesafe.config.{ConfigList, ConfigObject}
import play.api.Configuration

import scala.jdk.CollectionConverters._

@Singleton
class GitHubConfig @Inject()(configuration: Configuration):
  val restUrl: String     = configuration.get[String]("github.rest.api.url")
  val rawUrl: String      = configuration.get[String]("github.open.api.rawurl")
  val githubToken: String = configuration.get[String]("github.open.api.token")

  val tokens: List[(String, String)] =
    configuration.get[ConfigList]("ratemetrics.githubtokens").asScala.toList
      .map(cv => Configuration(cv.asInstanceOf[ConfigObject].toConfig))
      .flatMap: config =>
        for
          username <- config.getOptional[String]("user")
          token    <- config.getOptional[String]("token")
        yield (username, token)
