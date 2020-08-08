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
import forest.model.CRISPRModel

object postCRISPR {
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
    val outFile = "/mnt/c/Users/ricar/Documents/Research_SupervisedMLRF/Results_SupMLRF/" + outName

    // initialize spark
    val sparkConf = new SparkConf().setAppName("postCRISPR")
    val sc = new SparkContext(sparkConf)

    val out = new StringWriter()

    /*
    // configure hdfs for output
    val hadoopConf = new org.apache.hadoop.conf.Configuration()
    val hdfs = org.apache.hadoop.fs.FileSystem.get(
          new java.net.URI("hdfs://master00.local:8020"), hadoopConf
        )
    */
    // configure hdfs for output //https://www.programcreek.com/scala/org.apache.hadoop.fs.FileSystem
    val hadoopConf = new org.apache.hadoop.conf.Configuration()
    hadoopConf.set(outFile, "http://localhost:9870")
    val hdfs = org.apache.hadoop.fs.FileSystem.get(hadoopConf)
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
    val splits = data.randomSplit(Array(0.5, 0.5))
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
    val crispr = new CRISPRModel(trainingData, testingData, numForests, numTrees, numClasses, featureSubsetStrategy, impurity, maxDepth, maxBins,generations) 

    
    
    for(s <- crispr.out) {
      out.write(s + "\n")
    }

   
        // write string to file
    val outRDD = sc.parallelize(Seq(out.toString()))
    outRDD.saveAsTextFile(outFile)


    sc.stop()
  }
  

 
/**************************************************************************
   * Splits a dataset into stratified training and validation sets. The size
   * of the sets are user-determined. 
   * 	rdd: the dataset to split, read in from a CSV
   *  trainPercent: the size in percent of the training set
   *  Returns: RDDs of LabeledPoints for the training and testing sets
   *************************************************************************/
  def stratifiedRandomSplit(
    rdd:          RDD[Array[String]],
    trainPercent: Double): (RDD[LabeledPoint], RDD[LabeledPoint]) = {
    // map csv text to key value PairedRDD
    val kvPairs = rdd.map(row => (
      row.last.toInt,
      (row.take(row.length - 1).map(str => str.toDouble)).toIndexedSeq // must be immutable
    ))

    // set the size of the training set
    val fractions = Map(1 -> trainPercent, 0 -> trainPercent)
    // get a stratified random subsample from the full set
    val train = kvPairs.sampleByKeyExact(false, fractions, System.nanoTime())
    // remove the elements of the training set from the full set
    val test = kvPairs.subtract(train)
    (
      train.map(pair => new LabeledPoint(
        pair._1,
        Vectors.dense(pair._2.toArray))),
      test.map(pair => new LabeledPoint(
        pair._1,
        Vectors.dense(pair._2.toArray))))
  }

  /*

  /**
   * Predict values for a single data point using the model trained.
   *
   * @param features array representing a single data point
   * @return predicted category from the trained model
   */
  def predict(features: Vector): Double = {
    (algo, combiningStrategy) match {
      case (Regression, Sum) =>
        predictBySumming(features)
      case (Regression, Average) =>
        predictBySumming(features) / sumWeights
      case (Classification, Sum) => // binary classification
        val prediction = predictBySumming(features)
        // TODO: predicted labels are +1 or -1 for GBT. Need a better way to store this info.
        if (prediction > 0.0) 1.0 else 0.0
      case (Classification, Vote) =>
        predictByVoting(features)
      case _ =>
        throw new IllegalArgumentException(
          "TreeEnsembleModel given unsupported (algo, combiningStrategy) combination: " +
            s"($algo, $combiningStrategy).")
      }
    }
    *
    * */

}