# Albert Cardona for Nuno da Costa 2011-10-27
# Draw a ROI on a canvas, choose the layer range,
# then invoke the Simple Neurite Tracer on the virtual stack.
# (TrakEM2 does all the plumbing.)

import tempfile
import os
import re
import shutil
from tracing import Simple_Neurite_Tracer, SNTListener

# Get the open display
front = Display.getFront()
tabbedPane = front.getTabbedPane()

import sys
from java.awt.event import ActionListener
from javax.swing import JPanel, JButton

class Launcher(Runnable,SNTListener):

  def __init__(self, imp, roi, layers):
    self.imp = imp
    self.roi = roi
    self.layers = layers

  def run(self):
    self.plugin = Simple_Neurite_Tracer()
    self.plugin.addListener(self)
    self.plugin.run("")

  def onEvent(self, event):
    global front
    d = tempfile.mkdtemp()
    pafm = self.plugin.getPathAndFillManager()
    output_prefix = os.path.join(d,'snt-export')
    if not pafm.exportAllAsSWC(output_prefix):
      IJ.error('Exporting SNT paths as SWC files to "%s" failed' % (output_prefix,))
      return
    tlines = []
    for e in os.listdir(d):
      filename = os.path.join(d, e)
      tl = Treeline(front.project, filename)
      cal = self.imp.getCalibration().copy()
      lset = front.getLayerSet()
      nodes = {}
      offset = self.roi.getBounds()
      fp = open(filename)
      for line in fp:
        line = re.sub('\s*#.*$', '', line)
        line = line.strip()
        if not line:
          continue
        point_id, point_type, x, y, z, radius, parent_id = re.split('\s+', line)
        node = tl.newNode(offset.x + float(x) / cal.pixelWidth,
                          offset.y + float(y) / cal.pixelHeight,
                          self.layers[int(float(z) / cal.pixelDepth + 0.5)],
                          None)
        nodes[int(point_id)] = node
        pid = int(parent_id)
        parent = nodes.get(pid, None)
        if parent:
          parent.add(node, 5)
        elif -1 == pid:
          tl.setRoot(node)
      tl.calculateBoundingBox(None)
      tlines.append(tl)
    lset.addAll(tlines)
    front.project.getProjectTree().insertSegmentations(tlines)
    shutil.rmtree(d)

def initSNTInterfacing(roi, firstLayerIndex, lastLayerIndex):
  IJ.log(str(roi) + " " + str(firstLayerIndex) + " " + str(lastLayerIndex))
  layers = front.getLayerSet().getLayers(firstLayerIndex, lastLayerIndex)
  stack = LayerStack(layers, roi.getBounds(), 1.0, ImagePlus.GRAY8, Patch, -1, False)
  imp = ImagePlus("TrakEM2 substack", stack)
  imp.setCalibration(front.getLayerSet().getCalibrationCopy())
  imp.show()
  t = Thread(Launcher(imp, roi, layers))
  t.setPriority(Thread.NORM_PRIORITY)
  t.start()


class Listener(ActionListener):
  def __init__(self, c1, c2):
    self.c1 = c1
    self.c2 = c2
  def actionPerformed(self, event):
    global front, initSNTInterfacing
    IJ.log("Started SNT control")
    roi = front.getCanvas().getFakeImagePlus().getRoi()
    IJ.log("Roi is " + str(roi))
    if roi is None:
      IJ.showMessage("You must draw a rectangular ROI!")
      return
    try:
      initSNTInterfacing(roi, self.c1.getSelectedIndex(), self.c2.getSelectedIndex())
    except:
      IJ.log(str(sys.exc_info()))

def runSNT(ev):
  IJ.log("hello")

def removeSNTPanel(ev):
  for i in xrange(tabbedPane.getTabCount()):
    if tabbedPane.getTitleAt(i) == "SNT":
      tabbedPane.remove(i)
      break

def createSNTPanel():
  # Create out control panel
  panel = JPanel()
  #button = JButton("Run SNT", actionPerformed=runSNT)
  button = JButton("Run SNT")
  panel.add(button)
  panel.add(JButton("Remove panel", actionPerformed=removeSNTPanel))
  op = OptionPanel()
  layers = []
  cal = front.getLayerSet().getCalibrationCopy()
  for i, layer in enumerate(front.getLayerSet().getLayers()):
    layers.append(str(i+1) + ": " + str(IJ.d2s(layer.getZ() * cal.pixelWidth, 2)))
  c1 = op.addChoice("First section:", layers, 0, None)
  c2 = op.addChoice("Last section:", layers, len(layers)-1)
  button.addActionListener(Listener(c1, c2))
  panel.add(op)
  
  return panel

def init():
  # Add it to the TrakEM display
  panelTitle = "SNT"
  found = False
  for i in xrange(tabbedPane.getTabCount()):
    if tabbedPane.getTitleAt(i) == panelTitle:
      IJ.log("SNT Tab is already there!")
      tabbedPane.setSelectedIndex(i)
      found = True
      break
  if not found:
    tabbedPane.addTab(panelTitle, createSNTPanel())
    tabbedPane.setSelectedIndex(tabbedPane.getTabCount() -1)


init()
