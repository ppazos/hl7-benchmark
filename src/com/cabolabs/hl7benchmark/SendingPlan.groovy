package com.cabolabs.hl7benchmark

import com.cabolabs.mllpclient.MLLPClient

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
   MLLPClient client
   def main
   
   
   // To measure times
   def starttime
   def endtime
   
   // TODO: add a timeout to finish the plan after that.
   
   // main will be notified of the execution time when the plan is finished
   public SendingPlan(int messagesToSend, String ip, int port, Object main) {
      this.messagesToSend = messagesToSend
      this.client = new MLLPClient(ip, port, this)
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
      if (!started)
      {
         starttime = System.currentTimeMillis()
         started = true
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
      
      // PLAN FINISHED
      if (this.messageRecvCount == this.messagesToSend)
      {
         endtime = System.currentTimeMillis()
         
         //println "PLAN FINISHED "+ this.main
         
         this.main.notifyPlanExecutionTime(this, endtime - starttime)
         
         // closess the current thread of execution
         this.client.stop()
      }
   }
}
