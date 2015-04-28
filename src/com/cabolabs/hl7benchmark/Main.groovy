/**
 * 
 */
package com.cabolabs.hl7benchmark

import com.cabolabs.mllpclient.MLLPClient
import java.text.*

/**
 * @author pab
 *
 */
class Main {

   static main(args) {
      
      // http://mrhaki.blogspot.com/2009/09/groovy-goodness-parsing-commandline.html
      // http://docs.groovy-lang.org/latest/html/gapi/groovy/util/CliBuilder.html
      def cli = new CliBuilder(usage: 'hl7-benchmark -[c] concurrency -[n] messages -[i] ip -[p] port')
      // Create the list of options.
      // arges dice cuantos valores se esperan
      cli.with {
          h longOpt: 'help',        'Show usage information'
          c longOpt: 'concurrency', args: 1, argName: 'concurrency', 'number of processes sending messages'
          n longOpt: 'messages',    args: 1, argName: 'messages',    'number of messages to be send by each process'
          i longOpt: 'ip',          args: 1, argName: 'ip',          'server IP address'
          p longOpt: 'port',        args: 1, argName: 'port',        'server port number'
      }
      
      def options = cli.parse(args)
      
      //println options // groovy.util.OptionAccessor@10f3a9c
      println options.getOptions() // [[ option: c concurrency  [ARG] :: number of processes sending messages ], [ option: n messages  [ARG] :: number of messages to be send by each process ], [ option: i ip  [ARG] :: server IP address ], [ option: p port  [ARG] :: server port number ]]
      

      // empty
      // mandatory args not present
      // -h or --help option is used.
      if (!options || !options.c || !options.n || !options.i || !options.p || options.h) {
         cli.usage()
         return
      }
      
      println options.i
      println options.p
      println options.c
      println options.n
      
      
      def concurrency = Integer.parseInt(options.c)
      def messages = Integer.parseInt(options.n)
      def port = Integer.parseInt(options.p)
      def clients = [:]
      
      concurrency.times { thread ->
         
         Thread.start { // runnable closure
            
            clients[thread] = new MLLPClient(port, options.i)
            
            messages.times {
            
               clients[thread].sendToServer("MSH|^~\\&|ZIS|1^AHospital|ASD|FDGDG|199605141144||ADT^A01|20031104082400|P|2.3|||AL|NE|\rEVN|A01|20031104082400.0000+0100|20031104082400\rPID|||10||Vries^Danny^D.e||19951202|M|||Rembrandlaan^7^Leiden^^7301TH^^^P|\r")
            }
            
            //clients[thread].stop()
         }
      }
      
      // TODO: join threads
   }

}
