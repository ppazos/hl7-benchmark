package com.cabolabs.mllpclient

import java.nio.charset.StandardCharsets

class MLLPClient {

   def socket
   def input
   def output
   def connected = false
   def serverListener

   def MLLPClient(int serverPort, String serverIP)
   {
      try
      {
         // Conexion con al servidor MLLP
         this.socket = new Socket(InetAddress.getByName(serverIP), serverPort)
         this.connected = true
         println "MLLPClient: conectado a " + socket.getRemoteSocketAddress()
         this.input = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), StandardCharsets.UTF_8))
         this.output = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream(), StandardCharsets.UTF_8))
         
         this.receiveThread()
      }
      catch (Exception e)
      {
         println "e1 " + e.message
         this.connected = false
      }
   }
   
   public void receiveThread()
   {
      Runnable runnable = new Runnable() {

         public void run () {
         
            println "hilo run: " + Thread.currentThread().getId()
         
            // RECEIVE
            def hasData = true
            def data
            def responses = ''
            while (hasData)
            { // loop hasta que el server cierre la conexion
               try
               {
                  data = input.readLine()
                  
                  if (!data)
                  {
                     hasData = false
                  }
                  else
                  {
                     responses += data + "\n"
                  }
               }
               catch (Exception e)
               {
                  println "e3 "+ e.message
                  hasData = false
               }
               
               println "MLLPClient recibio: "
               println responses
               
            } // while
            
            
         }
      }

      // create the thread
      this.serverListener = new Thread(runnable)
      // start the thread
      this.serverListener.start()
   }
   
   
   public void sendToServer( String msg )
   {
      println "sendToServer " + this.connected
      
      if (!this.connected) throw new Exception("not connected")
      
      // SEND
      // Escribe en el socket con MLLP // http://hl7api.sourceforge.net/xref/ca/uhn/hl7v2/llp/MinLLPWriter.html (writeMessage)
      this.output.write('\u000b')
      this.output.write(msg)
      this.output.write('\u001c' + "\r")
      this.output.flush()
   }
   
   public void stop()
   {
      this.input.close()
      this.output.close()
         
      // Cierra el socket
      if (this.socket.isConnected() && !this.socket.isClosed())
      {
         this.socket.close()
      }
      
      this.serverListener.stop()
   }
}

// MAIN
/*
println "hilo: " + Thread.currentThread().getId()

int serverPort = 2617
String serverIP = 'localhost'
def client = new MLLPClient(serverPort, serverIP)

Runtime.runtime.addShutdownHook {
   client.stop()
}

try
{
   client.sendToServer("MSH|^~\\&|ZIS|1^AHospital|ASD|FDGDG|199605141144||ADT^A01|20031104082400|P|2.3|||AL|NE|\rEVN|A01|20031104082400.0000+0100|20031104082400\rPID|||10||Vries^Danny^D.e||19951202|M|||Rembrandlaan^7^Leiden^^7301TH^^^P|\r")
}
catch (Exception e)
{
   println "e2 " +  e.message
}
*/