package forest.model

import scala.collection.mutable.ListBuffer
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.tree.RandomForest
import org.apache.spark.mllib.tree.model.RandomForestModel
import org.apache.spark.mllib.tree.model.DecisionTreeModel
import java.io.StringWriter
import org.apache.spark.SparkContext

/******************************************************************************
 * GMOModel tries to imitate the idea of the GMO technique of selecting
 * a sequence of DNA and replacing it with another one. In this case we replace
 * a Node of a DecisionTreeModel with a Node of a randomly selected Tree. These
 * are the steps that this algorithm follows:
 *       - Create many RandomForest models and select the best decision trees
 *         from each model and add them to the a ListBuffer.
 *       - In each generation every tree gets mixed with another randomly selected
 *         tree. If the mix tree has better accuracy than the previous tree, then
 *         this mix tree added to the ListBuffer.
 *       - At the end of each generation, the ListBuffer is sorted and the worst
 *         decision Trees are removed
 *
 * @param trainingData a fully labeled RDD[LabeledPoint] for training RandomForests
 * @param testingData a fully labeled RDD[LabeledPoint] for evaluating.
 * @param numForests The number of RandomForests models to create.
 * @param numTrees The number of trees on each RandomForest model.
 * @param numClasses Number of classes for classification.
 * @param featureSubsetStrategy Number of features to consider for splits at each node.
 *                              Supported values: "auto", "all", "sqrt", "log2", "onethird".
 *                              If "auto" is set, this parameter is set based on numTrees:
 *                                if numTrees == 1, set to "all";
 *                                if numTrees is greater than 1 (forest) set to "sqrt".
 * @param impurity Criterion used for information gain calculation.
 *                 Supported values: "gini" (recommended) or "entropy".
 * @param maxDepth Maximum depth of the tree (e.g. depth 0 means 1 leaf node, depth 1 means
 *                 1 internal node + 2 leaf nodes).
 *                 (suggested value: 4)
 * @param maxBins Maximum number of bins used for splitting features
 *                (suggested value: 100)
 * @param generations Number of repetitions
 ******************************************************************************/
