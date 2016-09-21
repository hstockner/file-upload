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

package uk.gov.hmrc.fileupload.file.zip

import java.util.UUID

import cats.data.Xor
import play.api.libs.iteratee.Enumerator
import uk.gov.hmrc.fileupload.file.zip.Utils.Bytes
import uk.gov.hmrc.fileupload.file.zip.ZipStream.{ZipFileInfo, ZipStreamEnumerator}
import uk.gov.hmrc.fileupload.read.envelope.Envelope
import uk.gov.hmrc.fileupload.read.envelope.Service.{FindEnvelopeNotFoundError, FindResult}
import uk.gov.hmrc.fileupload.read.file.Service.{FileFound, GetFileResult}
import uk.gov.hmrc.fileupload.{EnvelopeId, FileId}

import scala.concurrent.{ExecutionContext, Future}

object Zippy {

  type ZipResult = Xor[ZipEnvelopeError, Enumerator[Bytes]]
  sealed trait ZipEnvelopeError
  case object EnvelopeNotFoundError extends ZipEnvelopeError
  case object EmptyEnvelopeError extends ZipEnvelopeError


  def zipEnvelope(getEnvelope: (EnvelopeId) => Future[FindResult], retrieveFile: (Envelope, FileId) => Future[GetFileResult])
                 (envelopeId: EnvelopeId)
                 (implicit ex: ExecutionContext): Future[ZipResult] = {

    getEnvelope(envelopeId) map {
      case Xor.Right(e@Envelope(_, _, _, _, _, _, Some(files), _, _)) =>
        val mainFolderPath = envelopeId.toString
        val envelopeFolderPath = s"$mainFolderPath/${envelopeId.toString}"
        val mainFolder = ZipFileInfo(mainFolderPath, isDir = true, new java.util.Date(), None)
        val envelopeFolder = ZipFileInfo(envelopeFolderPath, isDir = true, new java.util.Date(), None)

        val zipFiles = files.collect {
          case f =>
            val fileName: String = f.name.getOrElse(UUID.randomUUID().toString)
            ZipFileInfo(s"$envelopeFolderPath/$fileName", isDir = false, new java.util.Date(), Some(() => retrieveFile(e, f.fileId).map {
              case Xor.Right(FileFound(name, length, data)) => data
              case Xor.Left(error) => throw new Exception(s"File $envelopeId ${f.fileId} not found in repo" )
            }))
        }
        val infoes: Seq[ZipFileInfo] = mainFolder+:envelopeFolder+:zipFiles
        Xor.right( ZipStreamEnumerator(infoes))

      case Xor.Left(FindEnvelopeNotFoundError) => Xor.left(EnvelopeNotFoundError)
      case Xor.Right(e@Envelope(_, _, _, _, _, _, None, _, _)) => Xor.left(EmptyEnvelopeError)
    }

  }

}