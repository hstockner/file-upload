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

import akka.testkit.TestActorRef
import org.joda.time.DateTime
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.fileupload.actors.{ActorStub, EnvelopeManager, FileUploadTestActors, Actors}
import uk.gov.hmrc.fileupload.models.{Envelope, Constraints}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.{Await, ExecutionContext, Future}

class EnvelopeControllerSpec  extends UnitSpec with WithFakeApplication {

  import uk.gov.hmrc.fileupload.Support._

  import scala.concurrent.duration._
  import FileUploadTestActors._
  import scala.language.postfixOps
  import Envelope._

  implicit val ec = ExecutionContext.global
  override implicit val defaultTimeout = 500 milliseconds

  "create envelope with a request" should {
    "return response with OK status and a Location header specifying the envelope endpoint" in {
      val fakeRequest = FakeRequest("GET", "/")
      val requestWithBody = fakeRequest.withJsonBody( Json.parse("""
          |{
          |  "id": "0b215e97-11d4-4006-91db-c067e74fc653",
          |  "constraints": {
          |    "contentTypes": [
          |      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          |      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          |      "application/vnd.oasis.opendocument.spreadsheet"
          |    ],
          |    "maxItems": 100,
          |    "maxSize": "12GB",
          |    "maxSizePerItem": "10MB"
          |  },
          |  "callback": "http://absolute.callback.url",
          |  "expires": "2016-04-07T13:15:30Z",
          |  "metadata": {
          |    "anything": "the caller wants to add to the envelope"
          |  }
          |}
        """.stripMargin))

      val result = EnvelopeController.create()(requestWithBody)
      status(result) shouldBe Status.OK
      result.header.headers.get("Location") shouldEqual  Some("http://test.com/file-upload/envelope/0b215e97-11d4-4006-91db-c067e74fc653")
    }
  }

  "Get Envelope" should {
    "return an envelope resource when request id is valid" in {
      val id = BSONObjectID.generate
      val envelope = createEnvelope(id)
      val request = FakeRequest("GET", s"/envelope/${id.toString}")
      val envelopeMgr: ActorStub = FileUploadTestActors.envelopeMgr

      envelopeMgr.setReply(Some(envelope))


      val futureResult = EnvelopeController.show(id.toString)(request)

      val result = Await.result(futureResult, defaultTimeout)

      val actualRespone = Json.parse(consume(result.body))
      val expectedResponse = Json.toJson(envelope)

      result.header.status shouldBe Status.OK
      actualRespone shouldBe expectedResponse

    }
  }

}