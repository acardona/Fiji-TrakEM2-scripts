# Color the branches of a tree by their centrality

from ini.trakem2.display import Display
from ini.trakem2.analysis import Centrality
from java.awt import Color

tree = Display.getSelected()[0]

vsm = tree.asVertices()
bwc = Centrality.branchWise(vsm.values(), 2)

print "Number of nodes:", len(vsm)

def getColor(val, highest):
  red = 1 - val / float(highest)
  return Color(red , red, 1 - red)

highest = 0
for es in bwc:
  highest = max(highest, es.remaining_branch_vertices)

i = 0
for es in bwc:
  vs = []
  for branch in es.branches:
    for v in branch:
      vs.append(v)
  print i, es.remaining_branch_vertices, len(vs)
  i += 1
  color = getColor(es.remaining_branch_vertices, highest)
  for v in vs:
    v.data.setColor(color)

Display.repaint()