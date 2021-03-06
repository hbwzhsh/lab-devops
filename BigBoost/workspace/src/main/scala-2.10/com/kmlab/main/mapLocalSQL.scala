package com.kmlab.main

import com.kmlab.utils.CreateSparkContext
import kafka.serializer.StringDecoder
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.kafka.KafkaUtils

/**
 * Created by WeiChen on 2015/9/2.
 */
//case class Record(key: Int, value: String)

//case class Model(rowkey: Option[String], title: Option[String], content: Option[String], dtime: Option[Long])

object mapLocalSQL extends CreateSparkContext{
  def main(args: Array[String]) {
    if (args.length != 4) {
      System.err.println("Usage: mapLocalSQL <checkpointDirectory> <timeframe> <kafka-brokerList> <topic,...,>")
      System.exit(1)
    }
    val Array(checkpointDirectory, timeframe, kafkaBrokerList, topicList) = args

    def function2CreateContext(AppName: String, checkpointDirectory: String, timeframe: String, brokerList: String, topicList: String): StreamingContext = {
      val ssc = createContext(AppName, checkpointDirectory, timeframe.toLong)
      val kafkaParams = Map("metadata.broker.list" -> brokerList)
      val topics = topicList.split(",").toSet
      val stream = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topics)
      stream.foreachRDD(rdd =>
        alphabetCount(rdd.map(_._2))
      )
      ssc
    }
    val ssc = StreamingContext.getOrCreate(checkpointDirectory,
      () => {
        function2CreateContext("BigBoost", checkpointDirectory, timeframe, kafkaBrokerList, topicList)
      }
    )
    ssc.start()
    ssc.awaitTermination()
  }

  def alphabetCount(rdd: RDD[String]): (Int, Int) = {
    val returnMe1 = rdd.flatMap(_.split(" "))
      .map(_.length)
      .fold(0)((count: Int, w: Int) => count + w)

    val returnMe2 = rdd.flatMap(_.split(" "))
      .map(_.length)
      .reduce(
        (count: Int, w: Int) => count + w
      )
    println("[COUNT]" + returnMe1)
    (returnMe1, returnMe2)
  }

  def printRDD(rdd: RDD[String]): Long = {
    rdd.foreach(rdd => println(s"[printRDD]$rdd"))
    val returnMe = rdd.count()
    returnMe
  }

  //  def mappingLocalSQL() {
  //    val jarPaths = "target/scala-2.11/spark-hello_2.11-1.0.jar"
  //    val conf = new SparkConf().setMaster("spark://localhost:7077").setAppName("hdfs data count")
  //    conf.setJars(Seq(jarPaths))
  //    val sc = new SparkContext(conf)
  //    val sqlContext = new SQLContext(sc)
  //    import sqlContext.implicits._
  //    val df = sc.parallelize((1 to 100).map(i => Record(i, s"val_$i"))).toDF()
  //    df.registerTempTable("records")
  //    println("Result of SELECT *:")
  //    sqlContext.sql("SELECT * FROM records").collect().foreach(println)
  //    val count = sqlContext.sql("SELECT COUNT(*) FROM records").collect().head.getLong(0)
  //    println(s"COUNT(*): $count")
  //    sc.stop()
  //  }

}
