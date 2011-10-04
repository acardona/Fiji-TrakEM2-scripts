# Albert Cardona 2011-10-03 in Zurich
# 
#  Remove a Layer from a TrakEM2 LayerSet
# in such a way that the skeleton nodes are removed for that section
# (as they would anyway if one is to remove the Layer with conventional TrakEM2 library functions)
# but also move the text tags of those nodes to their parent nodes
# and move the connector origins and targets sitting on that section to be centered
# on the parent nodes of the skeleton nodes being removed.
# Move as well any plain DLabel text tags to the previous section,
# and any ball to the previous section.


indexLayerToRemove = 3  # 1-based, i.e. the first layer is 1, not 0.
indexSupportLayer = 2 # 1-based


from jarray import zeros

def transform(fp, nd, aff1, invAff2):
  fp[0] = nd.x
  fp[1] = nd.y
  aff1.transform(fp, 0, fp, 0, 1)
  invAff2.transform(fp, 0, fp, 0, 1)

def transferConnectorNode(c, cNode, skeletons, layerToRemove, supportLayer):
  # Is the origin sitting on top of a skeleton?
  if len(skeletons) > 0:
    fp = zeros(2, 'f')
    # Pick the first skeleton found
    sk = skeletons[0]
    cInvAff = c.getAffineTransform().createInverse()
    # For all nodes of that skeleton found in the layerToRemove:
    for nd in sk.getNodesAt(layerToRemove):
      # Transform node coordinates to the Connector's coordinate system and put them into fp
      transform(fp, nd, sk.getAffineTransform(), cInvAff)
      # Is the point contained in the connector's origin node area?
      if cNode.getArea().contains(fp[0], fp[1]):
        # Then, see if the parent or a child of the node is in the supportLayer
        target = None
        if nd.getParent() and nd.getParent().getLayer() == supportLayer:
          target = nd.getParent()
        elif nd.getChildrenCount() > 0:
          for child in nd.getChildrenNodes():
            if child.getLayer() == supportLayer:
              target = child
              break
        else:
          # Iterate parent chain until finding one in the supportLayer, if any
          parent = nd.getParent()
          while parent and parent.getLayer() != supportLayer:
            parent = parent.getParent()
          if parent:
            target = parent
          else:
            # Iterate children until finding one in the supportLayer
            closest = None
            shortestChain = Integer.MAX_VALUE
            for node in sk.getNodesAt(supportLayer):
              count = 0
              while node and node.getParent() != nd:
                node = node.getParent()
                count += 1
              if node and count < shortestChain:
                closest = node
                shortestChain = count
            if closest:
              target = closest
        if target:
          #  Yes, at least one node is in the supportLayer.
          # Position the connector's origin in the supportLayer, centered at 
          # the coordinates of the target node, to ensure that the connector
          # remains related to the skeleton.
          cNode.setLayer(supportLayer)
          transform(fp, target, sk.getAffineTransform(), cInvAff)
          cNode.setPosition(fp[0], fp[1])
        else:
          print "Raw transfer of node of connector #" + str(c.getId())
          cNode.setLayer(supportLayer)
        break
  else:
    print "Raw transfer of node of connector #" + str(c.getId())
    cNode.setLayer(supportLayer)


def run():
  project = ControlWindow.getActive()
  if project is None:
    print "No TrakEM2 project open!"
    return
  layerset = project.getRootLayerSet()
  layers = layerset.getLayers()
  if layers.size() <= indexLayerToRemove:
    print "Cannot remove layer at", indexLayerToRemove, "when there are only", layers.size(), "layers."
    return
  if layers.size() <= indexSupportLayer:
    print "The index of the support layer is out of range!"
    return
  layerToRemove = layers.get(indexLayerToRemove -1)
  supportLayer = layers.get(indexSupportLayer -1)
  #
  # Transfer skeleton text tags
  skeletons = list(layerset.getZDisplayables(Treeline)) + list(layerset.getZDisplayables(AreaTree))
  for sk in skeletons:
    for nd in sk.getNodesAt(layerToRemove):
      tags = nd.getTags()
      if tags:
        # find a parent or a child that sits on the support layer
        parent = nd.getParent()
        target = None
        if parent and parent.getLayer() == supportLayer:
          target = parent
        elif nd.getChildrenCount() > 0:
          for child in nd.getChildrenNodes():
            if child.getLayer() == supportLayer:
              target = child
              break
        if target:
          for tag in tags:
            target.addTag(tag)
        else:
          print "Transferring tags: could not find target for node", nd, "of skeleton", sk
  # Transfer connector nodes
  for c in layerset.getZDisplayables(Connector):
    # 1. See if the origin has to be transferred
    cRoot = c.getRoot()
    if cRoot is None:
      continue # empty Connector
    if cRoot.getLayer() == layerToRemove:
			origins = list(c.getOrigins(Treeline)) + list(c.getOrigins(AreaTree))
      transferConnectorNode(c, cRoot, origins, layerToRemove, supportLayer)
    # 2. See if the targets of the connector have to be transferred
    for nd in cRoot.getChildrenNodes():
      if nd.getLayer() == layerToRemove:
        a = nd.getArea() # a copy
        a.transform(c.getAffineTransform()) # to world coordinates
        targets = filter(lambda t: type(t) != Connector,
                         layerset.find(Tree, layerToRemove, a, False, True))
        transferConnectorNode(c, nd, targets, layerToRemove, supportLayer)
    # Update the affine of the Connector
    c.calculateBoundingBox(None)
    # Update cache of the Connector after altering the Layer pointers, very important!
    c.updateCache()
  # Transfer Ball points
  for b in layerset.getZDisplayables(Ball):
    coords = None
    for i,layerID in enumerate(b.getLayerIds()):
      if layerID == layerToRemove.getId():
        if coords is None:
          coords = b.getBalls()
        b.set(i, coords[i][0], coords[i][1], supportLayer, coords[i][3])
  # Transfer floating text labels
  labels = layerToRemove.getDisplayables(DLabel)
  layerToRemove.removeAll(set(labels))
  for label in labels:
    supportLayer.add(label)
  # Remove the layer
  layerToRemove.remove(False)
  # Update the supportLayer
  layerset.recreateBuckets(supportLayer, False)
  Display.update(layerset)

run()
