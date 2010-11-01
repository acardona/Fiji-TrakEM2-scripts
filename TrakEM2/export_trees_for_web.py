# Albert Cardona 2010-11-01
# A TrakEM2 script to export all Treeline and AreaTree instances
# in SWC, each on its own .swc file. Along with a .js file
# that contains a map with id, name, and connections
# to other Treeline or AreaTree.

from ini.trakem2 import Project
from ini.trakem2.display import Tree, Treeline, AreaTree, Connector
from ini.trakem2.parallel import Process, TaskFactory
from java.lang import StringBuilder
from java.io import File
from ij import IJ
from ij.gui import YesNoCancelDialog
from java.util.concurrent.atomic import AtomicInteger

def asSWC(tree):
  """ Return a String with the treeline in SWC format.
  If root is None, returns None. """
  root = tree.getRoot()
  if root is None:
    return None
  cal = tree.getLayerSet().getCalibrationCopy()
  pw = float(cal.pixelWidth)
  ph = float(cal.pixelHeight)
  swc = StringBuilder()
  table = {}
  nodeID = 1
  for nd in tree.getRoot().getSubtreeNodes():
    table[nd] = nodeID
    swc.append(nodeID).append(' ')
    swc.append('0').append(' ')
    swc.append(nd.x * pw).append(' ')
    swc.append(nd.y * ph).append(' ')
    swc.append(float(nd.layer.z) * pw).append(' ')
    swc.append(float(0)).append(' ')
    if nd.parent is None:
      swc.append(-1) # root node has id -1
    else:
      swc.append(table[nd.parent]) # id of the parent node
    swc.append(' \n')
    nodeID += 1
  return swc.toString()

def findConnections(tree):
  """ Return two tables: one of outgoing and one of incomming connections,
  with the name versus the number of connections. """
  outgoing, incomming = tree.findConnectors()
  tableOut = {}
  for c in outgoing:
    for targetSet in c.getTargets(Tree):
      for target in targetSet:
        if isinstance(target, Connector):
          continue
        if tableOut.has_key(target):
          tableOut[target] += 1
        else:
          tableOut[target] = 1
  tableIn = {}
  for c in incomming:
    for origin in c.getOrigins(Tree):
      if isinstance(origin, Connector):
        continue
      if tableIn.has_key(origin):
        tableIn[origin] += 1
      else:
        tableOut[origin] = 1
  return tableOut, tableIn

def getTitle(tree):
  return tree.project.getMeaningfulTitle2(tree) + " #" + str(tree.id)

def sortMapByValue(m, reverse):
  """ Returns an iterable list of [key,value] pairs, sorted by value. """
  v = [[v, k] for k,v in m.items()]
  v.sort()
  if reverse:
    v.reverse()
  return [[k, v] for v,k in v]
  

def asJSON(tree):
  """ Return a String with a JSON map containing the name, id, and list of connections. """
  json = StringBuilder()
  json.append('{"id" : "').append(tree.id).append('",\n')
  json.append(' "name" : "').append(getTitle(tree)).append('",\n')  
  tableOut, tableIn = findConnections(tree)
  json.append(' "outgoing" : [')
  for target, num in sortMapByValue(tableOut, True):
    json.append(' ["').append(getTitle(target)).append('", ').append(num).append('],\n')
  if len(tableOut) > 0:
    json.setLength(json.length()-2)
  json.append('],\n')
  json.append(' "incoming" : [')
  for origin, num in sortMapByValue(tableIn, True):
    json.append(' ["').append(getTitle(origin)).append('", ').append(num).append('],\n')
  json.append(']}\n')
  return json.toString()

def save(path, contents):
  """ Ignores files with empty contents. """
  if contents is None:
    return False
  return Utils.saveToFile(File(path), contents)

def toFiles(tree, targetFolder):
  """ Create two files: a .swc file with the calibrated nodes,
  and a .js file with a table in JSON specifying ID, name, and connections. """
  title = getTitle(tree)
  if save(targetFolder + title + ".swc", asSWC(tree)):
    save(targetFolder + title + ".js", asJSON(tree))

class TF(TaskFactory):
  def __init__(self, targetFolder, ai, total):
    self.targetFolder = targetFolder
    self.ai = ai
    self.total = total
  def process(self, tree):
    toFiles(tree, self.targetFolder)
    IJ.showProgress(self.ai.incrementAndGet(), self.total)

def process(trees, targetFolder):
  """ Process, in parallel, all trees. """
  Process.unbound(trees, TF(targetFolder, AtomicInteger(0), len(trees)))

def run():
  projects = Project.getProjects()
  if projects is None or projects.isEmpty():
    IJ.log('No project open!')
    return
  ls = projects.get(0).getRootLayerSet()
  trees = ls.getZDisplayables(Treeline)
  trees.addAll(ls.getZDisplayables(AreaTree))
  if trees.isEmpty():
    IJ.log('No trees to process!')
    return
  dc = DirectoryChooser('Target folder')
  targetFolder = dc.getDirectory()
  if targetFolder is None:
    return # user canceled
  if targetFolder[len(targetFolder) -1] != '/':
    targetFolder += '/'
  fdir = File(targetFolder)
  for f in fdir.listFiles():
    if f.isHidden():
      continue
    yn = YesNoCancelDialog(IJ.getInstance(), "WARNING", "Folder not empty! Continue?")
    if yn.yesPressed():
      break
    else:
      return
  process(trees, targetFolder)

run()
