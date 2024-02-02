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

package uk.gov.hmrc.platopsgithubproxy.controllers

import play.api.Logging
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.platopsgithubproxy.connector.GitHubConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class GitHubProxyController @Inject()(
  cc                   : ControllerComponents,
  gitHubgitHubConnector: GitHubConnector,
)(implicit
  ec                   : ExecutionContext
) extends BackendController(cc)
     with Logging {

  def githubRawUrl(repoName: String, path: String): Action[AnyContent] =
    Action.async { implicit request =>
      for {
        response <- gitHubgitHubConnector.getGithubRawContent(repoName, path, request.queryString)
      } yield response match {
        case Right(value)                           => Ok(value.body)
        case Left(value) if value.statusCode == 404 => logger.info(s"github-raw of $repoName with path $path returned ${value.statusCode}")
                                                       NotFound
        case Left(value)                            => logger.error(s"github-raw of $repoName with path $path returned ${value.statusCode}: ${value.message}")
                                                       InternalServerError
      }
    }

  def githubRestUrl(repoName: String, path: String): Action[AnyContent] =
    Action.async { implicit request =>
      for {
        response <- gitHubgitHubConnector.getGithubRestContent(repoName, path, request.queryString)
      } yield response match {
        case Right(value)                           => Ok(value.body)
        case Left(value) if value.statusCode == 404 => logger.info(s"github-rest of $repoName with path $path returned ${value.statusCode}")
                                                       NotFound
        case Left(value)                            => logger.error(s"github-rest of $repoName with path $path returned ${value.statusCode}: ${value.message}")
                                                       InternalServerError
      }
    }
}
