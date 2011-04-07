# To show the envelope of a single lineage, do:
#   regex = "DPLm2"   # the name of the lineage

# To create envelopes for all lineages, do:
#   regex = None
# ... and then don't show the plots (all will be together):
#   show_3D = False

# ASSUMES pipes have the same exact name across all TrakEM2 projects.
# The 'name' is the title of the pipe node in the Project Tree
# and any of its parent nodes, chained. So if you want all DPM lineages
# and these are grouped in a DPM lineage group, then use "DPM".


from ini.trakem2 import Project
from java.awt import Color
from java.io import File

projects = [p for p in Project.getProjects()]
# Add more colors if you have more than 6 projects open
colors = [Color.white, Color.yellow, Color.magenta, Color.green, Color.blue, Color.orange]
sources_color_table = {}
for project, color in zip(projects, colors):
  sources_color_table[project] = color


# 1. The project to use as reference--others are compared to it.
reference_project = projects[0]
# 2. The regular expression to match. Only pipes whose name matches it
# will be analyzed. If null, all are matched.
regex = None    # For a single one, put its name:  "DPMm2"
# 3. A list of text strings containing regular expressions.
# Any pipe whose name matches any will be ignored.
ignore = ["unknown.*", "poorly.*", "MB.*", "peduncle.*", "TR.*"]
# 4. Whether to show the dialog to adjust parameters manually
show_dialog = True
# 5. Whether to generate the variability plots
generate_plots = True
# 6. Whether to show the generated plots in windows
show_plots = False   # They will be stored in files.
# 7. The directory in which to save the plot .png files.
plots_dir = System.getProperty("user.home") + "/Desktop/variability-plots/"
# 8. Whether to show the 3D window
show_3D = True
# 9. Whether to show the average pipe in 3D
show_condensed_3D = True
# 10. Wether to show the original pipes in 3D
show_sources_3D = True
# 11. The table of Project instance vs color to use for that project
# sources_color_table # Already defined above
# 12. Whether to show the enclosing envelope
show_envelope_3D = True
# 13. The transparency of the envelope
envelope_alpha = 0.4
# 14. The resampling for the envelope
envelope_delta = 1
# 15. The type of envelope:
#     1 = 2 std Dev
#     2 = 3 std Dev
#     3 = average distance
#     4 = maximum distance
envelope_type = 3
# 16. Whether to show the axes in 3D
show_axes_3D = True
# 17. Whether to use a heat map for the envelope
use_heatmap = False
# 18. Store the condensed pipes in this table if not null
condensed = None
# 19. The list of projects to consider
# projects # Already defined above



# Ensure plots directory exists
if plots_dir:
  f = File(plots_dir)
  if not f.exists():
    print "Created plots directory:", f.mkdirs()

Compare.variabilityAnalysis(reference_project,
                            regex,
                            ignore,
                            show_dialog,
                            generate_plots,
                            show_plots,
                            plots_dir,
                            show_3D,
                            show_condensed_3D,
                            show_sources_3D,
                            sources_color_table,
                            show_envelope_3D,
                            envelope_alpha,
                            envelope_delta,
                            envelope_type,
                            show_axes_3D,
                            use_heatmap,
                            condensed,
                            projects)

