# Albert Cardona 20100119
# Export in XML a list of all edges between Treeline elements in a TrakEM2 project
#
# <graph>
#  <edge id="[the connector id]" origin="[the origin Treeline id]" target="[the target Treeline id]" />
#  ...
# </graph>
# 
# The XML opens in a TextWindow
# 
# While at it, will generate a .sif file for importing the networt into cytoscape
# as well as a list of id vs name.


from ini.trakem2.display import Display, Connector, Treeline
from java.lang import StringBuilder
from ij.text import TextWindow

layerset = Display.getFront().getLayerSet()

xml = StringBuilder("<graph>\n")
sif = StringBuilder()
names = StringBuilder()
indent = ' '
type = Treeline

alltargets = set()

for con in layerset.getZDisplayables(Connector):
	print "Processing Connector", con
	origins = con.getOrigins(type)
	if origins.isEmpty():
		print "Connector without origins:", con
		continue
	targetlists = con.getTargets(type)
	if targetlists.isEmpty():
		print "Connector without targets:", con
		continue
	for origin in origins:
		for tlist in targetlists:
			for target in tlist:
  				xml.append(indent).append("<edge cid=\"").append(con.id).append("\" origin=\"").append(origin.id).append("\" target=\"").append(target.id).append("\" />\n")
				sif.append(origin.id).append(" pd ").append(target.id).append('\n')
				alltargets.add(target)
			else:
				print "Empty target in Connector", con

for target in alltargets:
	names.append(target.id).append('\t').append(target.project.getShortMeaningfulTitle(target)).append('\n')

xml.append("</graph>\n");

TextWindow("Graph", xml.toString(), 500, 500)
TextWindow("SIF", sif.toString(), 500, 500)
TextWindow("Names", names.toString(), 500, 500);
