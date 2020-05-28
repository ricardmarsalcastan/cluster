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
import randForest.RandForest
import scala.collection.mutable.ListBuffer
import tree.Tree

object SupervisedMLRF {
  def main(args: Array[String]) {
    if (args.length < 7) {
      System.err.println("Must supply valid arguments: [numClasses] [numTrees] " +
        "[impurity] [maxDepth] [maxBins] [input filename] [output filename] " +
        "[percent labeled] [Num Of Forests] [percentage of elite forests] " + 
        "[percentage of elite trees] [number of generations]")
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
    val outFile = "/mnt/c/Users/ricar/Documents/Research_SupervisedMLRF/Results_SupMLRF/" + outName
    val percentLabeled = args(7).toDouble * 0.01
    val numForests = args(8).toInt
    val percentNumForests = args(9).toDouble * 0.01
    val percentEliteTrees = args(10).toDouble * 0.01
    val numGen = args(11).toInt

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

    //Buffered forests
    var listOfForest = new ListBuffer[RandForest]()

    //Save Performance and Models to text File
    val out = new StringWriter()

    //Lists of Training and Testing times to compute average
    var listOfTrainingTimes = new ListBuffer[Double]()
    var listOfTestingTimes = new ListBuffer[Double]()
    var listOfTreeAccuracyTime = new ListBuffer[Double]()
    
    //List of trees that contains wraper of DecisionTreeModel
    var listTrees = new ListBuffer[Tree]()

    //Train all the forests
    for (forestNum <- 0 to numForests - 1) {
/**************************************************************************
					 * Train a Spark mllib RandomForest model on the labeled and unlabeled
					 *  training data.f
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
							 *  evaluations of each tree of the forest.
							 *************************************************************************/
      val startTimeTreeAccuracy = System.nanoTime
      for (tree <- 0 to numTrees - 1) {
        val treeLabelAndPreds = testingData.map { point =>
          val predictionTree = model.trees(tree).predict(point.features)
          (point.label, predictionTree)
        }
        val treeMetrics = new MulticlassMetrics(treeLabelAndPreds)
        val treeAccuracy = treeMetrics.accuracy
        val t = new Tree(treeAccuracy, model.trees(tree))
        listTrees += t
      }
      val treeAccuracyTime = (System.nanoTime - startTimeTreeAccuracy) / 1e9d

/**************************************************************************
							 * Metrics calculation for classification and execution performance
							 *  evaluations of the forest.
							 *************************************************************************/

      val metrics = new MulticlassMetrics(labelAndPreds)

      listOfTrainingTimes += trainTime
      listOfTestingTimes += testTime
      listOfTreeAccuracyTime += treeAccuracyTime

      // Overall Statistics
      val accuracy = metrics.accuracy

      //Add this forest to the buffer of forest
      //Create a new randForest Class
      val rf = new RandForest(accuracy, model, listTrees)

      listOfForest += rf
    }
    
    //Execution Performance
    val averageTrainTime = listOfTrainingTimes.sum / listOfTrainingTimes.length
    val averageTestTime = listOfTestingTimes.sum / listOfTestingTimes.length
    val averageTreeAccuracyTime = listOfTreeAccuracyTime.sum / listOfTreeAccuracyTime.length

    out.write(outName + "\n")
    out.write("EXECUTION PERFORMANCE:\n")
    out.write("SplittingTime=" + splitTime + "\n")
    out.write("AverageTrainingTime=" + averageTrainTime + "\n")
    out.write("AverageTestingTime=" + averageTestTime + "\n\n")
    out.write("AverageTreeAccuracyCalculation=" + averageTreeAccuracyTime + "\n\n")

    //Sort the forest from grether accuracy to less accuracy
    listOfForest = listOfForest.sortWith(_.accuracy > _.accuracy)
    //Sort each tree of the forest from grether accuracy to less accuracy
    for (forest <- listOfForest) {
      forest.listTrees = forest.listTrees.sortWith(_.accuracy > _.accuracy)
    }

    out.write("\n\nORIGINAL FORESTS ACCURACY\n")
    for (forest <- listOfForest) {
      out.write((forest.accuracy).toString + "\n")
    }

    var listEliteForests = new ListBuffer[RandForest]()

    //Number of elite forests
    val numOfElitForests = (listOfForest.length * percentNumForests).ceil.toInt

    val rand = scala.util.Random

    //Split of forests trees
    val numEliteForestTrees = (numTrees * percentEliteTrees).toInt
    val numForestTrees = numTrees - numEliteForestTrees

    val startGenTime = System.nanoTime
    //Generations evolution
    for (gen <- 1 to numGen) {

      //Add elite forest to the list
      for (i <- 0 to numOfElitForests) {
        listEliteForests += listOfForest(i)
      }

      for (forestMixing <- 1 to numForests) {
        //Select Two rand forests to mix
        var randForestsNum = -1
        var randElitForestNum = -1
        //Aboid getting the same forest
        do {
          randForestsNum = rand.nextInt(numForests)
          randElitForestNum = rand.nextInt(numOfElitForests)
        } while (randForestsNum != randElitForestNum)

        var forestToMix = listOfForest(randForestsNum)
        var eliteForestToMix = listEliteForests(randElitForestNum)

        //Generate a new forest with the best trees of forestToMix and eliteForestToMix using percentEliteTrees to know how many trees from each group
        
        var listGeneratedForestTrees = new ListBuffer[Tree]()
        
        //Add the best trees of the elite forest
        for(forestTree <- 0 to numEliteForestTrees - 1) {
          listGeneratedForestTrees += eliteForestToMix.listTrees(forestTree)
        }
        
        //Add the best trees of the regular forest
        for(forestTree <- 0 to numForestTrees - 1) {
          listGeneratedForestTrees += forestToMix.listTrees(forestTree)
        }
        
        //Sort from better accuracy to lower accuracy the listGeneratedForestTrees
        listGeneratedForestTrees = listGeneratedForestTrees.sortWith(_.accuracy > _.accuracy)
        
        //Generate the new forest with a base model
        var mixedForest = new RandForest(0, forestToMix.rfModel, listGeneratedForestTrees)
        
        //Change all the trees of the base model by the listGeneratedForestTrees
        for(tree <- 0 to numTrees - 1) {
          mixedForest.rfModel.trees(tree) = mixedForest.listTrees(tree).treeModel
        }

        //Calculate accuracy of the mixedForest
        val labelAndPreds = testingData.map { point =>
          val prediction = mixedForest.rfModel.predict(point.features)
          (point.label, prediction)
        }

        val metrics = new MulticlassMetrics(labelAndPreds)
        val accuracy = metrics.accuracy

        mixedForest.accuracy = accuracy

        //Add mixedForest to list of forest
        listOfForest += mixedForest
      } //End of Mixing
      
      //TODO: Change the way eliminate the worse half so I only have to sort once
      //Sort the list of forest from lowest accuracy to hihgest accuracy
      listOfForest = listOfForest.sortWith(_.accuracy < _.accuracy)
      
      //Eliminate the worst half (top half)
      if (listOfForest.length != numForests) {
        //TODO: Check why this doesn't work:  listOfForest = listOfForest.remove((listOfForest.length/2).toInt, listOfForest.length - 1)
        listOfForest = listOfForest.drop((listOfForest.length / 2).toInt)
      }
      
      //Sort the list of forest from hihgest accuracy to lowest accuracy
      listOfForest = listOfForest.sortWith(_.accuracy > _.accuracy)
      
      //Print each generation
      out.write(s"\n\nGeneration ${gen}\n")
      out.write("\nGeneration Accuracies:\n")
      for (forest <- listOfForest) {
        out.write(s"${forest.accuracy} \n")
      }
      for (forest <- listOfForest) {
        out.write(s"\nAccuracy = ${forest.accuracy} \n")
        out.write(s"\nLearned classification forest model:\n ${forest.rfModel.toDebugString}\n")
      }

    } //End of Generation

    val genTime = (System.nanoTime - startGenTime) / 1e9d

    out.write("\n\nGENERATIONS PERFORMANCE\n")
    out.write("\nGenerations Time" + genTime + "\n")
    out.write("Accuracies: \n")
    for (forest <- listOfForest) {
      out.write(s"${forest.accuracy} \n")
    }
    for (forest <- listOfForest) {
      out.write(s"\nAccuracy = ${forest.accuracy} \n")
      out.write(s"\nLearned classification forest model:\n ${forest.rfModel.toDebugString}\n")
    }

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
}