def getPreAndPost(tl):
  """Returns a list of two elements; the first is the list of pre connectors
  and the second is the list of post connectors."""
  return tl.findConnectors()

def targetVsSynapses(syn):
  """ Return a map of target vs number of synapses with it. """
  m = {}
  for connector in syn[0]: # syn[0] are the pre
    for targets in connector.getTargets(Treeline, False):
       for target in targets:
         if target in m:
           m[target] += 1
         else:
           m[target] = 1
  return m

def sortedMap(m):
  """ Return a list of lists, where the inner lists are the key/value pairs of the given map. """
  return [[k, m[k]] for k in sorted(m.keys())]


def targetTable(m):
  """ Given a map of target keys and number of synapses as value,
  return the map of number of synapses as key and the number
  of targets that have that number as value."""
  table = {}
  for target,count in m.iteritems():
    if count in table:
      table[count] += 1
    else:
      table[count] = 1
  return sortedMap(table)

import re

def targetOntoMotorneurons(targets):
  """ Given a list of target treelines, return a map of motorneuron treelines as key
  and the number of synapses that they receive from all treelines in targets as value. """
  motorneurons = {}
  regex = re.compile(".*motor *neuron.*")
  for target in targets:
    for connector in target.findConnectors()[0]:
      for targetGroup in connector.getTargets(Treeline, False):
        for target in targetGroup:
          title = target.project.getMeaningfulTitle(target)
          if regex.match(title):
            if target in motorneurons:
              motorneurons[target] += 1
            else:
              motorneurons[target] = 1
  return motorneurons

def keys(m):
  return m.keys()

def combinedMotorneuronsTable(motorneurons):
  """ Given a list of maps of key=motorneuron treeline
  and value=number of synapses received, return a map
  of unique motorneurons as keys and total number of synapses received as values. """
  combinedMotorneurons = {}
  for m in motorneurons:
    for k,v in m.iteritems():
      if k in combinedMotorneurons:
        combinedMotorneurons[k] += v
      else:
        combinedMotorneurons[k] = v
  return combinedMotorneurons

def connectorsWithEmptyTargetSlots(connectors):
  empty = []
  for c in connectors:
    for targetGroup in c.getTargets(Treeline, False):
      if 0 == len(targetGroup):
        empty.append(c)
  return empty

def connectorsWithEmptyOriginSlots(connectors):
  empty = []
  for c in connectors:
    if 0 == len(c.getOrigins(Treeline, False)):
      empty.append(c)
  return empty

def first(ls):
  return ls[0]

def second(ls):
  return ls[1]

def compute(IDs, names):
  """ Given a list of Treeline IDs, output a few results on them."""
  ls = Display.getFront().getLayerSet()
  treelines = map(ls.findById, IDs)

  # How many pre and post each treeline has in the volume?
  synapses = map(getPreAndPost, treelines)

  # How many empty downstream?
  emptyDownstreamSlots = map(connectorsWithEmptyTargetSlots, map(first, synapses))
  
  # How many empty upstream?
  emptyUpstreamSlots = map(connectorsWithEmptyOriginSlots, map(second, synapses))
  
  # How many of the postsynaptic partners of each are traced?
  targetMaps = map(targetVsSynapses, synapses)
  targetStats = map(targetTable, targetMaps)
  
  # How many of the targets' targets are motor neurons?
  motorneurons = map(targetOntoMotorneurons, map(keys, targetMaps))
  combinedMotorneurons = combinedMotorneuronsTable(motorneurons)

  # RESULTS for each ID:
  for i,s in enumerate(synapses):
    print names[i], "#" + str(IDs[i])
    print "   presynaptic sites:", len(s[0])
    print "   number of postsynaptic partner slots:", sum([c.getTargetCount() for c in s[0]])
    print "   ... of which are not yet traced:", len(emptyDownstreamSlots[i]), " -- ", emptyDownstreamSlots[i]
    print "   postsynaptic inputs:", len(s[1])
    print "   ... of which are not yet traced:", len(emptyUpstreamSlots[i]), " -- ", emptyUpstreamSlots[i]
    print "   known downstream targets:", targetStats[i]
    print "   targets connect to N motorneurons:", len(motorneurons[i])
    print "   Connections to motorneurons:", sortedMap(motorneurons[i])
  # Combined
  print "Combined motorneurons:", sortedMap(combinedMotorneurons)


# Class IV:
ddaC = 74329
#          ddaC   vdaB   v'ada
classIVnames = ['ddaC', 'vdaB', "v'ada",
                'ddaC', 'vdaB', "v'ada"]
classIVIDs = [74329, 89060, 89078,
              77041, 116226, 116229]
compute(classIVIDs, classIVnames)


# Lateral chordotonals
lchIDs = [71887, 72064, 72175, 72481, 72295,
          75408, 75307, 105203, 74877, 74767]
lchnames = ['5-1', '5-2/4', '5-4/2', '5-5', '5-3',
            '5-1', '5-4/2', '5-5', '5-3', '5-2/4']
#compute(lchIDs, lchnames)
