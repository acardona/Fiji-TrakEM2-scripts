from ini.trakem2.display import AreaTree, Display
from ini.trakem2.utils import M
from jarray import array
from java.awt.geom import Area


def report(nd, pols, aff):
  f = array([nd.x, nd.y], 'f')
  aff.transform(f, 0, f, 0, 1)
  print "Found split area node at: x=", f[0], ", y=", f[1], " at layer:", nd.getLayer().getParent().indexOf(nd.getLayer())
  print " ... with bits at:"
  for pol in pols:
    bounds = aff.createTransformedShape(pol).getBounds()
    print "    x=", bounds.x, ", y=", bounds.y

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
  for nd in tree.getRoot().getSubtreeNodes():
    area = nd.getData()
    if area is None:
      continue
    pols = M.getPolygons(area)
    if len(pols) > 1:
      report(nd, pols, tree.getAffineTransform())
      removeAllButLargest(area, pols)
  Display.repaint()


run()