package randForest

import org.apache.spark.mllib.tree.model.RandomForestModel
import tree.Tree
import scala.collection.mutable.ListBuffer

class RandForest(acc: Double, rfm: RandomForestModel, listOfTrees: ListBuffer[Tree]) extends Serializable {
  var accuracy = acc
  var rfModel = rfm
  var listTrees = listOfTrees
}