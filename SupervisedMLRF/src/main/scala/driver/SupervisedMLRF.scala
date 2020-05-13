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

object SupervisedMLRF { 
  def main(args: Array[String]) {
    if (args.length < 7) {
      System.err.println("Must supply valid arguments: [numClasses] [numTrees] " +
        "[impurity] [maxDepth] [maxBins] [input filename] [output filename] " +
        "[percent labeled]")
      System.exit(1)
    }
    // setup parameters from command line arguments
    val numClasses = args(0).toInt
    val numTrees = args(1).toInt 
    val impurity = args(2)
    val maxDepth = args(3).toInt
    val maxBins = args(4).toInt
    val inFile = args(5)
    val outName = args(6)
    val outFile =  "/mnt/c/Users/ricar/Documents/Research_SupervisedMLRF/Results_SupMLRF/" + outName
    val percentLabeled = args(7).toDouble * 0.01
    
    
    // initialize spark 
    val sparkConf = new SparkConf().setAppName("SupervisedMLRF")
    val sc = new SparkContext(sparkConf)
    
    
   /*
    // configure hdfs for output
    val hadoopConf = new org.apache.hadoop.conf.Configuration()
    val hdfs = org.apache.hadoop.fs.FileSystem.get(
          new java.net.URI("hdfs://master00.local:8020"), hadoopConf
        )    
    */
    // configure hdfs for output //https://www.programcreek.com/scala/org.apache.hadoop.fs.FileSystem
    val hadoopConf = new org.apache.hadoop.conf.Configuration()
    hadoopConf.set(outFile,"http://localhost:9870")
    val hdfs = org.apache.hadoop.fs.FileSystem.get(hadoopConf)  
    /**************************************************************************
     * Read in data and prepare for sampling
     *************************************************************************/
    
    // load data file from hdfs
    val text =  sc.textFile(inFile)
    
    
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
            Vectors.dense(row.take(row.length - 1).map(str => str.toDouble))
      )
    ).cache()
    
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
    /**************************************************************************
     * Train a Spark mllib RandomForest model on the labeled and unlabeled
     *  training data.
     *************************************************************************/
    // Empty categoricalFeaturesInfo indicates all features are continuous.
    val categoricalFeaturesInfo = Map[Int, Int]()
    // Let the algorithm choose.Number of features to consider for splits at each node.
    // Supported values: "auto", "all", "sqrt", "log2", "onethird".
    // If "auto" is set, this parameter is set based on numTrees:
    //    if numTrees == 1, set to "all";
    //    if numTrees is greater than 1 (forest) set to "sqrt".
    val featureSubsetStrategy = "auto" 
    
    val startTimeTrain = System.nanoTime
    val model = RandomForest.trainClassifier(trainingData, numClasses, categoricalFeaturesInfo,
      numTrees, featureSubsetStrategy, impurity, maxDepth, maxBins)
    val trainTime = (System.nanoTime - startTimeTrain) / 1e9d
        
    
    /**************************************************************************
     * Test the RandomForest model on the fully labeled testing data.
     *************************************************************************/
    
    val startTimeTest = System.nanoTime
    val labelAndPreds = testingData.map { point =>
      val prediction = model.predict(point.features)
      (point.label, prediction)
    }
    val testTime = (System.nanoTime - startTimeTest) / 1e9d
    
    /**************************************************************************
     * Metrics calculation for classification and execution performance
     *  evaluations.
     *************************************************************************/
    
    val out = new StringWriter()
    
    val metrics = new MulticlassMetrics(labelAndPreds)
    
    out.write(outName + "\n")
    out.write("EXECUTION PERFORMANCE:\n")
    out.write("SplittingTime=" + splitTime + "\n")
    out.write("TrainingTime=" + trainTime + "\n")
    out.write("TestingTime=" + testTime + "\n\n")
    
    out.write("CLASSIFICATION PERFORMANCE:\n")
    // Confusion matrix
    out.write("Confusion matrix (predicted classes are in columns):\n")
    out.write(metrics.confusionMatrix + "\n")
    
    // Overall Statistics
    val accuracy = metrics.accuracy
    out.write("\nSummary Statistics:\n")
    out.write(s"Accuracy = $accuracy\n")
    
    // Precision by label
    val labels = metrics.labels
    labels.foreach { l =>
      out.write(s"Precision($l) = " + metrics.precision(l) + "\n")
    }
    
    // Recall by label
    labels.foreach { l =>
      out.write(s"Recall($l) = " + metrics.recall(l) + "\n")
    }
    
    // False positive rate by label
    labels.foreach { l =>
      out.write(s"FPR($l) = " + metrics.falsePositiveRate(l) + "\n")
    }
    
    // F-measure by label
    labels.foreach { l =>
      out.write(s"F1-Score($l) = " + metrics.fMeasure(l) + "\n")
    }
    
    // Weighted stats
    out.write(s"\nWeighted precision: ${metrics.weightedPrecision}\n")
    out.write(s"Weighted recall: ${metrics.weightedRecall}\n")
    out.write(s"Weighted F1 score: ${metrics.weightedFMeasure}\n")
    out.write(s"Weighted false positive rate: ${metrics.weightedFalsePositiveRate}\n")
    
    // output trees
    out.write(s"\nLearned classification forest model:\n ${model.toDebugString}\n")
    
    // output training and testing data
    //println("TRAINING DATA:\n")
    //trainingData.collect().map(println)
    //println("TESTING DATA:\n")
    //testingData.collect().map(println)

    
    // delete current existing file for this model
    try {
        hdfs.delete(new org.apache.hadoop.fs.Path(outFile), true)
    } catch { 
        case _ : Throwable => { println("ERROR: Unable to delete " + outFile)} 
    }
    
    
    // write string to file
    val outRDD= sc.parallelize(Seq(out.toString()))
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
      rdd: RDD[Array[String]],
      trainPercent: Double):
      (RDD[LabeledPoint], RDD[LabeledPoint]) = {
    // map csv text to key value PairedRDD
    val kvPairs = rdd.map(row => (
        row.last.toInt, 
        (row.take(row.length - 1).map(str => str.toDouble)).toIndexedSeq // must be immutable
      )
    )
        
    // set the size of the training set    
    val fractions = Map(1 -> trainPercent, 0 -> trainPercent)
    // get a stratified random subsample from the full set
    val train = kvPairs.sampleByKeyExact(false, fractions, System.nanoTime())
    // remove the elements of the training set from the full set
    val test = kvPairs.subtract(train) 
    (train.map(pair => new LabeledPoint(pair._1, 
         Vectors.dense(pair._2.toArray))),
     test.map(pair => new LabeledPoint(pair._1,
         Vectors.dense(pair._2.toArray))))
  }
}