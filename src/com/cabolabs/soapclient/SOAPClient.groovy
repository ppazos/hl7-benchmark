package com.cabolabs.soapclient

import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.ContentType.XML // http://grepcode.com/file_/repo1.maven.org/maven2/org.codehaus.groovy.modules.http-builder/http-builder/0.6/groovyx/net/http/ContentType.java/?v=source

class SOAPClient {

   public SOAPClient() {
      // TODO Auto-generated constructor stub
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
      def http = new HTTPBuilder( 'http://localhost:8086' )
      
      //def postBody = [arg0: "MSH|^~\\&|ZIS|1^AHospital|ASD|FDGDG|199605141144||ADT^A01|20031104082400|P|2.3|||AL|NE|\rEVN|A01|20031104082400.0000+0100|20031104082400\rPID|||10||Vries^Danny^D.e||19951202|M|||Rembrandlaan^7^Leiden^^7301TH^^^P|\r"]
      
      // FIXME: mandarlo con request(POST) para poder mandar el header custom soapaction
      http.post( path: '/services/Mirth/acceptMessage',
                 body: {
                    'SOAP-ENV:Envelope'(
                       'xmlns:SOAP-ENV': "http://www.w3.org/2001/12/soap-envelope",
                       'SOAP-ENV:encodingStyle': "http://www.w3.org/2003/05/soap-encoding"
                    ) {
                       'SOAP-ENV:Body' {
                          'm:acceptMessage'(
                           'xmlns:m': "http://ws.connectors.connect.mirth.com/"
                          ) {
                             arg0 {
                                msg
                             }
                          }
                       }
                    }
                 },
                 //contentType : 'application/soap+xml; action=http://localhost:8086/services/Mirth/acceptMessage',
                 'SOAPAction': "http://localhost:8086/services/Mirth/acceptMessage",
                 requestContentType: XML ) { resp ->
      
         println "POST Success: ${resp.statusLine}"
         assert resp.statusLine.statusCode == 201
      }
   }
}
