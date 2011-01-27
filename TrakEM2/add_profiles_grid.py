# This script should create a set of cube-shaped profile lists in a
# grid arrangement.  The values set below are suitable for testing
# with the cat visual cortext TrakEM2 project available from:
#
# http://www.incf.org/about/nodes/switzerland/data
#
# This doesn't work yet - the current problems are:
#
#  - The profiles seem to be invisible.  If I click on one in the
#    Profiles tab of a display, it will appear, but disappears as soon
#    as it is deselected.
# --> generateBezierInterpolates(0.05) was not called, and can't be called: protected
#
#  - The linking doesn't seem to work (the lock icon isn't shown for
#    any of the created profiles)
# --> linking means: linked to one or more Patch instances.
#      getLinkedGroup(None) will report a large size after calling profile.link(profiles[i-1]) many times.
#      The icon will not change when linking profiles to other profiles.
#      Despite all that, linking is needed for adjacent Profiles.
#      To be fair, Profile should not exist. It should be a ProfileList extends ZDisplayable,
#      with an interal list of profiles. The links merely reflect the order in that list,
#      creating a de-factor doubly-linked list.
#
#  - ConcurrentModification exceptions - I'm not sure how to
#    synchronize access to the TrakEM2 classes from Jython.
# --> this has to do with the repaints of the UI in JTree. It's a harmless error that has
#     been around ever since DNDTree was created.

from java.awt.geom import Point2D
from jarray import array
from java.awt import Color

grid_start_x = 1400
grid_start_y = 500
grid_start_z = 28

grid_max_x = 5000
grid_max_y = 3800
grid_max_z = 300

grid_separation = 300
cube_side = 100

def add_profile_list_cube( project, grid_pt, profile_list_tt, x, y, z, side ):

    print "======== Adding a cube at", x,y,z
    ls = project.getRootLayerSet()
    cal = ls.getCalibrationCopy()
    pw = float(cal.pixelWidth)
    ph = float(cal.pixelHeight)

    minimum_z = z - side / 2.0
    maximum_z = z + side / 2.0

    # "half side unscaled":
    hsu = cube_side / (2 * pw)

    minimum_layer = ls.getNearestLayer(minimum_z)
    maximum_layer = ls.getNearestLayer(maximum_z)

    # Contruct a bezier curve which is actually a square:
    square_corners = ( (-1,1), (1,1), (1,-1), (-1,-1) )
    corners = map( lambda t: (t[0]*hsu,t[1]*hsu), square_corners )

    plx = []
    ply = []
    px = []
    py = []
    prx = []
    pry = []

    for i,c in enumerate(corners):
        next_point = corners[(i+1)%4]
        px.append(c[0])
        py.append(c[1])
        plx.append(c[0])
        ply.append(c[1])
        prx.append(c[0])
        pry.append(c[1])

    profiles = []
    D2 = Class.forName("[D")  # a double[]

    layers = ls.getLayers(minimum_layer,maximum_layer)
    print "Got layers:", layers
    for l in layers:
        print "Adding to layer:", l
        profile = Profile( project, "square profile", x/pw, y/ph)
        profile.setPoints(array([plx, ply], D2), array([px, py], D2), array([prx, pry], D2), False)
        profile.setAlpha(1.0) # alpha of 1.0 means full opacity
        profile.toggleClosed() # defaults to open
        profiles.append(profile)
        l.add(profile)

    # Each Profile has to be linked to an adjacent one:
    for i, profile in enumerate(profiles):
        print "Considering profile", profile, "at index", i
        if i > 0:
            profile.link(profiles[i-1])

    # Now create a new profile_list:
    profile_list = ProjectThing(profile_list_tt, project, profile_list_tt.getType())
    grid_pt.addChild(profile_list)

    profile_tt = project.getTemplateThing("profile")
    for profile in profiles:
        profile_pt = ProjectThing(profile_tt,project,profile)
        profile_list.addChild(profile_pt)

def run():

    # Find the open project - there should be only one:
    ps = Project.getProjects()
    if len(ps) != 1:
        raise Exception, "You must have exactly one project open, in fact there were "+str(len(ps))
    project = ps[0]

    # A sanity check that the grid type is in the template:
    unique_types = project.getUniqueTypes()
    if "grid" not in unique_types:
        raise Exception, "There must be a 'grid' type in the template (left pane)"
    grid_tt = project.getTemplateThing("grid")
    if not grid_tt.canHaveAsChild(project.getTemplateThing("profile_list")):
        raise Exception, "The 'grid' must have a 'profile list' as child in the template (left pane)"

    # Find the grid in the project hierarchy - the user should
    # add it manually before running this script:
    root_pt = project.getRootProjectThing();
    grids_pt = root_pt.findChildrenOfTypeR("grid")
    if len(grids_pt) == 0:
        raise Exception, "There must be a 'grid' in the project (middle pane)"
    elif len(grids_pt) > 1:
        raise Exception, "There was more than one 'grid' in the project (middle pane)"
    grid_pt = grids_pt.iterator().next()

    # Find the profile_list from the template:
    profile_list_tt = project.getTemplateThing("profile_list")
    if not profile_list_tt:
        raise Exception, "Failed to find 'profile_list' in the template (left pane)"

    just_add_one = False

    if just_add_one:
        add_profile_list_cube( project, grid_pt, profile_list_tt, grid_start_x, grid_start_y, grid_start_z, cube_side )
    else:
        z = grid_start_z
        while z <= grid_max_z:
            y = grid_start_y
            while y <= grid_max_y:
                x = grid_start_x
                while x <= grid_max_x:
                    add_profile_list_cube( project, grid_pt, profile_list_tt, x, y, z, cube_side )
                    x += grid_separation
                y += grid_separation
            z += grid_separation

    project.getProjectTree().rebuild()

try:
    run()
except Exception, e:
    IJ.error(str(e))
