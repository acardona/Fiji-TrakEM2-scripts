from jarray import array
from ini.trakem2 import Project
from ini.trakem2.display import Treeline
from ini.trakem2.display import Selection
from java.awt import Color
from javax.vecmath import Color3f
from javax.vecmath import Point3f
from java.util.concurrent import Executors
from customnode import CustomTriangleMesh
from java.util.concurrent import Callable
from ini.trakem2.utils import M
from ini.trakem2.display import Coordinate

# Allow the user to center the TrakEM2 display on any point clicked
# on while holding down the control key, following the example here:
#  http://132.187.25.13/ij3d/?category=Documentation&page=devdoc/CustomBehavior

from ij3d.behaviors import InteractiveBehavior
from java.awt.event import MouseEvent

class CustomBehavior(InteractiveBehavior):

  def __init__(self,univ):
    InteractiveBehavior.__init__(self,univ)
    self.universe = univ

  def doProcess(self,e):
    if (not e.isControlDown()) or (e.getID() != MouseEvent.MOUSE_PRESSED):
      self.super__doProcess(e)
      return
    picker = self.universe.getPicker()
    content = picker.getPickedContent(e.getX(),e.getY())
    if not content:
      return
    p = picker.getPickPointGeometry(content, e)
    if not p:
      return
    display = Display.getFront()
    ls = display.getLayerSet()
    cal = ls.getCalibration()
    l = ls.getNearestLayer(p.z/cal.pixelWidth)
    coordinate = Coordinate(p.x/cal.pixelWidth,p.y/cal.pixelHeight,l,None)
    display.centerAt(coordinate)

def createMesh(triangles, color):
  mesh = CustomTriangleMesh(triangles, color, 0)
  pa = mesh.getAppearance().getPolygonAttributes()
  pa.setCullFace(pa.CULL_NONE)
  pa.setBackFaceNormalFlip(True)
  mesh.setColor(color)
  return mesh

class ContentCreator(Callable):
  def __init__(self, d3d, triangles, color, name):
    self.d3d = d3d
    self.triangles = triangles
    self.color = color
    self.name = name
  def call(self):
    mesh = createMesh(self.triangles, self.color)
    c = self.d3d.getUniverse().createContent(mesh, self.name)
    c.setLocked(True)
    return c


def createFatPoint(x, y, z, r):
  ico = M.createIcosahedron(2, r)
  for p in ico:
    p.x += x
    p.y += y
    p.z += z
  return ico

tl = Display.getFront().getSelected()[0]
affine = tl.getAffineTransform()
ls = tl.getLayerSet()
cal = ls.getCalibration()

locs = list()

for node in tl.getRoot().getSubtreeNodes():
	if node.getParent() is not None:
		if node.getConfidence() < 5:
			p = array([node.x, node.y], 'f')
  			affine.transform(p, 0, p, 0, 1)
  			x, y = p
  			x *= cal.pixelWidth
  			y *= cal.pixelHeight
  			z = node.getLayer().getZ() * cal.pixelWidth

			locs.append([x, y, z])
  
print locs

proj = Project.getProjects()[0]
ls = proj.getRootLayerSet()

neuToDisp = tl

d3d = Display3D.get(ls)
exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

allOuts = []
allOuts.append(exe.submit(d3d.createMesh(proj.findProjectThing(neuToDisp), neuToDisp, 1)))

ii = 0
for iffy in locs:
	print iffy[0], iffy[1], iffy[2]
	ii += 1
	allOuts.append(exe.submit(ContentCreator(d3d, createFatPoint(iffy[0],iffy[1],iffy[2],300), Color3f(Color.red), "name" + str(ii))))

d3d.addContent([outs.get() for outs in allOuts])
exe.shutdown()

universe = d3d.getUniverse()
universe.setInteractiveBehavior(CustomBehavior(universe))
