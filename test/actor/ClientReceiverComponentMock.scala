package actor

import actors.ClientReceiverComponent
import org.mockito.Mock
import org.specs2.mock.Mockito
import actors.ClientReceiver

trait ClientReceiverComponentMock extends ClientReceiverComponent with Mockito {
  val clientReceiver = mock[ClientReceiver]
}