@SerialVersionUID(100L)
class GMRF_CRISPR_Model(
  protected val trainingData:          RDD[LabeledPoint],
  protected val testingData:           RDD[LabeledPoint],
  protected val numForests:            Integer,
  protected val numTrees:              Integer,
  protected val numClasses:            Integer,
  protected val featureSubsetStrategy: String,
  protected val impurity:              String,
  protected val maxDepth:              Integer,
  protected val maxBins:               Integer,
  protected val generations:           Integer)
  extends Serializable {
  var trees = new ListBuffer[(DecisionTreeModel, Double)]()
  var out = new ListBuffer[String]()

  run()

  private def run() = {

/******************************************************************************
     * Create multiple forest and add the best trees to the trees ListBuffer
     * *****************************************************************************/
    //val startTimeCreateRFAndSelectBestTrees = System.nanoTime
    createForestSelectBestTrees()
    //val CreateRFAndSelectBestTreesTime = (System.nanoTime - startTimeCreateRFAndSelectBestTrees) / 1e9d
    //out += CreateRFAndSelectBestTreesTime.toString()

/******************************************************************************
     * Original size of trees ListBuffered
     *******************************************************************************/
    val numOfTreesInTheForest = trees.size

/******************************************************************************
     * Test the original model
     *******************************************************************************/
    //val startTimeTest = System.nanoTime
    val labelAndPreds = evaluateModel(testingData)
    //val testTime = (System.nanoTime - startTimeTest) / 1e9d
    //out += testTime.toString()

    labelAndPreds.persist()
    val metrics = new MulticlassMetrics(labelAndPreds)
    val forestAccuracy = metrics.accuracy
    val forestWeightedRecall = metrics.weightedRecall
    val forestWeightedFalsePositiveRate = metrics.weightedFalsePositiveRate
    out += ("\nBEST TREES SELECTION MODEL\n" + forestAccuracy)
    labelAndPreds.unpersist()

    /*
    var toPrint = "\n"
    var count = 1
    toPrint += "\n\nORIGINAL RFBT MODEL\n"
    for ((tree, accuracy) <- trees) {
      toPrint += ("Tree " + count + " accurcacy: " + accuracy + "\n")
      toPrint += (tree.toDebugString)
      count += 1
    }
    */

    //Genetic modifications on each generation
    //val startGenerationTime = System.nanoTime

    for (generation <- 1 to generations) {

      gmoAlgo()

      //Sort the trees by accuracy
      trees = trees.sortBy(_._2).reverse

      //Eliminate the worst trees
      var numTreesToDelete = trees.size - numOfTreesInTheForest
      if (numTreesToDelete > 0) {
        trees.remove(numOfTreesInTheForest, numTreesToDelete)
      }

      //Evaluate the new model
      var labelAndPreds = evaluateModel(testingData)
      labelAndPreds.persist()
      var metrics = new MulticlassMetrics(labelAndPreds)
      var forestAccuracy = metrics.accuracy
      var forestWeightedRecall = metrics.weightedRecall
      var forestWeightedFalsePositiveRate = metrics.weightedFalsePositiveRate
      out += (s"\nGENERATION-$generation\n" + forestAccuracy)
      labelAndPreds.unpersist()
    }

    //val generationTime = (System.nanoTime - startGenerationTime) / 1e9d
    //out += generationTime.toString()

    /*
    toPrint += "\n\nGENETIC RFBT MODEL\n"
    count = 1
    for ((tree, accuracy) <- trees) {
      toPrint += ("Tree " + count + " accurcacy: " + accuracy + "\n")
      toPrint += (tree.toDebugString)
      count += 1
    }
    out += toPrint
    */

  }

/*****************************************************************************
   * Creates numForests RandomForests and selects from each RandomForest the
   * decision trees with greater accuracy to add them to the trees ListBuffer
   *****************************************************************************/
  def createForestSelectBestTrees() = {
    //var trainingTimes = " "

    for (i <- 1 to numForests) {

      // Empty categoricalFeaturesInfo indicates all features are continuous.
      var categoricalFeaturesInfo = Map[Int, Int]()
      // Let the algorithm choose.Number of features to consider for splits at each node.
      // Supported values: "auto", "all", "sqrt", "log2", "onethird".
      // If "auto" is set, this parameter is set based on numTrees:
      //    if numTrees == 1, set to "all";
      //    if numTrees is greater than 1 (forest) set to "sqrt".

      //val startTimeTrain = System.nanoTime
      var model = RandomForest.trainClassifier(trainingData, numClasses, categoricalFeaturesInfo,
        numTrees, featureSubsetStrategy, impurity, maxDepth, maxBins)
      //val trainTime = (System.nanoTime - startTimeTrain) / 1e9d

      //trainingTimes += (trainTime.toString() + ",")

      var labelAndPreds = testingData.map { point =>
        var prediction = model.predict(point.features)
        (point.label, prediction)
      }

      labelAndPreds.persist()
      var metrics = new MulticlassMetrics(labelAndPreds)
      var forestAccuracy = metrics.accuracy
      var forestWeightedRecall = metrics.weightedRecall
      var forestWeightedFalsePositiveRate = metrics.weightedFalsePositiveRate
      out += (s"\nRANDOM FOREST $i\n" + forestAccuracy)
      labelAndPreds.unpersist()

      //Select the best trees from each forest and add it to the CRISPR model
      for (currentTree <- model.trees) {
        var labelAndPreds = testingData.map { point =>
          var prediction = currentTree.predict(point.features)
          (point.label, prediction)
        }

        labelAndPreds.persist()
        var metrics = new MulticlassMetrics(labelAndPreds)
        var currentTreeAccuracy = metrics.accuracy

        if (trees.isEmpty) {
          trees += ((currentTree, currentTreeAccuracy))
        } else {
          trees = trees.sortBy(_._2).reverse

          var (lastTree, lastTreeAccuracy) = trees(trees.size - 1)

          if (currentTreeAccuracy > lastTreeAccuracy) {

            trees += ((currentTree, currentTreeAccuracy))

            if (trees.size > numTrees) {
              trees = trees.sortBy(_._2).reverse
              //Delete the last element of the list
              trees.remove(trees.size - 1)
            }
          }
        }

        labelAndPreds.unpersist()
      }
    }

    //out += trainingTimes
  }

/******************************************************************************
* Performs weighted voting on each instance in the given, unseen test set using
* this CRISPR model. The predicted class is the majority vote from the CRISPR
* trees.
*
* @param testingData an unseen, fully labeled RDD[LabeledPoint] for evaluation of
*        this CoDRIFt model
* @return an RDD[(Double, Double)] for use by a MulticlassMetrics object, where
*         the first Double is the actual class value and the second double is
*         the predicted class value
******************************************************************************/
  def evaluateModel(
    testingData: RDD[LabeledPoint]): RDD[(Double, Double)] = {
    testingData.map { point =>
      val votes = scala.collection.mutable.Map.empty[Int, Double]
      for ((tree, accuracy) <- trees) {

        val prediction = tree.predict(point.features).toInt
        votes(prediction) = votes.getOrElse(prediction, 0.0)

      }
      (point.label, votes.maxBy(_._2)._1.toDouble)
    }
  }

/******************************************************************************
 *
 ******************************************************************************/
  def gmoAlgo() = {
    val rand = scala.util.Random
    for ((tree, accuracy) <- trees) {

      /*
       * Modify the right node of the tree
       */
      var randSelectedTree = rand.nextInt(numTrees)
      var (randTree, randTreeAccuracy) = trees(randSelectedTree)

      var originalRightNode = tree.topNode.rightNode
      tree.topNode.rightNode = randTree.topNode.rightNode

      /*
       * Check if this new tree is better than before, if it is add it to the list of trees,
       *  if it is not, leave it like it was
       */
      val rightModifiedLabelAndPreds = testingData.map { point =>
        val rightModifiedTreePrediction = tree.predict(point.features)
        (point.label, rightModifiedTreePrediction)
      }
      rightModifiedLabelAndPreds.persist() //What is this
      val rightModifiedMetrics = new MulticlassMetrics(rightModifiedLabelAndPreds)
      val rightModifiedTreeAccuracy = rightModifiedMetrics.accuracy

      if (accuracy < rightModifiedTreeAccuracy) {
        trees += ((tree, rightModifiedTreeAccuracy))
        tree.topNode.rightNode = originalRightNode
      } else {
        tree.topNode.rightNode = originalRightNode
      }
      rightModifiedLabelAndPreds.unpersist()

      /*
       * Modify the left part of the tree
       */
      randSelectedTree = rand.nextInt(numTrees)
      var (randTreeL, randTreeAccuracyL) = trees(randSelectedTree)

      var originalLeftNode = tree.topNode.leftNode
      tree.topNode.leftNode = randTreeL.topNode.leftNode
      /*
       * Check if this new tree is better than before, if it is add it to the list of trees,
       *  if it is not, leave it like it was
       */
      val leftModifedLabelAndPreds = testingData.map { point =>
        val leftModifiedTreePrediction = tree.predict(point.features)
        (point.label, leftModifiedTreePrediction)
      }
      leftModifedLabelAndPreds.persist()
      val leftModifiedMetrics = new MulticlassMetrics(leftModifedLabelAndPreds)
      val leftModifiedTreeAccuracy = leftModifiedMetrics.accuracy

      if (accuracy < leftModifiedTreeAccuracy) {
        trees += ((tree, leftModifiedTreeAccuracy))
        tree.topNode.leftNode = originalLeftNode
      } else {
        tree.topNode.leftNode = originalLeftNode
      }
      leftModifedLabelAndPreds.unpersist()

    }
  }
}
