# Find the set of target of
# both lch5-1 (left and right)
# and show them in 3D, in yellow.

from ini.trakem2 import Project
from ini.trakem2.display import Treeline
from java.awt import Color
from javax.vecmath import Color3f
from java.util.concurrent import Executors

#project = Project.openFSProject("/home/albert/lab/TEM/abd/abd/abd-casey.xml", False)
project = Project.getProjects()[0]
ls = project.getRootLayerSet()

l51 = ls.findById(71887)
#r51 = ls.findById(75408)

targets = set()
for treeline in [l51]:
  for con in treeline.findConnectors()[0]:
    for s in con.getTargets(Treeline):
      for t in s:
	if t.getClass() == Connector:
		continue # Connector is a subclass of Treeline!
        targets.add(t)

print "Found targets:", len(targets)

d3d = Display3D.get(ls)
exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
fus = []
for t in targets:
  t.setColor(Color.yellow) # WARNING changing the color of the Displayable
  fus.append(exe.submit(d3d.createMesh(project.findProjectThing(t), t, 1)))

d3d.addContent([fu.get() for fu in fus])

exe.shutdown()
