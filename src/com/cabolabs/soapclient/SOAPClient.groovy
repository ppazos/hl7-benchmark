package com.cabolabs.soapclient

import com.cabolabs.hl7benchmark.SendingPlan;

import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.ContentType.XML // http://grepcode.com/file_/repo1.maven.org/maven2/org.codehaus.groovy.modules.http-builder/http-builder/0.6/groovyx/net/http/ContentType.java/?v=source
import static groovyx.net.http.Method.POST

class SOAPClient {

   String serverIP
   int serverPort
   SendingPlan plan
   
   boolean connected = true // interface field needed by plan, it is used in mllp client
   
   public SOAPClient(String serverIP, int serverPort, SendingPlan plan)
   {
      this.serverIP = serverIP
      this.serverPort = serverPort
      this.plan = plan
   }
   

   /**
    * POST /StockQuote HTTP/1.1
      Host: www.stockquoteserver.com
      Content-Type: application/soap+xml; action=http://localhost:8086/services/Mirth/acceptMessage   << SOAP 1.2
      Content-Length: nnnn
      
      <SOAP-ENV:Envelope
         xmlns:SOAP-ENV="http://www.w3.org/2001/12/soap-envelope"
         SOAP-ENV:encodingStyle="http://www.w3.org/2003/05/soap-encoding">
         <SOAP-ENV:Body>
            <m:acceptMessage xmlns:m="http://ws.connectors.connect.mirth.com/">
               <arg0>"MSH|^~\\&|ZIS|1^AHospital|ASD|FDGDG|199605141144||ADT^A01|20031104082400|P|2.3|||AL|NE|\rEVN|A01|20031104082400.0000+0100|20031104082400\rPID|||10||Vries^Danny^D.e||19951202|M|||Rembrandlaan^7^Leiden^^7301TH^^^P|\r"</arg0>
            </m:acceptMessage>
         </SOAP-ENV:Body>
      </SOAP-ENV:Envelope>

    * @param msg
    */
   public void sendToServer( String msg )
   {
      def http = new HTTPBuilder( 'http://'+ this.serverIP +':'+ this.serverPort +'/services/Mirth/acceptMessage' )
      
      // FIXME: Catch exception de coneccion, si falla contar la falla de envio del mensaje y no esperar respuesta.
      // la falla se debe mostrar en el reporte.
      http.request( POST, 'text/xml' ) { req -> //, XML ) { req ->
         
         //uri.path = '/services/Mirth/acceptMessage'
         
         body = {
           'SOAP-ENV:Envelope'(
              'xmlns:SOAP-ENV': "http://schemas.xmlsoap.org/soap/envelope/", //SOAP 1.2 usa "http://www.w3.org/2001/12/soap-envelope",
              'SOAP-ENV:encodingStyle': "http://schemas.xmlsoap.org/soap/encoding/" // SOAP 1.2 usa "http://www.w3.org/2003/05/soap-encoding"
           ) {
              'SOAP-ENV:Body' {
                 'm:acceptMessage'('xmlns:m': "http://ws.connectors.connect.mirth.com/") {
                    'arg0' {
                       mkp.yield( msg ) // http://docs.groovy-lang.org/latest/html/api/groovy/xml/MarkupBuilder.html
                    }
                 }
              }
           }
         }
         //contentType : 'application/soap+xml; action=http://localhost:8086/services/Mirth/acceptMessage',
         //headers.'Content-Type' = 'text/xml'
         //headers.Accept = 'text/xml'
         headers.'SOAP-Action' = 'http://'+ this.serverIP +':'+ this.serverPort +'/services/Mirth/acceptMessage' // SOAP 1.1, 1.2 pone action en contentType
         
         response.success = { resp, xml ->
      
            //println "POST Success: ${resp.statusLine}"
            //println xml.text() // el ACK del Mirth!
            
            // Notifies the plan to quantify time
            this.plan.messageReceived(xml.text())
            
            //assert resp.statusLine.statusCode == 201
         }
      } // request
   }
   
   // Interfaces method to let the plan work
   public void stop()
   {
   }
}
