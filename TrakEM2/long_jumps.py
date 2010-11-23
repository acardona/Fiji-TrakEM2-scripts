from ini.trakem2.display import Display, Treeline
from java.lang import Math
from jarray import array

def longJumps(t, mindist):
  for nd in t.getRoot().getSubtreeNodes():
    if nd.parent is None:
      continue
    d = Math.sqrt(Math.pow(nd.x - nd.parent.x, 2) + Math.pow(nd.y - nd.parent.y, 2))
    if d > mindist:
      print nd.x, nd.y
      p = array([nd.x, nd.y], 'f')
      aff = t.affineTransform
      aff.transform(p, 0, p, 0, 1)
      cal = t.layerSet.getCalibration()
      print "Off:", p[0] * cal.pixelWidth, p[1] * cal.pixelHeight, (nd.layer.getParent().indexOf(nd.layer) + 1)

for t in Display.getFront().getLayerSet().getZDisplayables(Treeline):
  print t
  longJumps(t, 500)