/**
 * 
 */
package com.cabolabs.hl7benchmark

import com.cabolabs.mllpclient.MLLPClient
import com.cabolabs.soapclient.SOAPClient

import java.text.*
import java.util.concurrent.atomic.AtomicInteger

import groovy.transform.Synchronized

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
class Main {

   static int messages // number of messages to send on each concurrent thread
   static int concurrency
   static int finished = 0 // plans finished
   static int totalTime = 0 // Total execution time of all the plans
   static int totalMinTime // min time of all transactions
   static int totalMaxTime // max time of all transactions
   static Map msgTimes = [:] // [msgid -> [start, end]] times for each message
   
   static AtomicInteger msgid = new AtomicInteger(1) // to generate unique message control ids
   
   static String sample_message = "MSH|^~\\&|ZIS|1^AHospital|ASD|FDGDG|199605141144||ADT^A01|MESSAGE_CONTROL_ID|P|2.3|||AL|NE|\rEVN|A01|20031104082400.0000+0100|20031104082400\rPID|||10||Vries^Danny^D.e||19951202|M|||Rembrandlaan^7^Leiden^^7301TH^^^P|\r"
   
   
   static main(args) {
      
      // http://mrhaki.blogspot.com/2009/09/groovy-goodness-parsing-commandline.html
      // http://docs.groovy-lang.org/latest/html/gapi/groovy/util/CliBuilder.html
      def cli = new CliBuilder(usage: 'hl7-benchmark -[t] type -[c] concurrency -[n] messages -[s] ip:port [message_path] -[path]')
      // Create the list of options.
      // arges dice cuantos valores se esperan
      cli.with {
          h longOpt: 'help',        'Show usage information'
          t longOpt: 'type',        args: 1, argName: 'type',        'mllp|soap'
          c longOpt: 'concurrency', args: 1, argName: 'concurrency', 'number of processes sending messages'
          n longOpt: 'messages',    args: 1, argName: 'messages',    'number of messages to be send by each process'
          s longOpt: 'ip:port',     args: 2, argName: 'ip:port',     'server IP address:server port',                 valueSeparator: ':'
          path longOpt: 'message path', args: 1, argName: 'message_path', 'path to sample message'
      }
      
      def options = cli.parse(args)
      
      //println options // groovy.util.OptionAccessor@10f3a9c
      //println options.getOptions() // [[ option: c concurrency  [ARG] :: number of processes sending messages ], [ option: n messages  [ARG] :: number of messages to be send by each process ], [ option: i ip  [ARG] :: server IP address ], [ option: p port  [ARG] :: server port number ]]
      
      // empty
      // mandatory args not present
      // -h or --help option is used.
      if (!options || !options.c || !options.n || !options.s || options.h) {
         cli.usage()
         return
      }
      
//      println options.ss // ip:port // adding an 's' gets all the coded values separated by valueSeparator
//      println options.c
//      println options.n
//      println args // [-t, mllp, -c, 10, -n, 100, -s, 192.168.1.107:2617, -path, C:\path\path\file.hl7]
//      println options.path
      
      // Check input sample message
      if (options.path)
      {
         def file = new File(options.path)
         if (!file.exists())
         {
            println  "file "+ options.path +" doesn't exists"
            return
         }
         if (!file.canRead())
         {
            println  "can't read file "+ options.path
            return
         }
         
         // TODO: verify the content is HL7 (we need to use HAPI parser for this)
         sample_message = file.getText()
      }
      
      
      concurrency = Integer.parseInt(options.c)
      def plan_messages = Integer.parseInt(options.n)
      
      messages = concurrency * plan_messages
      
      def ip = options.ss[0] // adding an 's' gets all the coded values separated by valueSeparator from the arg with name 's' so it is ss
      def port = Integer.parseInt(options.ss[1])
      def clients = [:]
      def threads = []

      //def msgid = new AtomicInteger(1)
      
      concurrency.times { thread ->
         
         threads << Thread.start { // runnable closure
            
            // This is passed so we get notified of the execution time
            clients[thread] = new SendingPlan(plan_messages, ip, port, this, options.t)
            
            plan_messages.times {
            
               //clients[thread].send("MSH|^~\\&|ZIS|1^AHospital|ASD|FDGDG|199605141144||ADT^A01|${msgid}|P|2.3|||AL|NE|\rEVN|A01|20031104082400.0000+0100|20031104082400\rPID|||10||Vries^Danny^D.e||19951202|M|||Rembrandlaan^7^Leiden^^7301TH^^^P|\r")
               
               //msgid.getAndIncrement()
               
               clients[thread].send( filterMessage( sample_message ) )
            }
         }
      }
      
      //println threads
      
      // TODO: join threads
      threads.each { thread ->
         
         thread.join()
         //println "thread "+ thread.getId() + " joined"
      }

   }
   
