package com.cabolabs.hl7benchmark

import com.cabolabs.mllpclient.MLLPClient
import com.cabolabs.soapclient.SOAPClient

/**
 * Prepara un plan de envio de messageCount mensajes a un servidor.
 * Luego que envia esos mensasjes, cierra los canales.
 * @author pab
 *
 */
class SendingPlan {

   boolean started = false // true when the first message is sent
   int messagesToSend
   int messageRecvCount = 0
   def client // SOAP or MLLP
   def main
   
   
   // To measure times
   
   // Totals
   def starttime
   def endtime
   
   def msg_times = [:] // msgid -> [starttime, endtime]
   
   
   // TODO: add a timeout to finish the plan after that.
   
   // main will be notified of the execution time when the plan is finished
   /**
    * 
    * @param messagesToSend
    * @param ip
    * @param port
    * @param main
    * @param type soap/mllp
    */
   public SendingPlan(int messagesToSend, String ip, int port, Object main, String type) {
      this.messagesToSend = messagesToSend
      
      if (type == "mllp")
         this.client = new MLLPClient(ip, port, this)
      else
         this.client = new SOAPClient(ip, port, this)
      this.main = main
   }

   /**
    * Para poder devolver un mensaje, como son asinc, deberia registrar un observer
    * que sea notificado cuando recibo la respuesta del server para un mensaje
    * determinado que se envió. Sino hay que bloquear el send hasta que reciba el
    * ack y luego enviar el siguiente mensaje. Esto se puede hacer bloqueando
    * al proceso que invoca, o haciendo internamente una queue de mensajes. En
    * ese caso, quien invoca tambien deberia registrarse para recibir la respuesta
    * async porque no sabe cuando se va a enviar el mensaje ni cuando va a recibir
    * el ack de respuesta.
    * @param msg
    */
   public void send(String msg)
   {
      if (!this.client.connected) return
      
      // start time for the message id
      def msgid = msg.split("\\|")[9] // MSH-10
      this.msg_times[msgid] = [System.currentTimeMillis()]
      
      if (!started)
      {
         this.starttime = System.currentTimeMillis()
         this.started = true
      }
      
      // Avoids invoking after plan is done
      if (this.messageRecvCount == this.messagesToSend) throw new Exception("Plan executed, "+ this.messageRecvCount +" messages already sent")
      
      this.client.sendToServer(msg)
   }
   
   /**
    * The client notifies when a message is received, so the plan can check if its done.
    * This is because we nee to wait until we get all the responses.
    */
   public void messageReceived(String rcvmsg)
   {
      this.messageRecvCount ++
      
      //println "message received "+ this.messageRecvCount + " planned: "+ this.messagesToSend
      
      // SOAP no recibe \r
//recv [MSH|^~\&|ASD|FDGDG|ZIS|1|20150506134949.377||ACK|20150506134949.377|P|2.3
//MSA|AA|1
//]
      //println "recv "+ rcvmsg.split("\\n")
      
      // Some clients send \r some \n (windows)
      // We need to have 2 segments (MSH and MSA from the ACK)
      def segments = rcvmsg.split("\\r")
      if (segments.size() == 1) segments = rcvmsg.split("\\n")
      
      // end time for the message id (look into MSA-2)
      def msgid = segments[1].split("\\|")[2] // First split gets the MSA segment
      this.msg_times[msgid] << System.currentTimeMillis() // end time
      
      // PLAN FINISHED
      if (this.messageRecvCount == this.messagesToSend)
      {
         this.endtime = System.currentTimeMillis()
         
         //println "PLAN FINISHED "+ this.main
         
         this.main.notifyPlanExecutionTime(this, this.endtime - this.starttime)
         
         // closess the current thread of execution
         this.client.stop()
      }
   }
}
