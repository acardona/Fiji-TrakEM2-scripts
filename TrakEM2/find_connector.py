def findConnector(src, tgt):
  allcons = Display.getFront().getLayerSet().getZDisplayables(Connector)
  srccons = []
  for con in allcons:
    if src in con.getOrigins():
      srccons.append(con)
  if 0 == len(srccons):
    print "No connectors found for src", src
    return
  shared = []
  for con in srccons:
    for targetlist in con.getTargets():
      if tgt in targetlist:
        shared.append(con)
  if 0 == len(shared):
    print "None found!"
    return
  return shared


tl71076 = Display.getFront().getLayerSet().findById(71076)
tl71390 = Display.getFront().getLayerSet().findById(71390)
con1 = findConnector(tl71076, tl71390)

print con1
#[connector #71079]

tl71499 = Display.getFront().getLayerSet().findById(71499)
con2 = findConnector(tl71390, tl71499)

print con2
#[connector #71408, connector #71460]

sel = Display.getFront().getSelection()
sel.clear()

for c in con1: sel.add(c)
for c in con2: sel.add(c)
sel.add(tl71076)
sel.add(tl71390)
sel.add(tl71499)
