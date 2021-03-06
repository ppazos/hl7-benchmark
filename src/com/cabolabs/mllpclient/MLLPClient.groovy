package com.cabolabs.mllpclient

import com.cabolabs.hl7benchmark.SendingPlan
import java.nio.charset.StandardCharsets

class MLLPClient {

   def socket
   def input
   def output
   boolean connected = false
   def serverListener
   
   SendingPlan plan
   def rcvMessageCount = 0 // Cuenta mensajes recibidos

   def MLLPClient(String serverIP, int serverPort, SendingPlan plan)
   {
      try
      {
         // Conexion con al servidor MLLP
         this.socket = new Socket(InetAddress.getByName(serverIP), serverPort)
         this.connected = true
         //println "MLLPClient: conectado a " + socket.getRemoteSocketAddress()
         
         // Buffer 128 KB (8KB por defecto)
         this.input = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), StandardCharsets.UTF_8), 131072)
         this.output = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream(), StandardCharsets.UTF_8), 131072)
         //this.input = this.socket.getInputStream()
         //this.output = this.socket.getOutputStream()
         
         //println this.input.getClass() // SocketInputStream
         
         this.plan = plan
         
         this.receiveThread()
      }
      catch (Exception e)
      {
         //println "e1 " + e.message
         println "Verifique que el servidor está escuchando en "+ serverIP +":"+ serverPort
         this.connected = false
         this.stop()
      }
   }
   
   public void receiveThread()
   {
      Runnable runnable = new Runnable() {

         public void run () {
         
            //println "thread recv: " + Thread.currentThread().getId()
            
            // DEBUG
            //def start
            //def now
            
            
            // RECEIVE
            //def hasData = true
            def data
            def buffer = ''

            /*
            // Reads chars
            int i
            char c
            boolean mightBeMessageEnd = false
            while ( (i = input.read()) != -1 )
            {
               c = (char)i
               
               if (c == '\u000b') // Message start
               {
                  // Skip start character
                  //start = System.currentTimeMillis()
               }
               else if (c == '\u001c') // Might be a message end
               {
                  mightBeMessageEnd = true
               }
               else if (c == "\r" && mightBeMessageEnd) // Message end
               {
                  mightBeMessageEnd = false // Reset for the next message
                  rcvMessageCount ++
                  
                  // Print when message is received
                  //println "MLLPClient recibio #"+ rcvMessageCount +" thread "+ Thread.currentThread().getId()
                  //println buffer
                  
                  //now = System.currentTimeMillis()
                  //println "Receive "+ (now - start) // ms
                  
                  
                  this.plan.messageReceived(buffer)
                  
                  buffer = '' // Reset receive buffer
               }
               else
               {
                  buffer += c // Buffers just the message characters, and avoids MLLP delimiters
               }
            }
            */
            
            def line
            while ( ( line = input.readLine() ) )
            {
               // if its end of message (the last CR was read by readLine)
               if (line[line.size()-1] == '\u001c')
               {
                  rcvMessageCount ++
                  
                  // Print when message is received
                  //println "MLLPClient recibio #"+ rcvMessageCount +" thread "+ Thread.currentThread().getId()
                  //println buffer[1..-1] // removes the start message byte
                  
                  //now = System.currentTimeMillis()
                  //println "Receive "+ (now - start) // ms
                  
                  
                  this.plan.messageReceived(buffer[1..-1])
                  
                  buffer = '' // Reset receive buffer
               }
               else
               {
                  buffer += line + "\r"  // Buffers just the message characters, and avoids MLLP delimiters
               }
            }
         }
      }

      // create the thread
      this.serverListener = new Thread(runnable)
      // start the thread
      this.serverListener.start()
   }
   
   
   /**
    * Sends a message to the server. This can be called several times by the same thread (it is not thread safe!)
    * @param msg
    */
   public void sendToServer( String msg )
   {
      //println "sendToServer " + this.connected
      
      if (!this.connected) throw new Exception("not connected")
      
      // SEND
      // Escribe en el socket con MLLP // http://hl7api.sourceforge.net/xref/ca/uhn/hl7v2/llp/MinLLPWriter.html (writeMessage)
      // http://sourceforge.net/p/hl7api/code/764/tree/trunk/hapi-mvn/hapi-base/src/main/java/ca/uhn/hl7v2/llp/
      try
      {
         this.output.write('\u000b' + msg + '\u001c' + "\r")
         this.output.flush()
      }
      catch (IOException e)
      {
         this.plan.errors << e
      }
   }
   
   public void stop()
   {
      this.input?.close()
      this.output?.close()
         
      // Cierra el socket
      if (this.socket && this.socket.isConnected() && !this.socket.isClosed())
      {
         this.socket.close()
      }
      
      this.serverListener?.stop()
   }
}
