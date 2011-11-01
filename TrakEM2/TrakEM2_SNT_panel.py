# Albert Cardona for Nuno da Costa 2011-10-27
# Draw a ROI on a canvas, choose the layer range,
# then invoke the Simple Neurite Tracer on the virtual stack.
# (TrakEM2 does all the plumbing.)
# 
# MISSING: recovering the data from the SNT and adding it
# back to TrakEM2 as a AreaTree or Treeline.


# Get the open display
front = Display.getFront()
tabbedPane = front.getTabbedPane()

import sys
from java.awt.event import ActionListener
from javax.swing import JPanel, JButton

class Launcher(Runnable):
  def __init__(self, imp):
    self.imp = imp
  def run(self):
    IJ.run(self.imp, "Simple Neurite Tracer", "")

def initSNTInterfacing(roi, firstLayerIndex, lastLayerIndex):
  IJ.log(str(roi) + " " + str(firstLayerIndex) + " " + str(lastLayerIndex))
  layers = front.getLayerSet().getLayers(firstLayerIndex, lastLayerIndex)
  stack = LayerStack(layers, roi.getBounds(), 1.0, ImagePlus.GRAY8, Patch, -1, False)
  imp = ImagePlus("TrakEM2 substack", stack)
  imp.setCalibration(front.getLayerSet().getCalibrationCopy())
  imp.show()
  t = Thread(Launcher(imp))
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