package tree

import org.apache.spark.mllib.tree.model.DecisionTreeModel

class Tree(acc:Double, dtm: DecisionTreeModel) extends Serializable {
  var accuracy = acc
  var treeModel = dtm
}
