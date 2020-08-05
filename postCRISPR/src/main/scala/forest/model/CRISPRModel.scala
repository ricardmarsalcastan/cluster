package forest.model

import scala.collection.mutable.ListBuffer
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.tree.RandomForest
import org.apache.spark.mllib.tree.model.RandomForestModel
import org.apache.spark.mllib.tree.model.DecisionTreeModel
import java.io.StringWriter

class CRISPRModel(
  protected val trainingData:          RDD[LabeledPoint],
  protected val testingData:           RDD[LabeledPoint],
  protected val numForests:            Integer,
  protected val numTrees:              Integer,
  protected val numClasses:            Integer,
  protected val featureSubsetStrategy: String,
  protected val impurity:              String,
  protected val maxDepth:              Integer,
  protected val maxBins:               Integer,
  protected val out:                   StringWriter)
  extends Serializable {
  var trees = new ListBuffer[(DecisionTreeModel, Double)]()
  run()

  private def run() = {

    createForestSelectBestTrees(trainingData, testingData, numClasses, numTrees,
      featureSubsetStrategy, impurity, maxDepth, maxBins,
      numForests)

    val numOfTreesInTheForest = trees.size

/*********************************************************************************
* Test the Original model
* *********************************************************************************/
    val labelAndPreds = evaluateModel()
    labelAndPreds.persist()
    val metrics = new MulticlassMetrics(labelAndPreds)
    val forestAccuracy = metrics.accuracy
    out.write("Orifinal forest accuracy: " + forestAccuracy + "\n")
    labelAndPreds.unpersist()

/*********************************************************************************
* Genetic modifications in multiple generations
*********************************************************************************/
    for (generation <- 1 to 3) {

      crisprAlgo()

      //Sort the trees by accuracy
      trees.sortBy(_._2)

      out.write(s"Current Num Trees before delete: ${trees.size}\n")

      //Eliminate the worst trees
      val numTreesToDelete = trees.size - numOfTreesInTheForest
      if (numTreesToDelete > 0) {
        trees.remove(numOfTreesInTheForest, numTreesToDelete)
      }

      out.write(s"Current Num Trees after delete: ${trees.size}\n")

      val labelAndPreds = evaluateModel()
      labelAndPreds.persist()
      val metrics = new MulticlassMetrics(labelAndPreds)
      val forestAccuracy = metrics.accuracy
      out.write(s"$generation generation forest accuracy: " + forestAccuracy + "\n")
      labelAndPreds.unpersist()
    }

  }

  def evaluateModel(): RDD[(Double, Double)] = {
    testingData.map { point =>
      val votes = scala.collection.mutable.Map.empty[Int, Double]
      for ((tree, accuracy) <- trees) {
        val prediction = tree.predict(point.features).toInt
        votes(prediction) = votes.getOrElse(prediction, 0.0)
      }
      (point.label, votes.maxBy(_._2)._1.toDouble)
    }
  }

  def createForestSelectBestTrees(
    trainingData:          RDD[LabeledPoint],
    testingData:           RDD[LabeledPoint],
    numClasses:            Integer,
    numTrees:              Integer,
    featureSubsetStrategy: String,
    impurity:              String,
    maxDepth:              Integer,
    maxBins:               Integer,
    numForest:             Integer) = {
    for (i <- 1 to numForest) {
      // Empty categoricalFeaturesInfo indicates all features are continuous.
      val categoricalFeaturesInfo = Map[Int, Int]()
      // Let the algorithm choose.Number of features to consider for splits at each node.
      // Supported values: "auto", "all", "sqrt", "log2", "onethird".
      // If "auto" is set, this parameter is set based on numTrees:
      //    if numTrees == 1, set to "all";
      //    if numTrees is greater than 1 (forest) set to "sqrt".
      val model = RandomForest.trainClassifier(trainingData, numClasses, categoricalFeaturesInfo,
        numTrees, featureSubsetStrategy, impurity, maxDepth, maxBins)

      //Select the best trees from each forest and add it to the CRISPR model
      for (tree <- model.trees) {

        val labelAndPreds = testingData.map { point =>
          val prediction = tree.predict(point.features)
          (point.label, prediction)
        }

        labelAndPreds.persist()
        val metrics = new MulticlassMetrics(labelAndPreds)
        val accuracy = metrics.accuracy

        if (accuracy > 0.99)
          trees += ((tree, accuracy))

        labelAndPreds.unpersist()
      }
    }
  }

  def crisprAlgo() = {
    val rand = scala.util.Random
    for ((tree, accuracy) <- trees) {

      /*
      out.write("ORIGINAL TREE\n")
      out.write(tree.toDebugString + "\n\n")
      */

      /*
       * Modify the right node of the tree
       */
      var randSelectedTree = rand.nextInt(numTrees)
      var (randTree, randTreeAccuracy) = trees(randSelectedTree)

      /*out.write("RAND TREE\n")
      out.write(randTree.toDebugString + "\n\n")
      *
      */

      var originalRightNode = tree.topNode.rightNode
      tree.topNode.rightNode = randTree.topNode.rightNode

      /*out.write("MIX TREE\n")
      out.write(tree.toDebugString + "\n\n")
      *
      */

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

      /*out.write("BCAK TO ORIGINAL TREE\n")
      out.write(tree.toDebugString + "\n\n")*/

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