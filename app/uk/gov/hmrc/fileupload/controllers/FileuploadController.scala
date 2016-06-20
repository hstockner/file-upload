/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.fileupload.controllers

import akka.actor.ActorRef
import akka.util.Timeout
import play.api.mvc._
import uk.gov.hmrc.fileupload.actors.EnvelopeService.UpdateEnvelope
import uk.gov.hmrc.fileupload.actors.{Actors, FileUploader}
import uk.gov.hmrc.play.microservice.controller.BaseController
import akka.pattern._
import uk.gov.hmrc.fileupload.actors.Implicits.FutureUtil
import scala.language.postfixOps
import scala.concurrent.duration._


object FileuploadController extends BaseController{

	implicit val system = Actors.actorSystem
	implicit val ec = system.dispatcher
	implicit val timeout = Timeout(2 seconds)
	val envelopeService = Actors.envelopeService

	def upload(envelopeId: String, fileId: String) = Action.async(FileUploader.parseBody(envelopeId, fileId)){ request =>
		(envelopeService ? UpdateEnvelope(envelopeId, fileId))
		  .breakOnFailure
		  .map {
				case true => Ok
				case false => NotFound
	    }.recover { case e =>  ExceptionHandler(e) }
  }

}
