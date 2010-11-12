import random

# A simple example showing how to draw histograms using
# JFreeChart from Jython

from org.jfree.data.statistics import HistogramDataset
from org.jfree.data.statistics import HistogramType
from org.jfree.chart.plot import PlotOrientation
from org.jfree.chart import ChartFactory
from org.jfree.chart import ChartUtilities
from java.io import File
from java.awt import Dimension

values = [ random.randint(0,50) for x in xrange(1,100) ]
bins = 20

dataset = HistogramDataset()
dataset.setType( HistogramType.RELATIVE_FREQUENCY )

dataset.addSeries( "Random Stuff", values, bins)

chart = ChartFactory.createHistogram(
	"Example JFreeChart histogram",
	"This is the x axis",
	"This is the y axis",
	dataset,
	PlotOrientation.VERTICAL,
	True,  # showLegend
	True,  # toolTips
	True,) # urls

# Save it as a PNG:
ChartUtilities.saveChartAsPNG(
  File("/tmp/foo.png"),
  chart,
  800,
  600)

from org.jfree.chart import ChartPanel
from javax.swing import JFrame

class SimpleChart(JFrame):
	def __init__(self,chart):
		chartPanel = ChartPanel(chart)
		chartPanel.setPreferredSize(Dimension(800,600))
		chartPanel.setMouseZoomable(True,False)
		self.setContentPane(chartPanel)

s = SimpleChart(chart)
s.pack()
s.setVisible(True)
