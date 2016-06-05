package uk.gov.hmrc.fileupload.actors

import akka.testkit.TestActors
import org.joda.time.DateTime
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.fileupload.Support
import uk.gov.hmrc.fileupload.models.{Envelope, Constraints}
import scala.concurrent.duration._

/**
  * Created by Josiah on 6/4/2016.
  */
class StorageSpec extends ActorSpec{

  import scala.language.postfixOps
  import Support._
  import EnvelopeStorage._
  val storage = system.actorOf(EnvelopeStorage.props(envelopRepositoryStub))

  "A storage" should{
    "Respond with an Envelop when it receives a find by id message" in {
      within(500 millis){
        val optEnvelope = Some(createEnvelope)
        val envelope = optEnvelope.get
        envelopRepositoryStub.data = envelopRepositoryStub.data ++ Map(envelope._id -> envelope)
        storage ! FindById(envelope._id)
        expectMsg(optEnvelope)
      }
    }

    "Respond with nothing when it receives a find by id for a non existent id" in {
      val id = BSONObjectID.generate
      storage ! FindById(id)
      expectMsg(Option.empty[Envelope])
    }
  }

  def createEnvelope = {
    val contraints = Constraints(contentTypes = Seq("contenttype1"), maxItems = 3, maxSize = "1GB", maxSizePerItem = "100MB" )
    val expiryDate = new DateTime().plusDays(2)
    Envelope(_id = BSONObjectID.generate, constraints = contraints, callbackUrl = "http://localhost/myendpoint", expiryDate = expiryDate, metadata = Map("a" -> "1") )
  }
}