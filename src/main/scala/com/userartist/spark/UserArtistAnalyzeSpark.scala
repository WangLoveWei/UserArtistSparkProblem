package com.userartist.spark

import com.userartist.appconf.AppConf
import com.userartist.common.Constants
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Row, SQLContext, SparkSession}


object UserArtistAnalyzeSpark extends AppConf {


  def main(args: Array[String]): Unit = {


    val userArtistRdd = getUserArtistData(sc, sqlContext);


    val artistRdd = getArtistData(sc, sqlContext);


    val userArtistAggInfo = userArtistRdd.map(row => ((row.getLong(1), row)));
    val artistAggInfo = artistRdd.map(row => ((row.getLong(0), row)));

    val userArtistNameRDD = userArtistAggInfo.join(artistAggInfo);

    val userArtistNameFormatRDD = userArtistNameRDD.map(tuple => (tuple._2._1.getLong(0), tuple._1, tuple._2._1.getInt(2), tuple._2._2.getString(1)))

    val userArtistNameFormatGroupRDD = userArtistNameFormatRDD.groupBy(_._1);

    /**
      * 用户听歌数量
      */
    val userPlayCountsRdd = getUserPlayCounts(userArtistNameFormatGroupRDD)

    userPlayCountsRdd.saveAsTextFile("hdfs://hadoop102:9000/data")
  }

  private def getUserPlayCounts(userArtistNameFormatGroupRDD: RDD[(Long, Iterable[(Long, Long, Int, String)])]) = {
    userArtistNameFormatGroupRDD.map(tuple => {
      val userArtistItr = tuple._2.iterator;
      var playCounts = 0;
      while (userArtistItr.hasNext) {
        val userArtist = userArtistItr.next();
        playCounts += userArtist._3
      }
      (tuple._1, playCounts);
    }
    );
  }

  def getUserArtistData(sc: SparkContext, sqlContext: SQLContext): RDD[Row] = {
    val tableSql="use bigdata";
    sqlContext.sql(tableSql);
    val sql = "select * from bigdata.user_artist_data"
    val userArtistOriginRDD = sqlContext.sql(sql);
    val userArtistRdd = userArtistOriginRDD.rdd.filter(row => (!row.isNullAt(0) && !row.isNullAt(1) && !row.isNullAt(2)));
    userArtistRdd
  }

  def getArtistData(sc: SparkContext, sqlContext: SQLContext): RDD[Row] = {
    val artistDataSql = "select * from bigdata.artist_data"
    val artistOriginRDD = sqlContext.sql(artistDataSql);
    val artistRDD = artistOriginRDD.filter(row => (!row.isNullAt(0) && !row.isNullAt(1)))
    artistRDD.rdd
  }


}