   /**
    * Changes the control id of the message to make it unique.
    * @param message
    * @return
    */
   private static String filterMessage( String message )
   {
      // Changes the message control id for our generated msgid 
      
      // Some clients send \r some \n (windows)
      // MSH=segments[0], ...
      def segments = message.split("\\r")
      if (segments.size() == 1) segments = message.split("\\n")
      def msg_msgid = segments[0].split("\\|")[9] // MSH-10
      
      msgid.getAndIncrement()
      
      return message.replace(/|${msg_msgid}|/, '|'+ msgid.toString() +'|')
   }
   
   // TODO: sumarizar los resultados de todos los planes en un solo reporte (max, min, avg, % < range)
   // The plan will use .delegate to reference the Main class instance where the plan was called
   
   @Synchronized
   static public void notifyPlanExecutionTime(SendingPlan plan, long totaltimeperplan)
   {
      synchronized(this)
      {
         println "total plan " + totaltimeperplan + " ms"
         
         // ---------------------- Min / Max for current plan ------------------------
         //
         // http://mrhaki.blogspot.com/2010/12/groovy-goodness-determine-min-and-max.html
         def minse = plan.msg_times.min { it.value[1] - it.value[0] }.value // [start, end]
         def maxse = plan.msg_times.max { it.value[1] - it.value[0] }.value // [start, end]
         def min = (minse[1] - minse[0])
         def max = (maxse[1] - maxse[0])
         
         // ---------------------- Total min and max ----------------------
         //
         if (!totalMinTime || min < totalMinTime) totalMinTime = min
         if (!totalMaxTime || max > totalMaxTime) totalMaxTime = max
   
         // --------------------------- Avg ---------------------------
         //
         println "plan media: "+ ( (max + min) / 2 ) +" ms" // Es una media en realidad, no el promedio de todos los tiempos
         // TODO> AVG de todos los mensajes
         println "plan min: "+ min + " ms"
         println "plan max: "+ max + " ms"
         println "errors: "+ plan.errors.size()
         println "----------------------"
         
         
         // --------------------------- Merge message times for final report ---------------------------
         //
         msgTimes << plan.msg_times
         
         
         // --------------------------- Plan finished: final report ---------------------------
         //
         finished++
         totalTime += totaltimeperplan
      }
      
      if (concurrency == finished)
      {
         println "Total: "+ totalTime + " ms"
         
         println "Percentage of transactions completed in certain time (ms)"
         
         // --------------------------- Reporte de % menor que range ---------------------------
         //
         // Se divide el range de tiempos de recepcion en 5 segmentos iguales para ver cuantos mensajes
         // se recibieron en cada segmento de tiempos>
         // min..min+range, min+range..min+2range, min+2range..min+3range, min+3range..min+4range, min+4range..max
         
         def range = (totalMaxTime - totalMinTime) / 10
         def rangeActual = totalMinTime + range
         def res = [:]
         
         while (rangeActual <= totalMaxTime)
         {
            // Messages in current range
            // msgTimes ==> [msgid -> [start, end]]
            def entries = msgTimes.findAll {
               (it.value[1] - it.value[0]) <= rangeActual
            }
            
            // Saca entradas ya procesadas
            msgTimes = msgTimes - entries
            
            // Pone el % del total de mensajes en lugar de la cantidad
            def roundedPercentage = new BigDecimal( ( entries.size() / messages ) * 100 ).setScale(1, BigDecimal.ROUND_HALF_UP)
            res["< "+ rangeActual] = roundedPercentage.toString() +"%"
   
            // La suma total deberia ser self.messages
            //println "es: "+ entries.size()
            
            // Proximo range
            rangeActual += range
         }
         
         // msgTimes deberia estar vacio al terminar
         //println "Quedan tiempos!> "+ msgTimes.collectEntries {
         //   [(it.key): it.value[1] - it.value[0]]
         //}
         
         // Si el porcentaje da menos que 100% puede ser que algun mensaje se halla perdido ej. error de conexion poque se satura el servers
         res.each { time, percentage ->
            
            println "\t"+ percentage +"\t"+ time
         }
      }
   }
}
