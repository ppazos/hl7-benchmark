package com.cabolabs.hl7benchmark

class Benchmark {

   public Benchmark()
   {
      // TODO Auto-generated constructor stub
   }
   
   def run(Closure closure)
   {
      def start = System.currentTimeMillis()
      closure.call()
      def now = System.currentTimeMillis()
      
      return (now - start) // ms
  }
}
