package forest.model

import scala.collection.mutable.ListBuffer
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.tree.RandomForest
import org.apache.spark.mllib.tree.model.RandomForestModel
import org.apache.spark.mllib.tree.model.DecisionTreeModel

class CRISPRtreeModel(
  protected val trainingData: RDD[LabeledPoint],
  protected val testingData:  RDD[LabeledPoint],
  protected val model:        RandomForestModel,
  protected val numTrees:     Integer)
  extends Serializable {
  var trees = new ListBuffer[DecisionTreeModel]()
  run()

  private def run() = {
    trees = collection.mutable.ListBuffer(model.trees: _*)

    val rand = scala.util.Random

    for (tree <- model.trees) {
      /*
       * Test the tree
       */
      val originalLabelAndPreds = testingData.map { point =>
        val originalPrediction = tree.predict(point.features)
        (point.label, originalPrediction)
      }
      originalLabelAndPreds.persist() //What is this
      val originalMetrics = new MulticlassMetrics(originalLabelAndPreds)
      val originalTreeAccuracy = originalMetrics.accuracy //Is this of the forest or tree(should be the tree)
      /*
       * Modify the right node of the tree
       */
      var randSelectedTree = rand.nextInt(numTrees - 1)
      model.trees(model.trees.length - 1).topNode.rightNode = tree.topNode.rightNode //this saves the right side of the node tree in case we need it later (we always loose the last tree)
      tree.topNode.rightNode = model.trees(randSelectedTree).topNode.rightNode

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

      if (originalTreeAccuracy < rightModifiedTreeAccuracy) {
        trees += tree
        tree.topNode.rightNode = model.trees(model.trees.length - 1).topNode.rightNode
      } else {
        tree.topNode.rightNode = model.trees(model.trees.length - 1).topNode.rightNode
      }

      /*
       * Modify the left node of the tree
       */
      randSelectedTree = rand.nextInt(numTrees - 1)
      model.trees(model.trees.length - 1).topNode.leftNode = tree.topNode.leftNode
      tree.topNode.leftNode = model.trees(randSelectedTree).topNode.leftNode

      /*
       * Check if this new tree is better than before, if it is add it to the list of trees,
       *  if it is not, leave it like it was
       */
      val leftModifiedLabelAndPreds = testingData.map { point =>
        val leftModifiedTreePrediction = tree.predict(point.features)
        (point.label, leftModifiedTreePrediction)
      }
      leftModifiedLabelAndPreds.persist() //What is this
      val leftModifiedMetrics = new MulticlassMetrics(leftModifiedLabelAndPreds)
      val leftModifiedTreeAccuracy = leftModifiedMetrics.accuracy

      if (originalTreeAccuracy < leftModifiedTreeAccuracy && rightModifiedTreeAccuracy < leftModifiedTreeAccuracy) {
        trees += tree
        tree.topNode.rightNode = model.trees(model.trees.length - 1).topNode.rightNode
      } else {
        tree.topNode.rightNode = model.trees(model.trees.length - 1).topNode.rightNode
      }

    }
    
    /*
     * Eliinate half and Predict data
     */
    //Eliminate the worst half (top half) Todo: Pobabli no the worst half needs to be sorted first
      if (trees.length > numTrees) {
        //TODO: Check why this doesn't work:  listOfForest = listOfForest.remove((listOfForest.length/2).toInt, listOfForest.length - 1)
        trees = trees.drop((trees.length / 2).toInt)
      }
      

  }
  
   def predict(testingData: RDD[LabeledPoint]) : RDD[(Double,Double)] = {
        testingData.map{point => 
          val votes = scala.collection.mutable.Map.empty[Int, Double]
          for(tree <- trees) {
            val prediction = tree.predict(point.features).toInt
            votes(prediction) = votes.getOrElse(prediction, 0.0)
          }
          (point.label, votes.maxBy(_._2)._1.toDouble) 
          }
      }
}