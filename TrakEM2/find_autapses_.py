# Print autapses
for con in Display.getFront().getLayerSet().getZDisplayables(Connector):
  for o in con.getOrigins(Treeline):
    for tl in con.getTargets(Treeline):
      for t in tl:
        if o == t: IJ.log("Found autapse: " + str(t.getId()))
