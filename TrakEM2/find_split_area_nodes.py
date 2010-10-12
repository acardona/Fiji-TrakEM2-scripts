from ini.trakem2.display import AreaTree, Display
from ini.trakem2.utils import M
from jarray import array
from java.awt.geom import Area

remove_all_but_largest = False

def report(nd, pols, aff, cal):
  f = array([nd.x, nd.y], 'f')
  aff.transform(f, 0, f, 0, 1)
  print "Found split area node at: x=", f[0], ", y=", f[1], " at layer:", nd.getLayer().getParent().indexOf(nd.getLayer())
  print " ... with bits at:"
  for pol in pols:
    bounds = aff.createTransformedShape(pol).getBounds()
    print "  Small area:"
    print "    x=", bounds.x * cal.pixelWidth, ", y=", bounds.y * cal.pixelHeight
    print "    width=", bounds.width * cal.pixelWidth, ", height=", bounds.height * cal.pixelHeight

def removeAllButLargest(area, pols):
  s = {}
  for pol in pols:
    bounds = pol.getBounds()
    s[bounds.width * bounds.height] = pol
  items = s.items()
  items.sort()
  # Remove all but largest one
  for k, v in items[:-1]:
    area.subtract(Area(v))
    print "Removed small area!"
  

def run():
  if 0 == Display.getSelected().size():
    print "Nothing selected!"
    return
  tree = Display.getSelected()[0]
  if not isinstance(tree, AreaTree):
    print "Not an AreaTree!"
    return
  cal = tree.getLayerSet().getCalibration()
  for nd in tree.getRoot().getSubtreeNodes():
    area = nd.getData()
    if area is None:
      continue
    pols = M.getPolygons(area)
    if len(pols) > 1:
      report(nd, pols, tree.getAffineTransform(), cal)
      if remove_all_but_largest:
        removeAllButLargest(area, pols)
    elif len(pols) == 1:
      print "Found a single polygon with bounds: "+str(pols[0].getBounds())
  Display.repaint()

run()
