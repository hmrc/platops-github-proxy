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
import play.api.Configuration

import scala.concurrent.duration.FiniteDuration

case class SchedulerConfig(
  enabledKey  : String
, enabled     : Boolean
, interval    : FiniteDuration
, initialDelay: FiniteDuration
)

object SchedulerConfig:
  def apply(
    configuration  : Configuration
  , enabledKey     : String
  , intervalKey    : String
  , initialDelayKey: String
  ): SchedulerConfig =
    SchedulerConfig(
        enabledKey
      , enabled      = configuration.get[Boolean](enabledKey)
      , interval     = configuration.get[FiniteDuration](intervalKey)
      , initialDelay = configuration.get[FiniteDuration](initialDelayKey)
      )

@Singleton
class SchedulerConfigs @Inject()(configuration: Configuration):

  val metrixScheduler: SchedulerConfig =
    SchedulerConfig(
        configuration
      , enabledKey      = "scheduler.metrix.enabled"
      , intervalKey     = "scheduler.metrix.interval"
      , initialDelayKey = "scheduler.metrix.initialDelay"
    )
