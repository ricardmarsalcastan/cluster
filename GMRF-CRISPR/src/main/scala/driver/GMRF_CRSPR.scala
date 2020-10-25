package driver

/*****************************************************************************
* SupervisedMLRF.scala
* @author zennisarix
* This code implements a standard Spark mllib RandomForest classifier on the
* dataset provided with parameters passed via command-line arguments. The
* specified dataset must be fully labeled. The RandomForest model is trained on the
* training data, used to make predictions on the testing data, and evaluated
* for classification performance. The calculated metrics and model are sent
* to the hdfs with the provided filename.
******************************************************************************/
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.tree.RandomForest
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import java.io.StringWriter
import scala.collection.Seq
import scala.collection.mutable.ListBuffer
import org.apache.spark.mllib.tree.model.DecisionTreeModel
import org.apache.spark.mllib.tree.model.RandomForestModel
import forest.model.GMRF_CRISPR_Model

object GMRF_CRISPR {
  def main(args: Array[String]) {
    if (args.length < 8) {
      System.err.println("Must supply valid arguments: [numClasses] [numTrees] [numForests]" +
        "[impurity] [maxDepth] [maxBins] [input filename] [output filename] " +
        "[generations] [featureSubsetStrategy]")
      System.exit(1)
    }
    // setup parameters from command line arguments
    val numClasses = args(0).toInt
    val numTrees = args(1).toInt
    val numForests = args(2).toInt
    val impurity = args(3)
    val maxDepth = args(4).toInt
    val maxBins = args(5).toInt
    val inFile = args(6)
    val outName = args(7)
    val generations = args(8).toInt
    val featureSubsetStrategy = args(9)
    val outFile = "hdfs://master00.local:8020/data/castan/results/" + outName

    // initialize spark
    val sparkConf = new SparkConf().setAppName("GMRF-CRISPR")
    val sc = new SparkContext(sparkConf)

    // configure hdfs for output
    val hadoopConf = new org.apache.hadoop.conf.Configuration()
    val hdfs = org.apache.hadoop.fs.FileSystem.get(
      new java.net.URI("hdfs://master00.local:8020"), hadoopConf)
    val out = new StringWriter()

/**************************************************************************
     * Read in data and prepare for sampling
     *************************************************************************/

    // load data file from hdfs
    val text = sc.textFile(inFile)

    // remove header line from input file
    val textNoHdr = text.mapPartitionsWithIndex(
      (i, iterator) => if (i == 0 && iterator.hasNext) {
        iterator.next
        iterator
      } else iterator)

    // parse input text from csv to rdd

    val rdd = textNoHdr.map(line => line.split(","))

    //Printing rdd to file
    //rdd.coalesce(1).saveAsTextFile("C:\\Users\\ricar\\Desktop\\rddData")

    //println("\n\n")

/**************************************************************************
     * Create training and testing sets.
     *************************************************************************/
    val data = rdd.map(row =>
      new LabeledPoint(
        row.last.toDouble,
        Vectors.dense(row.take(row.length - 1).map(str => str.toDouble)))).cache()

    // Split the data into training and test sets (30% held out for testing)
    val startTimeSplit = System.nanoTime
    val splits = data.randomSplit(Array(0.7, 0.3))
    val (trainingData, testingData) = (splits(0), splits(1))
    trainingData.cache()
    val splitTime = (System.nanoTime - startTimeSplit) / 1e9d

    /*
    // Split the data into training and test sets (30% held out for testing)
    val startTimeSplit = System.nanoTime
    val Array(trainingData, testingData) = rdd.randomSplit(Array(percentLabeled, 1- percentLabeled))
    // remove labels from specified percent of the training data.
    // NOTE: Since SparkMLRF cannot handle unlabeled data, "unlabeling" the
    //  data is equivalent to removing it.
    val sslSplits = trainingData.randomSplit(Array(percentLabeled, 1 - percentLabeled))
    val trainingDataSSL = sslSplits(0)
    trainingData.cache()
    val splitTime = (System.nanoTime - startTimeSplit) / 1e9d
    */

/********************************************************************************
     * Create a CRISPRtree model
     ********************************************************************************/
    val gmo = new GMRF_CRISPR_Model(trainingData, testingData, numForests, numTrees, numClasses, featureSubsetStrategy, impurity, maxDepth, maxBins, generations)

/******************************************************************************
 * Print all the test
 ******************************************************************************/
    for (s <- gmo.out) {
      out.write(s)
    }

    // delete current existing file for this model
    try {
      hdfs.delete(new org.apache.hadoop.fs.Path(outFile), true)
    } catch {
      case _: Throwable => { println("ERROR: Unable to delete " + outFile) }
    }

    // write string to file
    val outRDD = sc.parallelize(Seq(out.toString()))
    outRDD.saveAsTextFile(outFile)

    sc.stop()
  }
}
