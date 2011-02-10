from java.awt import Polygon
from java.awt.geom import Area, AffineTransform

def findMergedContour(project):
	# Find the two bezier Profile instances that define the contour
	# and merge them into a single Area.
	lys = project.getRootLayerSet()
	contour = Area()
	for profile in lys.getDisplayables(Profile):
		pt = project.findProjectThing(profile)
		if pt is None:
			print "null pt for profile ", profile
			continue
		if pt.getParent().getParent().getTitle() == "contour":
			contour.add(Area(profile.getPerimeter()))

	# Extract a single Polygon from the Area
	pols = M.getPolygons(contour)
	if len(pols) > 1:
		print "More than one polygon for the contour!"
	return pols[0]

def scaleContour(pol, scale):
	# Scale up the polygon relative to its center
	cx = 0
	cy = 0
	for i in range(pol.npoints):
		cx += pol.xpoints[i]
		cy += pol.ypoints[i]

	cx /= pol.npoints
	cy /= pol.npoints

	aff = AffineTransform(1, 0, 0, 1, -cx, -cy)
	aff.preConcatenate(AffineTransform(scale, 0, 0, scale, 0, 0))
	aff.preConcatenate(AffineTransform(1, 0, 0, 1, cx, cy))

	tmp = Area(pol)
	tmp.transform(aff)
	return M.getPolygons(tmp)[0]


def findLocals(project, pol):
	# Test if any node of every treeline is outside the polygon,
	# to determine if a neuron is local or not.
	lys = project.getRootLayerSet()
	trees = [t for t in lys.getZDisplayables(Treeline)]
	trees = trees + [t for t in lys.getZDisplayables(AreaTree)]

	tags = {}

	for tree in trees:
		# Make the polygon local to the tree coordinates
		a = Area(pol)
		a.transform(tree.getAffineTransform().createInverse())
		localPol = M.getPolygons(a)[0]
		isLocal = False
		for node in tree.getRoot().getSubtreeNodes():
			if localPol.contains(node.x, node.y):
				continue
			isLocal = True
			break
		tags[tree.getId()] = isLocal

	return tags

def asConvexHull(pol):
	return PolygonRoi(pol, PolygonRoi.POLYGON).getConvexHull()

def setRoi(pol):
	Display.getFront().getCanvas().getFakeImagePlus().setRoi(PolygonRoi(pol, PolygonRoi.POLYGON))

def run():
	project = Project.getProjects()[0]
	if project is None:
		print "No TrakEM2 project open!"
		return
	pol = findMergedContour(project)
	spol = scaleContour(pol, 1.15)
	hullPol = asConvexHull(spol)
	setRoi(hullPol)
	
	print "pol:       ", pol.getBounds()
	print "scaled pol:", spol.getBounds()
	print "hull:      ", hullPol.getBounds()

	tags = findLocals(project, hullPol)

	for key in sorted(tags.keys()):
		print str(key) + "\t" + str(tags[key])

run()