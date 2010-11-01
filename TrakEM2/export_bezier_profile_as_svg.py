# Export Profile to SVG in world coordinates
# Opens in a TextWindow

from java.lang import StringBuilder
from ij.gui import GUI
from ij.text import TextWindow

def export(profile):
  pl, p, pr = profile.getBezierArrays()
  pl = profile.transformPoints(pl)
  p = profile.transformPoints(p)
  pr = profile.transformPoints(pr)
  cal = profile.getLayerSet().getCalibrationCopy()
  pw = float(cal.pixelWidth)
  ph = float(cal.pixelHeight)
  svg = StringBuilder()
  svg.append('<path id="').append(profile.id).append('" d="M')
  for i in range(0, len(p[0]) - 1):
    svg.append(' ').append(p[0][i] * pw).append(',').append(p[1][i] * ph)
    svg.append(' C ').append(pr[0][i] * pw).append(',').append(pr[1][i] * ph)
    svg.append(' ').append(pl[0][i+1] * pw).append(',').append(pl[1][i] * ph)
  svg.append(' ').append(p[0][len(p) -1] * pw).append(',').append(p[1][len(p) -1] * ph)
  if profile.isClosed():
    svg.append(' C ').append(pr[0][len(p) -1] * pw).append(',').append(pr[1][len(p) -1] * ph)
    svg.append(' ').append(pl[0][0] * pw).append(',').append(pl[1][0] * ph)
    svg.append(' ').append(p[0][0] * pw).append(',').append(p[1][0] * ph)
    svg.append(' z')
  svg.append('" \>')
  return svg.toString()

def run():
  front = Display.getFront()
  if front is None:
    print "No displays open!"
    return
  sel = front.getSelection().getSelected(Profile)
  if sel.isEmpty():
    print "No profiles selected!"
    return
  svg = export(sel.get(0))
  tw = TextWindow("Profile as SVG", svg, 500, 500)
  GUI.center(tw)

run()
