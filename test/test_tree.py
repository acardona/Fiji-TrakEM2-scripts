# Test the mysteriously disappearing nodes
from ini.trakem2.display import Display, Tree, Layer, LayerSet
from java.lang.reflect import Method, Field

def run():
  sel = Display.getSelected()
  if sel is None or sel.isEmpty():
    print "Please select a treeline or areatree!"
    return
  t = sel[0]
  if not isinstance(t, Tree):
    print "The active selected object is not a treeline or areatree!"
    return
  # Print data
  layer = Display.getFrontLayer()
  m = Tree.getDeclaredMethod("getNodesToPaint", [Layer])
  m.setAccessible(True)
  ndsp = m.invoke(t, [layer])
  print "Nodes to paint:", len(ndsp)
  print "-->", ndsp
  fcc = LayerSet.getDeclaredField("n_layers_color_cue")
  fcc.setAccessible(True)
  print "color cue value:", fcc.get(layer.getParent())
  layers = layer.getParent().getColorCueLayerRange(layer)
  print "Layers to paint, for color cues:", len(layers)
  print "-->", layers
  f = Tree.getDeclaredField("node_layer_map")
  f.setAccessible(True)
  ndmap = f.get(t)
  nds = ndmap.get(layer)
  print "Nodes in the current layer:", len(nds)
  print "-->", nds
  nds2 = [nd for nd in t.getRoot().getSubtreeNodes() if nd.getLayer() == layer]
  print "from root, there should be:", len(nds2)
  print "node layer map has:", len(ndmap), "entries"
  lm = {}
  for la in ndmap.keySet():
    lm[la] = [layer.getParent().getLayers().contains(la), layer.getParent().indexOf(la)]
  print "layers found:", lm

run()
