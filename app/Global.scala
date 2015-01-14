import models._
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession
import controllers._
import org.joda.time.DateTime
import play.api.mvc.RequestHeader
import scala.concurrent.Future

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    InitialData.init()
    ()
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    //Airbrake.notify(request, ex)
    Future.successful(InternalServerError(
      views.html.errorPage(ex)))
  }
}
