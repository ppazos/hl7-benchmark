/**
 * 
 */
package com.cabolabs.hl7benchmark

import com.cabolabs.mllpclient.MLLPClient
import com.cabolabs.soapclient.SOAPClient

import java.text.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author pab
 *
 */
class Main {

   static main(args) {
      
      // http://mrhaki.blogspot.com/2009/09/groovy-goodness-parsing-commandline.html
      // http://docs.groovy-lang.org/latest/html/gapi/groovy/util/CliBuilder.html
      def cli = new CliBuilder(usage: 'hl7-benchmark -[t] type -[c] concurrency -[n] messages -[i] ip -[p] port')
      // Create the list of options.
      // arges dice cuantos valores se esperan
      cli.with {
          h longOpt: 'help',        'Show usage information'
          t longOpt: 'type',        args: 1, argName: 'type',        'mllp|soap'
          c longOpt: 'concurrency', args: 1, argName: 'concurrency', 'number of processes sending messages'
          n longOpt: 'messages',    args: 1, argName: 'messages',    'number of messages to be send by each process'
          i longOpt: 'ip',          args: 1, argName: 'ip',          'server IP address'
          p longOpt: 'port',        args: 1, argName: 'port',        'server port number'
      }
      
      def options = cli.parse(args)
      
      //println options // groovy.util.OptionAccessor@10f3a9c
      //println options.getOptions() // [[ option: c concurrency  [ARG] :: number of processes sending messages ], [ option: n messages  [ARG] :: number of messages to be send by each process ], [ option: i ip  [ARG] :: server IP address ], [ option: p port  [ARG] :: server port number ]]
      
      
      if (!options) {
         cli.usage()
         return
      }
      
      
      if (options.t == 'mllp')
      {
         // empty
         // mandatory args not present
         // -h or --help option is used.
         if (!options.c || !options.n || !options.i || !options.p || options.h) {
            cli.usage()
            return
         }
         
         println options.i
         println options.p
         println options.c
         println options.n
         
         
         def concurrency = Integer.parseInt(options.c)
         def messages = Integer.parseInt(options.n)
         def ip = options.i
         def port = Integer.parseInt(options.p)
         def clients = [:]
         def threads = []
         
         // Se lo paso a cada plan para que me diga cuanto demoro en recibir todos los mensajes
         //def bench = new Benchmark()
         
         
         def msgid = new AtomicInteger(1)

         concurrency.times { thread ->
            
            threads << Thread.start { // runnable closure
               
               // This is passed so we get notified of the execution time
               clients[thread] = new SendingPlan(messages, ip, port, this)
               
               messages.times {
               
                  clients[thread].send("MSH|^~\\&|ZIS|1^AHospital|ASD|FDGDG|199605141144||ADT^A01|${msgid}|P|2.3|||AL|NE|\rEVN|A01|20031104082400.0000+0100|20031104082400\rPID|||10||Vries^Danny^D.e||19951202|M|||Rembrandlaan^7^Leiden^^7301TH^^^P|\r")
                  
                  msgid.getAndIncrement()
               }
            }
         }
         
         
         println threads
         
         // TODO: join threads
         threads.each { thread ->
            
            thread.join()
            //println "thread "+ thread.getId() + " joined"
         }
         
         // Report
         //println "======================================="
         //println "totaltime: "+ totaltime +" ms"
         
      }
      else // soap
      {
         // TODO MULTITHREAD
         def soap = new SOAPClient('192.168.1.106', 8086)
         
         // &#10; es CR en XML
         soap.sendToServer("MSH|^~\\&|ZIS|1^AHospital|ASD|FDGDG|199605141144||ADT^A01|20031104082400|P|2.3|||AL|NE|\rEVN|A01|20031104082400.0000+0100|20031104082400\rPID|||10||Vries^Danny^D.e||19951202|M|||Rembrandlaan^7^Leiden^^7301TH^^^P|\r")
      }
   }
   
   // TODO: sumarizar los resultados de todos los planes en un solo reporte (max, min, avg, % < rango)
   // The plan will use .delegate to reference the Main class instance where the plan was called
   static public void notifyPlanExecutionTime(SendingPlan plan, long totaltimeperplan)
   {
      println "total " + totaltimeperplan + " ms"
      
      Map timespermessage = plan.msg_times // [msgid -> [start, end]]
      
//      timespermessage.each{ msgid, time ->
//         
//         println msgid +": "+ (time[1] - time[0]) + " ms"
//      }
      
      // --------------------------- Min / Max ---------------------------
      //
      // http://mrhaki.blogspot.com/2010/12/groovy-goodness-determine-min-and-max.html
      def minse = plan.msg_times.min { it.value[1] - it.value[0] }.value // [start, end]
      def maxse = plan.msg_times.max { it.value[1] - it.value[0] }.value // [start, end]
      def min = (minse[1] - minse[0])
      def max = (maxse[1] - maxse[0])
      
      // --------------------------- Avg ---------------------------
      //
      println "avg: "+ ( (max + min) / 2 ) +" ms" // Es una media en realidad, no el promedio de todos los tiempos
      
      // --------------------------- Reporte de % menor que rango ---------------------------
      //
      // Se divide el rango de tiempos de recepcion en 5 segmentos iguales para ver cuantos mensajes
      // se recibieron en cada segmento de tiempos>
      // min..min+rango, min+rango..min+2rango, min+2rango..min+3rango, min+3rango..min+4rango, min+4rango..max
      
      def rango = (max - min) / 5
      def rangoActual = min + rango
      def res = [:]
      
      while (rangoActual <= max)
      {
         def entries = plan.msg_times.findAll {
            (it.value[1] - it.value[0]) < rangoActual
         }
         
         // Saca entradas ya procesadas
         plan.msg_times = plan.msg_times - entries
         
         // Pone el % del total de mensajes en lugar de la cantidad
         res["<"+ rangoActual +" ms"] = ( ( entries.size() / plan.messagesToSend ) * 100 ).toString() + "%"

         // Proximo rango
         rangoActual += rango
      }
      
      
      println "min: "+ min + " ms"
      println "max: "+ max + " ms"
      println res
      
      println "----------------------"
   }
   
}
